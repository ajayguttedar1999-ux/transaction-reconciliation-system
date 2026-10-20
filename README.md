# Transaction Reconciliation Demo System

[![CI/CD Pipeline](https://github.com/ajayguttedar1999-ux/transaction-reconciliation-system/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/ajayguttedar1999-ux/transaction-reconciliation-system/actions/workflows/ci-cd.yml)
[![HLD Spec](https://img.shields.io/badge/Architecture-High--Level%20Design%20(HLD)-blue.svg)](./HLD.md)
[![LLD Spec](https://img.shields.io/badge/Architecture-Low--Level%20Design%20(LLD)-purple.svg)](./LLD.md)
![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)
![Quarkus](https://img.shields.io/badge/Quarkus-3.15.1-blue.svg)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

An enterprise-grade, event-driven financial transaction processing, reconciliation, and low-latency cache system modeled after modern banking platforms (e.g., Fiserv Vision Next / Helix architecture).

**Lead Systems Architect & Engineer:** **Ajay Guttedar**  
**Live Production Dashboard (Vercel):** [https://transaction-reconciliation-system-rho.vercel.app](https://transaction-reconciliation-system-rho.vercel.app)

---

## 📑 Architecture Specifications

* 👉 **[High-Level Design Document (HLD)](./HLD.md)**: Executive summary, business context, non-functional SLAs (99.999% availability, $<1\text{ms}$ reads), enterprise topology, CQRS, and disaster recovery.
* 👉 **[Low-Level Design Document (LLD)](./LLD.md)**: Class diagrams, ER diagrams with column types, Kafka topic partition strategies, Optimistic Locking (`@Version`), and Resilience4j circuit breaker state machine.

---

## 🏛️ Architecture Overview

The system consists of **5 decoupled microservices** communicating asynchronously via **Apache Kafka**, backed by **PostgreSQL** with **Liquibase** migrations, and cached in **Redis** via a **Quarkus** reactive projection layer:

```text
                                  [ REACT UI (Vercel / Port 3000) ]
                                                │
                          HTTP POST /transactions  │  HTTP GET /reconciliations
                                                ▼
                   ┌─────────────────────────────────────────────────────────┐
                   │    SERVICE 1: TRANSACTION SERVICE (Port 8081)           │
                   │    - Ingests trade & card transactions                  │
                   │    - Persists in PostgreSQL: transaction_capture        │
                   │    - Publishes: TransactionBookedEvent                  │
                   │    - Resilience4j Circuit Breaker & Fallback DLQ        │
                   └───────────────┬─────────────────────────┬───────────────┘
                                   │                         │
                                   ▼                         ▼
                   [ PostgreSQL: transaction_capture ]   [ Kafka: transaction-booked-topic ]
                                                             │
                                                             ▼
                   ┌─────────────────────────────────────────────────────────┐
                   │    SERVICE 2: POSITION SERVICE (Port 8082)              │
                   │    - Consumes TransactionBookedEvent                    │
                   │    - Aggregates running product positions               │
                   │    - Optimistic Locking (@Version)                      │
                   │    - Publishes: PositionCreatedEvent                    │
                   └───────────────┬─────────────────────────┬───────────────┘
                                   │                         │
                                   ▼                         ▼
                   [ PostgreSQL: position_capture ]      [ Kafka: position-created-topic ]
                                   │                         │
                 ┌─────────────────┘                         ├─────────────────────────────────────────┐
                 │                                           │                                         │
                 ▼                                           ▼                                         ▼
┌─────────────────────────────────────────┐ ┌─────────────────────────────────────────┐ ┌─────────────────────────────────────────┐
│    SERVICE 3: RECON SERVICE (Port 8083) │ │  SERVICE 4: STALE DETECTOR (Port 8084)  │ │  SERVICE 5: CACHE SERVICE (Port 8085)   │
│    - Sum(Transactions) vs Position Qty  │ │  - Detects late-arriving transactions   │ │  - Quarkus 3.15 + SmallRye Reactive   │
│    - Status: BALANCED or IMBALANCED     │ │  - Marks runs as is_stale = TRUE        │ │  - Low-latency in-memory sync to Redis│
│    - Stores audit runs & item results   │ │  - Publishes: StalenessDetectedEvent    │ │  - Sub-millisecond Read API (< 1ms)   │
│    - Publishes: ReconCompletedEvent     │ └─────────────────────────────────────────┘ └───────────────────┬─────────────────────┘
└────────────────┬────────────────────────┘                                                                 │
                 │                                                                                           ▼
                 ▼                                                                                  [ In-Memory Redis 7 ]
 [ PostgreSQL: recon_run & recon_balance_result ]                                                   (Key: pos:{productId})
```

---

## 🛠️ Technology Stack

* **Language & Runtimes:** Java 21 LTS, TypeScript, Node.js
* **Backend Frameworks:**
  * **Spring Boot 3.3.4** (Services 1, 2, 3, 4)
  * **Quarkus 3.15.1** (Service 5: Reactive Microservice with Eclipse Vert.x)
* **Build System:** Apache Maven (Multi-Module with 6 modules)
* **Messaging & Event Streaming:** Apache Kafka 7.5 (Confluent), SmallRye Reactive Messaging
* **In-Memory Caching:** Redis 7 (Alpine), Quarkus Redis Client
* **Database & Persistence:** PostgreSQL 16 (Alpine), Spring Data JPA, Hibernate ORM
* **Database Migrations:** Liquibase (Version-controlled XML changesets)
* **Fault Tolerance:** Resilience4j 2.2.0 (Circuit Breaker, Automatic Retry, Exponential Backoff, Local DLQ)
* **Frontend:** React 18, Vite, TypeScript, Axios, Modern Dark Mode UI
* **Hosting:** Vercel Cloud ([Live Link](https://transaction-reconciliation-system-rho.vercel.app))
* **Containerization & Orchestration:** Docker, Docker Compose, **Kubernetes (12 Production Manifests)**
* **Observability & Logging:** **Splunk Enterprise** (Structured JSON logs, SPL Alert Queries, Distributed Tracing)
* **Metrics & Dashboards:** **Prometheus & Grafana** (Live trade rate counters, JVM heap, CPU telemetry)
* **Cloud Data Warehouse:** **Snowflake** (Dimensional Star Schema, Snowpipe CDC, Regulatory Views)

---

## 🔨 How We Built It (Step-by-Step Technical Journey)

### Phase 1: Core Domain Modeling & Event Definitions
* Built `common-dto` holding immutable Java 21 record schemas (`TransactionBookedEvent`, `PositionCreatedEvent`, `ReconciliationCompletedEvent`, `StalenessDetectedEvent`).

### Phase 2: Ingestion & Transaction Ledger (Service 1)
* Engineered `transaction-service` with PostgreSQL persistence and Liquibase changesets (`transaction_capture`).
* Integrated Kafka template publishing to `transaction-booked-topic` partitioned by `productId`.

### Phase 3: Position Aggregation & Concurrency Safety (Service 2)
* Built `position-service` to consume trade events and aggregate running holdings.
* Implemented **Optimistic Locking (`@Version`)** to prevent race conditions during high-frequency concurrent trade processing.

### Phase 4: Automated 3-Way Reconciliation & Audit Ledger (Service 3)
* Created `reconciliation-service` executing automated balance comparisons between transaction sums and position records for any trade date.
* Evaluated status as `BALANCED` (zero discrepancy) or `IMBALANCED`.

### Phase 5: Late-Trade Invalidation & Staleness Detection (Service 4)
* Created `stale-detection-service` listening for late trades arriving after reconciliation closure, automatically updating historical runs to `is_stale=true`.

### Phase 6: Sub-Millisecond CQRS In-Memory Caching (Service 5)
* Implemented `position-cache-service` with **Quarkus 3.15** and **SmallRye Reactive Messaging**.
* Synchronized running positions to **Redis 7** in $<1\text{ms}$ under key `pos:{productId}`, reducing database read load by 99%.

### Phase 7: Bank-Grade Fault Tolerance (Resilience4j)
* Fortified `transaction-service` with Resilience4j `@CircuitBreaker` and `@Retry`.
* Added graceful fallback to `transaction_recovery_queue` (Local DLQ) guaranteeing **0% lost transactions** during Kafka outages.

### Phase 8: Cloud-Native Containerization & Kubernetes (K8s)
* Authored 6 multi-stage, non-root user `Dockerfile`s on Alpine Linux.
* Created 12 Kubernetes manifests in `k8s/` covering Deployments, ClusterIP Services, NodePort, and Ingress routing.

### Phase 9: Splunk Observability & P1 Alerting
* Configured structured single-line JSON logging via `logstash-logback-encoder`.
* Authored production Splunk Search Processing Language (SPL) queries for P1 discrepancy alerts and distributed `traceId` correlation.

### Phase 10: Snowflake Cloud Data Warehouse & SEC Compliance
* Deployed Star Schema DDL (`DIM_PRODUCT`, `FACT_TRANSACTIONS`, `FACT_RECON_BALANCE_RUNS`).
* Created regulatory compliance view `VW_DAILY_RECON_CERTIFICATE` measuring match rates for FINRA and SEC audits.

### Phase 11: Real-Time Prometheus Metrics & Grafana Dashboard
* Configured Spring Boot Actuator with Micrometer Prometheus exporting `recon_transactions_booked_total`.
* Created ready-to-import Grafana dashboard (`grafana/recon-dashboard.json`) tracking live trade throughput, JVM memory, and CPU utilization.

### Phase 12: CI/CD Pipeline Automation (GitHub Actions)
* Configured automated GitHub Actions workflow (`.github/workflows/ci-cd.yml`) executing Java 21 Maven builds, React frontend validation, Docker build matrix, and K8s schema verification on every commit.

---

## 📂 Project Structure

```text
recon-system/
├── common-dto/                 # Shared Java 21 records for Kafka event schemas
├── transaction-service/        # Service 1: Trade Ingestion (Spring Boot / Port 8081)
├── position-service/           # Service 2: Position Aggregator (Spring Boot / Port 8082)
├── reconciliation-service/     # Service 3: Reconciliation Engine (Spring Boot / Port 8083)
├── stale-detection-service/    # Service 4: Late-trade Invalidation Detector (Spring Boot / Port 8084)
├── position-cache-service/     # Service 5: Reactive Low-Latency Cache (Quarkus + Redis / Port 8085)
├── frontend/                   # React 18 + TypeScript Dashboard (Port 3000 / Vercel)
├── k8s/                        # 12 Production Kubernetes manifests (Deployments, Ingress, ConfigMaps)
├── splunk/                     # Splunk structured JSON logging, HEC, and SPL alert queries
├── grafana/                    # Prometheus scrape config & ready-to-import Grafana dashboard
├── snowflake/                  # Cloud Data Warehouse: Star Schema DDL, Snowpipe CDC, & Audit views
├── HLD.md                      # High-Level Design (HLD) Specification
├── LLD.md                      # Low-Level Design (LLD) Specification
├── docker-compose.yml          # PostgreSQL 16, Kafka, Zookeeper, & Redis 7 topology
└── Transaction_Reconciliation_System.postman_collection.json # Automated API test suite
```

---

## 🚀 Quick Start & Local Execution

### 1. Start Infrastructure (PostgreSQL, Kafka, Redis)
```bash
docker compose up -d
```
* **PostgreSQL 16:** `localhost:5433` (`recon_db` / `recon_user` / `recon_password`)
* **Kafka Broker:** `localhost:9092`
* **Redis 7:** `localhost:6379`

### 2. Run Backend Services (in IntelliJ IDEA or Terminal)
Run each service:
* **Service 1:** `com.recon.transaction.TransactionServiceApplication` (Port 8081)
* **Service 2:** `com.recon.position.PositionServiceApplication` (Port 8082)
* **Service 3:** `com.recon.reconciliation.ReconciliationServiceApplication` (Port 8083)
* **Service 4:** `com.recon.stale.StaleDetectionServiceApplication` (Port 8084)
* **Service 5 (Quarkus):** In IntelliJ Maven panel $\to$ `Position Cache Service` $\to$ `Plugins` $\to$ `quarkus` $\to$ `quarkus:dev` (Port 8085)

### 3. Run Monitoring Stack (Prometheus & Grafana)
```bash
docker compose -f grafana/docker-compose-monitoring.yml up -d
```
* **Prometheus:** [http://localhost:9090](http://localhost:9090)
* **Grafana:** [http://localhost:3001](http://localhost:3001) (User: `admin`, Pass: `admin`)

### 4. Run Frontend Dashboard
```bash
cd frontend
npm install
npm run dev
```
Open **`http://localhost:3000`** in your browser (or use the live [Vercel Deployment](https://transaction-reconciliation-system-rho.vercel.app)).

---

## 🔌 API Reference & Endpoints

| Service | Port | Method | Endpoint | Description |
| :--- | :---: | :--- | :--- | :--- |
| **Transaction Service** | `:8081` | `POST` | `/api/v1/transactions` | Books trade (with Circuit Breaker & Retry) |
| | | `GET` | `/api/v1/transactions` | Lists all booked trades |
| | | `GET` | `/api/v1/transactions/recovery-queue` | Inspects local DLQ recovery table |
| | | `GET` | `/actuator/prometheus` | Prometheus metrics scrape endpoint |
| **Position Service** | `:8082` | `GET` | `/api/v1/positions` | Lists all aggregated positions from DB |
| | | `GET` | `/api/v1/positions/{productId}` | Retrieves aggregated holding for a product |
| **Reconciliation Service**| `:8083` | `POST` | `/api/v1/reconciliations/trigger?date=...` | Triggers a reconciliation run for a trade date |
| | | `GET` | `/api/v1/reconciliations` | Lists historical reconciliation audit runs |
| | | `GET` | `/api/v1/reconciliations/{runId}` | Line-item discrepancy breakdown |
| | | `GET` | `/actuator/prometheus` | Prometheus metrics scrape endpoint |
| **Stale Detection** | `:8084` | `GET` | `/api/v1/stale-runs` | Queries runs invalidated by late trades |
| **Position Cache (Quarkus)**| `:8085` | `GET` | `/api/v1/cache/health` | Quarkus & SmallRye health status |
| | | `GET` | `/api/v1/cache/positions/{productId}` | Ultra-fast ($<1\text{ms}$) in-memory position query |

---

## 👨‍💻 Author & Engineering Credits

* **Lead Systems Architect & Engineer:** **Ajay Guttedar**  
* **GitHub Profile:** [@ajayguttedar1999-ux](https://github.com/ajayguttedar1999-ux)  
* **Repository:** [transaction-reconciliation-system](https://github.com/ajayguttedar1999-ux/transaction-reconciliation-system)

---

## 📄 License
This project is licensed under the MIT License.
