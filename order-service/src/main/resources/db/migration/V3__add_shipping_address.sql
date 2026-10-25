-- Every new order must have a delivery address.
-- Orders that already exist get empty text so the NOT NULL columns can be added;
-- the defaults are dropped straight after, so new rows always have to supply real values.
ALTER TABLE orders
    ADD COLUMN shipping_full_name   VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN shipping_phone       VARCHAR(20)  NOT NULL DEFAULT '',
    ADD COLUMN shipping_line1       VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN shipping_line2       VARCHAR(200) NULL,
    ADD COLUMN shipping_city        VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN shipping_state       VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN shipping_postal_code VARCHAR(20)  NOT NULL DEFAULT '',
    ADD COLUMN shipping_country     VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE orders
    ALTER COLUMN shipping_full_name   DROP DEFAULT,
    ALTER COLUMN shipping_phone       DROP DEFAULT,
    ALTER COLUMN shipping_line1       DROP DEFAULT,
    ALTER COLUMN shipping_city        DROP DEFAULT,
    ALTER COLUMN shipping_state       DROP DEFAULT,
    ALTER COLUMN shipping_postal_code DROP DEFAULT,
    ALTER COLUMN shipping_country     DROP DEFAULT;
