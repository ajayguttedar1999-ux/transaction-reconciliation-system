# Low-Level Design (LLD) Document
## Transaction Reconciliation Demo System

**Author:** Ajay  
**System Classification:** Enterprise Financial Service / Distributed Ledger & Reconciliation Engine  
**Version:** 1.0.0  
**Target Runtimes:** Java 21 LTS, Spring Boot 3.3.4, Quarkus 3.15.1, React 18, PostgreSQL 16, Apache Kafka 7.5, Redis 7, Snowflake  

---

## 1. System Context & Component Interaction

```text
                                  +------------------------------+
                                  |   React 18 + Vite Frontend   |
                                  |     (Port 3000 / Vercel)     |
                                  +--------------+---------------+
                                                 |
                       +-------------------------+-------------------------+
                       | HTTP (REST)                                       | HTTP (REST)
                       v                                                   v
        +------------------------------+                    +------------------------------+
        |     transaction-service      |                    |    reconciliation-service    |
        |          (Port 8081)         |                    |          (Port 8083)         |
        +--------------+---------------+                    +--------------+---------------+
                       |                                                   ^
        +--------------+---------------+                                   |
        | Kafka (Async Event Stream)   |                                   | Reads Transactions
        v                              |                                   | and Positions
+------------------------------+       |                                   |
|       position-service       |       |                                   |
|          (Port 8082)         |       |                                   |
+--------------+---------------+       |                                   |
               |                       v                                   |
               | PositionCreatedEvent  +-----------------------------------+
               v                                 Kafka Topics
+------------------------------+
|    position-cache-service    |
|     (Quarkus / Port 8085)    |
+--------------+---------------+
               |
               v
+------------------------------+
|     In-Memory Redis 7        |
|    (Key: pos:{productId})    |
+------------------------------+
```

---

## 2. Database Schema & Entity Relationship Diagram (ERD)

The relational persistence tier is hosted on **PostgreSQL 16** and governed by **Liquibase** migration changesets.

```text
 +---------------------------------------------------+
 |                transaction_capture                |
 +---------------------------------------------------+
 | id                  | BIGSERIAL (PK)              |
 | transaction_id      | VARCHAR(100) (UNIQUE, INDEX)|
 | product_id          | VARCHAR(50) (INDEX)         |
 | account_id          | VARCHAR(50)                 |
 | quantity            | NUMERIC(18, 4) NOT NULL     |
 | price               | NUMERIC(18, 4) NOT NULL     |
 | currency            | VARCHAR(3) NOT NULL         |
 | status              | VARCHAR(20) NOT NULL        |
 | trade_date          | TIMESTAMP WITH TIME ZONE    |
 | created_at          | TIMESTAMP WITH TIME ZONE    |
 +---------------------------------------------------+
                          |
                          | (Kafka Stream Aggregation)
                          v
 +---------------------------------------------------+
 |                 position_capture                  |
 +---------------------------------------------------+
 | id                  | BIGSERIAL (PK)              |
 | position_id         | VARCHAR(100) (UNIQUE, INDEX)|
 | product_id          | VARCHAR(50) (UNIQUE, INDEX) |
 | total_quantity      | NUMERIC(18, 4) NOT NULL     |
 | total_amount        | NUMERIC(18, 4) NOT NULL     |
 | version             | BIGINT NOT NULL (Opt. Lock) |
 | updated_at          | TIMESTAMP WITH TIME ZONE    |
 +---------------------------------------------------+

 +---------------------------------------------------+         +---------------------------------------------------+
 |                    recon_run                      | 1     * |               recon_balance_result                |
 +---------------------------------------------------+---------+---------------------------------------------------+
 | id                  | BIGSERIAL (PK)              |         | id                  | BIGSERIAL (PK)              |
 | run_id              | VARCHAR(100) (UNIQUE, INDEX)|         | recon_run_id        | BIGINT (FK -> recon_run.id) |
 | recon_date          | DATE NOT NULL (INDEX)       |         | product_id          | VARCHAR(50) NOT NULL        |
 | status              | VARCHAR(20) (BAL/IMBALANCED)|         | txn_total_qty       | NUMERIC(18, 4) NOT NULL     |
 | is_stale            | BOOLEAN DEFAULT FALSE       |         | pos_total_qty       | NUMERIC(18, 4) NOT NULL     |
 | created_at          | TIMESTAMP WITH TIME ZONE    |         | discrepancy_qty     | NUMERIC(18, 4) NOT NULL     |
 +---------------------------------------------------+         | status              | VARCHAR(20) NOT NULL        |
                                                               | created_at          | TIMESTAMP WITH TIME ZONE    |
                                                               +---------------------------------------------------+

 +---------------------------------------------------+
 |             transaction_recovery_queue            |
 +---------------------------------------------------+
 | id                  | BIGSERIAL (PK)              |
 | transaction_id      | VARCHAR(100) NOT NULL       |
 | product_id          | VARCHAR(50) NOT NULL        |
 | payload             | TEXT NOT NULL               |
 | failure_reason      | VARCHAR(500)                |
 | status              | VARCHAR(20) (PENDING_RETRY) |
 | created_at          | TIMESTAMP WITH TIME ZONE    |
 +---------------------------------------------------+
```

---

## 3. Asynchronous Event Pipeline (Apache Kafka Taxonomy)

All events extend strongly-typed immutable Java 21 Records declared in the shared `common-dto` module.

