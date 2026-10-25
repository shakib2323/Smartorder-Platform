CREATE DATABASE products_db;
CREATE DATABASE inventory_db;
CREATE DATABASE orders_db;

CREATE USER 'product_user'@'%'   IDENTIFIED BY 'product_pass';
CREATE USER 'inventory_user'@'%' IDENTIFIED BY 'inventory_pass';
CREATE USER 'order_user'@'%'     IDENTIFIED BY 'order_pass';

GRANT ALL PRIVILEGES ON products_db.*  TO 'product_user'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'inventory_user'@'%';
GRANT ALL PRIVILEGES ON orders_db.*    TO 'order_user'@'%';

FLUSH PRIVILEGES;

-- Auth Service database/user for a fresh MySQL volume.
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'auth_pass';
GRANT ALL PRIVILEGES ON auth_db.* TO 'auth_user'@'%';

FLUSH PRIVILEGES;
