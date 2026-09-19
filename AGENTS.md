# QuickCommerce Agent Rules

This repository is a Java 21 Spring Boot 4.0.8 application using Spring Modulith 2.0.8. It is being built as a domain-centric modular monolith for the QuickCommerce domain.

This file is the OpenCode entrypoint. The canonical, shared project guidance lives under `.agents/` and is also consumed by Google Antigravity.

## Required context

Before changing code, read these files:

- `.agents/rules/00-project-context.md`
- `.agents/rules/10-modular-monolith.md`
- `.agents/rules/15-c4-ddd-modular-monolith.md`
- `.agents/rules/20-spring-modulith.md`
- The task-specific rule under `.agents/rules/`
- The relevant skill under `.agents/skills/`

If a referenced rule conflicts with an explicit user requirement, stop and explain the conflict before weakening an architectural boundary.

## Non-negotiable architecture rules

- Keep one deployable application. Business capabilities are application modules, not separate runtime services or technical layer packages.
- Create modules around bounded contexts and business capabilities. Do not create global `controller`, `service`, `repository`, `entity`, or `util` packages.
- A module owns its domain behavior, application use cases, persistence state, and external adapters.
- A module exposes only deliberate contracts through named interfaces or module API packages. Other modules must never use internal classes, repositories, JPA entities, tables, or schemas directly.
- Prefer asynchronous module integration through small Spring application integration events. Use synchronous in-process calls only through an explicit public contract when immediate consistency or a return value is genuinely required.
- Keep module dependencies acyclic. Every dependency must be intentional, minimal, and enforced with Spring Modulith verification.
- Keep domain code independent of Spring, JPA, HTTP, security, messaging, and other infrastructure concerns.
- Make types package-private by default. Public types are architectural surface area and require a reason.
- Put business invariants in aggregates, entities, value objects, or domain services. Controllers and application handlers orchestrate; they do not own domain rules.
- Keep integration events immutable, small, stable, and free of entities or persistence models.
- Never share mutable state or database tables between modules. Cross-module consistency is achieved through contracts, not shortcuts.

## Working rules

- Inspect the existing project and dependency versions before selecting an API or adding a dependency.
- Do not silently introduce a framework, messaging broker, migration tool, mediator, or shared library. Record the decision first when it changes architecture or operations.
- Do not run builds or tests unless the user explicitly requests them. When verification is not run, say so clearly.
- Do not claim that an architecture rule is enforced until a code-level test or Spring Modulith verification actually enforces it.
- Keep code, comments, documentation, and skill files in English. Communicate with the user in Hinglish when appropriate.

## Shared guidance

The detailed rules and workflows are in `.agents/rules/` and `.agents/skills/`. Do not duplicate them into a new competing rule system.
