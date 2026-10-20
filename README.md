# Transaction Reconciliation Demo System

An enterprise-grade, event-driven financial transaction processing and reconciliation system modeled after modern banking platforms (e.g., Fiserv Vision Next / Helix architecture).

---

## 🏛️ Architecture Overview

The system consists of **4 decoupled microservices** communicating asynchronously via **Apache Kafka** and backed by **PostgreSQL** with **Liquibase** migrations:

```
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
                                                             │
                                                             ▼
                   ┌─────────────────────────────────────────────────────────┐
                   │    SERVICE 3: RECONCILIATION SERVICE (Port 8083)        │
                   │    - Aggregates Sum(Transactions) vs Position Quantity  │
                   │    - Evaluates: BALANCED (0 discrepancy) or IMBALANCED  │
                   │    - Stores audit runs & line-item results              │
                   │    - Publishes: ReconciliationCompletedEvent            │
                   └───────────────┬─────────────────────────────────────────┘
                                   │
                                   ▼
                   [ PostgreSQL: recon_run & recon_balance_result ]
                                   │
                   ┌───────────────┴─────────────────────────────────────────┐
                   │    SERVICE 4: STALE DETECTION SERVICE (Port 8084)       │
                   │    - Detects late-arriving transactions                 │
                   │    - Marks existing recon runs as is_stale = TRUE       │
                   │    - Publishes: StalenessDetectedEvent                  │
                   └─────────────────────────────────────────────────────────┘
```

---

## 🛠️ Technology Stack

* **Language & Runtimes:** Java 21 LTS, TypeScript, Node.js
* **Framework:** Spring Boot 3.3.4
* **Build System:** Apache Maven (Multi-Module)
* **Messaging & Event Streaming:** Apache Kafka, Zookeeper
* **Database & Persistence:** PostgreSQL 16, Spring Data JPA, Hibernate ORM
* **Database Migrations:** Liquibase (Version-controlled changesets)
* **Frontend:** React 18, Vite, TypeScript
* **Testing:** JUnit 5, Mockito, Automated Postman Collection (v2.1)
* **Containerization:** Docker & Docker Compose

---

## 📂 Project Structure

```text
├── common-dto/                 # Shared Java records for Kafka event schemas
├── transaction-service/        # Service 1: Trade Ingestion (Port 8081)
├── position-service/           # Service 2: Position Aggregator (Port 8082)
├── reconciliation-service/     # Service 3: Reconciliation Engine (Port 8083)
├── stale-detection-service/    # Service 4: Late-trade Invalidation Detector (Port 8084)
├── frontend/                   # React + TypeScript Dashboard (Port 3000)
├── docker-compose.yml          # PostgreSQL & Kafka container topology
└── Transaction_Reconciliation_System.postman_collection.json # Automated API test suite
```

---

## 🚀 Quick Start Guide

### 1. Start Infrastructure (PostgreSQL & Kafka)
```bash
docker compose up -d
```
* **PostgreSQL:** `localhost:5433` (DB: `recon_db`, User: `recon_user`, Pass: `recon_password`)
* **Kafka Broker:** `localhost:9092`
* **Zookeeper:** `localhost:2181`

### 2. Run Backend Services (in IntelliJ or Terminal)
Run each Spring Boot application:
* `com.recon.transaction.TransactionServiceApplication` (Port 8081)
* `com.recon.position.PositionServiceApplication` (Port 8082)
* `com.recon.reconciliation.ReconciliationServiceApplication` (Port 8083)
* `com.recon.stale.StaleDetectionServiceApplication` (Port 8084)

### 3. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
Open **`http://localhost:3000`** in your browser.

---

## 🧪 Testing with Postman

1. Open Postman Desktop.
2. Click **Import** $\to$ Select `Transaction_Reconciliation_System.postman_collection.json`.
3. Execute the pre-configured sequential requests to simulate booking trades, checking positions, running reconciliations, and detecting stale states.

---

## 📄 License
This project is licensed under the MIT License.
