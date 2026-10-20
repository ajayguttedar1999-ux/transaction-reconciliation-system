# Prometheus & Grafana Real-Time Telemetry

This directory contains the production observability configuration for real-time Prometheus scraping and Grafana dashboard visualization.

---

## 🏛️ Observability Topology

```text
[ Spring Boot Actuator (:8081, :8083) ]
       │
       ▼ (/actuator/prometheus)
[ Prometheus Server (:9090) ]
       │
       ▼ (PromQL Live Queries)
[ Grafana Dashboard (:3001) ]
```

---

## 🚀 Running Monitoring Locally

```bash
docker compose -f grafana/docker-compose-monitoring.yml up -d
```
* **Prometheus Targets:** [http://localhost:9090/targets](http://localhost:9090/targets)
* **Grafana Dashboard UI:** [http://localhost:3001](http://localhost:3001) (User: `admin`, Pass: `admin`)

---

## 📈 Importing the Pre-built Dashboard

1. Log into Grafana at [http://localhost:3001](http://localhost:3001).
2. Go to **Connections** &rarr; **Data Sources** &rarr; Add **Prometheus** with URL: `http://recon-prometheus:9090`.
3. Go to **Dashboards** &rarr; Click **New** &rarr; **Import**.
4. Upload [`recon-dashboard.json`](./recon-dashboard.json).
5. Watch live trade throughput, JVM heap memory, and CPU usage update every 5 seconds!
