# US-55 GPS Edge Cases — CS03 Persistence Authorization

## Verdict

`PASS — IMPLEMENTATION_IN_PROGRESS / CS03_COMPLETE`

CS03 adds the smallest provider-neutral persistence required by the approved canonical Telemetry V2 contract. Story accounting remains 73 / 87. No physical-device acceptance is claimed.

## Persistence gap disposition

| Concern | Existing authority | CS03 disposition |
| --- | --- | --- |
| Provider registration and credential reference | V75 `tracking_provider_binding` | Reused; only opaque credential references are stored. |
| Device identity and polling watermark | V76 device/provider binding | Reused; Tenant/provider identity and restart-safe cursor behavior remain unchanged. |
| Vehicle binding | V73 effective-dated assignment | Reused; source-time resolution, overlap rejection and immutable history remain authoritative. |
| V2 signal evidence | V86/V87 Timescale history lacked optional signals | V96 adds nullable, constrained V2 evidence columns. |
| Capabilities | CS02 framework-neutral lookup port only | V96 adds effective-dated, Tenant/device-qualified capability history and a JDBC lookup adapter. |
| Deduplication | V86/V87 Tenant/source-time/canonical identity | Reused across V1/V2; event version is not part of identity. |
| Historical immutability | Intended append-only history | V96 adds database enforcement for update/delete rejection. |
| Live projection | Redis derived state | Unchanged; Redis remains disposable and non-authoritative. |

## V96 migration

`V96__persist_us55_telemetry_signals_and_capabilities.sql`:

- permits historical event versions 1 and 2;
- adds nullable `tamper_state`, `battery_level_percent`, `battery_voltage_volts`, `external_power_state` and `battery_charging_state`;
- constrains approved enums, numeric bounds and scale, and prohibits V2-only evidence on V1 rows;
- makes `tracking_position_history` database-enforced append-only;
- creates `tracking_device_telemetry_capability` with same-Tenant device integrity, approved capability/state vocabulary, half-open effective intervals, active uniqueness, overlap rejection and append-only/close-only history;
- adds only the Tenant-leading index needed by source-time capability lookup.

V1→V96, V95→V96, repeat startup, existing compressed-chunk preservation, exact metadata, index readiness/validity, transactional failure rollback and safe retry all pass. A simulated transactional failure leaves Flyway at V95 with neither partial V96 object nor invalid index. V1–V95 remain unchanged.

## Security and data integrity

- All lookup and uniqueness paths are Tenant-qualified.
- Cross-Tenant capability/device resolution returns safe absence; same external identity remains independent across Tenants under the retained authority model.
- Provider secrets, tokens, credentials, signatures and raw payloads are neither added to schema nor exposed by the adapter.
- Missing optional evidence remains `NULL`; explicit provider `UNKNOWN` remains a persisted, distinct enum value.
- Invalid numeric range or scale is rejected by PostgreSQL.
- V1 rows remain readable and cannot contain V2-only evidence.
- Provider disablement and Redis loss do not delete authoritative history.
- All destructive fixtures verify `transport_logistics_acceptance`; no development database was used.

## Verification

| Gate | Result |
| --- | --- |
| Focused domain/application/provider regression | 44 / 44 PASS |
| Focused V96 and Timescale persistence | 13 / 13 PASS |
| Complete PostgreSQL/Timescale/provider group | 47 / 47 PASS |
| Direct full-suite remediation rerun | 18 / 18 PASS |
| Architecture and Spring Modulith | 59 / 59 PASS |
| Complete clean Maven suite | 1,867 / 1,867 PASS; 0 skipped; BUILD SUCCESS |
| Checkstyle | 0 violations; BUILD SUCCESS |
| PMD | BUILD SUCCESS |
| SpotBugs | 0 findings; BUILD SUCCESS |
| Dependency analysis | BUILD SUCCESS; existing starter/transitive classification warnings only |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The first complete-suite invocation used the default unavailable localhost port and produced PostgreSQL connection cascades plus three stale V95 head assertions. The assertions were corrected factually, their focused rerun passed 18 / 18, and the authoritative complete rerun used only the isolated acceptance database on port 5433 and passed 1,867 / 1,867.

Frontend and Chromium were not rerun: CS03 changes no REST, UI, permission or consumed frontend contract.

## Rollback and residual risk

Before successful deployment, V96 is transactional: failure rolls back all V96 objects and leaves Flyway at V95. After deployment V96 is immutable; rollback is application-compatible because all new history columns are nullable and consumers remain dual-version. Any later schema reversal requires a separately governed forward migration.

Physical tamper, battery and provider-loss fidelity remains an independent final-acceptance requirement. Fixtures prove persistence mechanics only.

## Next queue

`US-55-HANDLE-GPS-EDGE-CASES-CS04-EVALUATION-REDIS-DETECTOR-GUARDS-001`
