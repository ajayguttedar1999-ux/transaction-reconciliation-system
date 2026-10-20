-- ==============================================================================
-- Snowflake Script 1: Database, Roles, and Virtual Warehouse Setup
-- ==============================================================================

USE ROLE ACCOUNTADMIN;

-- 1. Create Dedicated Functional Role for Reconciliation Operations
CREATE ROLE IF NOT EXISTS RECON_OPERATIONS_ROLE
    COMMENT = 'Role for automated data ingestion and financial reconciliation reporting';

GRANT ROLE RECON_OPERATIONS_ROLE TO ROLE SYSADMIN;

-- 2. Create High-Performance Multi-Cluster Virtual Warehouse
CREATE WAREHOUSE IF NOT EXISTS RECON_WH
    WITH 
    WAREHOUSE_SIZE = 'XSMALL'
    AUTO_SUSPEND = 60          -- Suspend after 60 seconds of inactivity to save compute credits
    AUTO_RESUME = TRUE
    MIN_CLUSTER_COUNT = 1
    MAX_CLUSTER_COUNT = 3      -- Auto-scales up to 3 clusters during heavy month-end reconciliations
    SCALING_POLICY = 'STANDARD'
    COMMENT = 'Dedicated compute warehouse for Transaction Reconciliation System';

-- 3. Create Enterprise Database
CREATE DATABASE IF NOT EXISTS RECON_DW
    DATA_RETENTION_TIME_IN_DAYS = 90  -- Snowflake Time Travel for 90 days of point-in-time recovery
    COMMENT = 'Enterprise Data Warehouse for Transaction Reconciliation Platform';

USE DATABASE RECON_DW;

-- 4. Create Layered Schemas (Staging, Dimensional Core, and Analytics Mart)
CREATE SCHEMA IF NOT EXISTS RECON_DW.STAGING
    COMMENT = 'Raw, append-only landing layer for Kafka events and S3 batch dumps';

CREATE SCHEMA IF NOT EXISTS RECON_DW.CORE_DWH
    COMMENT = 'Cleansed and modeled dimensional star schema (Facts & Dimensions)';

CREATE SCHEMA IF NOT EXISTS RECON_DW.ANALYTICS
    COMMENT = 'Aggregated views, regulatory audit models, and executive BI dashboards';

-- Grant Permissions
GRANT ALL ON WAREHOUSE RECON_WH TO ROLE RECON_OPERATIONS_ROLE;
GRANT ALL ON DATABASE RECON_DW TO ROLE RECON_OPERATIONS_ROLE;
GRANT ALL ON ALL SCHEMAS IN DATABASE RECON_DW TO ROLE RECON_OPERATIONS_ROLE;
