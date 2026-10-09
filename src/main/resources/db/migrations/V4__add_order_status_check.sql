-- ==========================================================
-- V4__add_order_status_check.sql
-- Ensures composite index on orders(buyer_id, status) for verified-purchase review checks
-- ==========================================================

CREATE INDEX IF NOT EXISTS idx_orders_buyer_status ON orders(buyer_id, status);
