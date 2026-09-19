# Remaining MVP Independent Implementation Authorization Proposal

## Purpose

US-55 post-extension technical closure is complete, while its genuine provider/device acceptance
remains externally blocked. The authoritative roadmap currently selects no independent executable
task. This proposal consolidates the decision needed to resume implementation without fabricating
an approved task identifier or treating an external acceptance hold as a technical failure.

## Current governed state

- Completed stories: 73/87; remaining: 14/87.
- Flyway head: V105.
- Open external acceptance: `US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001`.
- Other Tracking external holds: US-48, US-50, US-52, US-53 and US-54.
- US-51 is technically complete; production engine-running source activation and physical acceptance remain
  separately pending.
- US-72 CS01 inactive structural foundations are complete; D1–D11 and qualified policy authority still
  block CS02 and all runtime behavior.

## Current recommendation after US-87 prerequisite review

US-51 technical closure and US-72 CS01 are preserved at V105. The authorized documentation-only
`US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001` is complete. Its advisory-first D1–D20
package is recorded in `docs/product-decisions/US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001.md`.
No independently executable implementation task is authorized: product, security, privacy/records and the
first-wave source owner must approve the proposed signal catalogue, exact fact, threshold, review authority
and retention before proposed CS01 can start. No migration number is reserved.

## Previously selected candidate: US-72 Enforce Compliance

The roadmap orders US-72 before US-76 and Wave E. US-47 supplies the billing/tax fact boundary, and
US-74/75/77/80/81/83 plus Wave A foundations are complete. Externally blocked Tracking acceptances are not
dependencies for the proposed non-telemetry Phase-1 catalogue. Policy authority remains the blocker.

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
head and approved DDL; no migration number is reserved by this proposal.

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
| US-51 Monitor Idle Time | Technically complete; production source activation and physical acceptance remain separate gates |
| US-76 Support Mobile Operations | PWA/native choice, role/workflow matrix, device binding, offline protection, push/background policy and physical-device plan |
| US-85 Protect Data Integrity | Owner-by-owner invariant/correction catalogue, published validators and complete GPS/trip mismatch dependency |
| US-84 Handle Global System Failures | Responsibility matrix, supported degraded modes, monitoring/fault environment and truthful RTO/RPO basis |
| US-87 Detect User Risk | D1–D20 and the first exact source fact/threshold/reviewer/retention package require approval; proposed CS01–CS07 are not authorized |
| US-82 Use Operational Analytics | Frozen KPI lineage, producer/data-quality gates and deterministic forecast/recommendation governance |
| US-86 Handle Operational Disruptions | Disruption catalogue, constraint/replan authority and remaining scheduling/compliance dependencies |

## Decision required

Either approve the outstanding US-72 D1–D11 package with a qualified policy authority, or approve the
US-87 D1–D20 package with the named product, security, privacy/records and first-wave source authorities.
Until then, preserve all external acceptance queues and do not create a migration, permission, API, event
or implementation task.
