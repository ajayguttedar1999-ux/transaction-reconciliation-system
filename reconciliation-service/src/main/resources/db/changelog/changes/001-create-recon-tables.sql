-- Liquibase formatted SQL
-- changeset recon-author:001-create-recon-tables context:prod,dev

CREATE TABLE IF NOT EXISTS recon_run (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL UNIQUE,
    recon_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_stale BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recon_balance_result (
    id BIGSERIAL PRIMARY KEY,
    recon_run_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    txn_total_qty NUMERIC(18, 4) NOT NULL,
    pos_total_qty NUMERIC(18, 4) NOT NULL,
    discrepancy_qty NUMERIC(18, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_recon_run FOREIGN KEY (recon_run_id) 
        REFERENCES recon_run(run_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recon_run_date ON recon_run(recon_date);
CREATE INDEX IF NOT EXISTS idx_recon_balance_run_id ON recon_balance_result(recon_run_id);
