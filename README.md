# Transaction Reconciliation Demo System

[![CI/CD Pipeline](https://github.com/ajayguttedar1999-ux/transaction-reconciliation-system/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/ajayguttedar1999-ux/transaction-reconciliation-system/actions/workflows/ci-cd.yml)
![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)
![Quarkus](https://img.shields.io/badge/Quarkus-3.15.1-blue.svg)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

An enterprise-grade, event-driven financial transaction processing, reconciliation, and low-latency cache system modeled after modern banking platforms (e.g., Fiserv Vision Next / Helix architecture).

---

## 🏛️ Architecture Overview

The system consists of **5 decoupled microservices** communicating asynchronously via **Apache Kafka**, backed by **PostgreSQL** with **Liquibase** migrations, and cached in **Redis** via a **Quarkus** reactive projection layer:

```text
                                  [ REACT UI (Vite / Port 3000) ]
                                                │
                          HTTP POST /transactions  │  HTTP GET /reconciliations
                                                ▼
                   ┌─────────────────────────────────────────────────────────┐
                   │    SERVICE 1: TRANSACTION SERVICE (Port 8081)           │
                   │    - Ingests trade & card transactions                  │
                   │    - Persists in PostgreSQL: transaction_capture        │
                   │    - Publishes: TransactionBookedEvent                  │
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
  * **Quarkus 3.15.1** (Service 5: Lightweight Reactive Microservice)
* **Build System:** Apache Maven (Multi-Module with 6 modules)
* **Messaging & Event Streaming:** Apache Kafka 7.5 (Confluent), SmallRye Reactive Messaging (Vert.x)
* **In-Memory Caching:** Redis 7 (Alpine), Quarkus Redis Client
* **Database & Persistence:** PostgreSQL 16 (Alpine), Spring Data JPA, Hibernate ORM
* **Database Migrations:** Liquibase (Version-controlled XML changesets)
* **Frontend:** React 18, Vite, TypeScript, Axios, Lucide Icons
* **Testing:** JUnit 5, RestAssured, Mockito, Automated Postman Collection (v2.1)
* **Containerization & Orchestration:** Docker, Docker Compose, **Kubernetes (12 Production Manifests)**
* **Observability & Logging:** **Splunk Enterprise (Structured JSON logs, SPL Alert Queries, Distributed Tracing)**
* **Cloud Data Warehouse:** **Snowflake (Dimensional Star Schema, Snowpipe CDC, Regulatory Views)**

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
├── frontend/                   # React 18 + TypeScript Dashboard (Port 3000)
├── k8s/                        # 12 Production Kubernetes manifests (Deployments, Ingress, ConfigMaps)
├── splunk/                     # Splunk structured JSON logging, HEC, and SPL alert queries
├── snowflake/                  # Cloud Data Warehouse: Star Schema DDL, Snowpipe CDC, & Audit views
├── docker-compose.yml          # PostgreSQL 16, Kafka, Zookeeper, & Redis 7 topology
└── Transaction_Reconciliation_System.postman_collection.json # Automated API test suite
```

---

## 🚀 Quick Start Guide

### 1. Start Infrastructure (PostgreSQL, Kafka, Redis)
```bash
docker compose up -d
```
Verified Running Containers:
* **PostgreSQL 16:** `localhost:5433` (`recon_db` / `recon_user` / `recon_password`)
* **Kafka Broker:** `localhost:9092`
* **Zookeeper:** `localhost:2181`
* **Redis 7:** `localhost:6379`

### 2. Build All Modules
```bash
mvn clean install -DskipTests
```

### 3. Run Backend Services (in IntelliJ IDEA or Terminal)
Run each service:
* **Service 1:** `com.recon.transaction.TransactionServiceApplication` (Port 8081)
* **Service 2:** `com.recon.position.PositionServiceApplication` (Port 8082)
* **Service 3:** `com.recon.reconciliation.ReconciliationServiceApplication` (Port 8083)
* **Service 4:** `com.recon.stale.StaleDetectionServiceApplication` (Port 8084)
* **Service 5 (Quarkus):** In IntelliJ Maven panel $\to$ `Position Cache Service` $\to$ `Plugins` $\to$ `quarkus` $\to$ `quarkus:dev` (Port 8085)

### 4. Run Frontend Dashboard
```bash
cd frontend
npm install
npm run dev
```
Open **`http://localhost:3000`** in your browser.

#### 🖥️ Dashboard Features:
* **Interactive 6-Tab Console**:
  1. ➕ **Book Trade**: Ingests new trades with quick-select presets for Apple, Tesla, and Microsoft.
  2. 📋 **Trade Ledger**: Real-time trade log with formatted currency notional calculations.
  3. ⚖️ **Reconciliation Runs**: Automated Sum(Transactions) vs Position balance comparison with date filters.
  4. ⚠️ **Stale Run Detector**: Instant visibility into historical runs invalidated by late trades.
  5. ⚡ **Redis In-Memory Cache (Quarkus :8085)**: Live ticker query with sub-millisecond ($<1\text{ms}$) latency indicators.
  6. 🛡️ **Resilience Recovery Queue**: Visual DLQ tracking buffered trades when Kafka is offline.
* **Architecture Credits**: Designed & Engineered by **Ajay Guttedar** (Lead Systems Architect).

---

## 🔌 API Reference & Endpoints

### Service 1: Transaction Service (`:8081`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/transactions` | Ingests new financial trade/transaction (with Circuit Breaker & Retry) |
| `GET` | `/api/v1/transactions` | Retrieves all booked transactions |
| `GET` | `/api/v1/transactions/recovery-queue` | Inspects transactions safely buffered during Kafka blips |

### Service 2: Position Service (`:8082`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/positions` | Lists all aggregated positions from DB |
| `GET` | `/api/v1/positions/{productId}` | Retrieves live aggregated holding for a product |

### Service 3: Reconciliation Service (`:8083`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/reconciliations/run?date=YYYY-MM-DD` | Triggers a reconciliation run for a trade date |
| `GET` | `/api/v1/reconciliations/runs` | Lists historical reconciliation audit runs |
| `GET` | `/api/v1/reconciliations/runs/{runId}` | Line-item balanced/imbalanced discrepancy breakdown |

### Service 4: Stale Detection Service (`:8084`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/stale/runs` | Queries reconciliation runs marked invalid/stale by late trades |

### Service 5: Position Cache Service (`:8085` - Quarkus + Redis)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/cache/health` | Quarkus & SmallRye health status |
| `GET` | `/api/v1/cache/positions/{productId}` | Ultra-fast ($<1\text{ms}$) in-memory position query from Redis |

---

## 🧪 Testing with Postman

1. Open Postman Desktop.
2. Click **Import** $\to$ Select `Transaction_Reconciliation_System.postman_collection.json`.
3. Execute the requests sequentially:
   - **Step 1:** Book Transaction (`EQUITY-AAPL`, `BUY`, Qty `100`, Price `180.50`).
   - **Step 2:** View Position Aggregation.
   - **Step 3:** Query Redis Cache on `:8085` to see immediate sub-millisecond sync.
   - **Step 4:** Run Reconciliation (`POST /run?date=YYYY-MM-DD`) $\to$ Status `BALANCED`.
   - **Step 5:** Inject late trade $\to$ Verify run marked `is_stale=true`.

---

## ☸️ Kubernetes (K8s) Deployment

The entire system is cloud-ready and can be deployed to any Kubernetes cluster (Minikube, Docker Desktop K8s, EKS, GKE, or AKS) using the production manifests in the `k8s/` directory.

### 1. Build Docker Images
```bash
# Build backend JARs
mvn clean package -DskipTests

# Build Docker images
docker build -t transaction-service:latest transaction-service/
docker build -t position-service:latest position-service/
docker build -t reconciliation-service:latest reconciliation-service/
docker build -t stale-detection-service:latest stale-detection-service/
docker build -t position-cache-service:latest position-cache-service/
docker build -t recon-frontend:latest frontend/
```

### 2. Deploy to Kubernetes
```bash
# Apply namespace, config, secrets, and infrastructure
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap-secrets.yaml
kubectl apply -f k8s/02-postgres.yaml
kubectl apply -f k8s/03-kafka.yaml
kubectl apply -f k8s/04-redis.yaml

# Deploy microservices & frontend
kubectl apply -f k8s/05-transaction-service.yaml
kubectl apply -f k8s/06-position-service.yaml
kubectl apply -f k8s/07-reconciliation-service.yaml
kubectl apply -f k8s/08-stale-detection-service.yaml
kubectl apply -f k8s/09-position-cache-service.yaml
kubectl apply -f k8s/10-frontend.yaml
kubectl apply -f k8s/11-ingress.yaml
```

### 3. Verify Deployment
```bash
kubectl get pods -n recon-system
kubectl get svc -n recon-system
```

---

## 🛡️ Fault Tolerance & Circuit Breaking (Resilience4j)

To guarantee **zero lost trades** in enterprise environments where Kafka brokers or downstream networks experience temporary outages, `transaction-service` is fortified with **Resilience4j**:

```text
[ Trade Request ]
       │
       ▼
 [ Save to PostgreSQL (ACID) ]
       │
       ▼
 [ Kafka Event Producer ]
       │
       ├── (Success) ───────────────────────────> [ Kafka: transaction-booked-topic ]
       │
       └── (Kafka Down / 50% Failure Rate)
               │
               ▼ (Circuit Breaker TRIPS OPEN)
       [ Fallback Method Activated ]
               │
               ▼
       [ Buffer to Local DLQ: transaction_recovery_queue ]
       (Guarantees 0% trade loss; ready for automatic offline replay)
```

### Configured Policies:
* **Circuit Breaker**: Sliding window of 10 calls, 50% failure rate threshold, 10s wait in OPEN state before testing HALF-OPEN state.
* **Automatic Retry**: 3 attempts with exponential backoff multiplier (500ms $\to$ 1000ms $\to$ 2000ms).
* **Dead-Letter Queue (DLQ)**: Accessible via `GET /api/v1/transactions/recovery-queue`.

---

## 📄 License
This project is licensed under the MIT License.
