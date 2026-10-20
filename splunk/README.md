# Splunk Observability & Alerting Architecture

This directory contains the enterprise observability, structured logging, and real-time alerting setup for the **Transaction Reconciliation System**.

---

## 🏛️ How Splunk Works with Our Microservices

In banking operations, plain text logs are replaced by **structured JSON logs** enriched with distributed tracing tokens:

```text
[ Microservice ] 
       │ (JSON via Logstash Logback Encoder)
       ▼
 [ stdout / stderr ]
       │
       ▼
 [ Splunk Universal Forwarder / HEC ]
       │
       ▼
 [ Splunk Index: recon_logs ]
       │
       ├─────────────────────────────────┬─────────────────────────────────┐
       ▼                                 ▼                                 ▼
[ Real-Time Alert ]             [ End-to-End Tracing ]            [ Executive Dashboard ]
DiscrepancyQty > 0               traceId="c8a14b0b..."             Matched vs Stale Runs
(PagerDuty / Slack)             (Full trade lifecycle)             (Hourly SLA metrics)
```

---

## 📋 JSON Log Event Schema

When running with the `splunk` profile (`--spring.profiles.active=splunk`), our microservices emit single-line JSON log events matching this schema:

```json
{
  "@timestamp": "2026-10-21T00:15:32.418Z",
  "service": "reconciliation-service",
  "level": "WARN",
  "logger_name": "com.recon.reconciliation.engine.ReconciliationEngine",
  "message": "DISCREPANCY DETECTED for product EQUITY-AAPL: txnTotal=1500, posTotal=1470, discrepancy=30",
  "traceId": "c8a14b0b-7892-4f1a-b024-9182374619a0",
  "runId": "RUN-20261021-001",
  "productId": "EQUITY-AAPL",
  "discrepancyQty": 30,
  "status": "IMBALANCED",
  "environment": "production"
}
```

---

## 🚨 Configured Splunk Alerts (SPL)

See [`splunk_queries.spl`](./splunk_queries.spl) for the full suite of production queries.

### 1. P1 Critical Alert: Reconciliation Discrepancy
```spl
index=recon_logs service="reconciliation-service" level="WARN" "DISCREPANCY"
| stats count by runId, productId, discrepancyQty, status
| search discrepancyQty > 0
| eval alert_level="CRITICAL", message="Reconciliation discrepancy detected! Immediate investigation required."
```
* **Trigger Condition:** Number of events $> 0$ in a 5-minute rolling window.
* **Notification Action:** Sends high-priority alert to Operations PagerDuty & Slack channel `#recon-alerts`.

### 2. End-to-End Trade Tracing
```spl
index=recon_logs traceId="c8a14b0b-7892-4f1a-b024-9182374619a0"
| sort @timestamp
| table @timestamp, service, level, traceId, message
```

---

## 🚀 Running Splunk Locally (Optional)

To spin up a local Splunk Enterprise instance:
```bash
docker compose -f splunk/docker-compose-splunk.yml up -d
```
* **Splunk Web:** [http://localhost:8000](http://localhost:8000) (User: `admin`, Pass: `SplunkPassword123!`)
* **HEC Endpoint:** `http://localhost:8088/services/collector` (Token: `b89e32a1-0000-4b32-a901-789a42cd90ab`)
