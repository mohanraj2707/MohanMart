-- ==========================================================
-- V3__add_fk_indexes.sql
-- Ensures all foreign keys and lookup columns are indexed
-- ==========================================================

CREATE INDEX IF NOT EXISTS idx_products_seller_id     ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_orders_buyer_id        ON orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_user_id     ON cart_items(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id  ON cart_items(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_product_id     ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id        ON reviews(user_id);
