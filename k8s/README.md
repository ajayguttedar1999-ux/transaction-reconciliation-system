# Kubernetes (K8s) Production Deployment Guide

This directory contains the production-grade Kubernetes manifests for deploying the entire **Transaction Reconciliation Platform** to any Kubernetes cluster (Minikube, Docker Desktop K8s, Amazon EKS, Google GKE, or Azure AKS).

---

## 🏛️ Topology in Kubernetes

```text
                                  [ Ingress Gateway: recon-ingress ]
                                                │
         ┌───────────────────┬──────────────────┼──────────────────┬──────────────────┐
         │ /                 │ /api/v1/txns     │ /api/v1/pos      │ /api/v1/recon    │ /api/v1/cache
         ▼                   ▼                  ▼                  ▼                  ▼
[ recon-frontend ]   [ txn-service ]     [ pos-service ]     [ recon-service ]   [ cache-service ]
(Port 80 / NodePort) (Port 8081)         (Port 8082)         (Port 8083)         (Port 8085)
         │                   │                  │                  │                  │
         │                   └─────────┬────────┴──────────────────┴─────────┬────────┘
         │                             │                                     │
         │                             ▼                                     ▼
         │                    [ recon-postgres ]                       [ recon-kafka ]
         │                    (Port 5432)                              (Port 9092)
         │                                                                   │
         │                                                                   ▼
         └─────────────────────────────────────────────────────────────> [ recon-redis ]
                                                                        (Port 6379)
```

---

## 📂 Manifest Inventory

| File | Resource | Description |
| :--- | :--- | :--- |
| `00-namespace.yaml` | `Namespace` | Creates isolated `recon-system` namespace |
| `01-configmap-secrets.yaml` | `ConfigMap`, `Secret` | DB host, Kafka brokers, Redis hosts, and credentials |
| `02-postgres.yaml` | `Deployment`, `Service` | PostgreSQL 16 database |
| `03-kafka.yaml` | `Deployment`, `Service` | Zookeeper & Apache Kafka 7.5 broker |
| `04-redis.yaml` | `Deployment`, `Service` | Redis 7 in-memory cache |
| `05-transaction-service.yaml` | `Deployment`, `Service` | Trade Ingestion API (2 replicas, readiness probe) |
| `06-position-service.yaml` | `Deployment`, `Service` | Position Aggregator (2 replicas, readiness probe) |
| `07-reconciliation-service.yaml`| `Deployment`, `Service` | Reconciliation Engine (1 replica, readiness probe) |
| `08-stale-detection-service.yaml`| `Deployment`, `Service` | Late-trade Detector (1 replica) |
| `09-position-cache-service.yaml`| `Deployment`, `Service` | Quarkus + Redis Cache API (2 replicas, readiness probe) |
| `10-frontend.yaml` | `Deployment`, `Service` | React Dashboard (NodePort `30080`) |
| `11-ingress.yaml` | `Ingress` | Unified reverse proxy routing all HTTP traffic |

---

## 🚀 Quick Deployment Commands

```bash
# 1. Create namespace, configs, and secrets
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-configmap-secrets.yaml

# 2. Deploy infrastructure (DB, Kafka, Redis)
kubectl apply -f 02-postgres.yaml
kubectl apply -f 03-kafka.yaml
kubectl apply -f 04-redis.yaml

# 3. Deploy all microservices and UI
kubectl apply -f 05-transaction-service.yaml
kubectl apply -f 06-position-service.yaml
kubectl apply -f 07-reconciliation-service.yaml
kubectl apply -f 08-stale-detection-service.yaml
kubectl apply -f 09-position-cache-service.yaml
kubectl apply -f 10-frontend.yaml
kubectl apply -f 11-ingress.yaml

# 4. Check pod status
kubectl get pods -n recon-system -w
```
