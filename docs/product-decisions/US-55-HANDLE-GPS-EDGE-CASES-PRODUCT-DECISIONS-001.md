# US-55 Handle GPS Edge Cases — Product Decisions

## Status and authority

`PRODUCT_DECISIONS_COMPLETE / READY_FOR_IMPLEMENTATION`

These decisions reconcile the original US-55 requirement with US-48 live tracking, the Kafka/Redis/Timescale
platform, US-49/50/52 detectors, US-53 replay, US-54 dashboard, provider normalization, V75/V76 binding,
V87 history and V91 durable evaluation dispatch. Accounting remains 73/87 and Flyway remains V95.

## Scope and non-goals

US-55 recognizes unreliable GPS evidence, protects latest-trusted state, preserves admissible forensic history,
and gives Tracking/Control Room operators a minimized exception workflow. Phase 1 covers invalid position,
accuracy, clock/order, duplicate/replay, impossible movement, signal loss, recovery, binding violations,
provider-declared tamper and battery signals, burst handling and truthful UNKNOWN states.

It does not map-match, smooth, rewrite or delete historical evidence; infer ignition, tamper or battery state
from absence; distinguish tunnel/remote-area loss without an explicit provider signal; implement predictive
fraud/ML; expose raw packets; remotely control devices; or change another producer's acceptance status.

## Frozen thresholds

| Rule | Exact Phase 1 value |
| --- | --- |
| Latitude | finite decimal from -90 through 90 inclusive |
| Longitude | finite decimal from -180 through 180 inclusive |
| Coordinate precision | at most 7 fractional digits after trailing-zero normalization |
| Null Island `(0,0)` | retained as `NULL_ISLAND_SUSPECT`, UNTRUSTED; never latest-trusted or detector eligible |
| Accuracy accepted range | absent, or greater than 0 and at most 10,000 metres |
| Good accuracy | greater than 0 and at most 100 metres |
| Low accuracy | greater than 100 and at most 1,000 metres |
| Unusable accuracy | greater than 1,000 and at most 10,000 metres |
| Future tolerance | 0–120 seconds ahead is `CLOCK_SKEW`; more than 120 seconds is `FUTURE` |
| Delayed | source age greater than 5 minutes through 24 hours |
| Late | source age greater than 24 hours but inside retained-history boundary |
| Retention rejection | source time older than the effective retained-history boundary |
| Impossible jump | differing coordinates at equal source time, or at least 2 km within 10 minutes with implied speed above 250 km/h |
| Recovery from jump | two consecutive good-accuracy, in-order, plausible points from the valid binding |
| Fresh/live | source and receipt ages both at most 60 seconds |
| Recent/delayed display | trusted source age over 60 seconds through 5 minutes |
| Stale | trusted source age over 5 minutes |
| Offline | no successful receipt for over 5 minutes |
| Never seen | no accepted receipt exists for the device/Vehicle |
| History batch | 1–500 canonical records |
| Detector eligibility age | at most 5 minutes at evaluation |
| Low battery | provider-normalized battery percentage at or below 20% |
| Critical battery | percentage at or below 10% |
| Rapid drain | drop of at least 20 percentage points within 30 minutes across at least two valid readings |

Freshness thresholds are globally frozen for Phase 1, not Tenant-configurable. Configuration would make
cross-widget truth inconsistent and requires a later product decision.

## Edge-case decision matrix

