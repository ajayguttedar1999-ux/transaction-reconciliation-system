# High-Level Design (HLD) Document
## Enterprise Transaction Reconciliation & Position Engine

**Author:** Ajay  
**System Classification:** Tier-1 Investment Banking & FinTech Platform  
**Target SLAs:** 99.999% Availability &bull; < 1ms Read Latency &bull; 0% Data Loss  
**Version:** 1.0.0  

---

## 1. Executive Summary & Business Objective

Modern capital markets institutions process millions of multi-currency financial transactions across disparate payment rails, exchanges, and settlement networks daily. A single discrepancy between internal ledgers and exchange books can result in multi-million-dollar regulatory fines (FINRA, SEC, Basel III compliance) or severe capital misallocation.

The **Transaction Reconciliation System** provides an event-driven, fault-tolerant platform that:
1. **Ingests financial trades** with zero loss, guaranteed by active circuit breaking and dead-letter queue buffering.
2. **Aggregates running real-time positions** across financial instruments using optimistic concurrency control.
3. **Executes automated 3-way balance reconciliations** between transaction sums and position snapshots.
4. **Detects late-arriving trade anomalies** to retroactively flag and invalidate stale reconciliations.
5. **Caches position holdings in Redis** via a reactive non-blocking projection layer (Quarkus + Vert.x) delivering $<1\text{ms}$ latency.
6. **Archives audit history in Snowflake** for 10-year regulatory retention and compliance analytics.

---

## 2. Requirements & Non-Functional SLAs (Service Level Agreements)

### Functional Requirements
* **FR-1 (Trade Ingestion):** Ingest equity, FX, and fixed-income transactions via REST API with strict idempotency and schema validation.
* **FR-2 (Real-Time Aggregation):** Incrementally aggregate net positions per product as trades occur.
* **FR-3 (Automated Reconciliation):** Compare `Sum(Transactions)` vs `Position.totalQuantity` for any given trade date and evaluate `BALANCED` or `IMBALANCED`.
* **FR-4 (Stale Trade Invalidation):** If a late trade arrives for a closed date, automatically mark prior reconciliation runs as `is_stale=true`.
* **FR-5 (Low-Latency Cache):** Provide ultra-fast read APIs for algorithmic trading desks and risk dashboards.
* **FR-6 (Regulatory Compliance):** Maintain historical audit ledgers in a dimensional data warehouse for SEC inspection.

### Non-Functional SLAs
| Attribute | Production Metric | Architecture Implementation |
| :--- | :--- | :--- |
| **Availability** | 99.999% ("Five Nines") | Multi-replica Kubernetes pods with readiness probes & self-healing |
| **Write Latency** | $< 15\text{ms}$ (p99) | Non-blocking Kafka producer with asynchronous commit |
| **Read Latency** | $< 1\text{ms}$ (p99) | Quarkus Eclipse Vert.x Reactive Event Loop + Redis In-Memory |
| **Fault Tolerance** | 0% Lost Transactions | Resilience4j Circuit Breaker + 3x Exponential Retry + Local DLQ |
| **Audit Retention** | 7–10 Years | Snowflake Cloud Data Warehouse Star Schema (90-day Time Travel) |

---

## 3. High-Level Architecture Topology

