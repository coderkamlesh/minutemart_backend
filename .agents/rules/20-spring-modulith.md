# Spring Modulith Rules

**Activation:** Always On for Java and test changes

## Module detection

The application main package is `com.minutemart.quickcommerce`. Spring Modulith's default arrangement treats direct subpackages as application modules. Keep the main application class at the root package.

When the first real application module is introduced:

- Annotate the application class with `@Modulithic` in addition to `@SpringBootApplication`.
- Add or maintain an architecture test that calls `ApplicationModules.of(QuickcommerceApplication.class).verify()`.
- Make module dependencies explicit with `@ApplicationModule(allowedDependencies = ...)` in each module's `package-info.java` where the dependency graph is non-trivial.

Do not switch to custom or explicitly annotated module detection unless the package arrangement requires it and the decision is documented.

## Named interfaces

Use `@NamedInterface` for contracts in subpackages that are intentionally visible to other modules. The conventional contract packages are:

```java
@org.springframework.modulith.NamedInterface("api")
package com.minutemart.quickcommerce.catalog.api;
```

```java
@org.springframework.modulith.NamedInterface("events")
package com.minutemart.quickcommerce.catalog.events;
```

Then restrict consumers to the smallest contract they need, for example:

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"catalog :: events"}
)
package com.minutemart.quickcommerce.inventory;
```

Use the exact module name detected by Spring Modulith. Do not use `*` unless the module genuinely needs every named interface and the reason is documented.

## Verification guarantees

`ApplicationModules.verify()` must remain green. It is expected to catch:

- Cycles at application-module level.
- References to another module's internal packages.
- References outside explicitly allowed dependencies when those are declared.

Do not suppress, filter, or ignore a violation to make a change pass. Fix the dependency or record an approved architecture change first.

## Application events

Use Spring's application event mechanism and Spring Modulith event support for module integration.

- Publish an immutable integration event from the owning module after the owning state transition is valid.
- Keep integration events in the owning module's `events` named interface.
- Prefer Java records for new event contracts unless serialization or framework requirements justify another immutable type.
- Include identifiers and minimal facts required by consumers, not entities, repositories, internal DTOs, or a complete aggregate snapshot.
- Treat an event as a public contract. Keep it stable and evolve it compatibly.
- Consume cross-module integration events with `@ApplicationModuleListener` by default. This provides the intended asynchronous, transactional-after-commit module interaction and event publication registry integration.
- Make handlers idempotent. Event delivery and retry behavior must not create duplicate business effects.
- Document eventual consistency, retry, failure, and replay behavior for non-trivial listeners.

Do not use a raw `@Async` listener for business integration when the event publication registry or transaction semantics are required. Do not use a cross-module `@EventListener` as an accidental shortcut.

If strict same-transaction behavior is required, make it an explicit decision. Explain the failure coupling and transaction boundary instead of silently changing listener semantics.

## Synchronous contracts

When an in-process call is justified:

- Expose only a small interface in `api`.
- Keep the implementation in the owning module.
- Declare the dependency with `allowedDependencies`.
- Call through a consumer-side gateway or adapter.
- Return a contract DTO or scalar result, never a domain object or persistence entity.

## Module tests and documentation

- Use `@ApplicationModuleTest` for module integration tests where the module boundary and Spring configuration matter.
- Use `PublishedEvents` or `AssertablePublishedEvents` to assert published integration contracts.
- Generate or update module documentation when a module's public contract or interaction graph changes.
- Keep the Spring Modulith version in examples aligned with the version in `pom.xml`.