| Condition | Ingress/history | Redis/latest trusted | Detectors | Operator result |
| --- | --- | --- | --- | --- |
| Missing, NaN/infinite, out-of-bounds or over-precision coordinate | reject before Kafka; malformed canonical record goes to DLT | no update | none | invalid-telemetry metric/audit; no false location |
| `(0,0)` | retain immutable untrusted history | no update | none | `NULL_ISLAND_SUSPECT` warning |
| Accuracy absent | retain with `ACCURACY_UNKNOWN`, trust UNKNOWN | no latest-trusted update | none | warning, visible in replay as uncertain |
| Accuracy 0/negative or over 10,000 m | reject before Kafka / DLT if canonical poison | no update | none | invalid accuracy |
| Accuracy 0–100 m | trusted when every other trust rule passes | eligible | eligible | normal |
| Accuracy >100–1,000 m | retain `LOW_ACCURACY`, UNTRUSTED | no trusted update | none | low-accuracy warning |
| Accuracy >1,000–10,000 m | retain `UNUSABLE_ACCURACY`, UNTRUSTED | no update | none | unusable warning |
| Clock skew 0–120 s future | retain with warning | no update until a later eligible source point | none | clock-skew warning |
| Future >120 s | retain UNTRUSTED/FUTURE | no update | none | future-time exception |
| Delayed 5 min–24 h | retain with `DELAYED` quality | no update | none | replay warning; current state unchanged |
| Late >24 h | retain UNTRUSTED/LATE | no update | none | replay warning |
| Out of order versus latest trusted | retain OUT_OF_ORDER | no regression | none | replay preserves source-time order |
| Exact replay | one logical history fact | refresh TTL only for exact eligible replay; no value change | no repeat | duplicate metric only |
| Impossible movement | retain UNTRUSTED/SUSPECTED_SPOOFING | no update | none | HIGH exception, last trusted position retained |
| Provider-declared tamper | retain the minimized normalized signal and related position | block trusted promotion until cleared and recovery confirmed | none while active | HIGH exception |
| Battery low/rapid drain | retain normalized battery evidence | position eligibility unaffected unless another trust rule fails | normal detector eligibility | WARNING; CRITICAL threshold creates HIGH exception |
| No tamper/battery signal | retain UNKNOWN, never infer normal or fault | unchanged | unchanged | capability unavailable/UNKNOWN |
| No receipt >5 min | history unchanged | last trusted retained as LAST KNOWN | no new detector evaluation | OFFLINE exception and label |
| Recovery | retain points | promote only after applicable recovery rule | resumes after promotion | close signal-loss/jump episode; preserve history |

## Trust, quality and state transitions

Trust and quality are distinct. `TRUSTED` means provider authority, active source-time binding, valid coordinate,
good accuracy, acceptable time/order and no active tamper/impossible-movement hold all pass. `UNKNOWN` means a
required truth such as accuracy or optional device signal is absent. `UNTRUSTED` means a positive disqualifier
exists. Quality flags explain the reason and never upgrade trust.

Allowed reliability states are `NORMAL`, `DEGRADED`, `SUSPECT`, `OFFLINE`, `RECOVERING` and `UNKNOWN`.
NORMAL requires trusted current evidence. A nonfatal warning enters DEGRADED. Impossible movement or explicit
tamper enters SUSPECT. Receipt age over five minutes enters OFFLINE. The first plausible point after SUSPECT or
OFFLINE enters RECOVERING; the second consecutive eligible point enters NORMAL. Any new disqualifier resets the
recovery sequence. Historical facts and prior exceptions remain immutable.

## Duplicate, identity and ordering

Deduplication is Tenant-scoped. When a nonblank provider message ID exists, identity is the hash of Tenant,
provider alias and message ID. Otherwise it is the hash of Tenant, device, source timestamp, exact normalized
coordinates and provider sequence. The same identity with a different payload is a conflict, not a duplicate.
Equal source timestamps with different identities remain separate history; deterministic live selection uses
source timestamp then lexicographically greatest immutable event UUID. Same coordinates at different timestamps
are distinct facts and may be statically reduced only by the existing V87 exact stationary rule.

Kafka replay and consumer reassignment may redeliver but cannot create another logical history, exception,
detector action or notification. Redis exact replay may refresh TTL; older or losing equal-time records cannot
regress state. Metrics count duplicate delivery without identifiers.

## Connectivity, burst and retry

Devices may buffer offline and send source-time-ordered or unordered bursts. Kafka remains the durable handoff,
keyed by Tenant/Vehicle. Consumers accept batches of 1–500, persist history and V91 evaluation intents atomically,
and acknowledge only after commit. Redis independently selects the newest eligible live point; arrival order does
not rewrite source order. Backpressure is Kafka lag, never dropped history or unbounded memory.

