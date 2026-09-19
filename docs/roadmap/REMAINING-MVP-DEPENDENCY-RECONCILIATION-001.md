# Remaining MVP Dependency Reconciliation

## Governed baseline

```text
Application baseline: 1edc7110f8b31d1b01d087f05482224278991717
Knowledge Base baseline: 6f6079e31c7ba72bf13792a315707b12e6acc5d0
Flyway head: V105
Accepted stories: 73 / 87
Remaining stories: 14 / 87
```

This is a planning reconciliation. It authorizes no production code, schema, permission, API, event,
dependency, policy, or external-acceptance claim.

## Reconciled remaining-story matrix

| Story | Technical implementation | Acceptance | Actual dependencies / missing decisions or evidence | Independently executable software task authorized? | Existing queue |
| :--- | :--- | :--- | :--- | :--- | :--- |
| US-48 | Implementation complete; provider-neutral Flespi, Traccar and Generic paths exist | Blocked externally | Genuine supported physical device/provider telemetry and operator sign-off | No; acceptance can resume only when external facts exist | `US-48-LIVE-VEHICLE-TRACKING-FINAL-ACCEPTANCE-001` |
| US-50 | Implementation and technical closure complete | Blocked externally | Physical provider/device speed field, native-unit and normalization evidence | No; acceptance is externally gated | `US-50-MONITOR-SPEED-FINAL-ACCEPTANCE-001` |
| US-51 | Technically complete at V105 | Production activation and acceptance pending | Authoritative source-time Fleet powertrain classification, verified native engine-running mapping, physical evidence and operator sign-off | No activation or final-acceptance task is authorized | None |
| US-52 | Implementation and technical closure complete | Blocked externally | Physical route/accuracy journey, real Dispatcher delivery and operator sign-off | No; acceptance is externally gated | `US-52-MONITOR-ROUTE-DEVIATIONS-FINAL-ACCEPTANCE-001` |
| US-53 | Technically complete | Blocked externally | Genuine retained provider/device journey and operator sign-off | No; acceptance is externally gated | `US-53-REPLAY-JOURNEYS-FINAL-ACCEPTANCE-001` |
| US-54 | Technically complete | Blocked externally | Genuine telemetry, privacy review and operator sign-off | No; acceptance is externally gated | `US-54-VIEW-TRACKING-DASHBOARD-FINAL-ACCEPTANCE-001` |
| US-55 | Technically complete, including provider extensions | Blocked externally | Genuine signal-loss/recovery, reassignment, supported tamper/power/battery and burst evidence plus operator sign-off | No; acceptance is externally gated | `US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001` |
| US-72 | CS01 inactive structural foundations complete | Not technically complete; policy approval pending | D1–D11, initial jurisdiction/policy scope and qualified policy authority | No; CS02–CS07 are proposals only | None; `US-72-ENFORCE-COMPLIANCE-CS02-PERSISTENCE-001` remains proposed/unapproved |
| US-76 | Not started beyond accepted platform foundations | Not started | PWA/native channel, role/workflow matrix, device binding, offline protection, camera/signature, push/background behavior and physical-device acceptance; runtime integration remains downstream of US-72 | No | None |
| US-82 | Not started | Not started | KPI catalogue/lineage, freshness, producer/data-quality gates, deterministic forecast/recommendation rules, privacy and ownership; US-85 is needed for complete quality-gated acceptance | No | None |
| US-84 | Not started | Not started | Failure catalogue, responsibility matrix, degraded modes, monitoring/fault environment, RTO/RPO basis and integrity recovery evidence; US-85 is needed for complete recovery certification | No | None |
| US-85 | Not started | Not started | Owner-specific invariant/correction catalogue and validators; complete GPS/trip mismatch acceptance depends on genuine US-48 evidence | No | None |
| US-86 | Not started | Not started | Disruption catalogue, constraint/replan authority, scheduling/tracking/compliance contracts and optional advisory source | No | None |
| US-87 | Prerequisite analysis complete; no runtime implementation | Not started | Approval of proposed D1–D20, exact first producer facts, thresholds, review authority, privacy/retention and any IdP action | No; proposed CS01–CS07 remain unapproved | None |

Classification is deliberately split between technical completion and story acceptance. The seven
Tracking stories do not become accepted from fixture or Testcontainers evidence, and no later story may
inherit their physical evidence.

