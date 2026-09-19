# US-87 Detect User Risk — Prerequisite and Product Decisions

**Task:** `US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001`

**Task status:** `COMPLETE` (documentation and governance analysis only)

**Decision package status:** `FIRST_WAVE_APPROVED / LATER_WAVES_UNRESOLVED`

**Implementation status:** `IMPLEMENTATION_IN_PROGRESS / CS01_COMPLETE`

**Baseline inspected:** application `52b748efe86e2bf8f7cc2bc434e02471ebd050bb`; Knowledge Base
`60dad2048bf07a7422674f7eca87d1b82fb8e223`; Flyway V105

**First-wave refinement baseline:** application `0ff49188889e9dfc9364683e7c0bfc133f3fe0e5`;
Knowledge Base `c6253c7321214f5e4b5f086b47ea4cf70ecf2cb5`

**First-wave approval status:** `APPROVED_FOR_CS01`

**Accounting:** 73 / 87 COMPLETE; 14 / 87 remaining

## 1. Purpose and authority boundary

The original US-87 requirement asks a System Administrator to detect unauthorized overrides, missing
mandatory fields, fraudulent activity, shared-login usage and delayed reporting, assign severity, preserve
an audit trail and optionally apply a configured security action. It does not define authoritative signals,
thresholds, evidence quality, review rights, retention or safe enforcement.

This document turns that intent into a concrete Phase-1 proposal. Nothing here authorizes production code,
schema, permissions, APIs, events, an Identity-provider integration or enforcement. Every choice marked
**PROPOSED** requires the approvals listed in section 8.

### 1.1 First-wave approval provenance

On 2026-09-19, the project user explicitly approved the exact first-wave package recorded at application
commit `c8e811f6c55ec91859680048c51b8e08b1ac4b35` and stated that they hold or have delegated approval
authority for Product, Security, Privacy/records, Identity source ownership, and Architecture/data. This is
one approval statement by the user; it does not assert separate named reviewers, signatures, or approvals.

That approval is limited to the Identity permission-ceiling denial signal and its four action codes, three
distinct facts per Tenant/actor/action family in 15 minutes, MEDIUM priority, five minutes of delivery
lateness, `ADVISORY_REVIEW_ONLY`, the minimized payload and duplicate/conflict rules, distinct same-Tenant
review, 180-day retention, and one internal appeal within 30 days. An indicator is not proof of malicious
intent or misconduct. No automatic account/session restriction, permission change, MFA challenge, or
Operations case is approved. D1-D20 choices outside this first wave remain proposed and unresolved.

US-87 is independent of physical Tracking acceptance and inactive US-72 Compliance for prerequisite
analysis. It may not inherit or clear either hold.

## 2. Verified current platform reality

| Capability | Verified current state | Planning consequence |
| --- | --- | --- |
| Identity and RBAC | Local username/password, BCrypt, short-lived signed JWT, hashed rotating refresh tokens, active server-side Tenant membership and permission reload on each bearer request | Useful for actor/Tenant facts. There is no approved risk-enforcement port. |
| Session capability | Stateless access tokens plus revocable/rotating refresh tokens | No server-side access-token session registry, device binding, global session revocation, step-up or risk challenge exists. |
| MFA / enterprise IdP | Not implemented in the inspected runtime | Reauthentication/MFA cannot be claimed or activated. An external IdP is a future prerequisite only if such effects are approved. |
| Authorization | Fail-closed HTTP rules and use-case permission checks; Identity prevents grants above the actor's permission ceiling | Source owners continue to reject unauthorized commands. A denial may become a minimized signal only through a separately approved contract. |
| Mandatory-field validation | Jakarta/web validation plus owner-domain invariants and the global API error contract | US-87 must not revalidate or repair commands. Owners may later publish an approved minimized rejection fact. |
| Audit | Accepted roadmap story, but implementation is feature-owned (for example Integration and Tracking audit stores); no generic cross-domain audit query repository was found | US-87 cannot scrape audit tables or assume a reusable enterprise audit feed. Each producer needs a published minimized signal contract. |
| Workflow | Accepted roadmap story, but no generic configurable workflow module/port was found; existing workflows are fixed in owning domains | Review lifecycle must be US-87-owned if approved; it cannot assume a generic workflow engine. |
| Operations | Accepted US-78 case lifecycle and a strict `OperationalExceptionFactV1` catalogue | The current catalogue does not admit Identity/user-risk source facts. No Operations integration is authorized by this package. |
| Tenancy | Server-resolved active membership/Tenant; Tenant-scoped persistence conventions | Every future signal, rule, finding, review and action must be explicitly Tenant-qualified. |
| Durable events | P1-01 outbox provides governed at-least-once publication and consumer idempotency | Reusable only after exact US-87 signal contracts and producers are approved. |

