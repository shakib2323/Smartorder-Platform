CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_cart_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT chk_cart_quantity CHECK (quantity BETWEEN 1 AND 99)
) ENGINE=InnoDB;

CREATE INDEX idx_cart_customer ON cart_items (customer_id);
