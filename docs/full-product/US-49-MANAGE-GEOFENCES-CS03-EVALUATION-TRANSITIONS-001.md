# US-49 Manage Geofences CS03 — Evaluation and Transitions

**Task:** `US-49-MANAGE-GEOFENCES-CS03-EVALUATION-TRANSITIONS-001`  
**Result:** COMPLETE  
**US-49:** `IMPLEMENTATION_IN_PROGRESS`  
**Flyway:** V77; no V78  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`

## Atomic enqueue and worker model

The accepted US-48 ingestion transaction now inserts one idempotent
`tracking_geofence_evaluation_job` for each newly accepted eligible position in the same transaction as
the immutable position and latest projections. Duplicate facts return before enqueue and rollback removes
both position and job. Ingress performs no polygon evaluation and publishes no per-position event.

One feature-flagged Tracking coordinator (`app.tracking.geofence-evaluator.enabled`, disabled by default)
claims V77 jobs through bounded `FOR UPDATE SKIP LOCKED` leases. Defaults are a claim of 16, four fixed
workers, a queue of 32, a two-minute lease and a 30-second persisted retry. Expired work is reclaimable;
release, renewal, completion, retry and terminal failure require the current unexpired owner. Saturation
releases unstarted work. Queue, active workers, queued/claimed backlog, oldest due age, job outcomes and
transition latency use bounded Micrometer dimensions. There is one scheduler entry point and no scheduler
or thread per Tenant, Vehicle, device or geofence.

## Evaluation and transition semantics

Execution reloads the authoritative position by Tenant and position ID and revalidates nonduplicate,
TRUSTED, Vehicle-associated, valid WGS84, IN_ORDER and at-most-five-minute eligibility. The persisted job
Tenant is authority; no HTTP or security context participates. More than 500 ACTIVE definitions fails
closed. Repository selection uses V77 bounding boxes first, while retaining definitions with current state
so exits remain observable. Definitions outside the box with no existing state are initialized OUTSIDE
through a lightweight identity/version query without polygon deserialization.

Each candidate is processed in its own owning Tracking transaction. The definition is locked and
revalidated as same-Tenant, ACTIVE and the expected version; the Tenant/geofence/Vehicle state identity is
serialized even before its first row exists, then locked or created. The CS01 domain performs geometry,
source-time/UUID ordering, duplicate suppression, pending reset and two-position hysteresis. Initial state
and a changed definition version initialize silently. Confirmed state plus deterministic immutable
transition are committed atomically. Retry is at least once and remains safe through job identity, state
ordering/locking and transition identity.

DEPOT and CUSTOMER_SITE produce normal ENTERED/EXITED facts. Confirmed UNAUTHORIZED_ZONE entry produces
`UNAUTHORIZED_ZONE_ENTERED`, HIGH severity and mandatory alert intent. Overlaps are independent. The
CS01 publication port is invoked only after a committed configured transition; CS03 binds a no-op adapter,
so immutable transition evidence is retained while CS05 still owns durable P1-01/Notification delivery.
No event contract, Notification dependency or Operations exception integration was added.

Disable, retire and definition-version races are resolved by the locked definition revalidation. If the
lifecycle change commits first, evaluation produces no transition; if evaluation commits first, its
immutable transition remains legitimate. A later definition version starts with silent initialization.

## Verification

- Focused CS03 PostgreSQL evaluation/persistence/atomic-ingestion selection: 22 tests, 0 failures, 0 errors.
- Complete Tracking Java: 152 tests, 0 failures, 0 errors, 0 skipped.
- Worker saturation: PASS with one fixed worker and zero-capacity test queue; excess claim safely released.
- Tenant isolation, job idempotency, rollback, lease recovery and stale-owner rejection: PASS.
- Real Chromium ingress with evaluator enabled: 580.9 accepted msg/s sustained; 1,577.1 msg/s burst.
- Latest lookup p95: 0.380 ms; history lookup p95: 0.273 ms.
- Complete Maven verification: 1,558 tests, 0 failures, 0 errors, 15 skipped; `BUILD SUCCESS` in 09:38.
- Architecture and Modulith: 52 tests, 0 failures, 0 errors, 0 skipped.
- Checkstyle: 0 configured violations. PMD: PASS. SpotBugs: 0 findings and 0 errors.
- Flyway V1→V77: PASS on `transport_logistics_acceptance`; development database authoritative evidence: NO.
- `git diff --check`: PASS.

The evaluator-enabled real startup initially exposed ambiguous Spring constructor selection. The production
constructor is now explicitly autowired; its focused regression and every final gate above passed afterward.

## Scope exclusions

No migration, V78, REST API, OpenAPI contract, permission seed, frontend, new event contract,
Notification consumer, durable publication adapter, external dependency or US-50–55 behavior was added.

## Next controlled change set

`US-49-MANAGE-GEOFENCES-CS04-APIS-RBAC-AUDIT-001`