The roadmap labels for US-75 and US-80 are not evidence of generic runtime APIs. Feature-owned audit and
workflow remain authoritative unless a later approved change publishes a narrow contract.

## 3. Recommended Phase-1 scope

**PROPOSED:** an advisory-first, deterministic, human-reviewed Identity-owned risk-assessment feature.
It records explainable findings from explicitly registered, minimized source facts. It never repairs the
source action and never treats an indicator as proof of fraud or misconduct.

Recommended initial behavior:

1. Source authorization and validation continue to allow or reject the original command independently.
2. An approved source may publish a minimized fact after its own decision commits.
3. US-87 evaluates only the active effective-dated rule version for that Tenant and signal type.
4. Sufficient evidence opens or updates one explainable advisory finding; unknown, missing or conflicting
   evidence never becomes an affirmative allegation.
5. A qualified same-Tenant reviewer records disposition and reason. The subject and signal-producing actor
   cannot review their own finding.
6. Phase 1 produces no automatic lockout, access-token revocation, MFA challenge, disciplinary result,
   legal conclusion or Operations case.

The first usable release should activate only signal types whose owners, payloads, thresholds, reviewers,
retention and false-positive treatment have all been approved. Disabled signal types must remain visibly
`UNAVAILABLE`, not silently treated as no risk.

## 4. Concrete decision matrix