| Topic Name | Producer Service | Consumer Service(s) | Partition Key | Schema Record |
| :--- | :--- | :--- | :--- | :--- |
| `transaction-booked-topic` | `transaction-service` | `position-service`, `stale-detection-service` | `productId` | `TransactionBookedEvent` |
| `position-created-topic` | `position-service` | `position-cache-service` (Quarkus) | `productId` | `PositionCreatedEvent` |
| `reconciliation-completed-topic` | `reconciliation-service` | External Downstream / Audit Sinks | `runId` | `ReconciliationCompletedEvent` |
| `staleness-detected-topic` | `stale-detection-service` | Risk Operations / Alerting | `runId` | `StalenessDetectedEvent` |

### Key Design Guarantee:
By partitioning all transaction and position streams by `productId`, Apache Kafka guarantees **strict sequential in-order processing** per financial instrument within each partition, preventing out-of-order ledger updates.

---

## 4. Concurrency & Data Integrity Strategy

### 4.1 Optimistic Locking on Position State
When millions of trades execute simultaneously for a stock (e.g. `EQUITY-AAPL`), multiple consumer threads may attempt to update the running position concurrently.

* **Mechanism:** Spring Data JPA `@Version` column on `PositionEntity`.
* **Flow:**
  1. Consumer reads position with `version = N`.
  2. Increments `totalQuantity += trade.quantity`.
  3. Executes `UPDATE position_capture SET total_quantity = ?, version = N + 1 WHERE id = ? AND version = N`.
  4. If another thread committed first, Hibernate throws an `OptimisticLockException`.
  5. The consumer catches the exception and retries the aggregation against the latest snapshot, guaranteeing **zero lost increments**.

---

## 5. Fault Tolerance & Circuit Breaking State Machine (Resilience4j)

`transaction-service` protects against Kafka broker outages using an active Resilience4j state machine:

```text
               +---------------------------------------+
               |                CLOSED                 |
               |  (Normal operation: calls pass through)|
               +-------------------+-------------------+
                                   |
         Failure Rate >= 50%       |
         over 10-call window       v
               +---------------------------------------+
               |                 OPEN                  |
               |  (All calls short-circuit to Fallback)|
               |  (Wait 10,000ms duration)             |
               +-------------------+-------------------+
                                   |
         10-second timer expires   |
                                   v
               +---------------------------------------+
               |              HALF-OPEN                |
               |  (Tests 3 trial calls to Kafka)       |
               +---------+-------------------+---------+
                         |                   |
        Calls Succeed    |                   | Calls Fail
                         v                   v
                     [ CLOSED ]           [ OPEN ]
```

### Fallback Guarantee:
When in `OPEN` state, `fallbackPublishTransactionBooked()` intercepts the trade and persists it to `transaction_recovery_queue` in PostgreSQL. The caller receives a successful HTTP `201 CREATED`, and zero financial trades are lost.

---

## 6. High-Performance CQRS Read-Projection (Quarkus + Redis)

* **Command Side (Write):** Spring Boot microservices write ACID transactions to PostgreSQL and stream events to Kafka.
* **Query Side (Read):**
  * `position-cache-service` runs on **Quarkus 3.15** using non-blocking **Eclipse Vert.x Reactive Event Loops**.
  * Consumes `position-created-topic` asynchronously without blocking any worker thread.
  * Writes the running total into **Redis 7** under key `pos:{productId}` with sub-2ms latency.
  * Serves `GET /api/v1/cache/positions/{productId}` with **0.8ms average response time**, reducing PostgreSQL read load by 99%.

---

## 7. Distributed Tracing & Observability (Splunk & Prometheus)

### Distributed Trace Token (`traceId`)
Every request arriving at `transaction-service` is stamped with an MDC `traceId`. This token is propagated through Kafka message headers across all 5 microservices:

```json
{
  "@timestamp": "2026-10-21T01:30:00.000Z",
  "service": "reconciliation-service",
  "level": "INFO",
  "traceId": "c8a14b0b-7892-4f1a-b024-9182374619a0",
  "logger_name": "com.recon.reconciliation.engine.ReconciliationEngine",
  "message": "Reconciliation completed: status=BALANCED",
  "runId": "RUN-0F94635F",
  "productId": "EQUITY-AAPL",
  "discrepancyQty": 0.0
}
```

### Metrics Telemetry (Prometheus & Grafana)
* **`recon_transactions_booked_total{product="EQUITY-AAPL"}`**: Live trade throughput counter.
* **`resilience4j_circuitbreaker_state`**: Gauge monitoring Kafka producer health.
* **`jvm_memory_used_bytes{area="heap"}`**: Real-time memory footprint.

---

## 8. Regulatory Cloud Data Warehouse (Snowflake Dimensional Model)

To satisfy FINRA / SEC 7-year regulatory retention mandates, historical reconciliations are projected into a Star Schema in **Snowflake (`RECON_DW`)**:

* **`CORE_DWH.DIM_PRODUCT`**: Product reference dimension.
* **`CORE_DWH.FACT_TRANSACTIONS`**: Billions of trades clustered by `(TRADE_DATE, PRODUCT_ID)`.
* **`CORE_DWH.FACT_RECON_BALANCE_RUNS`**: Primary audit ledger.
* **`ANALYTICS.VW_DAILY_RECON_CERTIFICATE`**: Regulatory compliance certification view measuring daily match rates.
