# AI Governance Documentation

This directory contains the governance framework that regulates how AI coding agents (ChatGPT Codex, Google Antigravity, and future tools) may interact with the Transport & Logistics Management System repository.

## Contents

- `README.md` – Overview of the governance workflow.
- `agent-task-scope-template.md` – Template for defining the explicit scope of an agent task.
- `examples/example-agent-task-scope.md` – Example manifest for a Fuel module bug‑fix.
- `agent-completion-report-template.md` – Required report format agents must emit after completing a task.
- `destructive-change-policy.md` – Definition of destructive changes and required approval steps.
- `agent-incident-process.md` – Procedure for handling incidents where an agent violates the policy.
- `google-antigravity-rules.md` – Specific operational rules for Antigravity agents.
- `codex-rules.md` – Specific operational rules for ChatGPT Codex agents.
- `github-branch-protection.md` – Recommended GitHub branch‑protection settings (documented only).
- `CODEOWNERS-recommendations.md` – Suggested CODEOWNERS entries for protected areas (real usernames to be supplied by admins).

All files are pure markdown and reside under `docs/ai-governance/`. They are referenced by `AGENTS.md` and should be reviewed by humans before any AI‑generated change is merged.