| ID | Decision area | Concrete recommendation (**PROPOSED**) | Material alternatives | Dependency and exact approval |
| --- | --- | --- | --- | --- |
| D1 | Owner and boundary | Implement a cohesive user-risk feature inside `identity`. Identity owns rule versions, findings, review disposition and future enforcement requests. Source modules retain validation, authorization, facts and correction. No foreign repositories, SQL or entities. | A separate top-level context would add a module boundary without a demonstrated need. | Product + architecture approve Identity ownership and published-port-only integration. |
| D2 | Signal catalogue | Freeze five structural types: `UNAUTHORIZED_OVERRIDE_ATTEMPT`, `MANDATORY_FIELD_REJECTION_PATTERN`, `BOUNDED_FRAUD_INDICATOR`, `SHARED_LOGIN_INDICATOR`, `DELAYED_REPORTING_INDICATOR`. Activate each independently; inactive means `UNAVAILABLE`. | Activate only the first type initially. | Product/security approve each active type and its owner; privacy approves its fields. |
| D3 | Unauthorized override | A fact is eligible only when an owner recognizes an explicit override command or protected action and denies it because the authenticated actor lacked that exact authority or violated an approved SoD rule. Ordinary 401/403 traffic, malformed requests and target-not-found responses are not override facts. The denial remains authoritative at source. | Include repeated generic authorization denial (higher false-positive risk). | Each owner approves its override action codes and safe fact; security approves threshold and exclusions. |
| D4 | Mandatory fields | Owners continue to reject missing fields. US-87 may consume only a safe, stable validation-rule code, actor, command type and time—never submitted values. Recommend an indicator only for a product-approved repeated pattern, not a single validation error. | Exclude this signal entirely from Phase 1. | Product approves eligible owner rule codes, time window and count; privacy approves minimization. |
| D5 | Fraud indicators | Use a closed catalogue of deterministic source-owned anomaly facts. Name findings `POTENTIAL_FRAUD_INDICATOR`; never `FRAUD_CONFIRMED`. No generic score over arbitrary transactions, no ML and no cross-domain data mining. Until a source catalogue is approved, this signal remains unavailable. | Defer all fraud indicators; later introduce an independently governed analytics feed. | Product, security and the source-domain owner approve every fact/threshold; legal/privacy review labels and access. |
| D6 | Shared login | Multiple sessions or a shared IP alone are insufficient. Recommend requiring concurrent active sessions plus a separately approved strong contradiction from an IdP/session/device authority. The current runtime has no approved device/session evidence, so this signal is `UNAVAILABLE` in the first technical release. | Treat multiple refresh-token families as an advisory signal (not recommended without device context). | Security/privacy approve evidence and threshold; real IdP/session capability and operational validation are required before activation. |
| D7 | Delayed reporting | Source owner supplies `activityOccurredAt`, `reportReceivedAt`, stable activity type, and an offline/synchronization status. Delay uses UTC instants and an owner-approved threshold plus offline grace. Missing clocks or offline state yield `UNKNOWN`; clock reversal yields `CONFLICTING`. No global threshold. | Exclude delayed reporting initially. | Every producer owner approves clock authority, threshold, offline allowance and safe fields. |
| D8 | Evidence state | Use `SUFFICIENT`, `INSUFFICIENT`, `UNKNOWN`, `CONFLICTING`, `STALE` as evidence quality, separate from finding state. Missing/conflicting/stale evidence cannot raise a new finding or trigger an effect. Store deterministic rule/result reason codes. | Treat missing evidence as risk (rejected for Phase 1). | Product/security approve fail-safe behavior. |
| D9 | Severity and false positives | Use `LOW`, `MEDIUM`, `HIGH`, aligned with advisory priority—not guilt or legal severity. A rule defines one severity deterministically. New findings start `OPEN_REVIEW`; reviewers choose `CONFIRMED_INDICATOR`, `FALSE_POSITIVE`, `INSUFFICIENT_EVIDENCE` or `CLOSED_NO_ACTION`, with reason. No `CRITICAL` in Phase 1. | Reuse Operations `CRITICAL` (not recommended); numeric score (not recommended). | Product/security approve mappings, thresholds and dispositions. |
| D10 | Effects | Phase 1 effect is `ADVISORY_REVIEW_ONLY`. Original source denial stays denied; allowed actions are not reversed. Reauthentication, refresh-token-family revocation, session restriction, lockout and Operations investigation are future individually controlled effects and default unavailable. No automatic punitive action. | Approve selected HIGH reauthentication after IdP support exists. | Product/security separately approve each effect, fallback, timeout, recovery and break-glass path; IdP capability acceptance required. |
| D11 | Review, appeal and SoD | Reviewer must be active, same-Tenant, authorized, and distinct from the subject and the actor whose action produced the signal. A reviewer cannot approve their own rule/version change. Recommend HIGH confirmation by a second distinct reviewer. Subjects may request review/appeal through an authorized internal process, never mutate evidence. | Single reviewer for all severities; no appeal. | Product/security approve reviewer roles, HIGH dual review and appeal owner/SLA. |
| D12 | Rules and authority | Immutable Tenant-qualified rule versions with stable rule key, signal type, parameters, `effectiveFrom` inclusive, optional `effectiveUntil` exclusive, status and approval evidence. No implicit global active rule and no retroactive rewrite. Rules are not legal policy. | Global templates copied to Tenants. | Product names rule authority; security approves; privacy approves rules touching behavioral data. |
| D13 | Tenant isolation | Tenant comes from trusted authentication/worker context and the registered source fact, never a client-selected query scope. Deduplication and all keys lead with Tenant. Cross-Tenant IDs are safe-not-found. | Cross-Tenant security administrator (not authorized). | Security/architecture approval; PostgreSQL and literal-route tests required. |
| D14 | Privacy/minimization | Permit user ID, actor ID where distinct, source module, safe action/rule code, source event ID, occurred/received time, evidence state, severity and reason code. Prohibit passwords, tokens, credential references, raw payloads, mandatory-field values, coordinates, customer/driver content, free-text allegations and biometric/device fingerprints. Do not copy raw IP addresses into findings. | Store a separately keyed/HMAC network indicator under a later privacy decision. | Privacy/security approve every payload field, visibility and redaction. |
| D15 | Retention | Retention duration is unresolved. Recommend append-only finding/evidence/review records under an approved finite Tenant policy, legal hold where separately authorized, and scheduled owner-controlled disposition. No hard-coded duration and no implementation before authority is named. | One fixed global duration. | Privacy/records authority approves durations, holds, erasure/anonymization and audit retention. |
| D16 | Persistence | Future Identity-owned Tenant-leading tables for rule/version, finding, immutable evidence, review/appeal history, action request/result and durable idempotency. Logical source IDs only; no cross-module foreign keys. Exact DDL and indexes require separate authorization from then-current Flyway head. | Event-only projection (insufficient for review durability). | Architecture/data approval; no migration number is reserved. |
| D17 | Events/integration | Source owners publish exact minimized durable facts through P1-01 only after registration. At-least-once delivery with Tenant/source-event/rule-version logical idempotency. No generic “user activity stream.” Operations integration remains `NONE` in Phase 1. | Synchronous query into source modules (rejected); broad audit scraping (rejected). | Contract owner + consumer + privacy approve each event version and producer activation. |
| D18 | API/RBAC/audit | Future bounded read/list/detail/review/appeal/rule-version commands only; no generic PATCH/status or client-supplied Tenant. Recommend distinct permissions for view, review, rule manage and restricted evidence view. Every rule/review/action is append-only audited. Exact routes/codes/grants remain unapproved. | One broad security-admin permission (not recommended). | Product/security approve routes, permissions, role grants, pagination and audit access. |
| D19 | Frontend | Operator UI explains signal, source, rule version, evidence quality, severity and permitted action; shows `UNAVAILABLE`, `UNKNOWN` and `CONFLICTING` honestly. It must never label an indicator as proven fraud. Session/Tenant changes clear cached risk data. | Dashboard-only summary. | Product/UX/privacy approve terminology, fields and role journeys. |
| D20 | Acceptance | Technical acceptance uses deterministic fixtures and real PostgreSQL/outbox concurrency to prove rules, idempotency, Tenant isolation and privacy. Operational acceptance requires authorized source producers, named reviewers, false-positive exercises, retention sign-off, and—only for enabled actions—a real supported IdP/session path. | Treat unit/browser tests as operational acceptance (rejected). | Product, security operations and privacy/records owners sign off independently. |

