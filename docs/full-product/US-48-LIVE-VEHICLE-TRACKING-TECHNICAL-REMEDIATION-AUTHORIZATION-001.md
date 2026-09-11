# US-48 Live Vehicle Tracking — Technical Remediation Authorization

**Task:** `US-48-LIVE-VEHICLE-TRACKING-TECHNICAL-REMEDIATION-AUTHORIZATION-001`  
**Decision:** `PASS / REMEDIATION_AUTHORIZED`  
**Story state:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`  
**Technical closure:** `FAIL / REMEDIATION_REQUIRED`  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**Current Flyway head:** V73  
**Authorized migration:** V74, required but not created by this task  
**Next task:** `US-48-LIVE-VEHICLE-TRACKING-TECHNICAL-REMEDIATION-001`

## Source findings

The failed closure findings are confirmed. `TrackingIngestionController` derives Tenant authority from caller-controlled `X-Tracking-Tenant`; `TrackingIngressGuard` records only received and rate-limited counters; `JdbcTrackingStore` has no deterministic latest-projection rebuild; replay protection is scoped by caller Tenant and provider alias; and the required too-old, denied-command, provider-configuration, retention-policy, health and negative-security evidence is incomplete.

The accepted US-73 implementation cannot represent inbound telematics configurations: it is deliberately limited to `FILE_EXCHANGE / FILE_JSON_V1 / OUTBOUND`. Tracking must not access its tables or repositories and telemetry packets must not traverse its exchange machinery. Remediation may reuse only the published provider-neutral `IntegrationSecretResolver` to resolve an opaque credential reference. P1-01 remains unchanged: US-48 emits no per-packet event and activates no durable event family.

## Authorized trust and authentication model

Tenant authority is derived only from a trusted, active Tracking provider binding. The external request supplies an opaque `X-Tracking-Provider-Key-Id`, never a Tenant identifier or secret. The exact chain is:

1. Parse the bounded provider key ID, asserted provider alias, timestamp, nonce, signature and body without establishing Tenant context.
2. Resolve exactly one active `tracking_provider_binding` by globally unique provider key ID.
3. Derive trusted Tenant, provider alias and opaque credential reference from that binding.
4. Resolve secret material through US-73's `IntegrationSecretResolver`; never persist or expose the secret.
5. Require the asserted provider alias to equal the binding alias and validate timestamp within the frozen signed-request allowance.
6. Verify HMAC over `epoch + "\n" + nonce + "\n" + providerKeyId + "\n" + providerAlias + "\n" + rawBody`.
7. Reserve the nonce against the trusted binding identity, not a caller-declared Tenant.
8. Resolve the registered device only inside the derived Tenant, require its provider alias to match, and resolve the effective Vehicle association at source time.
9. Ingest under the derived Tenant context.

All authentication/configuration failures return the same sanitized `401 TRACKING_PROVIDER_UNAUTHORIZED`. There is no cross-Tenant fallback. A human JWT cannot authenticate provider ingress. A Tenant header or payload field, if temporarily accepted for compatibility, is only a signed consistency assertion and cannot select or override Tenant; removal is preferred before acceptance.

`INGEST_AUTH_CONTRACT_CHANGE_REQUIRED = YES`. The provider authentication headers/canonical signature change before acceptance, but the accepted human REST paths, methods and JSON bodies do not change, so `PUBLIC_API_CHANGE_REQUIRED = NO` and no compatibility promise is made for the defective ingress authentication scheme.

## V74 authorized persistence

`V74_REQUIRED = YES`. V74 is the next free number. V1–V73 are immutable. Exactly one forward migration is authorized for these Tracking-owned structures only:

### `tracking_provider_binding`

- UUID `id`; UUID `tenant_id`; globally unique bounded `provider_key_id`; bounded `provider_alias`; opaque `credential_reference`; `ACTIVE|DISABLED` lifecycle; created/updated actor and timestamps; optimistic `version`.
- `(tenant_id, id)` is unique for Tenant-consistent references and Tenant-leading indexes support management access.
- The same textual provider alias may exist in multiple Tenants. Alias alone is never authority.
- Tenant association is immutable. Rebinding requires disabling the old binding and creating a new one.
- No plaintext credential and no physical reference to an Integration table.

### `tracking_provider_ingest_nonce`

- UUID `tenant_id`; UUID `provider_binding_id`; nonce hash; used/expiry timestamps.
- Tenant-consistent same-module foreign key to the provider binding and uniqueness on `(provider_binding_id, nonce_hash)`.
- Runtime replay protection moves to this trusted scope. The V73 nonce table is retained, not destructively rewritten or used as an authority fallback.

### `tracking_retention_policy`

- One current Tenant-owned policy record containing UUID policy identity, UUID `tenant_id`, positive retention duration, policy version, effective time, created/updated actor and timestamps, and optimistic version.
- Absence of a row means external retention policy is unconfigured: automatic purge remains disabled and age alone cannot produce `TRACKING_POSITION_TOO_OLD`.
- The migration adds no purge scheduler and changes no immutable position history.

No geofence, speed, idle, route-deviation, replay UI, dashboard, GPS-edge detector, Customer tracking, Kafka or outbox structure is authorized.

## Retention and `TOO_OLD`

At receipt time, a configured policy yields the retained boundary `receivedAt - retentionDuration`. A source timestamp strictly before the boundary is rejected as `TRACKING_POSITION_TOO_OLD`; equality is accepted. An accepted position records the applicable policy identity/version and computed `retainUntil`. With no configured policy, `retainUntil` remains absent, no age-only too-old rejection occurs, and the existing greater-than-24-hour `LATE` classification still applies. No automatic deletion is authorized.

Mandatory tests cover inside, exact, and older-than boundary plus the unconfigured-policy case.

## Projection rebuild

Authorize a Tracking-internal application maintenance use case and repository operation; no public endpoint is authorized. It rebuilds one Tenant/Vehicle per transaction from retained immutable `tracking_position` history only:

- `latestReceived`: greatest deterministic tuple `(received_at, source_timestamp, provider_sequence with null below a present sequence, dedupe_identity)` among valid retained rows.
- `latestTrusted`: greatest tuple using the same ingress compare-and-set order among rows classified `TRUSTED` and eligible to advance trusted state.
- `last_successful_receipt_at`: maximum retained valid receipt time; policy metadata comes from the selected/current applicable policy.
- Empty history deletes only that Vehicle's derived projection. Two runs over unchanged history produce the identical projection.

The test may delete/reset derived projection data only, then rebuild twice and prove exact `latestReceived` and `latestTrusted`. No Fleet, Trip or other module table participates.

## Observability and health

Add only the missing Tracking telemetry: accepted, duplicate, conflict, invalid, late, out-of-order and untrusted counters; processing and database latency timers; bounded ingestion-rate measurement; provider-authentication failures; latest successful ingest; and stale/offline device gauges. Retain received and rate-limited counters.

Labels are restricted to bounded provider alias and result/order/trust categories. Tenant is omitted unless an existing platform metric policy expressly permits it. Device/Vehicle UUID, coordinates, nonce, signature, credential identity, Driver and Customer data are forbidden labels.

Add a sanitized Tracking health contributor under the existing management health surface, not a new public API. It reports configured/reachable credential state, last successful ingest, ingestion lag and stale-device count. A stale device does not make the provider `DOWN`; missing, disabled or unresolvable provider configuration may fail that provider component closed.

## Audit authorization

Use `tracking_audit_event` for provider-binding create/activate/disable/credential-reference changes, retention-policy changes and denied Tracking management commands. Tenant, actor, action/route, safe logical target, reason code, timestamp and correlation ID are allowed. Secrets, credential references, coordinates, signatures, nonces and raw payloads are prohibited. Tenant association is not mutated; replacement is represented by disable/create audit events. No audit row is written per telemetry packet.

## Mandatory remediation verification

The remediation task must add and pass:

- Dedicated security cases: valid signature; invalid signature; expired and excessive-future timestamps; nonce replay; unknown and disabled binding; wrong credential; wrong device; provider/device Tenant mismatch; human JWT denial; guessed foreign device safe denial; and Tenant header/payload override denial.
- Freshness boundaries: 59s LIVE, 60s LIVE, greater than 60s RECENT, 5m RECENT, greater than 5m STALE, no trusted point UNKNOWN, and recent receipt without a trusted point not LIVE.
- Connectivity boundaries: at most 60s CONNECTED, greater than 60s through 5m DEGRADED, greater than 5m OFFLINE, and no receipt UNKNOWN.
- PostgreSQL evidence for V74, binding uniqueness/Tenant consistency, binding-scoped nonce replay, retention boundaries, deterministic rebuild/idempotency, append-only history, dedupe/conflict isolation, projection correctness, rollback atomicity, association-at-source-time, Tenant A/B, index-backed plans and all existing nine deterministic races.
- Existing focused backend, security, architecture/Modulith, Checkstyle, PMD, SpotBugs, full Maven, TypeScript, Vitest, production build, changed-file lint and complete real PostgreSQL-backed Chromium suites.

No test may weaken the frozen association, dedupe, ordering, privacy or Tenant rules. Final physical provider/device acceptance remains a later independent gate.

Performance regression must preserve at least 200 accepted messages/second sustained, 1,000/second burst, latest-query p95 at most 200 ms and 24-hour single-Vehicle history-page p95 at most 500 ms. Provider-key lookup must be bounded and indexed. Credential resolution must use a bounded, rotation-aware configuration/cache strategy if the resolver is external; it may not persist plaintext or route each telemetry packet through US-73 persistence/network processing.

## Remediation stop conditions

Stop and return to architecture/product authority if implementation would require a human-facing public API, a new permission or event family, a change to accepted US-73 configuration semantics, direct Integration persistence access, a second migration, modification of V1–V73, destructive history rewriting, US-49..55 behavior, or changed product/accounting semantics. Ordinary defects in the authorized implementation or its tests are remediation work, not reasons to broaden scope.

## Rollback and containment

Disable the affected binding to fail closed; never restore the caller-Tenant fallback. Rebuild only derived latest projections from retained history. Observability may be independently disabled if operationally unsafe, without disabling authentication. Schema correction is forward-only; V73 and immutable accepted position history are never rewritten. No product decision changes, new permissions, new domain event, new dependency, story accounting change or US-49 work are authorized.

## Authorization result

The remediation is contained within the frozen US-48 contract. It does not reopen US-48 product decisions or accepted US-73/P1-01 boundaries. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING`, technical closure remains `FAIL / REMEDIATION_REQUIRED`, and Wave C cannot advance until the remediation and independent closure rerun pass.