## US-72 approval hold

The existing decision package remains authoritative:
`docs/product-decisions/US-72-ENFORCE-COMPLIANCE-PREREQUISITE-AND-PRODUCT-DECISIONS-001.md`.
The unresolved decisions are recorded once: D1–D11, initial jurisdiction/policy scope, qualified policy
authority, exact source facts, mandatory/advisory classification, effects/precedence, authority and
publication, override/appeal/SoD, retention/privacy, public contracts/security and acceptance ownership.
CS01 does not weaken or satisfy that gate. Compliance remains inactive.

## Completed recommended governance workstream

No existing implementation task is authorized and independently executable. The selected governance task:

`US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001` is **COMPLETE** as a documentation-only
governance task. Its D1–D20 package and every proposed implementation change set remain unapproved.

was independent because Identity/Security (US-74), Audit (US-75), Workflow (US-80) and Operations (US-78)
were accepted as stories. Inspection established that Audit and Workflow are feature-owned rather than
generic reusable runtime services. The prerequisite analysis did not require physical GPS, US-72 policy
activation or US-82 analytics. An IdP remains required only for real MFA/session-enforcement claims.

US-76 product decisions can be studied independently, but its runtime delivery remains downstream of
US-72 and requires a selected mobile/device/push path. US-85 remains incomplete without owner contracts
and physical GPS/trip mismatch evidence; US-84 complete recovery certification depends on US-85; US-82
complete acceptance depends on quality-gated producer lineage; and US-86 still depends on compliance and
disruption/replanning authority. These are actual completion dependencies, not reasons to block US-87
prerequisite analysis.

## Proposed first-slice boundary

The completed US-87 prerequisite review inspected authoritative requirements and current Identity,
feature-owned Audit/Workflow, Operations, Tenant and domain validation contracts. Its decision matrix covers:

- exact deterministic Phase-1 signals and fact owners;
- unauthorized-override semantics without duplicating domain authorization;
- mandatory-field responsibility remaining at the source command boundary;
- bounded fraud indicators and shared-login evidence without opaque profiling;
- delayed-reporting sources, clocks and thresholds;
- severity, confidence/unknown behavior and false-positive review;
- advisory, reauthentication, session restriction, lockout or investigation effects;
- IdP/MFA boundary and safe degraded behavior;
- Tenant isolation, privacy, retention, audit, appeal and segregation of duties;
- module ownership, published contracts, persistence/API/permission/frontend boundaries; and
- technical versus external/operational acceptance.

The resulting D1–D20 recommendations are recorded in
`docs/product-decisions/US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001.md`. They remain
`PROPOSED`; no code, migration reservation, permission, enforcement or legal/fraud authority exists.

## Self-contained authorization prompt

```text
Approve the US-87 D1–D20 product/security/privacy decisions recorded in
docs/product-decisions/US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001.md and execute the
proposed US-87-DETECT-USER-RISK-CS01-DOMAIN-SIGNAL-CONTRACTS-001.

Before implementation, record the approving product owner, security authority and privacy/records
authority. Identify the first active signal type and provide its owning module, exact stable action/reason
codes, safe fields, evidence threshold/window, clock authority and retention decision. Stop if any input is
absent rather than converting a proposal into production policy.

Implement only framework-neutral, Tenant-qualified Identity-owned rule/version, finding identity, evidence
quality, advisory lifecycle and review value objects plus narrow producer-neutral signal ports. Source
domains remain authoritative for validation and authorization. Use only the approved catalogue and
minimized fact fields; no foreign repositories or generic activity/fraud engine.

Phase 1 is advisory-only. Do not add reauthentication, MFA, session restriction, token revocation, lockout,
Operations cases, opaque ML, biometric/device fingerprinting or legal/fraud conclusions. Missing, stale,
conflicting and unavailable evidence cannot produce an affirmative finding.

Add structural, immutability, Tenant identity, effective-time, evidence-state, privacy and Modulith tests.
Do not add persistence, a migration, REST API, permission, event producer, frontend or runtime activation.

Run focused/architecture gates and synchronize evidence, roadmaps and Knowledge Base after verification.
Select no migration number. Keep Flyway V105, accounting 73/87, US-72 inactive and all Tracking holds open.
```
