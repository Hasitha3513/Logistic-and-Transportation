# Destructive Change Policy

## Definition of Destructive Changes

Destructive changes are modifications that **remove or degrade** existing functionality, tests, contracts, or infrastructure. In this repository, a change is considered destructive when it:

- Deletes production source files (e.g., Java classes, configuration files).
- Deletes or disables test files or test cases.
- Removes or alters public REST endpoints, request/response schemas, or domain events.
- Modifies or deletes historical Flyway migrations (any migration that has already been applied in production or CI).
- Removes database tables/columns or changes column types in a way that would cause data loss.
- Removes or weakens authentication, authorization, validation, audit logging, or any security‑related code.
- Removes or changes dependency declarations that other modules rely on.
- Performs mass rename/move of packages or modules without explicit approval.

## Approval Process for Destructive Changes

1. **Explicit Task Manifest** – The task manifest must contain a section explicitly stating that a destructive change is required and must be approved.
2. **Human Review** – A designated reviewer (as defined in CODEOWNERS) must approve the change.
3. **Impact Analysis** – Provide a brief impact analysis describing:
   - What is being removed/changed.
   - Potential downstream effects.
   - Roll‑back plan.
4. **Verification** – Run all relevant CI tests, integration tests, and migration checks.
5. **Documentation** – Update relevant ADRs or documentation to reflect the change.

## Enforcement

- CI checks (documented in `github-branch-protection.md`) will **flag** any commit that deletes files in `src/main/java`, `src/main/resources`, `src/test/java`, or any Flyway migration under `src/main/resources/db/migration`.
- Agents must **stop** and raise a scope‑expansion request if a destructive change is detected but not authorized in the manifest.

---

*This policy is referenced by `AGENTS.md` and the AI governance workflow.*
