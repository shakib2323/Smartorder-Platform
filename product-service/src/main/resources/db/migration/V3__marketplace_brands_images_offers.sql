-- ===================================================================================
-- Marketplace: brands (stores), richer products, multiple images, seller offers, outbox
-- ===================================================================================

CREATE TABLE brands (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id  BIGINT        NOT NULL,
    name           VARCHAR(120)  NOT NULL,
    legal_name     VARCHAR(200)  NOT NULL,
    description    VARCHAR(1000),
    logo_url       VARCHAR(500),
    contact_email  VARCHAR(254)  NOT NULL,
    gstin          VARCHAR(15),
    city           VARCHAR(100)  NOT NULL,
    state          VARCHAR(100)  NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_brands_owner UNIQUE (owner_user_id),
    CONSTRAINT uq_brands_name  UNIQUE (name),
    CONSTRAINT chk_brands_status CHECK (status IN ('PENDING', 'APPROVED', 'SUSPENDED'))
) ENGINE=InnoDB;

-- Products that existed before sellers did belong to a built-in "platform" brand (owner 0).
-- Only admins can manage it. Edit its city/state/GSTIN to match your real business details.
INSERT INTO brands (owner_user_id, name, legal_name, description, contact_email, city, state, status)
VALUES (0, 'SmartOrder Marketplace', 'SmartOrder Marketplace', 'Products listed by the platform itself',
        'support@smartorder.local', 'Hyderabad', 'Telangana', 'APPROVED');

ALTER TABLE products
    ADD COLUMN brand_id         BIGINT        NULL,
    ADD COLUMN category         VARCHAR(30)   NOT NULL DEFAULT 'OTHER',
    ADD COLUMN size             VARCHAR(20)   NULL,
    ADD COLUMN color            VARCHAR(30)   NULL,
    ADD COLUMN sku              VARCHAR(50)   NULL,
    ADD COLUMN gst_rate_percent DECIMAL(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN hsn_code         VARCHAR(8)    NULL,
    ADD COLUMN created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

UPDATE products SET brand_id = (SELECT id FROM brands WHERE owner_user_id = 0);

ALTER TABLE products
    MODIFY COLUMN brand_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    ADD CONSTRAINT uq_products_brand_sku UNIQUE (brand_id, sku),
    ADD CONSTRAINT chk_products_gst CHECK (gst_rate_percent >= 0 AND gst_rate_percent <= 28);

CREATE INDEX idx_products_active_category ON products (active, category);

CREATE TABLE product_images (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_product_images_product ON product_images (product_id, sort_order);

-- Keep the images that were uploaded with the old single-image feature.
INSERT INTO product_images (product_id, url, sort_order, is_primary)
SELECT id, image_url, 0, TRUE FROM products WHERE image_url IS NOT NULL;

ALTER TABLE products DROP COLUMN image_url;

-- Seller offers. product_id NULL means "every product of the brand".
CREATE TABLE offers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id       BIGINT        NOT NULL,
    product_id     BIGINT        NULL,
    title          VARCHAR(120)  NOT NULL,
    discount_type  VARCHAR(10)   NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    min_quantity   INT           NOT NULL DEFAULT 1,
    starts_at      DATETIME(6)   NOT NULL,
    ends_at        DATETIME(6)   NOT NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_offers_brand   FOREIGN KEY (brand_id)   REFERENCES brands (id),
    CONSTRAINT fk_offers_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_offers_type   CHECK (discount_type IN ('PERCENT', 'FLAT')),
    CONSTRAINT chk_offers_value  CHECK (discount_value > 0),
    CONSTRAINT chk_offers_minqty CHECK (min_quantity BETWEEN 1 AND 1000),
    CONSTRAINT chk_offers_window CHECK (ends_at > starts_at)
) ENGINE=InnoDB;

CREATE INDEX idx_offers_brand_active ON offers (brand_id, active, ends_at);

-- Transactional outbox (same shape as the other services).
CREATE TABLE outbox_events (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id       CHAR(36)     NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    correlation_id VARCHAR(64),
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at        DATETIME(6),
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'SENT'))
) ENGINE=InnoDB;

CREATE INDEX idx_outbox_status_id ON outbox_events (status, id);
