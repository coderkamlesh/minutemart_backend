# QuickCommerce C4 Views

This document is the initial architecture baseline. It describes the current deployment shape and the accepted first business capability map. The map is intentionally coarse and will evolve through domain discovery and use-case implementation.

## C1: System Context

Audience: product, domain, and engineering stakeholders.

```text
                           +------------------+
                           | Customer          |
                           | Mobile / Web      |
                           +---------+--------+
                                     |
                                     v
+------------------+       +--------+---------+       +------------------+
| Store Operator   |------>|   QuickCommerce  |<------| Delivery Partner |
| Web / Operations  |       |   System         |       | Mobile / Web      |
+------------------+       +--------+---------+       +------------------+
                                     ^
                                     |
                           +---------+--------+
                           | Operations/Admin |
                           +------------------+

QuickCommerce integrates with external providers for capabilities such as:
- SMS or OTP delivery
- Payments and refunds
- Maps, geocoding, or delivery location services
- Push notifications
```

The external providers are dependencies of the owning business module or adapter. They are not shared domain services.

## C2: Container

Audience: engineering and operations.

```text
+----------------------+       +-----------------------------+
| Mobile and Web       |------>| QuickCommerce Spring Boot    |
| client applications  | HTTPS | application                  |
+----------------------+       | One deployable modular       |
                               | monolith                     |
                               +--------------+--------------+
                                              |
                                              | JPA / Modulith event storage
                                              v
                               +--------------+--------------+
                               | PostgreSQL                   |
                               | Module-owned tables/schemas  |
                               +-----------------------------+

QuickCommerce application ---> SMS/OTP provider
QuickCommerce application ---> Payment provider
QuickCommerce application ---> Maps/location provider
QuickCommerce application ---> Push notification provider
```

The Spring Boot application is the deployable container. PostgreSQL is an operational container, not a shared domain model. Logical data ownership remains with individual modules.

## C3: Initial business module map

These are logical Spring Modulith modules inside the one deployable application. They are not independently deployable services.

```text
 Identity      Customer       Seller       Catalog       Store
    |             |              |            |            |
    |             +--------------+------------+            |
    |                            |                         |
    |                         Cart <----- Inventory <------+
    |                            |             ^
    |                            v             |
    |                         Ordering ------> Payment
    |                            |
    |                            v
    |                       Fulfillment ------> Delivery

 Dependency direction is consumer-to-supplier in package metadata. The arrows above show
 the planned business flow; each interaction will use a named API or integration event.
```

### Module ownership

| Module | Owns | Primary roles |
| --- | --- | --- |
| `identity` | Accounts, authentication, role assignment, and access policy inputs | `ADMIN` |
| `customer` | Consumer profile, saved addresses, and consumer lifecycle | `CONSUMER` |
| `seller` | Seller onboarding, seller status, and seller account data | `SELLER` |
| `catalog` | Products, categories, attributes, and sellable catalog facts | `CATALOG_ADMIN`, `SELLER` |
| `store` | Dark stores, operating state, service areas, and store configuration | `STORE_MANAGER` |
| `inventory` | Stock ledger, availability, reservations, and release policy | `WAREHOUSE_MANAGER`, `STORE_MANAGER` |
| `cart` | Active consumer carts and cart item intent | `CONSUMER` |
| `ordering` | Checkout decision and order lifecycle | `CONSUMER` |
| `payment` | Payment intent, authorization, capture, refund, and provider adapter | `CONSUMER`, `ADMIN` |
| `fulfillment` | Picking, packing, dispatch readiness, and fulfillment task state | `WAREHOUSE_MANAGER`, `STORE_MANAGER` |
| `delivery` | Rider assignment, delivery state, and last-mile tracking | `RIDER` |

Roles are actors and authorization inputs, not application modules. `ADMIN` is a platform-wide role; an admin operation remains owned by the module whose business state it changes.

### Initial dependency graph

The initial package-level dependency graph is intentionally acyclic:

```text
customer    -> identity :: events
seller      -> identity :: events
inventory   -> catalog :: events, store :: events
cart        -> catalog :: api, inventory :: api
ordering    -> cart :: api, customer :: api, payment :: api
fulfillment -> ordering :: events, inventory :: api, store :: api
delivery    -> fulfillment :: events
```

`ordering` uses the payment API synchronously because checkout needs an immediate payment decision. `payment` does not depend on `ordering`; it receives an external reference and owns payment state. Order facts are published as events to `fulfillment`, where eventual consistency is acceptable.

The following capabilities are deliberately not standalone modules yet: pricing, promotions, search, notifications, support, reporting, and platform operations. They will be extracted from an owning module only when their language, invariants, data ownership, and independent change pattern justify a boundary.

Rules for this view:

- A module owns its state and business decisions.
- Arrows represent explicit named contracts or integration events, not direct repository access.
- The diagram does not require every interaction to be asynchronous.
- A module is not a service boundary or a separate deployment by default.
- Every accepted module dependency must be reflected in `package-info.java`, named interfaces, and architecture verification.

## C4 maintenance rule

Update this document when a business module, external system, deployable container, or important cross-module contract changes. Keep implementation class diagrams out of this document unless a code-level design is specifically needed.
