# Architecture Decisions and Change Control

**Activation:** For new modules, boundary changes, cross-module integration, persistence, security, deployment, and new dependencies

## When an ADR is required

Create a short ADR under `docs/adr/` before or with the implementation when a change:

- Creates, merges, splits, or renames a business module.
- Changes a module's public API, named interface, or integration event.
- Adds a synchronous dependency or changes event timing/consistency.
- Changes database ownership, schema strategy, transaction boundaries, or migration tooling.
- Adds a broker, external service, framework, persistence technology, or cross-cutting runtime mechanism.
- Changes authentication, authorization, sensitive data handling, or tenant isolation.
- Makes a module open or weakens an existing architecture rule.

Small implementation choices that do not affect a boundary do not need ceremony. The purpose of an ADR is to preserve context for decisions that future contributors might otherwise undo accidentally.

## ADR template

```markdown
# ADR-NNNN: Short decision title

## Status

Proposed | Accepted | Superseded

## Context

What problem, business requirement, quality attribute, or constraint requires a decision?

## Decision

What is the chosen approach and which module owns it?

## Alternatives considered

What credible alternatives were rejected and why?

## Consequences

What coupling, consistency, operational, testing, and future-extraction consequences follow?

## Enforcement

Which package boundary, Spring Modulith rule, architecture test, integration test, or review check enforces this decision?
```

## Decision quality

- Start with business and quality drivers, not technology preference.
- Prefer the smallest contract that satisfies the interaction.
- Prefer compile-time visibility and automated architecture tests over review-only conventions.
- Do not copy a pattern from Simon Brown, Kamil Grzybek, or Spring Modulith mechanically. Adapt it to the actual QuickCommerce domain and document meaningful deviations.
- If an exception is necessary, make the exception narrow, named, tested, and time-bounded where possible.
