# US-48 FMC130 / Flespi External Capture

**Task:** `US-48-FMC130-FLESPI-EXTERNAL-CAPTURE-001`  
**Document state:** `TEMPLATE / NOT_EXECUTED`  
**Result:** `NOT_EXECUTED / BLOCKED_EXTERNAL_SYSTEM`  
**Authoritative database:** `transport_logistics_acceptance` only  
**Flyway expected head:** V76

Do not mark this PASS with a fixture, simulator, manually injected Flespi message or non-FMC130 source.
Never paste a Flespi token, credential reference value, full IMEI/serial, SIM identifier, provider account
identifier, signature, nonce or unrestricted raw payload into this document.

## Execution identity

| Fact | Evidence |
| :--- | :--- |
| UTC execution start/end | `NOT_EXECUTED` |
| Application commit/branch | `NOT_EXECUTED` |
| Acceptance DB identity and Flyway head | `NOT_EXECUTED` |
| Tenant / Vehicle UUID (masked) | `NOT_EXECUTED` |
| Physical FMC130 IMEI/serial (masked) | `NOT_EXECUTED` |
| Flespi channel/device IDs (masked) | `NOT_EXECUTED` |
| Operator / witness | `NOT_EXECUTED` |

## Genuine provider message

Attach a sanitized screenshot/export proving the message originated from the physical FMC130 through its
real Teltonika channel. Record the provider receipt time and application receipt time. Unsanitized raw
payload is temporary, access-limited evidence and must be deleted after validation.

| Application fact | Flespi field | Observed value/status | Verification |
| :--- | :--- | :--- | :--- |
| External device identity | `ident` | `NOT_EXECUTED` | Exact configured identity; display masked |
| Source timestamp | `timestamp` | `NOT_EXECUTED` | Preserved; distinct from `received_at` |
| Latitude | `position.latitude` | `NOT_EXECUTED` | Numeric WGS84 range |
| Longitude | `position.longitude` | `NOT_EXECUTED` | Numeric WGS84 range |
| Accuracy (metres) | `position.accuracy` | `NOT_EXECUTED` | Preserve non-negative value or `NOT_PRESENT_IN_CAPTURE` |
| Speed | `position.speed` | `NOT_EXECUTED` | Preserve valid observed provider value or `NOT_PRESENT_IN_CAPTURE` |
| Heading | `position.direction` | `NOT_EXECUTED` | Preserve `[0,360)` or `NOT_PRESENT_IN_CAPTURE` |
| Ignition | not requested/advertised | `NOT_EXECUTED` | Do not infer; normally `NOT_PRESENT_IN_CAPTURE` |
| Odometer | not requested/advertised | `NOT_EXECUTED` | Do not infer; normally `NOT_PRESENT_IN_CAPTURE` |
| Engine hours | not requested/advertised | `NOT_EXECUTED` | Do not infer; normally `NOT_PRESENT_IN_CAPTURE` |
| Message identity | not requested/advertised | `NOT_EXECUTED` | Do not synthesize provider identity |
| Sequence | not requested/advertised | `NOT_EXECUTED` | Do not synthesize provider sequence |

Sanitized provider excerpt: `NOT_EXECUTED`

## Runtime proof

- [ ] Provider connection test PASS and lifecycle ACTIVE.
- [ ] DRAFT device bound to the ACTIVE Flespi connection by manual external reference.
- [ ] Same-Tenant Vehicle association effective at source time; device ACTIVE.
- [ ] Coordinator fetches through the Flespi SPI; no legacy/dual poll or HMAC loopback.
- [ ] Exact source timestamp, separate `received_at`, coordinates and truthful optional values persist.
- [ ] Latest-received updates; latest-trusted updates only when the position qualifies.
- [ ] UI truthfully shows point, accuracy/UNKNOWN, LIVE and CONNECTED.
- [ ] No token/reference/raw payload/full identity/Driver PII/Customer location exposure.

## Adversarial and recovery proof

| Scenario | Expected | Actual |
| :--- | :--- | :--- |
| Duplicate/overlap fetch | Idempotent; no second business fact | `NOT_EXECUTED` |
| Delayed/out-of-order provider message | Immutable history; trusted latest not regressed | `NOT_EXECUTED` |
| Invalid coordinate | Rejected; no trusted-latest advance | `NOT_EXECUTED` |
| Pause >60s / >5m | RECENT/STALE and DEGRADED/OFFLINE independently | `NOT_EXECUTED` |
| Restore physical telemetry | LIVE/CONNECTED recovery without restart | `NOT_EXECUTED` |
| Disable device binding | Fetch/ingest stops; history retained | `NOT_EXECUTED` |
| Disable provider connection | Provider work stops; history retained | `NOT_EXECUTED` |
| Credential rotation | Test/recovery succeeds; no secret exposure | `NOT_EXECUTED` |
| Tenant B access | Denied without existence leakage | `NOT_EXECUTED` |
| Limited-role management/location | Denied by backend permissions | `NOT_EXECUTED` |

## Result

`NOT_EXECUTED`. PASS requires every mandatory row/check above plus genuine physical-device provenance.
BLOCK on missing hardware/provider authority, unresolved credential, inability to prove physical origin,
missing mandatory mapped fields, unsafe data exposure, Tenant/RBAC failure, incorrect latest/freshness
semantics, or any requested contract expansion. After PASS, execute exactly
`US-48-LIVE-VEHICLE-TRACKING-FINAL-ACCEPTANCE-001`.

