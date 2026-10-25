# SmartOrder Platform

A multi-seller marketplace backend built as Spring Boot microservices. A seller registers a brand,
lists products with pictures and offers; customers search, reserve or buy; every confirmed order gets a
GST invoice (JSON and PDF).

```
client -> api-gateway :8084 -> auth-service      :8085  (users, JWT, roles CUSTOMER / SELLER / ADMIN)
                            -> product-service   :8081  (brands, products, images, offers)
                            -> inventory-service :8082  (stock, reservations)
                            -> order-service     :8083  (cart, checkout, pricing, GST, invoices, fulfilment)
          eureka-server :8762        MySQL :3307 (one database per service)        Kafka :9092
```

Services talk through **Kafka with a transactional outbox** (an event is saved in the same database
transaction as the change that caused it, then published), and through one REST call (Order -> Product).

| Event | From -> To | Effect |
|---|---|---|
| `ProductCreated` | product -> inventory | creates the stock row and remembers the owning seller |
| `OrderCreated` | order -> inventory | reserves stock |
| `InventoryReserved` / `InventoryRejected` | inventory -> order | order becomes CONFIRMED (or RESERVED) / REJECTED |
| `OrderCancelled` | order -> inventory | releases stock (cancel, or an expired reservation) |

## The marketplace flow

1. **Seller** registers (`POST /api/v1/auth/register-seller`) and registers a **brand** (`POST /api/v1/brands`).
   It is `PENDING` until an **admin** approves it.
2. Seller creates **products** (name, price, category, size, colour, SKU, GST rate, HSN, first stock),
   uploads up to 8 **images** each, and adds **offers** (percent or flat, optional "buy N or more", date window).
3. **Customers** search (`q`, category, brand, price range, size, colour, sort), fill a **cart** and **check out**.
   Prices shown already include the best seller offer. One order is created per brand.
4. A customer can **reserve** instead of buying: stock is held for 1 to 72 hours, then confirm or it expires.
5. When an order is confirmed, `GET /api/v1/orders/{id}/invoice.pdf` returns the **bill**.
6. The **seller** sees their orders, marks them shipped (with tracking) and delivered, and has a sales summary.

Import `postman/SmartOrder-marketplace.postman_collection.json` and run folders 1 to 8 in order.

## Run it locally

Prerequisites: Java 21, Maven 3.9+, Docker.

```bash
docker compose up -d                      # MySQL + Kafka
# If your MySQL volume is from the first version, recreate the auth user with the new password:
#   docker exec -i smartorder-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-rootpass}" < docker/add-auth-db.sql

export JWT_SECRET=$(openssl rand -base64 48)
export ADMIN_PASSWORD='choose-a-strong-password'

mvn clean verify                          # builds and tests everything
# start each service in its own terminal, eureka-server first:
mvn -pl eureka-server spring-boot:run
mvn -pl auth-service spring-boot:run      # same for product-, inventory-, order-service and api-gateway
```

Database migrations (Flyway) run on startup. Hibernate only validates the schema.

## Configuration

| Setting | Where | Default |
|---|---|---|
| `JWT_SECRET` | all services | none, required |
| `ADMIN_PASSWORD` | auth-service | none, no admin created without it |
| `billing.shipping.flat-fee` / `free-above` | order-service | 49.00 / 999.00 |
| `reservation.default-hold-hours` | order-service | 24 |
| `app.images.max-per-product` | product-service | 8 |
| `CORS_ALLOWED_ORIGINS` | api-gateway | `http://localhost:3000` |

## Money rules

* Product prices are **GST-inclusive** (like an Indian MRP). The invoice extracts the tax:
  `taxable = total x 100 / (100 + rate)`.
* Seller state equals the delivery state: **CGST + SGST** (half each). Different state: **IGST**.
  States are matched by name (case-insensitive), so use the same spelling in brand and address.
* Offers never stack; the single best saving per line wins. A discount can never push a price below zero.
* Delivery fee is charged below the free-shipping limit.
* Allowed GST slabs a seller may choose: 0, 5, 12, 18, 28. The platform does not decide which slab applies to
  a product; the seller declares it (with the HSN code), as on real marketplaces.

## Known limits

See `IMPROVEMENTS.md` for the full list. Notably: no payment gateway (cash on delivery only), no refunds
or returns, local-disk image storage, and invoice PDFs print ASCII only.
