# US-52 CS07 PostgreSQL Concurrency and Performance Closure

## Verdict

PASS. CS07 is technically complete at Flyway V92. Story accounting remains 73/87 because independent US-52 technical closure and final acceptance have not run.

## Concurrency and consistency

Real PostgreSQL tests used deterministic barriers/latches and bounded futures rather than arbitrary sleeps. They proved:

- concurrent same-Vehicle confirmation converges to one open episode;
- duplicate telemetry and repeated HIGH telemetry are idempotent;
- detected/escalated events are not duplicated and severity cannot downgrade;
- stale and out-of-order facts cannot regress current state while immutable history remains available;
- route-context/version attribution remains exact, including historical null route versions;
- concurrent review converges, review history is append-only, self-reversal is denied, and a different authorized reviewer can correct;
- durable dispatch uses `FOR UPDATE SKIP LOCKED`, independent work proceeds, active leases are not stolen, expired leases recover, completed work is terminal, and logical duplicates converge per Tenant;
- advisory locks remain Tenant-qualified; no deadlock occurred.

The final focused selection passed 250/250. The dedicated durable-dispatch concurrency class passed 3/3. Recent acceptance evidence showed ten COMPLETED dispatches for each of GEOFENCE, SPEED and ROUTE_DEVIATION, zero FAILED/PROCESSING dispatches, and Kafka lag zero for both history and live-projector groups after application shutdown.

## Chromium timeout remediation

The retained US-49 and US-50 timeouts shared an environment prerequisite defect, not a detector regression. Playwright started Spring Boot on the host while:

1. hybrid storage was disabled by default, so the Kafka history consumer and durable evaluation worker did not exist; and
2. Compose advertised Kafka only inside Docker as `kafka:9092`, while the host application attempted `localhost:9092`.

Ingress could acknowledge Kafka publication in contaminated/reused environments, but the required history to dispatch to evaluator path was not deterministically available. US-49 also had an independent fixture-isolation defect: scenario 4 reused the geofence created and activated by scenarios 1-3 and therefore could not run alone.

The correction:

- exposes a dedicated Compose host Kafka listener at `localhost:9094` while retaining internal `kafka:9092`;
- enables hybrid storage and topic management in the Playwright server environment;
- gives US-49 scenario 4 a unique active geofence fixture;
- adds stage-specific bounded polling messages without increasing timeouts, adding retries, bypassing Kafka, or pre-seeding detector state.

Stability evidence:

- US-49 scenario 4 alone: 3/3 PASS (4.3 s, 3.8 s, 3.7 s).
- US-50 scenario 1 alone: 3/3 PASS (2.4 s, 3.0 s, 2.0 s).
- Full US-49: 6/6 PASS.
- Full US-50: 10/10 PASS.
- Combined US-49/US-50: 16/16 PASS.
- Final US-49/US-50/US-52 Chromium continuity: 26/26 PASS, zero skipped/not-run/retries.

All fixtures use run-unique external references, messages, nonces and business identities. Tenant-B denial remains exercised. No coordinates, credential, signature, raw telemetry, Driver or Customer PII is recorded in this evidence.

## Final verification

| Gate | Result |
| --- | --- |
| Focused V92/CS07/TS02-TS04/US-48-US-52 | 250/250 PASS |
| PostgreSQL production invariants | 16/16 PASS |
| Architecture/Modulith | 59/59 PASS |
| Complete Maven | 1,754/1,754 PASS; 0 failures/errors/skips; BUILD SUCCESS in 17:05 |
| Vitest | 319/319 PASS |
| TypeScript | PASS |
| Production frontend build | PASS (existing chunk-size warning only) |
| Changed-file ESLint | PASS |
| Checkstyle | PASS; zero violations |
| PMD | PASS |
| SpotBugs | PASS; zero findings |
| Dependency analysis | PASS with pre-existing broad declaration warnings |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The V92 maintenance-window deployment and rollback procedure is recorded in `US-52-MONITOR-ROUTE-DEVIATIONS-CS07-V92-MAINTENANCE-WINDOW-INDEX-HARDENING-001.md`.

## Next governed action

`US-52-MONITOR-ROUTE-DEVIATIONS-TECHNICAL-CLOSURE-001`