## 5. Proposed source contracts

These are shapes for approval, not public contracts:

- Common envelope: stable event ID, Tenant ID, source module, source event ID, subject user ID, optional actor
  user ID, occurred/received UTC instants, safe signal code, evidence state and correlation ID.
- Override fact: owner action code, required authority/SoD rule code and denial reason code only.
- Validation-pattern fact: command type and validation-rule code only; no field value or request payload.
- Bounded-anomaly fact: registered indicator code and source-owned evidence reference only.
- Shared-login fact: unavailable until an approved session/IdP authority can provide a strong contradiction.
- Delayed-reporting fact: activity type, occurred/received instants and approved offline-state code.

Exact event names, versions, topics, payloads, producer activation and idempotency keys remain unapproved.

### 5.1 Recommended first-wave signal

**PROPOSED:** activate only `UNAUTHORIZED_OVERRIDE_ATTEMPT`, limited to an Identity permission-ceiling
denial. This is the smallest source whose decision is already authoritative and Tenant-aware.

The existing source owner is `identity`. The authoritative decision occurs in
`IdentityService.requirePermissionCeiling(...)`, reached by Tenant-local user/role administration. The
current source has no risk producer. The smallest later instrumentation is to give the existing typed
`AuthorizationDeniedException` stable action/reason codes and invoke an Identity-owned denied-action output
port before returning the existing `403`. An infrastructure adapter would persist the approved P1-01 fact
independently of the failed command transaction. Failure to record a fact must never authorize the rejected
command or change its response; it must produce a minimized health error for recovery/operations.

#### Exact first-wave action catalogue

