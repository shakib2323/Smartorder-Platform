-- Who owns the stock. Filled from the ProductCreated event; 0 = platform product.
-- NULL for rows created before sellers existed (only admins can change those).
ALTER TABLE inventory ADD COLUMN seller_id BIGINT NULL;
CREATE INDEX idx_inventory_seller ON inventory (seller_id);
