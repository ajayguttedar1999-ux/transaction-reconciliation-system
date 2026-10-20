-- Liquibase formatted SQL
-- changeset recon-author:001-create-position-capture context:prod,dev

CREATE TABLE IF NOT EXISTS position_capture (
    id BIGSERIAL PRIMARY KEY,
    position_id VARCHAR(64) NOT NULL UNIQUE,
    product_id VARCHAR(32) NOT NULL UNIQUE,
    total_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    total_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pos_product_id ON position_capture(product_id);
