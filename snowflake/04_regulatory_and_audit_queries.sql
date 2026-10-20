-- ==============================================================================
-- Snowflake Script 4: Regulatory, Compliance, and Audit Analytical Views
-- ==============================================================================

USE DATABASE RECON_DW;
USE SCHEMA ANALYTICS;

-- ------------------------------------------------------------------------------
-- 1. Daily Reconciliation Balance Certificate View (SEC / FINRA Compliance)
-- Shows exact matching rate across all products for any historical day
-- ------------------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_DAILY_RECON_CERTIFICATE AS
SELECT 
    r.RECON_DATE,
    COUNT(DISTINCT r.PRODUCT_ID) AS TOTAL_PRODUCTS_RECONCILED,
    SUM(CASE WHEN r.STATUS = 'BALANCED' THEN 1 ELSE 0 END) AS BALANCED_PRODUCTS,
    SUM(CASE WHEN r.STATUS = 'IMBALANCED' THEN 1 ELSE 0 END) AS DISCREPANT_PRODUCTS,
    ROUND((SUM(CASE WHEN r.STATUS = 'BALANCED' THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) AS COMPLIANCE_MATCH_RATE_PCT,
    SUM(r.DISCREPANCY_QTY) AS TOTAL_NET_DISCREPANCY_UNITS,
    SUM(CASE WHEN r.IS_STALE = TRUE THEN 1 ELSE 0 END) AS STALE_INVALIDATED_RUNS
FROM RECON_DW.CORE_DWH.FACT_RECON_BALANCE_RUNS r
GROUP BY r.RECON_DATE
ORDER BY r.RECON_DATE DESC;

-- ------------------------------------------------------------------------------
-- 2. Material Discrepancy Risk Analysis (Exceptions > $10,000 or > 100 units)
-- ------------------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_MATERIAL_DISCREPANCY_EXCEPTIONS AS
SELECT 
    r.RUN_ID,
    r.RECON_DATE,
    r.PRODUCT_ID,
    p.PRODUCT_NAME,
    p.ASSET_CLASS,
    r.TRANSACTION_TOTAL_QTY,
    r.POSITION_TOTAL_QTY,
    r.DISCREPANCY_QTY,
    r.STATUS,
    r.IS_STALE,
    r.EXECUTED_AT
FROM RECON_DW.CORE_DWH.FACT_RECON_BALANCE_RUNS r
LEFT JOIN RECON_DW.CORE_DWH.DIM_PRODUCT p ON r.PRODUCT_ID = p.PRODUCT_ID
WHERE r.DISCREPANCY_QTY != 0
ORDER BY ABS(r.DISCREPANCY_QTY) DESC, r.EXECUTED_AT DESC;

-- ------------------------------------------------------------------------------
-- 3. Stale Trade Rate Anomaly Detector (Late Trades vs Reconciled State)
-- Measures latency between transaction booking and end-of-day reconciliation
-- ------------------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_LATE_TRADE_STALENESS_METRICS AS
SELECT 
    DATE_TRUNC('month', r.RECON_DATE) AS RECON_MONTH,
    COUNT(*) AS TOTAL_RUNS,
    SUM(CASE WHEN r.IS_STALE = TRUE THEN 1 ELSE 0 END) AS TOTAL_STALE_RUNS,
    ROUND((SUM(CASE WHEN r.IS_STALE = TRUE THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) AS STALENESS_PERCENTAGE
FROM RECON_DW.CORE_DWH.FACT_RECON_BALANCE_RUNS r
GROUP BY DATE_TRUNC('month', r.RECON_DATE)
ORDER BY RECON_MONTH DESC;
