# Google Antigravity Rules

These rules are to be enforced by the Antigravity agent when operating on this repository.

## Required Behaviors

1. **Read AGENTS.md** before any code generation or modification. Treat the rules in that file as authoritative.
2. **Respect Task Scope Manifest** – only modify files/directories listed under `Allowed Files`/`Allowed Directories`. If a required change falls outside, the agent must stop and request scope expansion.
3. **Do Not Delete or Rewrite Unrelated Code** – any attempt to delete, rename, move, or rewrite files not explicitly permitted must be halted.
4. **Preserve Formatting & Conventions** – Antigravity must not run repository‑wide formatters or linters unless explicitly asked.
5. **Do Not Modify Historical Flyway Migrations** – any change to a migration file older than the latest version must trigger a stop condition.
6. **Never Disable Tests** – removing or commenting out test assertions is prohibited without explicit approval.
7. **Respect Architecture Boundaries** – do not introduce cross‑module dependencies or move domain logic across modules.
8. **Enforce Diff Discipline** – after modifications, Antigravity must produce a diff limited to the files declared in the manifest and present it for human review.
9. **Stop on Large Diff** – if the diff size exceeds a reasonable threshold for the declared task (e.g., > 500 lines), the agent must stop and request clarification.
10. **Human Approval Required** – the agent must not merge or push changes on its own; it must create a PR and await human approval.

## Prohibited Actions (must cause immediate stop)

- Modifying any file under `src/main/java` that is not part of the allowed modules.
- Changing any existing Flyway migration under `src/main/resources/db/migration`.
- Deleting any test file under `src/test/java`.
- Altering public API contracts (controllers, request/response DTOs) without explicit permission.
- Introducing new dependencies not declared in `pom.xml`.
- Running destructive Git commands (reset, clean, force‑push) without explicit human instruction.

These rules supplement the generic governance defined in `AGENTS.md` and ensure Antigravity behaves consistently across environments.
