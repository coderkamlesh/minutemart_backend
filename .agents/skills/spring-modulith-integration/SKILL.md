---
name: spring-modulith-integration
description: Implements or reviews Spring Modulith module contracts, named interfaces, application events, transactional listeners, event publication, and synchronous in-process integration for this project. Use when modules need to communicate or when a contract changes.
---

# Spring Modulith Integration Skill

Use the version in `pom.xml` as the API authority. The current project uses Spring Modulith 2.0.8.

## Decide the integration style

Use an integration event by default when:

- The consumer can react after the producer commits.
- No immediate result is required.
- Eventual consistency is acceptable.
- More consumers may be added later.

Use a synchronous module API only when:

- The caller needs an immediate result or decision.
- Strong consistency is required and cannot be modeled as a workflow.
- The dependency is small, stable, and acyclic.

Record the choice when it affects consistency, failure behavior, or module autonomy.

## Event contract rules

Place the public contract in the owning module's `events` package and expose that package as a named interface. Prefer a small immutable record:

```java
public record OrderPlaced(UUID orderId, UUID customerId) {
}
```

Rules:

- Publish facts that happened, not commands disguised as events.
- Include stable identifiers and minimal facts only.
- Do not include JPA entities, aggregates, repositories, Spring types, or internal DTOs.
- Do not expose sensitive data unless the consumer is explicitly authorized to receive it.
- Keep handlers idempotent and define duplicate behavior.
- Evolve contracts compatibly. Treat breaking changes as an ADR and migration.

Keep local domain events private to the module. Map them to an integration event at the application boundary when other modules need the fact.

## Publish and consume

Publish through `ApplicationEventPublisher` after the owning aggregate has accepted the state transition. Cross-module consumers should normally use `@ApplicationModuleListener` so event processing has the intended transaction and publication-registry semantics.

A listener should:

1. Accept only the public event contract.
2. Translate the event into the consumer's local model.
3. Execute one focused reaction through a consumer-owned use case.
4. Be safe to retry.
5. Have tests for successful handling and failure/retry behavior when important.

Do not use a raw `@Async` or accidental `@EventListener` for durable business integration. If synchronous handling is intentional, state why its failure expands the producer transaction.

## Synchronous contract rules

Put the smallest interface or DTO in `api`, annotate it as a named interface, and restrict the consumer with an explicit dependency such as:

```java
@ApplicationModule(allowedDependencies = {"inventory :: api"})
package com.minutemart.quickcommerce.order;
```

The consumer calls a gateway/adapter. The supplier keeps implementation, persistence, and domain objects private. Do not pass a domain object across the boundary.

## Transaction and reliability review

Before finalizing integration, answer:

- When is the event published relative to the state change?
- Is the consumer eventually consistent or same-transaction?
- What happens if the listener fails?
- Can the event be retried or replayed?
- Is the listener idempotent?
- Is the event publication registry configured by the existing Modulith starter?
- Is a broker actually needed, or is in-process Modulith integration sufficient?

Do not add Kafka, RabbitMQ, an outbox library, or a custom event bus without an explicit operational decision. The existing `spring-modulith-starter-jpa` is already part of this project and should be understood before adding duplicate machinery.

## Verification

- Run `ApplicationModules.verify()` through the project's architecture test when requested.
- Assert event publication with `PublishedEvents` or `AssertablePublishedEvents`.
- Verify that the dependency is only on `module :: events` or `module :: api`.
- Check that no consumer imports a supplier implementation package or persistence type.