| Action code | Existing command path | Included denial |
| --- | --- | --- |
| `IDENTITY_USER_CREATE_ROLE_PERMISSION_CEILING` | Tenant-local user creation with requested roles | Requested role permissions are not a subset of the authenticated actor's current server-resolved permissions |
| `IDENTITY_USER_UPDATE_ROLE_PERMISSION_CEILING` | Tenant-local user update with requested roles | Same ceiling violation |
| `IDENTITY_ROLE_CREATE_PERMISSION_CEILING` | Tenant-local role creation | Requested permission set exceeds the actor ceiling |
| `IDENTITY_ROLE_UPDATE_PERMISSION_CEILING` | Tenant-local role update | Requested permission set exceeds the actor ceiling |

The only first-wave reason code is `REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING`. Generic authentication
failures, endpoint-level `403`, missing membership, inactive Tenant, cross-Tenant not-found, malformed
requests and `ROLE_ASSIGNED_OUTSIDE_TENANT` are excluded. They require their own evidence and false-positive
review before activation.

#### What the observation proves

It proves that an authenticated same-Tenant actor submitted one of the four registered Identity commands
and that Identity rejected the requested grant because it exceeded that actor's authoritative permission
ceiling at decision time. It does **not** prove malicious intent, account compromise, shared credentials,
fraud, successful privilege escalation, or any change to a user or role.

#### Proposed minimized durable fact

| Field | Rule |
| --- | --- |
| `eventId` | Deterministic UUID derived from Tenant, actor, action code, target identity and request correlation ID |
| `tenantId` | Trusted current Tenant only |
| `signalType` | Exact value `UNAUTHORIZED_OVERRIDE_ATTEMPT` |
| `sourceModule` | Exact value `IDENTITY` |
| `sourceEventId` | Same stable denial-event identity as `eventId` |
| `subjectUserId` | Authenticated actor UUID; the finding concerns the actor who attempted the grant |
| `actorUserId` | Same UUID in this first wave; retained as a distinct semantic field for compatibility |
| `actionCode` | One of the four allow-listed codes above |
| `reasonCode` | Exact value `REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING` |
| `targetType` | `USER` or `ROLE` |
| `targetId` | Logical UUID supplied/generated by the existing command; nullable only if no stable target exists before rejection |
| `occurredAt` | Source server UTC time when Identity makes the denial decision |
| `receivedAt` | Consumer UTC receipt time; separate from `occurredAt` |
| `correlationId` | Existing bounded request correlation ID; clients must reuse it for an intentional retry |
| `evidenceState` | `SUFFICIENT` only after structural validation; otherwise reject/quarantine without a finding |

Passwords, tokens, usernames, email, IP address, user agent, requested permission names, role contents,
request bodies, exception messages, stack traces and free text are prohibited.

#### Transaction and retry identity

- The original user/role command remains rejected and makes no source mutation.
- The approved producer later writes the denial fact to the P1-01 outbox in a bounded independent
  transaction because the business command is intentionally failing. This is an explicit exception to the
  usual “event with successful aggregate transaction” shape, not permission for a second outbox.
- Event identity is deterministic over `tenantId + actorUserId + actionCode + normalized target identity +
  correlationId`. Replaying the same request with the same correlation ID yields the same logical fact.
- The consumer additionally deduplicates by Tenant and `sourceEventId`. Same identity with different payload
  is `CONFLICTING` and is quarantined/health-reported; it never raises a finding.
- A retry using a new correlation ID is a new observed attempt. The UI/client guidance must reuse the
  correlation ID for uncertain retries.

#### Proposed rule and behavior

| Decision | First-wave recommendation requiring approval |
| --- | --- |
| Threshold | Three distinct sufficient denial facts for the same Tenant, actor and action family in a rolling 15-minute source-time window |
| Action family | All four codes aggregate as `IDENTITY_PERMISSION_CEILING`; duplicate retry identities count once |
| Severity | `MEDIUM`, expressing review priority only |
| Single/second fact | Retain as bounded evidence; no finding and no user-facing allegation |
| Window ordering | Use `occurredAt`; accept at most five minutes of delivery lateness for evaluation; later facts remain immutable history but cannot retroactively open a finding |
| Missing data | Structurally incomplete facts are rejected/quarantined and health-reported; no finding |
| Stale data | Older than the lateness allowance or outside the active rule window: history only; no finding |
| Conflicting data | Same identity with differing safe payload: `CONFLICTING`; quarantine; no finding |
| Duplicate data | Same Tenant/source identity and payload: idempotent no-op |
| Offline behavior | Not applicable: this signal requires an authenticated online Identity administration command; no offline grace |
| Finding grouping | One open finding per Tenant, subject actor, signal type, rule version and 15-minute episode; later sufficient facts append evidence without duplicate findings |
| Effect | `ADVISORY_REVIEW_ONLY`; no account/session restriction, MFA, lockout, permission mutation or Operations fact |

