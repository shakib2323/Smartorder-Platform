-- ===================================================================================
-- Marketplace orders: one seller per order, price breakdown with offers and GST,
-- reservations with an expiry time, shipping steps, and invoices.
-- ===================================================================================

-- 1. Idempotency keys are now unique PER CUSTOMER. Before, they were unique globally, so one
--    customer's key could block (and reveal the existence of) another customer's key.
ALTER TABLE orders DROP INDEX uq_orders_idempotency_key;
ALTER TABLE orders ADD CONSTRAINT uq_orders_customer_idempotency UNIQUE (customer_id, idempotency_key);

-- 2. New order statuses.
ALTER TABLE orders DROP CHECK chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (status IN
    ('PENDING', 'RESERVED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'REJECTED', 'CANCELLED', 'EXPIRED'));

-- 3. Order-level columns.
ALTER TABLE orders
    ADD COLUMN order_type         VARCHAR(12)    NOT NULL DEFAULT 'PURCHASE',
    ADD COLUMN reserve_expires_at DATETIME(6)    NULL,
    ADD COLUMN brand_id           BIGINT         NULL,
    ADD COLUMN brand_name         VARCHAR(120)   NULL,
    ADD COLUMN seller_id          BIGINT         NULL,
    ADD COLUMN seller_legal_name  VARCHAR(200)   NULL,
    ADD COLUMN seller_gstin       VARCHAR(15)    NULL,
    ADD COLUMN seller_state       VARCHAR(100)   NULL,
    ADD COLUMN seller_city        VARCHAR(100)   NULL,
    ADD COLUMN subtotal           DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN discount_total     DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN taxable_total      DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_total         DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_total         DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_total         DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN shipping_fee       DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN currency           VARCHAR(3)     NOT NULL DEFAULT 'INR',
    ADD COLUMN payment_method     VARCHAR(10)    NOT NULL DEFAULT 'COD',
    ADD COLUMN tracking_number    VARCHAR(60)    NULL,
    ADD COLUMN shipped_at         DATETIME(6)    NULL,
    ADD COLUMN delivered_at       DATETIME(6)    NULL,
    ADD CONSTRAINT chk_orders_type CHECK (order_type IN ('PURCHASE', 'RESERVATION'));

-- Orders that existed before pricing was recorded: no discount, no tax split.
UPDATE orders SET subtotal = total_amount, taxable_total = total_amount;

CREATE INDEX idx_orders_seller_created ON orders (seller_id, created_at);
CREATE INDEX idx_orders_reservation_expiry ON orders (status, reserve_expires_at);

-- 4. Line-level columns (all are snapshots taken when the order was placed).
ALTER TABLE order_items
    ADD COLUMN sku              VARCHAR(50)    NULL,
    ADD COLUMN size             VARCHAR(20)    NULL,
    ADD COLUMN color            VARCHAR(30)    NULL,
    ADD COLUMN hsn_code         VARCHAR(8)     NULL,
    ADD COLUMN gst_rate_percent DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    ADD COLUMN discount_amount  DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN offer_title      VARCHAR(120)   NULL,
    ADD COLUMN line_total       DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN taxable_value    DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount      DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount      DECIMAL(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount      DECIMAL(14, 2) NOT NULL DEFAULT 0;

UPDATE order_items SET line_total = unit_price * quantity, taxable_value = unit_price * quantity;

-- 5. Invoices. One per order, created the first time it is requested after the order is confirmed.
CREATE TABLE invoices (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id       BIGINT      NOT NULL,
    invoice_number VARCHAR(30) NOT NULL,
    issued_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_invoices_order  UNIQUE (order_id),
    CONSTRAINT uq_invoices_number UNIQUE (invoice_number),
    CONSTRAINT fk_invoices_order  FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;
