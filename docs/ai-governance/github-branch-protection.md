# GitHub Branch Protection Recommendations

These are *recommended* settings for the repository. They must be applied by a repository administrator via the GitHub UI or API. The settings are not automatically enforced by the codebase.

## `main` Branch

- **Require pull request reviews before merging**
- **Require at least one approving review**
- **Dismiss stale pull request approvals when new commits are pushed**
- **Require status checks to pass before merging** (e.g., CI build, test suite)
- **Require conversation resolution** before merging
- **Require signed commits** (optional but adds security)
- **Restrict who can push** – only maintainers or designated CI bots
- **Enable branch protection rules** – block force pushes and deletion

## `release/*` Branches (e.g., `release/1.0`, `release/2.0`)

- Same as `main`, plus:
- **Require linear history** (no merge commits)
- **Require passing integration tests**

## AI‑Generated PRs

- Must **not** have auto‑merge enabled.
- Must pass all required status checks.
- Must have at least one human reviewer from the appropriate CODEOWNERS team.

## How to Apply

1. Navigate to **Settings → Branches** in the GitHub repository.
2. Add a **Branch protection rule** for `main` and each `release/*` pattern.
3. Configure the options above.
4. Save the rule.

These recommendations complement the governance model in `AGENTS.md` and ensure that AI‑generated changes cannot be merged without explicit human oversight.
