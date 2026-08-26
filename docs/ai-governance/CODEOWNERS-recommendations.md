# CODEOWNERS Recommendations

The following entries are suggested to protect critical areas of the repository from unintended AI‑generated changes. Replace `@team` or `@username` with actual GitHub team or user handles used by the organization.

```
# Protect architecture and governance documentation
/docs/adr/* @architecture-team
/docs/ai-governance/* @ai-governance-team
/AGENTS.md @ai-governance-team

# Protect database migrations and security code
/src/main/resources/db/migration/* @db-team
/src/main/java/com/transportlogistics/app/identity/** @security-team
/src/main/java/com/transportlogistics/app/** @backend-team

# Protect test suites
/src/test/java/** @qa-team
```

**Notes for repository administrators**:
- Ensure the referenced teams/users exist and have write access.
- Adjust paths if the project structure changes.
- These entries are *recommendations* only; they must be added to the repository's `.github/CODEOWNERS` file manually.

---

*This file is placed under `docs/ai-governance/` and referenced by `AGENTS.md`.*
