-- Customer ownership for orders. Nullable only to preserve pre-existing development orders.
-- All orders created through the authenticated API must set customer_id.
ALTER TABLE orders ADD COLUMN customer_id BIGINT NULL;
CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at);
