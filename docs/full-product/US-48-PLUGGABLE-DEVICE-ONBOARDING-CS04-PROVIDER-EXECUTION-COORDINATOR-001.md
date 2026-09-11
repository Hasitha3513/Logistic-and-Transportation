# US-48 CS04 Provider Execution Coordinator — Technical Evidence

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS04-PROVIDER-EXECUTION-COORDINATOR-001` is complete at Flyway V76. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining.

## Coordinator and execution authority

Tracking now has one feature-flagged, database-discovered coordinator entry point. It is disabled by default until CS05. The coordinator claims bounded batches of due ACTIVE provider connections with PostgreSQL `FOR UPDATE SKIP LOCKED`, records a runtime-stable lease owner and expiry, and requires the current owner for renewal or release. Expired work is recoverable; stale owners cannot complete reclaimed work.

Execution uses a fixed-size worker pool and bounded queue, connection single-flight protection and bounded provider-type semaphores. Saturation releases and reschedules claimed work instead of creating unbounded threads or queues. There is no scheduler or thread per device. Graceful shutdown is bounded and closes adapter resources.

## Provider dispatch, secrets and batching

Provider execution dispatches through the existing immutable `TrackingProviderAdapterRegistry`; there is no provider switch. Every job reloads its ACTIVE connection authority and current configuration after claim, resolves the opaque credential reference through `IntegrationSecretResolver`, and clears the transient character buffer after use. Credential values, device secrets and payloads are absent from metrics, health details and persisted errors.

Due ACTIVE device bindings are discovered dynamically in Tenant-scoped, bounded pages. Hot additions become eligible without restart; disabled devices, bindings or providers stop future work. Per-connection poll/page limits, a bounded response size, job deadline and per-provider concurrency quota protect resources and preserve fair polling.

## Internal ingestion and watermarks

`TrackingProviderIngestionPort` is an internal, non-web provider-neutral ingress. Its adapter reloads the leased ACTIVE connection, trusted Tenant, ACTIVE binding and ACTIVE device under database locks before delegating to the existing normalized Tracking ingestion use case. It therefore preserves source-time Vehicle association, deduplication, trust qualification, retention and latest-received/latest-trusted behavior. The signed HMAC/nonce external ingress is unchanged and the coordinator performs no HTTP/HMAC loopback.

Each normalized candidate receives an ACCEPTED, DUPLICATE or REJECTED outcome. Binding source/message watermarks advance only for ACCEPTED or DUPLICATE facts and remain unchanged for rejection or failed batches. Per-candidate transactions provide partial-batch safety and at-least-once retry semantics. Connection and binding next-poll cursors are persisted; failures use safe bounded categories and backoff.

## Observability and scale

Health exposes only aggregate enabled/running state and active-job counts; one provider failure does not make Tracking unhealthy. Metrics use bounded provider/result/category labels. There is no packet audit, event, outbox or Integration packet routing.

The PostgreSQL acceptance proof covered 100-device bounded/fair paging, multiple connections and Tenants, dynamic add/disable, provider disable, rebind races, exact-once optimistic watermark advancement and an index-backed due-work plan at 10,000 bindings. Runtime objects remained bounded with zero per-device threads and a maximum page size of 100.

## Verification

- Focused CS04 tests: 10/10 PASS.
- Existing Tracking regression selection: 91/91 PASS, including existing Tracking concurrency 9/9 and binding concurrency 4/4.
- Coordinator concurrency: 8/8 PASS (seven PostgreSQL lease/authority races plus one bounded-worker saturation race).
- Full Maven: 1,492 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 08:41.
- Architecture/Modulith: 52/52 PASS.
- Checkstyle: 0 violations. SpotBugs: 0 findings.
- PMD: zero unsuppressed CS04 findings; the direct repository-wide goal still reports 95 pre-existing findings outside the CS04 change set.
- Complete controlled PostgreSQL-backed Chromium rerun: 11/11 PASS in 36.3 seconds. Sustained measurement: 346.5 msg/s for 200 messages. Burst measurement: 1,153.6 msg/s for 1,000 messages.
- The preceding combined Chromium attempt passed all 10 functional cases but measured 943.0 msg/s for the burst and failed the threshold. An isolated diagnostic rerun passed at 1,137.0 msg/s; the complete suite was then rerun and passed 11/11. No assertion was weakened.
- `git diff --check`: PASS.
- All accepted PostgreSQL evidence used only `transport_logistics_acceptance`; the development database was not used.

## Scope exclusions

No migration or V77, Flespi SPI cutover, public API, frontend, permission, event family, cross-module persistence or US-49 work was introduced. Physical FMC130/flespi capture and external final acceptance remain mandatory.

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS05-FLESPI-SPI-MIGRATION-001`
