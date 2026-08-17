# Agent Incident Process

## Definition of an Incident

An incident occurs when an AI coding agent performs any of the following prohibited actions:

- Modifies or deletes unrelated production code.
- Deletes or weakens tests.
- Alters historical Flyway migrations.
- Breaks module boundaries or public API contracts.
- Removes or weakens security checks.
- Produces a diff that exceeds the declared scope without prior approval.

## Immediate Response

1. **Stop the PR** – Mark the pull request as blocked.
2. **Preserve Evidence** – Keep the branch and commit history intact; do not revert automatically.
3. **Notify Stakeholders** – Add a comment tagging the responsible team/reviewer.
4. **Create Incident Report** – Use `docs/ai-governance/agent-incident-process.md` to document:
   - What was changed.
   - Which guardrail was violated.
   - Why the agent performed the action.
   - Suggested corrective action.
5. **Rollback** – Manually revert the offending commit(s) if necessary.

## Root‑Cause Analysis

- Review the task manifest and AGENTS.md compliance.
- Identify missing or ambiguous stop‑condition specifications.
- Determine if the agent mis‑interpreted the scope.

## corrective Actions

- Update `AGENTS.md` or the task‑scope template to close the gap.
- Add or tighten CI guardrails (e.g., diff‑size checks, test‑deletion detection).
- Provide additional training or prompt guidance to the agent.
- If recurring, consider restricting the agent's permissions for the repository.

---

*All incidents must be reviewed by a human before any further AI‑generated changes are allowed.*
