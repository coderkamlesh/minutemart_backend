# Testing and Architecture Verification

**Activation:** For test, feature, module, persistence, and integration changes

## Test the architecture

Architecture is executable policy, not a diagram only. Maintain a Spring Modulith architecture test once application modules exist:

```java
class ArchitectureTests {

    @Test
    void applicationModulesShouldBeValid() {
        ApplicationModules.of(QuickcommerceApplication.class).verify();
    }
}
```

Add focused ArchUnit rules only for constraints that Spring Modulith does not express, such as domain package independence from Spring or a forbidden dependency on a technical package. Keep custom rules small and explain why they exist.

## Test levels

### Domain unit tests

- Test aggregates, value objects, policies, and domain services without starting Spring.
- Cover invariants, state transitions, invalid commands, and domain events.
- Prefer behavior-based assertions over field-by-field implementation assertions.

### Application and module integration tests

- Use `@ApplicationModuleTest` when testing a module with its Spring wiring, use cases, persistence, or event listeners.
- Test a module through its public use-case or API boundary, not by coupling tests to internal implementation classes unnecessarily.
- Use `PublishedEvents` or `AssertablePublishedEvents` to verify integration event contracts.
- Include failure, retry, idempotency, and eventual-consistency scenarios for important listeners.

### Web tests

- Test HTTP mapping, validation, authentication/authorization behavior, and response contracts.
- Do not repeat all domain tests through HTTP. The web adapter must stay thin.

### Persistence and system tests

- Test module-owned persistence mappings and migrations at the infrastructure boundary.
- Test module interaction separately from full end-to-end tests when possible.
- Keep end-to-end tests for critical user journeys and deployment-level concerns, not as the only evidence of correctness.

## Test placement and visibility

- Keep tests close to the module they exercise.
- Package tests that need package-private access in the same package as the production code.
- Do not make production types public only to simplify tests.
- Do not share fixtures that smuggle one module's entities or state into another module.

## Verification discipline

- Run the smallest relevant verification requested by the user.
- For a boundary change, architecture verification is mandatory before considering the change complete.
- For an event change, test publication and listener behavior, including transaction and retry assumptions.
- For a persistence change, test schema/mapping behavior and module ownership.
- If tests are not run, state that explicitly and do not report the change as fully verified.
