---
name: modular-feature
description: Implements a QuickCommerce feature inside the correct Spring Modulith business module without leaking domain logic, persistence, or contracts across module boundaries. Use when adding or changing a business capability, use case, endpoint, aggregate behavior, or module-owned persistence.
---

# Modular Feature Skill

Use this skill for every feature that changes business behavior.

## 1. Establish context

Read the project rules in `.agents/rules/00-project-context.md`, `.agents/rules/10-modular-monolith.md`, `.agents/rules/20-spring-modulith.md`, and `.agents/rules/30-java-spring.md`. Then inspect:

- `pom.xml` and active application configuration
- The candidate owning module and its `package-info.java`
- Existing API and event contracts
- Existing architecture tests and relevant module tests
- Relevant ADRs and domain documentation

Do not create a new module until the owning business capability is clear.

## 2. Choose the owner

Write down, before coding:

- The bounded context or business capability that owns the behavior
- The aggregate or domain concept whose invariant changes
- The use case being added
- The module state that changes
- Other modules that need to know about the outcome

If ownership is ambiguous, stop and ask a focused question or propose an ADR. Do not put the feature in a convenient global service.

## 3. Design the slice

Prefer a vertical use-case slice inside the owning module:

```text
web adapter -> application use case -> domain behavior -> persistence port
                                                    -> domain event
application event publication -> other module listeners
```

Use the module's existing internal structure. Do not add empty layers. For a non-trivial write:

1. Define an input command and a small result.
2. Validate input shape at the adapter boundary.
3. Load the aggregate through a module-owned port.
4. Invoke intention-revealing domain behavior.
5. Persist the state within the application transaction.
6. Publish a minimal integration event if another module must react.

For a read, use a module-owned read model or projection. Do not query another module's tables.

## 4. Implement boundaries

- Keep controllers thin and module-local.
- Keep use-case handlers focused and package-private unless they are deliberate contracts.
- Keep domain code framework-independent.
- Keep JPA and external clients in infrastructure.
- Add public types only to `api` or `events` when they are contracts.
- Add a named interface and `allowedDependencies` only when another module truly needs the contract.
- Map all external DTOs, events, and persistence records into the owning module's language.

## 5. Integrate modules

Prefer an immutable integration event and `@ApplicationModuleListener` for cross-module reactions. Use a synchronous `api` contract only when the feature requires immediate data or a result. Never call another module's repository, entity, aggregate, or internal service.

Keep events small and idempotent. Describe eventual consistency and failure handling in the implementation or ADR when it is not obvious.

## 6. Verify the change

Add or update:

- Domain unit tests for new invariants
- Module integration tests for the use case and persistence boundary
- Event publication/listener tests for cross-module behavior
- Web tests only for HTTP behavior that is not covered below the adapter
- Architecture verification when packages, module dependencies, or public contracts change

Do not run verification unless the user requests it. If it is not run, report that fact.

## Completion checklist

- Correct business module owns the behavior
- No global technical package was introduced
- No cross-module internal or database access exists
- Public contract is minimal and named
- Domain rules are not in controllers or handlers
- Transaction boundary is explicit
- Integration style is justified
- Tests and ADRs are updated as required
