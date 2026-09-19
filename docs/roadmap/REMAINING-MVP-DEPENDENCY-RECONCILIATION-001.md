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
| US-87 | Not started | Not started | Deterministic signal catalogue, severity/thresholds, enforcement and review/appeal boundaries, IdP/MFA scope, privacy/retention and acceptance | No; prerequisite review is proposed below | None |

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

## Recommended next independent workstream

No existing implementation task is authorized and independently executable. The recommended next
governance workstream is a newly proposed planning task:

`US-87-DETECT-USER-RISK-PREREQUISITE-AND-PRODUCT-DECISIONS-001` **(PROPOSED, NOT AUTHORIZED)**.

US-87 is the strongest independent candidate because Identity/Security (US-74), Audit (US-75), Workflow
(US-80) and Operations (US-78) are accepted. Its prerequisite analysis does not need a physical GPS
device, US-72 policy activation, or US-82 predictive analytics. An external IdP is required only if the
eventual scope claims real MFA/SSO enforcement. Missing-field validation remains with each owning domain;
US-87 must not become a cross-module validation or fraud “god engine.”

US-76 product decisions can be studied independently, but its runtime delivery remains downstream of
US-72 and requires a selected mobile/device/push path. US-85 remains incomplete without owner contracts
and physical GPS/trip mismatch evidence; US-84 complete recovery certification depends on US-85; US-82
complete acceptance depends on quality-gated producer lineage; and US-86 still depends on compliance and
disruption/replanning authority. These are actual completion dependencies, not reasons to block US-87
prerequisite analysis.

## Proposed first-slice boundary

The proposed US-87 prerequisite review should inspect authoritative requirements and current Identity,
Audit, Workflow, Operations, Tenant and domain validation contracts, then produce one decision matrix for:

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

The review must recommend concrete options while labeling every unapproved choice `PROPOSED`. It must not
create code, reserve a migration, seed permissions, activate enforcement or claim legal/fraud authority.

## Self-contained authorization prompt

```text
Prepare the US-87 Detect User Risk prerequisite and product-decision package.

Verify the actual application and Knowledge Base baselines, repository instructions, both roadmaps,
the original US-87 requirements, and current Identity/Security, Audit, Workflow, Operations, Tenancy
and owner-domain validation contracts. Preserve all US-48/50/51/52/53/54/55 external holds and the
US-72 policy-approval hold.

This authorizes analysis and documentation only. Produce one consolidated decision matrix covering:
deterministic Phase-1 signal types and fact owners; unauthorized overrides; source-owned missing-field
validation; bounded fraud indicators; shared-login evidence; delayed-reporting sources and thresholds;
severity and unknown/conflicting evidence; false-positive review and appeal; advisory, reauthentication,
session restriction, lockout and investigation effects; IdP/MFA boundaries; effective versions;
Tenant isolation; privacy and retention; audit and segregation of duties; module/integration ownership;
persistence, migration, API, event, permission and frontend boundaries; and technical versus operational
acceptance.

Do not introduce opaque ML, biometric/device fingerprinting, automatic punitive action, cross-module
repository access, a generic fraud engine, legal conclusions or invented IdP capabilities. Missing fields
must continue to fail at their owning command/API boundary rather than being repaired by US-87.

Distinguish existing approved contracts, recommended choices and decisions requiring product/security/
privacy authority. Break any future implementation into small ordered change sets, without reserving a
migration or presenting proposed identifiers as authorized.

Update only planning and affected Knowledge Base documentation, commit and push documentation separately,
and report one consolidated authorization request for the first implementation slice. Keep Flyway V105,
accounting 73/87 and every existing activation/acceptance hold unchanged.
```
