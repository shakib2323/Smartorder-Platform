CREATE TABLE inventory (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id         BIGINT      NOT NULL,
    available_quantity INT         NOT NULL,
    updated_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                   ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_inventory_product UNIQUE (product_id),
    CONSTRAINT chk_inventory_qty CHECK (available_quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE stock_reservations (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    quantity   INT         NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                           ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_reservation_order_product UNIQUE (order_id, product_id),
    CONSTRAINT chk_reservation_qty CHECK (quantity > 0),
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'RELEASED'))
) ENGINE=InnoDB;
