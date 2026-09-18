# Remaining MVP Independent Implementation Authorization Proposal

## Purpose

US-55 post-extension technical closure is complete, while its genuine provider/device acceptance
remains externally blocked. The authoritative roadmap currently selects no independent executable
task. This proposal consolidates the decision needed to resume implementation without fabricating
an approved task identifier or treating an external acceptance hold as a technical failure.

## Current governed state

- Completed stories: 73/87; remaining: 14/87.
- Flyway head: V100.
- Open external acceptance: `US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001`.
- Other Tracking external holds: US-48, US-50, US-52, US-53 and US-54.
- US-51 is blocked by the absence of an accepted engine-state source or proxy.
- No existing task document authorizes the product decisions needed for US-72, US-76 or Wave E.

## Superseded recommendation

The prior recommendation to begin US-72 is superseded by the explicit selection of US-51.
The active governance package is
`docs/product-decisions/US-51-MONITOR-IDLE-TIME-PREREQUISITE-AND-PRODUCT-DECISIONS-001.md`.
US-72 remains a later candidate with its policy-authority blocker unchanged.

## Prior candidate analysis: US-72 Enforce Compliance

The roadmap orders US-72 before US-76 and Wave E. US-47 already supplies the required billing/tax
facts, but policy authority remains missing. Authorization should begin with product decisions,
not schema or implementation.

### Scope requiring approval

- Supported Phase-1 jurisdictions and authoritative policy owner.
- Typed compliance checks for Vehicle, Driver, cargo/hazmat, billing/tax, regional operation and
  retention; each domain remains owner of its facts.
- Effective dating, applicability and policy-version lifecycle.
- Exact effects: advisory, restrict or block; fail-closed versus fail-open per check.
- Override, appeal, expiry and segregation-of-duties rules.
- Evidence, audit, privacy and retention requirements.
- Explicit exclusions, including a generic expression-language rules engine and direct foreign
  persistence access.

### Proposed bounded architecture

- A dedicated `compliance` decision/evidence context orchestrates typed published facts and owns
  decisions, policy versions, override/appeal evidence and query projections.
- Source modules retain their records and publish or expose narrow Tenant-qualified contracts.
- No cross-module JPA relationship, repository access, SQL join or physical foreign key.
- P1-01 durable events carry only approved minimized decision facts where downstream notification
  or workflow integration is authorized.

### Proposed public contracts for review

No contract is authorized by this proposal. A product-decision task should freeze:

- policy catalogue/version management commands;
- a Tenant-qualified compliance evaluation command/query family;
- typed domain fact ports and their owners;
- decision, override and appeal responses;
- durable event names, versions, payload minimization and idempotency;
- error, pagination and optimistic-concurrency behavior.

### Proposed persistence boundary

No migration is authorized yet. After contracts freeze, one forward-only migration would likely be
needed for Tenant-leading policy/version, decision/evidence, override/appeal, history and durable
idempotency structures. The exact next migration must be determined from the then-current Flyway
head and approved DDL; V101 must not be reserved merely by this proposal.

### Proposed RBAC/ABAC and segregation of duties

Permissions are not authorized yet. The decision task should determine distinct view, policy
management, evaluation/operations, override and appeal/review authorities; same-Tenant enforcement;
contextual ABAC; and whether the requester may approve an override or appeal.

### Required verification and acceptance criteria

- Domain rule and application orchestration tests for every approved typed check and effect.
- PostgreSQL migration, constraints, idempotency, effective dating, concurrency and Tenant A/B.
- Literal HTTP RBAC/ABAC/SoD, pagination, privacy and safe-error tests.
- Contract tests for every producer fact and durable consumer.
- Architecture/Modulith/table-ownership tests proving no foreign persistence access.
- Full Maven, static analysis, dependency and Compose validation.
- Frontend component, accessibility, TypeScript, build and real Chromium journeys if UI is approved.
- Deterministic acceptance journeys for allow/restrict/block, override, appeal, policy replacement,
  stale/missing facts and foreign-Tenant denial.
- Regulatory/tax policy-owner sign-off; controlled fixtures must not be presented as legal or
  jurisdictional certification.

## Other candidates and why they cannot start

| Candidate | Missing authorization or prerequisite |
| --- | --- |
| US-51 Monitor Idle Time | Accepted engine-state signal/proxy, gap semantics and fuel-estimate ownership |
| US-76 Support Mobile Operations | PWA/native choice, role/workflow matrix, device binding, offline protection, push/background policy and physical-device plan |
| US-85 Protect Data Integrity | Owner-by-owner invariant/correction catalogue, published validators and complete GPS/trip mismatch dependency |
| US-84 Handle Global System Failures | Responsibility matrix, supported degraded modes, monitoring/fault environment and truthful RTO/RPO basis |
| US-87 Detect User Risk | Explainable signal/enforcement catalogue, IdP/MFA boundary, review/appeal and retention policy |
| US-82 Use Operational Analytics | Frozen KPI lineage, producer/data-quality gates and deterministic forecast/recommendation governance |
| US-86 Handle Operational Disruptions | Disruption catalogue, constraint/replan authority and remaining scheduling/compliance dependencies |

## Decision required

Approve a governed US-72 product-decision task with the boundaries above and supply or designate
the regulatory/tax policy authority. Until then, preserve the external acceptance queues and do
not create a migration, permission, API, event or implementation task.
