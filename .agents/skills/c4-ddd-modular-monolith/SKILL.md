---
name: c4-ddd-modular-monolith
description: Applies C4 communication and domain-centric modular-monolith principles to QuickCommerce architecture decisions. Use when designing the system context, containers, business modules, or a significant module interaction.
---

# C4 and DDD Modular Monolith Skill

Use this skill when an architecture decision changes the system view, deployable shape, business module map, module contract, data ownership, or dependency graph. Read `.agents/rules/15-c4-ddd-modular-monolith.md` together with the core modular-monolith and Spring Modulith rules.

## Start at the right C4 level

State the level, audience, and question before designing:

- **C1, System Context:** Who uses QuickCommerce, what does it do, and which external systems does it rely on?
- **C2, Container:** What is deployable or operationally significant? Keep the single Spring Boot application, database, and external providers here.
- **C3, Component:** Which business capabilities are inside the application, and how do they interact through contracts or events?
- **C4, Code:** Which implementation detail is necessary to explain a non-obvious boundary or contract?

Do not use a lower-level diagram to make a higher-level decision. Do not present a logical module as an independently deployable service without an explicit operational decision.

## Discover the bounded context

Before creating a package or module, record:

1. Actors and business capabilities.
2. Commands, queries, decisions, and facts that happened.
3. Invariants and the aggregate or policy that protects each invariant.
4. State and data that must be owned exclusively.
5. Terms that have different meanings in neighboring contexts.
6. External systems and operational constraints.

Group concepts that share language, invariants, ownership, and change patterns. Reject boundaries created only by controllers, services, repositories, or other framework concepts.

## Design the module

For every proposed module, answer:

- What business responsibility does it own?
- Which aggregate or consistency boundary does it protect?
- What state and tables does it own?
- What is the smallest public API or event contract?
- Which modules consume that contract?
- Why is each dependency necessary?
- Is the dependency graph acyclic?
- Could the capability remain coherent if a network boundary were added later?

The extraction question is a design test, not a reason to split the application now.

## Select integration deliberately

Choose one integration style and record the reason:

- **Integration event:** The consumer needs a fact after the producer commits, no immediate result is needed, and eventual consistency is acceptable.
- **Synchronous module API:** The caller needs an immediate result or decision, or a strong consistency requirement cannot be modeled as a workflow. Keep the API small and the dependency acyclic.
- **Consumer-owned projection:** The consumer needs a read shape and can tolerate a separately maintained model. It must be built from contracts or events, not another module's tables.

For events, define publisher, consumer, transaction timing, retry behavior, duplicate handling, replay implications, and the minimal payload. For synchronous APIs, define the consistency reason and failure coupling.

## Implement and enforce

When the design is accepted:

- Create a direct subpackage module under `com.minutemart.quickcommerce`.
- Keep domain behavior and persistence ownership inside that module.
- Expose only deliberate `api` and `events` named interfaces.
- Add `package-info.java` and explicit `allowedDependencies` when required.
- Keep other modules away from implementation packages, repositories, entities, tables, and schemas.
- Update the relevant C4 view and add an ADR when the graph, contract, consistency, or ownership changes.
- Add or update `ApplicationModules.verify()` coverage when the first real module or a module dependency is introduced.
- Add domain tests for invariants and module/integration tests for contract behavior.

Never use an open module, wildcard dependency, shared table, cross-module foreign key, or public implementation class as a shortcut.

## Required design output

Return or document:

- C4 level, audience, and scope.
- Business ownership and bounded-context language.
- Aggregate/invariant ownership.
- Module data ownership.
- Public APIs and integration events.
- Dependency graph and consistency model.
- Failure, retry, idempotency, and eventual-consistency behavior.
- Rejected alternatives and extraction implications.
- C4 documentation and executable enforcement to be updated.
