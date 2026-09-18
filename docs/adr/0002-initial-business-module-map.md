# ADR-0002: Initial QuickCommerce Business Module Map

## Status

Accepted

## Context

QuickCommerce is targeting a Zepto/Blinkit-style quick-commerce workflow with consumer ordering, dark-store operations, inventory control, picking, and last-mile delivery. The finalized roles are:

- `ADMIN`
- `CATALOG_ADMIN`
- `SELLER`
- `WAREHOUSE_MANAGER`
- `STORE_MANAGER`
- `RIDER`
- `CONSUMER`

Roles describe who performs an action. They do not define the business state or the module that owns the action. Creating one module per role would duplicate workflows and spread ownership across the application.

## Decision

Create the following initial Spring Modulith modules under `com.minutemart.quickcommerce`:

| Module | Business responsibility | Owned state |
| --- | --- | --- |
| `identity` | Identity, authentication, role assignment, and access policy inputs | Accounts, credentials, role assignments, sessions/tokens where applicable |
| `customer` | Consumer account experience | Consumer profile and saved addresses |
| `seller` | Seller lifecycle | Seller account, onboarding, and seller status |
| `catalog` | Product discovery facts | Products, categories, attributes, and catalog entries |
| `store` | Dark-store network operations | Stores, operating state, service areas, and store configuration |
| `inventory` | Stock availability and reservation | Stock ledger, availability, reservations, and release records |
| `cart` | Shopping intent before checkout | Active carts and cart items |
| `ordering` | Checkout and order lifecycle | Orders, order lines, checkout decisions, and order status |
| `payment` | Money movement | Payment intents, provider references, authorization, capture, and refunds |
| `fulfillment` | Store-side order execution | Picking, packing, dispatch readiness, and fulfillment tasks |
| `delivery` | Last-mile delivery | Rider assignment, delivery state, and tracking facts |

The first module skeleton exposes only reserved `api` and `events` named interfaces. No domain model, repository, entity, or controller is created until its first use case is implemented. Internal package shape will be added as a vertical slice rather than as empty technical layers.

The initial dependency graph is:

```text
customer    -> identity :: events
seller      -> identity :: events
inventory   -> catalog :: events, store :: events
cart        -> catalog :: api, inventory :: api
ordering    -> cart :: api, customer :: api, payment :: api
fulfillment -> ordering :: events, inventory :: api, store :: api
delivery    -> fulfillment :: events
```

The graph is consumer-to-supplier and must remain acyclic. Events are the default integration style. The `ordering` to `payment` API is the initial explicit synchronous exception because checkout needs an immediate payment decision; payment owns its state and does not import ordering internals.

## Role ownership

| Role | Primary module responsibilities |
| --- | --- |
| `ADMIN` | Platform-wide access and administrative actions, implemented by the module that owns the changed state |
| `CATALOG_ADMIN` | Catalog management in `catalog` |
| `SELLER` | Seller lifecycle in `seller` and seller-authorized catalog workflows in `catalog` |
| `WAREHOUSE_MANAGER` | Stock and store-side fulfillment in `inventory` and `fulfillment` |
| `STORE_MANAGER` | Store operations and store-side execution in `store`, `inventory`, and `fulfillment` |
| `RIDER` | Assigned delivery work in `delivery` |
| `CONSUMER` | Consumer profile, cart, checkout, and order experience in `customer`, `cart`, and `ordering` |

## Alternatives considered

### One module per role

Rejected because a role crosses multiple business capabilities. For example, `STORE_MANAGER` works with store, inventory, and fulfillment state; a role module would become a privileged facade over other modules rather than an owner.

### Technical-layer packages

Rejected because global controllers, services, repositories, and entities obscure data ownership and allow accidental cross-capability coupling.

### Split every future capability immediately

Rejected for pricing, promotions, search, notifications, support, reporting, and platform operations. These concerns will become modules only when their language, invariants, state, and independent change pattern are understood.

## Consequences

- Business state has one clear owner even when several roles can modify it.
- Catalog, inventory, store, fulfillment, and delivery can evolve independently inside one deployment.
- Consumer checkout can coordinate small contracts without importing persistence or domain objects.
- Event-driven flows require idempotent listeners and acceptance of eventual consistency after order placement.
- The initial skeleton contains no business behavior; each module still needs use-case, domain, persistence, and adapter decisions as features are implemented.
- The C3 view and `ApplicationModules.verify()` test are the executable/documented baseline for future boundary changes.

## Enforcement

- Each module is a direct subpackage of `com.minutemart.quickcommerce`.
- Each module has `package-info.java` with `@ApplicationModule`.
- Deliberate contracts are reserved for `api` and `events`, each marked with `@NamedInterface`.
- `ArchitectureTests.applicationModulesShouldBeValid()` verifies the Spring Modulith graph.
- `docs/architecture/c4-views.md` records ownership, role mapping, and the initial dependency graph.
