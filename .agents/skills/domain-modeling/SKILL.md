---
name: domain-modeling
description: Designs or implements domain-centric Java models for QuickCommerce bounded contexts using aggregates, value objects, domain services, domain events, and persistence ignorance. Use when business rules, state transitions, or aggregate behavior are being added or reviewed.
---

# Domain Modeling Skill

Use this skill when a feature changes business meaning, not merely transport or persistence plumbing.

## Discover the model

Start from the business language:

- Identify commands, policies, decisions, and domain events.
- Separate facts from requests.
- Identify invariants that must always hold.
- Identify the aggregate responsible for each invariant.
- Identify concepts that belong to a different bounded context.
- Name types and behavior using the language of that context.

Do not create one universal model for the whole application. Similar words in different bounded contexts may represent different concepts.

## Design aggregates

An aggregate is a consistency boundary, not a database table grouping.

- Protect invariants inside the aggregate.
- Expose intention-revealing behavior such as `confirm()`, `cancel()`, or `reserve()`, not public setters.
- Keep references to other aggregates by identifier unless a same-boundary invariant genuinely requires more.
- Keep aggregate transactions short and owned by one module.
- Do not load or mutate another module's aggregate.
- Publish a domain event for meaningful state changes when local policies or integration mapping need it.

If an aggregate becomes large, split by invariant and transaction boundary rather than by arbitrary fields.

## Value objects and policies

Use value objects where they make validation, equality, formatting, or business meaning explicit. Keep them immutable. Use domain policies or services when a rule spans concepts but still belongs to the same bounded context.

Do not use a domain service as a dumping ground for behavior that belongs on an aggregate. Do not wrap primitives without a business reason.

## Application boundary

Application use cases coordinate:

1. Input validation that is not purely transport-level
2. Loading the owning aggregate through a port
3. Invoking domain behavior
4. Persisting the result
5. Mapping domain events to integration events

Application code must not reimplement aggregate invariants. Controllers must not implement use cases.

## Persistence ignorance

Keep domain types free of Spring and JPA dependencies. If JPA mapping constraints conflict with domain encapsulation, use a persistence model and mapper in infrastructure rather than adding public setters or framework annotations to weaken the domain.

Repositories are persistence ports for the owning module. They are not cross-module APIs.

## CQRS and vertical slices

Use a command model for behavior that changes state and a query/read model when the read shape differs materially from the write model. A query may use a module-owned projection. Do not introduce CQRS, a mediator, or generic handlers merely to follow a label.

Keep code close to its use case when that improves change locality. Shared domain abstractions are justified only when they represent a stable concept in the same bounded context.

## Domain tests

Write plain unit tests for invariants and state transitions. Test failure paths and boundary values. Avoid starting Spring or a database for domain rules that can be tested in memory.
