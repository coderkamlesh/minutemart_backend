# QuickCommerce Agent Guidance

This directory is the shared project guidance for OpenCode and Google Antigravity.

## Layout

- `rules/` contains persistent project constraints. Configure the numbered rules as workspace rules in Antigravity. The first three are always-on; the task-specific rules should be enabled when their scope matches the work.
- `skills/` contains reusable, on-demand workflows. Each skill follows the Agent Skills `SKILL.md` format and is discoverable by both tools through `.agents/skills/`.

## Architecture baseline

QuickCommerce is a single Spring Boot deployment organized as a domain-centric modular monolith. The main package is `com.minutemart.quickcommerce`. Direct subpackages below it are Spring Modulith application modules. Modules are bounded by business capability, encapsulated by package visibility and named interfaces, and integrated through explicit contracts.

## Tool compatibility

- Google Antigravity discovers workspace rules under `.agents/rules/` and workspace skills under `.agents/skills/`.
- OpenCode discovers skills under `.agents/skills/`. The root `AGENTS.md` is the OpenCode bridge and instructs OpenCode to load the detailed rules from this directory.

## Authority

The guidance is based on the following references:

- Simon Brown, C4 model and modular monolith principles: <https://www.youtube.com/watch?v=5OjqD-ow8GE>
- Kamil Grzybek, Modular Monolith series: <https://www.kamilgrzybek.com/blog/series/modular-monolith>
- Kamil Grzybek, Modular Monolith: A Primer: <https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer>
- Kamil Grzybek, Architecture Enforcement: <https://www.kamilgrzybek.com/blog/posts/modular-monolith-architecture-enforcement>
- Kamil Grzybek, Integration Styles: <https://www.kamilgrzybek.com/blog/posts/modular-monolith-integration-styles>
- Kamil Grzybek, Domain-Centric Design: <https://www.kamilgrzybek.com/blog/posts/modular-monolith-domain-centric-design>
- Spring Modulith 2.0 documentation: <https://docs.spring.io/spring-modulith/2.0/reference/>

These references provide principles, not permission to copy a design without considering this application's business requirements and operational constraints.