The three-in-fifteen recommendation balances accidental role-selection mistakes and automated retry noise
against repeated privilege-ceiling probes. It is a product/security choice, not an empirically certified
fraud threshold.

#### Visibility, retention, review, appeal and disposition

- **Visibility:** only active same-Tenant actors holding the future `USER_RISK_VIEW` permission may see safe
  finding summaries/details. `USER_RISK_REVIEW` is separately required for disposition. Exact permission
  codes and grants remain proposed until the permission change set.
- **Retention:** recommend 180 days for denial facts, findings and immutable review history, measured from
  each record's occurrence/decision time. This is an operational recommendation, not a legal requirement;
  privacy/records authority must approve it before persistence implementation.
- **Review:** a reviewer must be active, same-Tenant, authorized, and distinct from the subject actor. First
  wave has one `MEDIUM` reviewer; HIGH dual review is later-wave and does not block this slice.
- **Disposition:** exactly `CONFIRMED_INDICATOR`, `FALSE_POSITIVE`, `INSUFFICIENT_EVIDENCE`, or
  `CLOSED_NO_ACTION`, with an allow-listed reason code and optional bounded non-sensitive note if privacy
  approves notes. Disposition never changes the original denial or Identity permissions.
- **Appeal:** recommend one appeal within 30 days of disposition, submitted through an authorized internal
  operator on behalf of the subject and decided by a different `USER_RISK_REVIEW` actor. No subject-facing
  public/self-service API is proposed in Phase 1.

### 5.2 First-wave D1–D20 approval map

| Decisions | Existing approved contract reused | Concrete first-wave choice requiring approval | Later-wave and non-blocking |
| --- | --- | --- | --- |
| D1, D13 | Identity ownership; trusted Tenant context; published-boundary architecture | Identity-internal risk feature; Tenant-leading identities; no foreign access | Separate context and cross-Tenant administration excluded |
| D2, D3 | Identity permission-ceiling enforcement already rejects the command | Activate only the four ceiling action codes and one reason code above | Other override/denial types |
| D4–D7 | Source-owned validation/authorization principle | No mandatory-field, fraud, shared-login or delayed-reporting activation | All four categories remain `UNAVAILABLE` |
| D8, D9 | Existing safe error and deterministic rule conventions | Evidence states; three in 15 minutes; five-minute lateness; MEDIUM | LOW/HIGH rules and other thresholds |
| D10 | Existing source denial remains authoritative | Advisory review only | Reauthentication, restriction, lockout, Operations intake |
| D11 | Existing Tenant and SoD patterns | Distinct same-Tenant reviewer; one 30-day appeal; four dispositions | HIGH dual review and external subject portal |
| D12 | Existing effective-dated version patterns | Immutable Tenant rule version with exact threshold/window | Global templates and automatic activation |
| D14, D15 | Existing minimization and append-only audit conventions | Exact allow-listed payload; no IP/raw content; 180-day retention | Network indicators, legal hold and other retention classes |
| D16, D17 | P1-01 outbox and logical cross-module references | Later Identity-owned persistence plus one minimized denial fact; deterministic retry identity | Exact DDL/version remains separately authorized |
| D18, D19 | Server-side RBAC and session-scoped frontend cache rules | Proposed view/review permissions, bounded review API and operator UI | Rule-management UI and restricted evidence permission |
| D20 | Technical versus operational acceptance separation | Technical concurrency/privacy tests plus authorized reviewer false-positive exercise | Real IdP enforcement acceptance is not applicable |

### 5.3 Authority functions required for this first wave

