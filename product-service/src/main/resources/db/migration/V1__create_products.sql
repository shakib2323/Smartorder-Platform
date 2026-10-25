CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(1000),
    price       DECIMAL(12, 2) NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_products_price CHECK (price >= 0)
) ENGINE=InnoDB;