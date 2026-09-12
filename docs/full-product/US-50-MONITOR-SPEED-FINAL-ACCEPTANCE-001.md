# US-50 Monitor Speed — Final Acceptance

**Verdict:** `BLOCKED_EXTERNAL_SYSTEM`  
**US-50:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Reason:** `PHYSICAL_SPEED_FIDELITY_EVIDENCE_PENDING`  
**Flyway:** V83; V84 absent  
**Accounting:** unchanged at 73/87 complete and 14/87 remaining

## Executive verdict and source traceability

The original requirements, US-50 UML, frozen product decisions, CS01-CS07 evidence, technical closure and
actual implementation were independently reconciled. The technical implementation remains correct. Final
acceptance cannot pass because no genuine physical GPS/telematics device, live provider-generated speed field,
documented native unit, adapter conversion proof or physical end-to-end episode journey was supplied or is
available. Signed deterministic fixtures are valid technical evidence but are not physical fidelity evidence.

## Representative technical revalidation

Fresh acceptance revalidation passed 78/78 backend tests covering PostgreSQL persistence and immutability,
runtime state/episode behavior, literal API/RBAC/Tenant denial, durable publication, Notification consumption
and architecture/Modulith boundaries. Focused frontend revalidation passed 10/10. Flyway reports V83 and no
V84 exists; `git diff --check` passes.

The fresh CS07 evidence remains source-aligned: race matrix 41/41 three times (123/123); all five intended V81
indexes used; Maven 1,656 tests with zero failures/errors and 15 skipped; architecture 52/52; Vitest 309/309;
Chromium 11/11; Checkstyle, PMD, SpotBugs, TypeScript and production build pass. Signed ingress measured 441.3
msg/s sustained, 1,265.5 msg/s burst, 19.6 ms latest p95 and 18.4 ms history p95. The 71 Delivery ESLint
findings remain unrelated, pre-existing and unchanged. Authoritative database evidence is
`transport_logistics_acceptance`; development-database authoritative evidence is NO.

## Technical, security and privacy result

Tracking consumes normalized `PositionEvent.speedKph` in km/h, applies the frozen eligibility, route/Tenant
threshold, zero-tolerance, two-sample confirmation, one-sample clearance, deterministic episode, inclusive
repeat and WARNING/HIGH rules. V81-V83, bounded jobs and APIs, exact three permissions, authenticated Tenant,
management idempotency/audit, the exact 14-field `VehicleSpeedingDetectedV1`, shared outbox and same-Tenant
Dispatcher IN_APP notification remain correct. The UI is permission-aware, truthful for UNKNOWN, displays
Tracking WARNING/HIGH, makes no legal-limit claim, and excludes prohibited telemetry, credentials and PII.
No Driver mutation or cross-module ownership leakage exists.

## Physical fidelity gate

Unavailable evidence:

- physical provider/device identity and real provider ingestion;
- provider speed field and native unit;
- exact adapter conversion and raw-to-`speedKph` sample;
- real below/candidate/confirmation/clearance observations;
- physical telemetry through Position, episode, outbox, Notification and operator UI.

No fixture or simulator is represented as physical evidence. Safe acceptance may later use a low configured
threshold in a private/controlled environment; it must never require unsafe or unlawful driving.

## Disposition

There is no implementation defect and no product decision required. Resume only when material physical
provider/device speed evidence and unit mapping become available. US-48 receives no acceptance inheritance and
remains independently externally blocked. Accounting does not change. Wave C continues with
`US-52-MONITOR-ROUTE-DEVIATIONS-PRODUCT-DECISIONS-001`.
