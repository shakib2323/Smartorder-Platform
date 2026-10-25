-- Creates the database and user for the auth-service.
-- mysql-init.sql only runs when the MySQL volume is brand new, so for an existing setup run this file once:
--   docker exec -i smartorder-mysql mysql -uroot -prootpass < docker/add-auth-db.sql

CREATE DATABASE IF NOT EXISTS auth_db;

CREATE USER IF NOT EXISTS 'auth_user'@'%'
IDENTIFIED BY 'auth_pass';

ALTER USER 'auth_user'@'%'
IDENTIFIED BY 'auth_pass';

GRANT ALL PRIVILEGES ON auth_db.*
TO 'auth_user'@'%';


FLUSH PRIVILEGES;
