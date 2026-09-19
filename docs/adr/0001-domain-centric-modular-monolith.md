# ADR-0001: Domain-Centric Modular Monolith

## Status

Accepted

## Context

QuickCommerce is a single Spring Boot deployment that will serve consumer mobile and web clients and support operational capabilities such as catalog, inventory, ordering, fulfillment, payments, and identity. The application must remain easy to deploy while preventing the codebase from becoming a big ball of mud.

The architecture needs two complementary properties:

- C4 views that communicate the system at the right level of detail.
- Domain-centric modules that own business decisions, state, and contracts.

Spring Modulith 2.0.8 is already part of the application and can detect module boundaries, named interfaces, dependency violations, and cycles.

## Decision

We will build QuickCommerce as one deployable modular monolith. Modules will be created around bounded capabilities and business language, not around technical layers.

Simon Brown's C4 model will be used for architecture communication:

- C1 describes users, QuickCommerce, and external systems.
- C2 describes the deployable application and runtime-relevant supporting containers.
- C3 describes business modules inside the application.
- C4 is reserved for code detail that clarifies a non-obvious design.

Kamil Grzybek's domain-centric modular-monolith principles will guide implementation:

- Each module owns its domain behavior, invariants, use cases, persistence state, and adapters.
- Modules expose only deliberate `api` and `events` contracts.
- Consumers never access another module's implementation packages, repositories, entities, tables, or schemas.
- Modules communicate through immutable integration events by default, or through a small synchronous API when an immediate result or strong consistency is required.
- The dependency graph must remain acyclic and be verified with Spring Modulith.
- Each module owns its data. Shared tables and cross-module foreign-key mappings are prohibited.

The application will prefer a well-factored monolith first. Future extraction is an option driven by real operational or ownership needs, not a requirement for every module.

## Alternatives considered

### Technical-layer monolith

Rejected because global controllers, services, repositories, and entities hide business ownership and create broad coupling.

### Microservices from the start

Rejected because it adds network, deployment, data-distribution, and operational complexity before scale or ownership boundaries are proven.

### Shared database model

Rejected because shared tables and cross-module entity mappings make boundaries nominal and prevent independent evolution.

### Events for every interaction

Rejected as an absolute rule. Events are the default for post-commit reactions, but a small synchronous contract is appropriate when the caller needs an immediate result or decision and the dependency remains acyclic.

### Treating every Spring Modulith module as a deployable service

Rejected because a Spring Modulith module is a logical boundary inside one deployment. C4 C3 components and Spring modules guide ownership; neither implies an immediate service split.

## Consequences

Positive consequences:

- Business ownership and data ownership are explicit.
- Illegal coupling is detectable before it spreads.
- The monolith keeps simple deployment and in-process performance.
- Module contracts make later extraction more deliberate.
- C4 views provide different audiences with appropriate architectural detail.

Costs and risks:

- Boundary design requires domain discovery before coding.
- Events introduce eventual consistency, retries, and idempotency requirements.
- Separate module data models can require projections or explicit contract mapping.
- Spring Modulith verification and architecture tests must be maintained as the codebase grows.
- A clean logical boundary does not guarantee that future extraction will be operationally worthwhile.

## Enforcement

- `.agents/rules/10-modular-monolith.md` defines module ownership, encapsulation, dependency direction, and data ownership.
- `.agents/rules/15-c4-ddd-modular-monolith.md` defines C4, DDD, and evolution principles.
- `.agents/rules/20-spring-modulith.md` defines named interfaces, allowed dependencies, events, and verification.
- `.agents/skills/module-boundary-design/SKILL.md` is required for module graph changes.
- Every meaningful boundary change updates the relevant C4 view and this decision's related ADR trail.
- Once the first real module is added, `ApplicationModules.verify()` must be maintained as an architecture test.
