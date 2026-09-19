# Project Context

**Activation:** Always On

## Repository facts

- Project: QuickCommerce
- Java: 21
- Build: Maven Wrapper
- Spring Boot: 4.0.8
- Spring Modulith: 2.0.8
- Persistence: Spring Data JPA with PostgreSQL runtime driver
- Other current starters: Spring Security, Bean Validation, Spring MVC
- Main package: `com.minutemart.quickcommerce`
- Application class: `com.minutemart.quickcommerce.QuickcommerceApplication`
- Current state: starter application with no business modules implemented yet

The dependency versions in `pom.xml` are authoritative. Do not copy examples from a different Spring Boot or Spring Modulith release without checking API compatibility.

## Expected source arrangement

The application class remains at the root package. Each direct subpackage below that root represents one business application module once it contains business code.

```text
src/main/java/com/minutemart/quickcommerce/
  QuickcommerceApplication.java
  <business-module>/
    package-info.java
    api/
    events/
    application/
    domain/
    infrastructure/
    web/
```

The subpackages are a default shape, not a reason to create empty layers. A small module may keep a simpler vertical-slice arrangement. A complex module may split domain, application, and infrastructure further. The business boundary is mandatory; a particular internal folder layout is not.

## Project inspection

Before implementing a change:

1. Read `pom.xml`, the relevant `application*.yaml` or `application*.properties`, and the target module.
2. Identify the module that owns the business decision and its existing public contracts.
3. Inspect module dependencies before adding a new dependency.
4. Check for an existing ADR or architecture documentation before creating a new pattern.

## Commands

The Maven Wrapper is available as `mvnw.cmd` on Windows and `mvnw` on Unix-like systems. Typical commands are:

```text
./mvnw test
./mvnw verify
```

Do not run a build, test, or dependency download unless the user explicitly requests it. If a command is requested, prefer the smallest relevant verification first.

## Dependency discipline

- Use the versions managed by the Spring Boot parent or the Spring Modulith BOM.
- Do not add libraries for patterns that can be implemented with existing Spring facilities without first recording why the library is needed.
- Do not add a broker, migration tool, query framework, CQRS library, or distributed transaction mechanism as a default architectural reflex.
- If a new dependency changes runtime behavior or deployment, create an ADR before implementation.

## Documentation language

Code, comments, ADRs, architecture documents, rule files, and skill files must be written in English. User-facing explanations may be Hinglish.
