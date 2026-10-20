-- Liquibase formatted SQL
-- changeset recon-author:001-create-transaction-capture context:prod,dev

CREATE TABLE IF NOT EXISTS transaction_capture (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    product_id VARCHAR(32) NOT NULL,
    account_id VARCHAR(32) NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL,
    price NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    trade_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_txn_product_id ON transaction_capture(product_id);
CREATE INDEX IF NOT EXISTS idx_txn_trade_date ON transaction_capture(trade_date);