| Authority function | Exact approval requested |
| --- | --- |
| Product owner | First signal, four action codes, three-in-15-minute threshold, MEDIUM severity, dispositions, 30-day appeal and advisory-only effect |
| Security authority | Permission-ceiling observation meaning, exclusions, five-minute lateness, correlation-based retry identity, reviewer SoD and no automatic restriction |
| Privacy/records authority | Exact payload prohibition/allow-list, operator visibility, optional-note policy, 180-day retention and appeal record handling |
| Identity source owner | Typed exception/action codes, actor/target/correlation facts, independent P1-01 denial transaction and health behavior |
| Architecture/data authority | Identity feature ownership, P1-01 exception-transaction pattern, future Tenant-leading persistence and no foreign access |

No named individual is inferred, and no approval is recorded by this refinement.

## 6. Proposed implementation sequence

CS01 is approved and complete. CS02-CS07 remain **PROPOSED / NOT AUTHORIZED**.

| Order | Proposed change set | Scope and dependencies | Completion criterion |
| --- | --- | --- | --- |
| 1 | `US-87-DETECT-USER-RISK-CS01-DOMAIN-SIGNAL-CONTRACTS-001` | Framework-neutral Identity-owned rule/finding/evidence/review model; freeze the exact permission-ceiling fact and denied-action port. No producer, persistence or migration. | Structural/privacy/identity/version tests and Modulith boundaries pass. The four action codes are represented exactly. |
| 2 | `US-87-DETECT-USER-RISK-CS02-PERSISTENCE-001` | Separately authorized forward migration for approved rule versions, denial evidence, findings, review/appeal history and idempotency; exact number selected then. | Clean/upgrade PostgreSQL, 180-day disposition support, constraints, concurrency, append-only history and Tenant plans pass. |
| 3 | `US-87-DETECT-USER-RISK-CS03-FIRST-SOURCE-INTEGRATION-001` | Add typed Identity permission-ceiling denial instrumentation and the independent P1-01 outbox write for exactly four action codes. | Existing `403`/no-mutation behavior remains; duplicate/conflicting/retry and publication-failure cases pass; no generic denial capture. |
| 4 | `US-87-DETECT-USER-RISK-CS04-EVALUATION-REVIEW-001` | Activate the approved three-in-15 rule, MEDIUM advisory finding, review dispositions, SoD and internal 30-day appeal. | Three distinct facts create one finding; late/duplicate/conflicting facts do not; review/appeal is durable and owner-safe. |
| 5 | `US-87-DETECT-USER-RISK-CS05-APIS-RBAC-AUDIT-001` | Separately approved routes, permission seed/grants, safe errors, pagination and immutable audit. | Literal `/api/v1/...` denial, direct-use-case authorization, Tenant A/B and privacy pass. |
| 6 | `US-87-DETECT-USER-RISK-CS06-FRONTEND-001` | Permission-aware review UI, truthful evidence states and safe terminology. | Component/accessibility/session clearing and real Chromium journeys pass. |
| 7 | `US-87-DETECT-USER-RISK-CS07-CONCURRENCY-SECURITY-001` | PostgreSQL/outbox concurrency, redelivery, privacy, performance and operational recovery. | No duplicate findings/reviews, no cross-Tenant leakage, controlled measurements and recovery evidence. |
| 8 | Technical closure and final acceptance | Consolidated technical evidence followed by independent operational acceptance. | Technical completion remains distinct from signal/IdP activation and operational sign-off. |

Later signal integrations are separate producer-owned change sets. A future reauthentication, restriction or
lockout effect requires its own decision, supported IdP/session capability, rollback and acceptance.

## 7. Verification plan for later implementation

- Domain: immutable identities/versions, half-open effective time, evidence-state and deterministic severity.
- Producer contracts: owner denial/validation remains authoritative, exact minimization, no raw values.
- PostgreSQL: Tenant-leading constraints/indexes, append-only evidence/history, one logical finding identity,
  optimistic concurrency, crash/retry and cross-Tenant denial.
- Events: P1-01 transactionality, at-least-once redelivery and consumer idempotency.
- Security: literal and effective HTTP paths, use-case authorization, SoD, role grants, safe not-found and
  sensitive-field absence from logs, events, API and audit.