Contract/version/header poison goes directly to the seven-day privacy-minimized DLT. A transient Redis or
database dependency receives the existing two bounded consumer retries and remains unacknowledged for broker
redelivery rather than being misclassified as poison. V91 evaluator dispatch claims at most 100, leases for
30 seconds, retries up to 10 attempts with the existing capped exponential backoff, then records safe failure.
DLT replay is an authorized operator action after cause correction and remains idempotent.

## Device and provider binding

Source-time association is authoritative. Reassignment closes the old association and opens the new association
without rewriting history. A point belongs only to the association effective at its source timestamp. An unknown
device, inactive/disabled/retired provider binding, provider/device alias mismatch, missing source-time
association or cross-Tenant claim is rejected safely, audited with opaque IDs and never reveals foreign existence.
Provider alias changes require an explicit new binding/effective period; aliases are not silently rewritten.
External references are masked outside privileged management responses.

## Detector eligibility

| Consumer | Eligible input |
| --- | --- |
| Geofence | TRUSTED, IN_ORDER, good accuracy, no active hold, at most 5 minutes old |
| Speed | same, plus present normalized speed; missing speed is UNKNOWN, never zero |
| Route deviation | same, with accuracy supplied to the existing effective-corridor rule |
| Journey Replay | all retained facts in source-time/UUID order, with trust/quality warnings; no smoothing |
| Dashboard | latest received may explain degradation; map/motion truth uses latest trusted only |
| Idle | unavailable until an accepted engine-state source exists |

## GPS exception lifecycle and escalation

Tracking owns immutable exception evidence and an episode with states `OPEN`, `ACKNOWLEDGED`, `RECOVERING`,
`RESOLVED`. Types are `INVALID_TELEMETRY`, `CLOCK_ANOMALY`, `LOW_ACCURACY`, `IMPOSSIBLE_MOVEMENT`,
`SIGNAL_LOSS`, `DEVICE_TAMPER`, `BATTERY_LOW`, `BATTERY_RAPID_DRAIN`, `BINDING_VIOLATION` and
`PROCESSING_FAILURE`. The system opens/deduplicates episodes; operators may acknowledge and add a minimized
review reason but cannot rewrite evidence. Recovery resolves only system-observable types; binding/processing
failures require confirmed correction.

WARNING exceptions remain Tracking evidence and IN_APP Dispatcher notification candidates. HIGH exceptions
(impossible movement, explicit tamper, critical battery, cross-Tenant/binding attack, or repeated processing
failure) publish one minimized `OperationalExceptionFactV1` through P1-01 to the accepted US-78 intake and one
IN_APP Dispatcher notification. No email/SMS is approved. Repeated evidence updates one episode without duplicate
logical delivery. Operations owns case workflow; Tracking retains detection and correction ownership.

## API, RBAC and frontend

Phase 1 adds a distinct operator workflow because the source story requires review/escalation. Proposed APIs are
bounded Tenant-scoped list/detail and acknowledge endpoints under `/api/v1/tracking/gps-exceptions`; there is no
manual create, delete, arbitrary status mutation or raw-payload endpoint. Permissions are exactly
`GPS_EXCEPTION_VIEW` and `GPS_EXCEPTION_REVIEW`. Backend authorization is authoritative.

The frontend adds one permission-gated Tracking GPS Exceptions page and adds reliability warnings to existing
Live Tracking, Journey Replay and Dashboard surfaces. It shows last trusted/last known separately from rejected
or uncertain evidence and never renders credentials, raw payloads, full external device references, Driver PII
or Customer PII. There is no Phase 1 administrative threshold UI.

## Security, privacy, audit and observability

Tenant comes only from authenticated provider-key/server context. Signature, nonce, active binding and source-time
device association validation remain mandatory. Raw payload retention is prohibited. Credentials, signatures,
exact coordinates, exact timestamps, Tenant/Vehicle/device/provider references and personal data are prohibited
from logs, DLT diagnostics and metric labels.