```text
                                  +-------------------------------------------------------------+
                                  |                     USERS & EXTERNAL CLIENTS                |
                                  |   [ React 18 Web & Android APK ]    [ Algo Desks / Risk APIs ]    |
                                  +------------------------------+------------------------------+
                                                                 | HTTPS (TLS 1.3)
                                                                 v
                                  +-------------------------------------------------------------+
                                  |                     KUBERNETES INGRESS                      |
                                  |     /api/v1/transactions     |      /api/v1/cache           |
                                  +--------------+---------------+--------------+---------------+
                                                 |                              |
                                                 v                              v
+---------------------------------------------------------------------------------------------------------------+
|                                            KUBERNETES CLUSTER (K8s)                                           |
|                                                                                                               |
|  +------------------------------+                           +------------------------------+                  |
|  |     transaction-service      |                           |    position-cache-service    |                  |
|  |    (Spring Boot 3 / :8081)   |                           |     (Quarkus 3.15 / :8085)   |                  |
|  |    * Liquibase Migrations    |                           |     * Non-blocking Vert.x    |                  |
|  |    * Resilience4j CB / DLQ   |                           |     * < 1ms Read Response    |                  |
|  +--------------+---------------+                           +--------------+---------------+                  |
|                 | (Async Event Stream)                                     ^                                  |
|                 v                                                          |                                  |
|  +-------------------------------------------------------------------------+---------------+                  |
|  |                              APACHE KAFKA EVENT STREAMING CLUSTER (:9092)               |                  |
|  |   [ transaction-booked-topic ]       [ position-created-topic ]                         |                  |
|  |   [ reconciliation-completed-topic ] [ staleness-detected-topic ]                       |                  |
|  +---------------------+-----------------------------------+-------------------------------+                  |
|                        |                                   |                                                  |
|                        v                                   v                                                  |
|  +------------------------------+   +------------------------------+   +------------------------------+       |
|  |       position-service       |   |    reconciliation-service    |   |    stale-detection-service   |       |
|  |    (Spring Boot 3 / :8082)   |   |    (Spring Boot 3 / :8083)   |   |    (Spring Boot 3 / :8084)   |       |
|  |    * Optimistic Lock (@Ver)  |   |    * 3-Way Balance Engine    |   |    * Late-trade Invalidator  |       |
|  +--------------+---------------+   +--------------+---------------+   +--------------+---------------+       |
|                 |                                  |                                  |                       |
|                 +------------------+---------------+----------------------------------+                       |
|                                    |                                                                          |
|                                    v                                                                          |
|                     +------------------------------+         +------------------------------+                 |
|                     |     PostgreSQL 16 (:5433)    |         |        Redis 7 (:6379)       |                 |
|                     |     (ACID Ledger Store)      |         |     (In-Memory Cache Store)  |                 |
|                     +------------------------------+         +------------------------------+                 |
+---------------------------------------------------------------------------------------------------------------+
                                     |                                                  |
                                     | Logstash Single-Line JSON Logs                   | Snowpipe CDC /
                                     v                                                  v S3 Continuous Sync
                      +------------------------------+                   +------------------------------+
                      |      SPLUNK OBSERVABILITY    |                   |     SNOWFLAKE DATA CLOUD     |
                      |   * P1 Discrepancy Alerts    |                   |   * 10-Yr Regulatory Ledger  |
                      |   * TraceId Distributed Span |                   |   * Star Schema (FACT/DIM)   |
                      |   * SLA Health Dashboards    |                   |   * FINRA Audit Cert Views   |
                      +------------------------------+                   +------------------------------+
```

---

## 4. Architectural Patterns Applied

### 4.1 Command Query Responsibility Segregation (CQRS)
* **Write Path (Command):** Handled by `transaction-service` and `position-service` using relational transactional semantics (PostgreSQL ACID) to guarantee 100% financial correctness.
* **Read Path (Query):** Handled by `position-cache-service` running Quarkus Reactive. It bypasses PostgreSQL completely and queries in-memory Redis keys (`pos:{productId}`), eliminating 99% of read database contention.

### 4.2 Event-Driven Architecture (EDA)
* Microservices are fully decoupled.
* `transaction-service` does not know or care about `position-service` or `reconciliation-service`; it simply publishes an immutable `TransactionBookedEvent` to Kafka.
* If `position-service` or `reconciliation-service` restarts, Kafka retains events, allowing consumers to catch up with **zero data loss**.

### 4.3 Database Per Service / Shared Core Schema Pattern
* Core tables are partitioned by bounded contexts:
  * `transaction_capture` (Transaction Domain)
  * `position_capture` (Position Domain)
  * `recon_run` and `recon_balance_result` (Reconciliation Domain)
  * `transaction_recovery_queue` (Resilience DLQ Domain)

---

## 5. Security & Governance

1. **Non-Root Container Execution:** All Docker containers run as unprivileged user `reconuser:recongroup` on Alpine Linux.
2. **Kubernetes Isolation:** All pods, configmaps, and secrets reside within an isolated `recon-system` namespace.
3. **Database Security:** Credentials are decoupled via Kubernetes `Secret` resources and encrypted at rest.
4. **Data Masking & Privacy:** Account numbers and trader details follow PCI-DSS truncation in log streams.

---

## 6. Disaster Recovery & Business Continuity (BCP)

* **Kafka Broker Replication:** Offsets and topics configured with replication factor $\ge 1$ and automatic partition rebalancing.
* **Snowflake 90-Day Time Travel:** Allows auditors to restore any table to the exact second of a historical trading glitch (`AT (TIMESTAMP => ...)`).
* **Circuit Breaker Fallback:** Guaranteed in-memory / local database storage even during global cloud messaging outages.
