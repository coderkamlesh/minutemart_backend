---
name: architecture-review
description: Reviews QuickCommerce changes for modular-monolith violations, Spring Modulith boundary breaks, coupling, data ownership leaks, domain leakage, security risks, and missing architecture tests. Use when reviewing a diff, feature, module design, or pull request.
---

# Architecture Review Skill

Review for defects and boundary regressions first. Do not begin with a summary.

## Review order

1. Read the project rules and relevant ADRs.
2. Inspect the complete diff and surrounding package structure.
3. Identify the owning module and all changed contracts.
4. Trace imports and dependency direction.
5. Check persistence and transaction boundaries.
6. Check event semantics, idempotency, and failure behavior.
7. Check tests and architecture enforcement.
8. Check security and sensitive-data exposure.

## Findings checklist

### Module boundaries

- Does the change belong to the correct bounded context?
- Does any module import another module's internal package?
- Does it create a cycle or an unexplained dependency?
- Is a public type exposed without being a deliberate contract?
- Is an `OPEN` module or wildcard dependency used as a shortcut?

### Data and transactions

- Does a module access another module's repository, entity, table, schema, or persistence DTO?
- Are cross-module foreign keys or shared mutable models introduced?
- Does one transaction directly mutate multiple module-owned aggregates?
- Is eventual consistency explicit where events are used?

### Domain and application design

- Are invariants in the domain model rather than controllers or application handlers?
- Are aggregates protected from public setters and invalid state?
- Does the domain depend on Spring, JPA, HTTP, or infrastructure?
- Are bounded-context concepts incorrectly shared?

### Integration

- Is the event small, immutable, and owned by the supplier?
- Is a domain event incorrectly exposed as an integration contract?
- Is event delivery retry-safe and idempotent?
- Is a synchronous call justified, minimal, and restricted to `api`?
- Is a raw asynchronous listener being used without publication/retry semantics?

### Verification

- Does `ApplicationModules.verify()` cover the changed arrangement?
- Are named interfaces and `allowedDependencies` accurate?
- Are domain, module integration, event, and web tests appropriate to the change?
- Is an ADR required and present?

## Finding format

Report findings in descending severity:

```text
[Blocker] path/to/File.java:42 - Short title
Why: concrete architectural or behavioral risk.
Fix: smallest safe correction.
```

Use `High`, `Medium`, and `Low` only when the issue is not a blocker. Include file and line references whenever possible. Distinguish actual findings from questions and residual risks. If no findings exist, state that explicitly and mention untested or unenforced areas.

## Review conclusion

After findings, provide only a short change summary, open questions, and verification status. Never call a change safe solely because the application compiles; architecture and ownership rules must also be enforced.
