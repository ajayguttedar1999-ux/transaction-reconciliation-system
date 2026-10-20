-- ==============================================================================
-- Snowflake Script 3: Snowpipe & Automated Ingestion Pipeline
-- ==============================================================================

USE DATABASE RECON_DW;
USE SCHEMA STAGING;

-- 1. Create Parquet / JSON File Formats
CREATE OR REPLACE FILE FORMAT JSON_FORMAT
    TYPE = 'JSON'
    STRIP_OUTER_ARRAY = TRUE
    IGNORE_UTF8_ERRORS = FALSE;

-- 2. Staging Landing Table (Raw JSON Payload from Kafka Connect / AWS S3)
CREATE OR REPLACE TABLE RAW_KAFKA_EVENTS (
    EVENT_ID VARCHAR(100) DEFAULT UUID_STRING(),
    TOPIC_NAME VARCHAR(100) NOT NULL,
    EVENT_PAYLOAD VARIANT NOT NULL,
    RECEIVED_AT TIMESTAMP_NTZ DEFAULT CURRENT_TIMESTAMP()
);

-- 3. Staging Internal Stage (or External S3 Stage in AWS)
CREATE OR REPLACE STAGE RECON_EVENT_STAGE
    FILE_FORMAT = JSON_FORMAT
    COMMENT = 'Staging location for microservice Kafka event dumps';

-- 4. Snowpipe for Continuous Zero-Ops Ingestion
CREATE OR REPLACE PIPE PIPE_INGEST_TRANSACTION_EVENTS
    AUTO_INGEST = TRUE
    AS
    COPY INTO RAW_KAFKA_EVENTS (TOPIC_NAME, EVENT_PAYLOAD)
    FROM (
        SELECT 'transaction-booked-topic', $1
        FROM @RECON_EVENT_STAGE
    );

-- 5. Snowflake Stream to track Change Data Capture (CDC) from Staging
CREATE OR REPLACE STREAM STREAM_RAW_KAFKA_EVENTS ON TABLE RAW_KAFKA_EVENTS;

-- 6. Automated Scheduled Task to Transform and Load into CORE_DWH Fact Table
CREATE OR REPLACE TASK TASK_PROCESS_TRANSACTION_EVENTS
    WAREHOUSE = RECON_WH
    SCHEDULE = '1 MINUTE'
    WHEN SYSTEM$STREAM_HAS_DATA('STREAM_RAW_KAFKA_EVENTS')
    AS
    MERGE INTO RECON_DW.CORE_DWH.FACT_TRANSACTIONS target
    USING (
        SELECT 
            EVENT_PAYLOAD:transactionId::VARCHAR as TXN_ID,
            EVENT_PAYLOAD:productId::VARCHAR as PROD_ID,
            EVENT_PAYLOAD:type::VARCHAR as TXN_TYPE,
            EVENT_PAYLOAD:quantity::NUMBER(18,4) as QTY,
            EVENT_PAYLOAD:price::NUMBER(18,4) as PRC,
            (EVENT_PAYLOAD:quantity::NUMBER(18,4) * EVENT_PAYLOAD:price::NUMBER(18,4)) as TOT_AMT,
            EVENT_PAYLOAD:tradeDate::DATE as TRD_DATE,
            EVENT_PAYLOAD:status::VARCHAR as STAT
        FROM STREAM_RAW_KAFKA_EVENTS
        WHERE TOPIC_NAME = 'transaction-booked-topic'
    ) source
    ON target.TRANSACTION_ID = source.TXN_ID
    WHEN NOT MATCHED THEN
    INSERT (
        TRANSACTION_ID, PRODUCT_ID, TRANSACTION_TYPE, QUANTITY, PRICE, TOTAL_AMOUNT, TRADE_DATE, STATUS
    )
    VALUES (
        source.TXN_ID, source.PROD_ID, source.TXN_TYPE, source.QTY, source.PRC, source.TOT_AMT, source.TRD_DATE, source.STAT
    );

-- Resume Task
-- ALTER TASK TASK_PROCESS_TRANSACTION_EVENTS RESUME;
