# ADR-001: Governance Model for AI Coding Agents

**Status:** Accepted

**Date:** 2026-08-17

**Deciders:** Engineering Lead / Team

**Scope:** All repositories where AI coding agents such as ChatGPT Codex, Google Antigravity, or future agentic development tools generate, modify, refactor, test, or review code.

---

## Context

Autonomous or semi‑autonomous coding agents are employed for a wide range of tasks: feature development, bug fixes, refactoring, testing, documentation, architecture work, and implementation delegation.  Over time, the following governance problems have been observed:

1. Deleting or silently rewriting completed, working code while performing unrelated tasks.
2. Overwriting existing naming conventions, formatting, architectural patterns, package structures, or coding styles.
3. Producing large, multi‑file diffs that are difficult for humans to review.
4. Opportunistic refactoring outside the requested scope.
5. Weakening or deleting tests when they conflict with the agent’s implementation.
6. Modifying historical database migrations.
7. Introducing architecture changes while supposedly implementing a small feature.

These are *process* problems; the agents are implementation assistants, not architectural authorities.

---

## Decision Drivers (Non‑Negotiable)

1. **Code Protection** – Completed and tested code must never be deleted or rewritten without explicit task scope and human review.
2. **Standard Preservation** – Existing coding standards, formatting, and naming must be preserved.
3. **Repository Authority** – The repository’s existing architectural decisions, ADRs, and module boundaries are authoritative.
4. **Reviewability** – Every significant agent‑generated change must be reviewable through a Git diff.
5. **Human Approval** – Human approval controls merge decisions; passing tests alone are insufficient.
6. **Vendor‑Agnostic** – The model must work across multiple AI agents and tools.
7. **Lightweight** – Guardrails should not unduly hinder productive AI‑assisted development.

---

## Governance Model (Layered Guardrails)

| Layer | Scope |
|------|-------|
| **1 – Repository‑level** | `AGENTS.md`, ADR hierarchy, CODEOWNERS, CI enforcement, Git‑hook policies. |
| **2 – Agent‑instruction‑level** | Explicit task‑scope manifests, mandatory stop‑conditions, required reports. |
| **3 – Process / Human‑review** | Human creates task manifest, reviews diff, approves merge, runs CI. |

All three layers are mandatory; no single layer is sufficient on its own.

---

## Required Artifacts

- **AGENTS.md** – Canonical operating rules for all AI agents.
- **Task Scope Manifest Template** – `docs/ai-governance/agent-task-scope-template.md`.
- **Example Task Manifest** – `docs/ai-governance/examples/example-agent-task-scope.md`.
- **Agent Completion Report Template** – `docs/ai-governance/agent-completion-report-template.md`.
- **Destructive‑Change Policy** – `docs/ai-governance/destructive-change-policy.md`.
- **Incident Process** – `docs/ai-governance/agent-incident-process.md`.
- **Vendor‑specific Rules** – `docs/ai-governance/google-antigravity-rules.md` & `docs/ai-governance/codex-rules.md`.
- **CI Guardrail Recommendations** – `docs/ai-governance/github-branch-protection.md` and related CI checks (documented only).
- **CODEOWNERS Recommendations** – `docs/ai-governance/CODEOWNERS-recommendations.md` (real owners to be supplied by admins).
- **AI Governance README** – `docs/ai-governance/README.md` describing the workflow.

---

## Verification & Safeguards

1. **Baseline Capture** – Record current branch, commit hash, `git status`, and any uncommitted changes (no destructive actions taken).
2. **Diff Audit** – After any agent‑generated change, run `git diff --stat` and ensure only files listed in the task manifest are modified.
3. **CI Checks** – Document lightweight CI scripts that:
   - Detect modifications to historical Flyway migrations.
   - Flag deletion of test files.
   - Enforce presence of a task‑scope manifest for PRs.
   - Warn on unexpectedly large diffs.
4. **Human Review** – Every PR containing agent‑generated changes must be manually reviewed and approved before merge.

---

## Periodic Review

Governance shall be reviewed **quarterly** or whenever any of the following occurs:

- Introduction of a new AI coding‑agent platform.
- A major incident involving an agent‑generated change.
- Significant architectural changes to the repository.
- Updates to tooling that affect how agents interact with the repo.

---

## Non‑Scope Statement

This ADR **does not**:

- Change any production application code.
- Modify REST API contracts.
- Alter database schemas or Flyway migrations.
- Upgrade dependencies or refactor business logic.
- Touch any files outside the `docs/` hierarchy (except `AGENTS.md`).

All other governance artifacts are confined to documentation and configuration directories.

---

*End of ADR-001*
