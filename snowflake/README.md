# Snowflake Cloud Data Warehouse & Regulatory Analytics

This directory contains the enterprise **Snowflake Cloud Data Warehouse (OLAP)** architecture, Dimensional Star Schema (DDL), Snowpipe streaming pipelines, and regulatory compliance views for the **Transaction Reconciliation Platform**.

---

## 🏛️ Enterprise Data Architecture

In financial institutions, **PostgreSQL** handles high-throughput transactional writes (OLTP), while **Snowflake** stores long-term historical records (7-10 years) for analytical queries and regulatory audits without slowing down the core banking engine:

```text
[ PostgreSQL & Kafka ]
         │
         │ (Snowpipe / Kafka Connect Snowflake Sink)
         ▼
[ Snowflake: RECON_DW ]
         ├── STAGING
         │     └── RAW_KAFKA_EVENTS (Raw JSON payloads via Kafka Connect)
         │
         ├── CORE_DWH (Dimensional Star Schema)
         │     ├── DIM_PRODUCT (Product reference dimension)
         │     ├── FACT_TRANSACTIONS (Clustered by TRADE_DATE, PRODUCT_ID)
         │     ├── FACT_POSITION_SNAPSHOTS (Daily holding records)
         │     └── FACT_RECON_BALANCE_RUNS (Primary balance ledger)
         │
         └── ANALYTICS (Regulatory Reporting Mart)
               ├── VW_DAILY_RECON_CERTIFICATE (SEC/FINRA compliance certification)
               ├── VW_MATERIAL_DISCREPANCY_EXCEPTIONS (P1 risk audit exceptions)
               └── VW_LATE_TRADE_STALENESS_METRICS (Operational risk analytics)
```

---

## 📂 Included Scripts

| Script | Purpose |
| :--- | :--- |
| **`01_database_and_warehouse_setup.sql`** | Provisions multi-cluster virtual warehouse `RECON_WH`, database `RECON_DW`, roles, and 3-tier schemas (`STAGING`, `CORE_DWH`, `ANALYTICS`). |
| **`02_dimensional_model_ddl.sql`** | Defines the dimensional star schema with automatic clustering keys for fast multi-million-row partition pruning. |
| **`03_snowpipe_and_stream_ingestion.sql`** | Zero-ops streaming ingestion using Snowflake Streams (CDC), Snowpipe, and scheduled transformation Tasks. |
| **`04_regulatory_and_audit_queries.sql`** | Out-of-the-box analytical views used by risk officers and external auditors to prove 100% balance integrity. |

---

## 🚀 Execution Guide

1. Log into your **Snowflake Web Console (Snowsight)**.
2. Open a new **SQL Worksheet**.
3. Run the scripts in numerical order:
   ```sql
   -- 1. Setup Database & Compute
   !source 01_database_and_warehouse_setup.sql;
   
   -- 2. Create Star Schema Tables
   !source 02_dimensional_model_ddl.sql;
   
   -- 3. Configure Ingestion Pipeline
   !source 03_snowpipe_and_stream_ingestion.sql;
   
   -- 4. Deploy Audit Views
   !source 04_regulatory_and_audit_queries.sql;
   ```
