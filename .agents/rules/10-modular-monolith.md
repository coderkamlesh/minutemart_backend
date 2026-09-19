# Modular Monolith Architecture

**Activation:** Always On

## Definition

This system is one deployable unit with explicit, independently understandable business modules. A monolith describes deployment. Modularity describes boundaries, cohesion, coupling, ownership, and contracts. A single deployable artifact must not become a reason to bypass boundaries.

The goal is evolutionary architecture: a module should be understandable, testable, changeable, and potentially extractable without designing a distributed system prematurely.

## Module boundaries

Create modules around bounded contexts or business capabilities, for example catalog, identity, ordering, inventory, fulfillment, pricing, or payments. The final module list must come from domain discovery and requirements, not from this example list.

Use these boundary tests:

- A module owns a coherent set of business capabilities.
- Most changes to one capability stay inside one module.
- The module has its own language, invariants, state, and use cases.
- Its public contract is smaller than its implementation.
- A dependency is justified by a business interaction, not by convenience or shared database access.
- If two proposed modules have constant, strong, bidirectional collaboration, reconsider whether the boundary is wrong.

Do not create modules solely named `common`, `core`, `shared`, `database`, `api`, `infrastructure`, or `security`. Technical concerns belong inside the business module that owns the behavior, except for genuinely cross-cutting platform concerns with a documented reason.

## Default package shape

```text
com.minutemart.quickcommerce.<module>
  package-info.java       module metadata and allowed dependencies
  api/                    deliberate synchronous inbound or outbound contracts
  events/                 public integration-event contracts
  application/            use cases and orchestration
  domain/                 aggregates, entities, value objects, rules, domain events
  infrastructure/         persistence and external-system adapters
  web/                    HTTP primary adapters
```

Only `api` and `events` are normally exposed as named interfaces. `application`, `domain`, `infrastructure`, and `web` are implementation details. A module can use a different internal shape when that improves cohesion, but it must preserve the same visibility and ownership rules.

## Encapsulation

- Start with package-private classes, constructors, methods, and fields.
- Make a type public only when it is a deliberate module contract, a framework requirement, or an explicitly documented adapter boundary.
- Do not expose JPA entities, Spring Data repositories, domain aggregates, command handlers, query handlers, configuration classes, or internal DTOs to another module.
- Do not make a module `OPEN` to avoid fixing package boundaries. Open modules are for legacy migration and are not the default for this new application.
- A public type is API. Treat changing it as a contract change.

## Dependency direction

Within a module, keep dependencies directed toward business policy:

```text
web -> application -> domain
                         ^
infrastructure -> application/domain ports
```

The domain must not depend on Spring, JPA, HTTP, security, a message broker, or an external SDK. Application code may depend on domain abstractions and ports. Infrastructure implements ports and adapts external systems. Web code validates and translates requests, then delegates.

Across modules, depend only on an explicitly allowed named interface. Never import another module's `internal`, `application`, `domain`, `infrastructure`, `web`, entity, repository, or implementation package.

## Integration policy

- Prefer an integration event when the consumer does not need an immediate result and eventual consistency is acceptable.
- Use a synchronous in-process call only when immediate consistency, a required return value, or a clearly documented transactional requirement justifies temporal coupling.
- A synchronous call must target a small public port or facade. It must not target a repository or an aggregate.
- The consumer owns an adapter or gateway for the call and maps the supplier's model into its own language.
- Do not create bidirectional module dependencies. If the interaction becomes cyclic, redesign the contract or introduce an event.

## Data ownership

Every module owns its state. A deployment may use one PostgreSQL server, but logical ownership must remain separate by schema, table set, or another explicitly documented boundary.

- No module reads or writes another module's tables.
- No cross-module foreign keys or shared JPA entity mappings.
- No shared mutable entity or DTO model.
- No transaction that updates two module-owned aggregates directly.
- Reporting and search projections are integration read models, not permission to bypass ownership.

## C4 and architecture communication

For every meaningful module addition or boundary change, update architecture documentation using text-based C4 views where practical:

- C1: system and external users/systems
- C2: the single deployable application and significant supporting containers
- C3: modules and their contracts/interactions
- C4: code-level detail only where it explains a non-obvious decision

Do not create diagrams that show implementation classes as if they were independent deployable services.