Audit records safe exception type, severity, lifecycle action, actor, reason code and opaque aggregate reference.
Metrics use bounded labels only: result, exception type, severity, trust, quality, ordering, retry outcome and
provider type from the approved finite registry. Required counters cover invalid, DLT, duplicate, skew,
late/out-of-order, impossible jump, stale/offline, recovery, burst-size buckets and retries.

## Persistence and migration expectation

Implementation is expected to require Tracking-owned exception episode/history persistence, normalized optional
tamper/battery signal fields and supporting Tenant-leading indexes. It may also require a versioned canonical
telemetry V2 while consumers remain backward-compatible with V1. No migration number is reserved or authorized
by this decision. Any schema/event expansion requires a separately reviewed forward migration and contract gate;
V1–V95 remain immutable.

## Acceptance boundaries

Automated technical acceptance must prove every threshold/boundary, deterministic identity, concurrent episode
dedupe, Redis non-regression, immutable Timescale history, detector suppression/resumption, DLT/retry, Tenant/RBAC,
Operations/Notification idempotency, UI warnings and complete regression.

Physical acceptance separately requires real invalid/low-accuracy/delayed/burst telemetry where the provider can
safely generate it, a genuine loss/recovery journey, physical reassignment, provider-declared tamper/battery
signals for any claimed capability, privacy review and operator sign-off. Unsupported signals remain UNKNOWN;
simulation cannot establish device/provider fidelity. US-55 may be technically complete while field acceptance is
externally blocked.

## Rollback and risks

Feature flags disable exception evaluation/API/navigation without deleting Kafka/history or promoting uncertain
state. Consumer rollback retains Kafka backlog. Accepted migrations/events remain forward-only. Primary risks are
false spoof/tamper claims, alert storms, live-state regression, sensitive DLT content and cross-Tenant binding;
two-point recovery, episode dedupe, minimized evidence and fail-closed authority mitigate them.

## Controlled implementation change sets

1. `US-55-HANDLE-GPS-EDGE-CASES-CS01-DOMAIN-TRUST-QUALITY-CONTRACTS-001` — implement framework-neutral thresholds, classifications, reliability/episode domain model and ports; no migration, API or consumer wiring.
2. `US-55-HANDLE-GPS-EDGE-CASES-CS02-CANONICAL-SIGNAL-CONTRACT-001` — version optional tamper/battery quality signals and provider capabilities with V1 compatibility; no persistence change without separate authorization.
3. `US-55-HANDLE-GPS-EDGE-CASES-CS03-PERSISTENCE-AUTHORIZATION-001` — review the exact forward schema/index requirement and authorize the next free migration separately.
4. `US-55-HANDLE-GPS-EDGE-CASES-CS04-EVALUATION-REDIS-DETECTOR-GUARDS-001` — wire immutable history classification, Redis trust protection, episode evaluation and detector eligibility.
5. `US-55-HANDLE-GPS-EDGE-CASES-CS05-OPERATIONS-NOTIFICATION-INTEGRATION-001` — publish minimized durable HIGH exception and IN_APP notification facts idempotently.
6. `US-55-HANDLE-GPS-EDGE-CASES-CS06-APIS-RBAC-AUDIT-001` — implement the two permissions, bounded API and minimized audit.
7. `US-55-HANDLE-GPS-EDGE-CASES-CS07-FRONTEND-001` — implement the operator page and existing-surface warnings.
8. `US-55-HANDLE-GPS-EDGE-CASES-CS08-POSTGRES-KAFKA-REDIS-CONCURRENCY-PERFORMANCE-001` — prove real infrastructure, burst, retry, ordering, idempotency and bounds.
9. `US-55-HANDLE-GPS-EDGE-CASES-TECHNICAL-CLOSURE-001`.
10. `US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001`.

Exact first implementation queue:

`US-55-HANDLE-GPS-EDGE-CASES-CS01-DOMAIN-TRUST-QUALITY-CONTRACTS-001`