- Frontend: permissions, evidence terminology, false-positive/review journey and session/Tenant cache clearing.
- Operations: named reviewers exercise false-positive and appeal scenarios; retention/privacy owners approve.
- IdP effects: not applicable until a real supported provider and capability have separate acceptance.

## 8. Approval disposition and remaining authority gates

1. **Product owner:** D1–D12, including which signal types activate first, thresholds/windows, severity,
   review dispositions, HIGH dual-review and Phase-1 advisory-only effect.
2. **Security authority:** authoritative override semantics, source/action catalogue, session evidence limits,
   permission/SoD model, rule authority, break-glass posture and the explicit absence of automatic lockout.
3. **Privacy/records authority:** permitted fields, raw-IP prohibition or alternative, visibility, retention,
   appeal/subject-access handling, legal hold and disposition.
4. **Each first-wave source owner:** exact safe fact, clocks, offline allowance, stable reason/action codes and
   producer transaction boundary.
5. **Architecture/data authority:** Identity ownership, P1-01-only asynchronous integration, no foreign
   persistence, proposed persistence boundary and migration authorization when DDL exists.

The user approval recorded in section 1.1 satisfies these functions only for the exact first-wave choices
and CS01 domain-contract scope. Producer activation remains prohibited. CS02 persistence requires a
separate exact DDL and migration authorization; CS03-CS07 each require their own bounded authorization.

## 9. First-slice authorization prompt (executed for CS01)

```text
Approve the US-87 first-wave D1–D20 mapping recorded in sections 5.1–5.3 of
docs/product-decisions/US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001.md and execute the
proposed US-87-DETECT-USER-RISK-CS01-DOMAIN-SIGNAL-CONTRACTS-001.

This approval freezes the first signal as `UNAUTHORIZED_OVERRIDE_ATTEMPT`, owned by Identity and limited to
the four documented Identity user/role permission-ceiling action codes with reason
`REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING`. It approves three distinct facts in 15 minutes, five minutes
of delivery lateness, MEDIUM advisory severity, the exact minimized payload, same-Tenant distinct-reviewer
SoD, four dispositions, one internal 30-day appeal and 180-day retention. Record approval by the product,
security, privacy/records, Identity source-owner and architecture/data authority functions. If any function
has not approved these exact choices, stop; do not infer approval from this conditional prompt.

Implement framework-neutral Identity-owned Tenant-qualified rule/version, finding identity, evidence
quality, MEDIUM severity, advisory lifecycle and review/appeal value objects plus the narrow typed denied-
action port. Represent exactly the four action codes, one reason code and payload allow-list. Source Identity
permission-ceiling enforcement remains authoritative. Do not query repositories or create a generic denial,
activity or fraud engine.

Phase 1 is advisory-only. Do not add reauthentication, MFA, session restriction, refresh-token revocation,
lockout, Operations cases, opaque ML, biometric/device fingerprinting or legal/fraud conclusions. Missing,
stale, conflicting or unavailable evidence cannot become an affirmative finding.

Add structural, immutability, Tenant identity, effective-time, evidence-state, privacy and Modulith boundary
tests. Do not create a migration, persistence adapter, REST API, permission, event producer, frontend or
runtime activation in CS01. Record CS02 persistence, CS03 Identity producer and CS04 evaluation/review as
the bounded usability path; do not claim the first signal usable before those stages pass.

Run the required focused and architecture quality gates, update evidence and both roadmaps, synchronize the
affected Knowledge Base files, and commit/push application and Knowledge Base changes separately only after
verification. Select no migration number. Preserve Flyway V105, accounting 73/87, US-72 inactivity and all
Tracking activation/physical-acceptance holds.
```

## 10. Preserved holds and exclusions

- Flyway remains V105; no migration number is reserved.
- Accounting remains 73 / 87.
- US-72 remains `CS01_COMPLETE_INACTIVE / POLICY_APPROVAL_PENDING`.
- All Tracking source-activation and physical-acceptance holds remain unchanged.
- US-87 CS01 domain contracts are implemented; the story is neither technically complete, legally
  compliant, usable end to end, nor approved for enforcement.
- No dependency, schema, permission, API, event producer or runtime behavior changed.
