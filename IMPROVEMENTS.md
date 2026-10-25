# What changed in this version

## 1. Problems found in the original code, and the fixes

### Bugs
| # | Problem | Effect | Fix |
|---|---|---|---|
| 1 | `GlobalExceptionHandler` in auth-service imported Apache's `InvalidCredentialsException` (IDE auto-import) instead of your own class | A wrong password was not caught as 401 and fell into the generic handler: **HTTP 500** | Import removed |
| 2 | Every service had a catch-all `@ExceptionHandler(Exception.class)` that also caught Spring's own errors | Unknown URL, wrong method, unsupported media type and all image-upload errors (`ResponseStatusException`) returned **500** | Framework 4xx errors keep their status (all services); product-service uses its own `ApiException` |
| 3 | Cart view called the Product service per item, and a deactivated product returned 404 | **One deactivated product made the whole cart fail** (the `if (!active) continue` line could never run) | One batch call; unavailable products are shown as `available:false` and left out of totals |
| 4 | `idempotency_key` was unique across *all* customers | Customer B reusing a key got an error that revealed customer A's key; one customer could block another's key | Unique per `(customer_id, idempotency_key)` (migration V6) |
| 5 | Image upload trusted the client's `Content-Type` header | A script renamed to `.jpg` was accepted | Type detected from the file's first bytes (JPG/PNG/WebP only); orphan files removed if the database write fails |
| 6 | Stock `delta` had no bounds | Integer overflow in the `INT` column | Bounded to +/- 1,000,000 |

### Security and hygiene
* A default JWT secret (4 services), the admin password `Admin@12345` and the auth database password `root` were committed. Now `JWT_SECRET` is **required** (service refuses to start without it), the admin is only created if `ADMIN_PASSWORD` is set, and the DB password is no longer `root`.
* No brute-force protection on login: now 5 failed attempts lock that email for 15 minutes (in memory; use Redis for several instances).
* `/internal/**` controllers (confirm any order, reserve any stock) were blocked only by a security rule. They were dead code and a hazard, so they were removed.
* Product writes were ADMIN-only with no concept of an owner. Ownership is now enforced.

### Design and performance
* N+1 remote calls (one Product request per cart/order line) replaced by one batch request.
* List endpoints returned bare arrays without totals. They now return `{content, page, size, totalElements, totalPages}`.
* Gateway had no CORS configuration (a browser front end could not call it): added, configurable.
* Package typo `messageing` and the stray `com.smartorder` package in product-service were normalised.

## 2. New features

**Marketplace**
* `SELLER` role and `POST /api/v1/auth/register-seller`.
* **Brands** (a seller's store): register, edit, upload logo; **admin approval** before products become visible; admins can suspend.
* Seller **product management** with ownership checks: category, size, colour, SKU, GST rate, HSN code, activate/deactivate, "my products".
* Product **search**: text, category, brand, price range, size, colour, sorting, paging.
* **Multiple product images** (up to 8, choose the cover, delete) behind an `ImageStorage` interface (local disk now, S3 later).
* Initial stock is entered with the product. A `ProductCreated` event (transactional outbox) makes Inventory create the stock and remember the owner; sellers can adjust only their own stock, and list it at `/inventory/mine`.

**Offers**
* Seller offers: percent or flat, per product or brand-wide, "buy N or more", date window, switch off. The best single offer wins (no stacking); the price can never go below zero. Product pages show `discountedPrice` and the offer; the cart and orders apply the same rule.

**Ordering**
* **Cart checkout** creates one order per brand, retry-safe with one `Idempotency-Key`.
* **Reservations**: reserve for 1 to 72 hours (stock held, status `RESERVED`), confirm before the time runs out, or a background job expires it and releases the stock.
* Seller fulfilment: `SHIPPED` (with tracking number) and `DELIVERED`; seller order list and sales summary.

**Bill (invoice)**
* GST invoice per order: invoice number, seller (legal name, GSTIN), buyer and delivery address, items with HSN, list price, offer discount, taxable value, GST rate; CGST + SGST or IGST by state, GST summary per rate, delivery charge, grand total, amount in words (Indian system), "you saved" banner, payment status, tracking number, multi-page support, "CANCELLED" mark.
* `GET /orders/{id}/invoice` (JSON) and `/invoice.pdf`. The PDF is generated without any library. All amounts come from a snapshot stored with the order, so old invoices never change when a seller edits prices or details.

## 3. What was verified, and what was not

**Run and passed here (30 tests):** pricing engine, offer selection, GST split, rounding, shipping rule, money formatting, amount in words, image-type detection. The tax test checks 200,000 price/rate combinations that `taxable + CGST + SGST == total` to the paisa; I confirmed it fails when I deliberately introduce a rounding bug. The PDF was generated, checked with `pdfinfo`, and inspected visually (single page, multi-page, inter-state, cancelled). That is how a cancelled-stamp layout bug was found and fixed.

**Not run:** there was no Maven, network, database or Kafka in my environment, so nothing was compiled against Spring, and none of the following has been executed: SQL migrations, Hibernate schema validation, security rules, Kafka flows, the existing Testcontainers tests. I only checked syntax, that every call between your own classes matches, and that every library class used is imported. Expect to fix a few small things on the first `mvn clean verify`. Test these first:

1. Flyway on a **copy of existing data**: product V3 (adds brands, moves images), order V6 (re-keys idempotency, new statuses), auth V2, inventory V3.
2. Application start-up: Hibernate `validate` for the new embedded order columns and entities.
3. Security rules per role (seller vs customer vs admin) and the `/mine` and `/me` paths.
4. The end-to-end Postman run (folders 1 to 8).

## 4. Decisions you may want to change
* One brand per seller account. One order contains one brand (checkout splits a mixed cart).
* "Reserve" is implemented as a time-limited stock hold that the customer later confirms.
* India-first billing: GST-inclusive prices, INR, `Rs.` in the PDF (the rupee sign is not in the built-in PDF fonts; names in non-Latin scripts print as `?`; use a library with an embedded font to fix that).
* The GST slab is declared by the seller. The platform does not validate it against the product's HSN code.
* No payment gateway: cash on delivery. Invoice numbers are `INV-<year>-<order id>`, unique but not gapless.

## 5. Suggested next steps (not built)
Payment gateway with webhooks and payment states; returns, refunds and credit notes; reviews and ratings; coupon codes with usage limits; seller payouts and commission; S3 and CDN for images; OpenSearch for search; refresh tokens and logout; rate limiting at the gateway (Redis); one shared security module instead of three copies; Dockerfiles and a full compose stack; Kubernetes manifests; OpenTelemetry tracing and Grafana dashboards; email and SMS notifications from Kafka events.
