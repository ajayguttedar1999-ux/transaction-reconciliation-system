-- liquibase formatted sql
-- changeset ajay:002-create-recovery-queue

CREATE TABLE IF NOT EXISTS transaction_recovery_queue (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    failure_reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_RETRY',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recovery_status ON transaction_recovery_queue(status);
