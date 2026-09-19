# Java and Spring Coding Rules

**Activation:** For Java, Spring, persistence, web, and configuration changes

## Use-case design

Model application behavior as explicit use cases. A use case should have one clear responsibility and should coordinate the domain rather than duplicate domain rules.

- Prefer one command or query handler per meaningful use case.
- Use command/query separation when read and write behavior have different models, consistency needs, or performance characteristics. Do not introduce CQRS ceremony for trivial code without a reason.
- Commands may return a small result when the caller genuinely needs it, such as a newly created identifier. A command result must not expose a domain aggregate.
- Queries must not mutate state. Use read DTOs or projections owned by the module.
- Put transaction boundaries at the application use-case boundary, not in controllers or domain objects.

## Domain model

- Use the bounded context's ubiquitous language in type and method names.
- Keep business invariants inside aggregates, entities, value objects, or domain services.
- An aggregate protects its own invariants and exposes intention-revealing behavior, not public setters.
- Prefer immutable value objects for concepts with validation or meaning.
- Use domain services only when behavior does not naturally belong to one entity or value object.
- Keep domain events local to the domain model until the owning application layer deliberately maps them to an integration event.
- Avoid primitive obsession where a value object makes a rule explicit. Do not create value objects merely to wrap every primitive.
- Avoid an anemic domain model for non-trivial business behavior.

## Spring boundaries

- Controllers are thin primary adapters: authenticate/authorize, bind and validate input, invoke a use case, and map the result to an HTTP response.
- Controllers must not contain business decisions, repository calls, transaction orchestration, or cross-module collaboration.
- Keep HTTP request/response DTOs in the web adapter or a deliberate API contract package. Never expose JPA entities.
- Validate transport shape at the web boundary and enforce business invariants again in the domain. Bean Validation is not a replacement for domain rules.
- Prefer constructor injection.
- Keep configuration close to the module it configures. Do not centralize all module wiring in the application class.
- Keep security policy at the system/web boundary, but keep authorization decisions that depend on domain state in the owning module.

## Persistence

- Domain code must not depend on JPA annotations, Spring Data interfaces, database types, or transaction APIs.
- Define persistence ports at the application/domain boundary and implement them in infrastructure.
- Keep JPA entities and Spring Data repositories inside the owning module's infrastructure package.
- Map persistence models to domain models or use a deliberate persistence mapping strategy. Do not leak managed entities outside the transaction or module.
- Do not use `ddl-auto` as a production schema-management strategy. Use versioned migrations when database schema management is introduced, and record the selected tool in an ADR.
- A repository is not a module API. Another module must request behavior through a contract or consume an event.

## Code quality

- Keep classes focused and methods intention-revealing.
- Prefer explicit types and meaningful names over generic `Manager`, `Helper`, `Utils`, or `Data` names.
- Use Lombok only when it does not hide invariants, constructors, visibility, or lifecycle behavior.
- Avoid speculative abstractions and generic base classes.
- Do not add comments that restate code. Add comments only for non-obvious business or architectural reasoning.
