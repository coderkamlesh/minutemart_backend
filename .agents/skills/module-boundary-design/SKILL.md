---
name: module-boundary-design
description: Designs or reviews QuickCommerce bounded contexts, Spring Modulith module boundaries, dependency graphs, public contracts, and C4 views using domain-centric modular-monolith principles. Use when creating, splitting, merging, renaming, or substantially changing a business module.
---

# Module Boundary Design Skill

Use this skill before changing the module graph. It combines C4 communication, DDD boundary design, and Spring Modulith enforcement. For the full architecture workflow, also use `c4-ddd-modular-monolith`. Read `.agents/rules/15-c4-ddd-modular-monolith.md` with the other architecture rules before starting.

## Start from domain behavior

Do not begin with package names. Identify:

- Actors and business capabilities
- Commands and queries
- Domain events and state transitions
- Invariants and ownership of decisions
- Data that must be private to one capability
- External systems and operational constraints

Group concepts that use the same language and change together. Treat each candidate group as a bounded context, not as a technical layer.

Before choosing a package, identify the C4 scope being discussed:

- C1: actors and external systems around QuickCommerce.
- C2: deployable or runtime-relevant containers.
- C3: business modules inside the QuickCommerce application.
- C4: code detail only where it explains a contract or non-obvious design.

Do not describe a Spring package as a C2 container or assume every C3 component is independently deployable.

## Evaluate a proposed boundary

For each candidate module, answer:

1. What business responsibility does it own?
2. Which aggregate or state does it protect?
3. What is its smallest public API?
4. Which integration events does it publish?
5. Which events or APIs does it consume?
6. What data must it own exclusively?
7. Which dependencies are unavoidable?
8. Is the dependency graph acyclic?
9. Do changes normally stay inside this boundary?
10. Would merging two highly interdependent candidates improve cohesion?

Reject boundaries that exist only because a framework has a controller or repository concept.

Apply the extraction test: if this capability were later placed behind a network boundary, could its ownership, data, and contract remain coherent? Passing this test does not justify extracting it now.

## Design the contract

Choose one of these deliberately:

- Integration event for notification and loose coupling
- Synchronous module API for an immediate result or strong consistency requirement
- A read projection for reporting/search needs without exposing operational tables

Keep contracts small, immutable, and owned by the supplier. A consumer must map the contract into its own model. Never use entities, repositories, or rich domain objects as a contract.

## Draw the graph

Represent the intended dependency graph as a directed graph. Every edge must have:

- Supplier module
- Consumer module
- Named interface or event contract
- Consistency expectation
- Reason the edge cannot be removed or replaced with an event

Cycles are design failures. Resolve them by changing ownership, using an event, introducing a stable abstraction, or merging the incorrectly separated modules.

## Enforce the design

For an accepted boundary:

- Create the module package under `com.minutemart.quickcommerce`.
- Add `package-info.java` with `@ApplicationModule` and explicit allowed dependencies where needed.
- Mark contract packages with `@NamedInterface`.
- Keep implementation packages non-public to other modules.
- Add/update `ApplicationModules.verify()` coverage.
- Add a C3 module diagram or text view when the graph changed.
- Create an ADR under `docs/adr/` for the boundary and its consequences.
- Keep the C1 and C2 views accurate when actors, external systems, deployment containers, or operational responsibilities change.

Do not use `@ApplicationModule(type = OPEN)` as a shortcut. If legacy access is unavoidable, document the temporary exception and its removal plan.

## Review output

When proposing a boundary, return:

- Context and business ownership
- Proposed module list
- Public contracts and events
- Dependency graph
- Data ownership map
- Consistency and integration choices
- Risks and rejected alternatives
- Enforcement and migration steps

## Design review checklist

Before accepting a boundary, answer these questions explicitly:

1. What business decision does this module own?
2. Which invariant and consistency boundary does it protect?
3. Which data is private to it?
4. What is the smallest contract a consumer needs?
5. Why is each dependency present, and why is it not a cycle?
6. Is the interaction an event, a synchronous API, or a consumer-owned projection? Why?
7. What happens on duplicate delivery, failure, retry, and eventual consistency?
8. Which C4 view changed, and who is the audience for that view?
9. Which architecture test or Spring Modulith rule enforces the decision?
