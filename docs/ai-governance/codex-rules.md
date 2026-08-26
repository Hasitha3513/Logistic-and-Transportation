# Codex Rules

These rules are to be enforced by the ChatGPT Codex agent when operating on this repository.

## Required Behaviors

1. **Read AGENTS.md** before any code generation or modification. Follow its rules strictly.
2. **Obey Task Scope Manifest** – only edit files/directories listed under `Allowed Files`/`Allowed Directories`. If a needed change lies outside, stop and request scope expansion.
3. **No Unrelated Deletions or Renames** – any attempt to delete, rename, move, or rewrite files not explicitly permitted must be halted.
4. **Preserve Formatting & Conventions** – do not run bulk formatters or lint fixers unless explicitly asked.
5. **Do Not Touch Historical Flyway Migrations** – modifications to migrations that pre‑date the current change must trigger a stop condition.
6. **Never Disable Tests** – removing or commenting out test code is prohibited without explicit approval.
7. **Respect Architecture Boundaries** – do not introduce cross‑module dependencies or relocate domain logic.
8. **Diff Discipline** – after changes, produce a diff limited to the files declared in the manifest and present it for human review.
9. **Stop on Large Diff** – if the diff exceeds a reasonable size for the declared task (e.g., > 500 lines), halt and request clarification.
10. **Human Approval Required** – create a pull request and await human review; never merge or push autonomously.

## Prohibited Actions (must cause immediate stop)

- Modifying any file under `src/main/java` not listed in the allowed modules.
- Changing any existing Flyway migration under `src/main/resources/db/migration`.
- Deleting any test file under `src/test/java`.
- Altering public API contracts (controllers, DTOs) without explicit permission.
- Introducing new Maven dependencies not declared in `pom.xml`.
- Executing destructive Git commands (reset, clean, force‑push) without explicit human instruction.

These rules complement the generic governance in `AGENTS.md` and ensure Codex conforms to repository policies.
