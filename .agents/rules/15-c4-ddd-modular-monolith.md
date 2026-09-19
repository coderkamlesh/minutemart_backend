# C4, DDD, and Modular Monolith Principles

**Activation:** For architecture design, module creation, module boundary changes, and significant cross-module features

This project applies Simon Brown's pragmatic C4 modeling approach and Kamil Grzybek's domain-centric modular-monolith practices through Spring Modulith. These principles guide design; they do not replace the executable rules in `10-modular-monolith.md` and `20-spring-modulith.md`.

## C4 modeling rules

Use the level that answers the current question. Do not draw implementation detail on a system-context diagram or pretend that a package is a deployable service.

- **C1, System Context:** Show QuickCommerce, its user types, and external systems. Keep the view understandable to non-technical stakeholders.
- **C2, Container:** Show independently deployable or runtime-relevant containers, such as the single QuickCommerce application, PostgreSQL, and an external SMS or payment provider. A package is not a C2 container merely because it has a name.
- **C3, Component:** Show the major business modules inside the QuickCommerce application. In this project, a Spring Modulith application module is normally represented as a C3 business component.
- **C4, Code:** Use only when implementation detail explains a non-obvious design or contract. Do not create class-level diagrams by default.

The C4 model is a communication and navigation tool, not a reason to create extra packages or services. Every diagram must state its scope, audience, and abstraction level. Update the relevant view when a real module, external system, or important integration contract changes.

## Domain-centric module rules

Design boundaries from business behavior and language, not from technical layers.

- A module owns a bounded capability, its decisions, invariants, use cases, state, and external adapters.
- A module must have a clear owner, a small public contract, and a reason to change independently from neighboring modules.
- Aggregates protect invariants and are consistency boundaries, not table groupings.
- References across aggregates and modules are identifiers or explicit contracts, not object graphs.
- Similar words in different bounded contexts may represent different concepts. Do not create a universal domain model.
- Data ownership follows the bounded context. A module never reads or writes another module's tables, repositories, entities, or persistence projections.
- Technical packages such as `controller`, `service`, `repository`, `entity`, `common`, and `util` are not application modules.

## Encapsulation and contracts

Treat each module as if it could be extracted behind an HTTP boundary, without prematurely deploying it as a service.

- Implementation packages remain private by convention and by Spring Modulith verification.
- Public types require a concrete consumer and belong in a deliberate `api` or `events` named interface.
- Never expose entities, repositories, aggregates, application handlers, framework configuration, or internal DTOs as module contracts.
- Consumers depend on the smallest supplier contract and map it into their own model.
- Do not use `OPEN` modules, wildcard dependencies, or public implementation classes to bypass a boundary.

## Integration and dependency rules

The dependency graph must remain intentional, minimal, and acyclic.

- Prefer an immutable integration event when a consumer only needs to react after the producer commits and eventual consistency is acceptable.
- Use a synchronous module API only when an immediate result or strong consistency is genuinely required. Record why an event is insufficient.
- Shared database tables and cross-module foreign-key mappings are prohibited.
- Integration events are facts that happened, not commands disguised as events. Keep them small, stable, and free of domain or persistence objects.
- Cross-module listeners must be retry-safe and idempotent. Document failure and replay behavior where it is not obvious.
- Event-driven integration does not make extraction automatic. Extraction is a later operational decision based on real scaling, ownership, or deployment constraints.

## Enforcement and evolution

Documentation and enforcement are both required.

- Add or update the relevant C4 view for meaningful boundary changes.
- Add an ADR when a module, public contract, dependency direction, consistency model, database ownership, or external integration changes.
- Annotate module metadata and named interfaces deliberately.
- Keep `ApplicationModules.verify()` coverage current and green when architecture verification is requested.
- Add domain tests for invariants and module/integration tests for contracts and event behavior.
- Prefer a well-factored monolith first. Do not split a module into a service only because a diagram makes it look independent.
