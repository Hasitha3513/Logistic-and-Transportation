# US-48 CS07 RBAC, Audit and Security — Technical Evidence

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS07-RBAC-AUDIT-SECURITY-001` is complete at Flyway V76. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining. No migration, permission, public route, frontend feature, event, outbox or US-49 work was introduced.

## RBAC, Tenant authority and routes

All installed-provider, provider-connection, discovery, device-binding, device lifecycle and Vehicle-association management routes remain protected by `TRACKING_DEVICE_MANAGE` in both the literal HTTP security chain and controller method security. `TRACKING_VIEW` remains current-state/device-summary read-only and `TRACKING_HISTORY_VIEW` remains telemetry-history-only. Tenant authority comes exclusively from authenticated active membership through `CurrentTenant`; request DTOs expose no Tenant, audit actor, lease, watermark, test-state or provider-authority mutation fields.

Connection and binding reads/writes predicate on Tenant. Cross-Tenant identifiers are not-found-shaped or stale-shaped and same-Tenant database constraints remain final composition authority. The internal coordinator/ingestion path remains non-web and derives Tenant, provider, device, binding and lease authority only from current persisted ACTIVE rows. Existing fail-closed coordinator, rebind, lifecycle and signed-ingress matrices remain unchanged.

## Credential, endpoint and observability security

Only opaque credential references are persisted. Management responses expose `credentialConfigured`, never the reference. Test, discovery and coordinator execution resolve a fresh transient `char[]` through `IntegrationSecretResolver` and clear it after use. Safe configuration continues to reject secret-like keys; conflicts and provider failures use bounded safe codes. Reviews found no credential, external identity, endpoint, Tenant or raw provider payload in metrics/health labels or details.

The Flespi adapter now accepts only HTTPS on default port 443 to `flespi.io` or a provider-controlled `*.flespi.io` hostname, with no URI credentials, query or fragment. This adapter-level allowlist rejects loopback, private, link-local, metadata and arbitrary internal targets and constrains DNS authority to the provider zone. The JDK client keeps certificate/hostname validation, never follows redirects, uses a three-second connect timeout and bounded request deadlines, and reads at most the configured response limit plus one byte.

## Audit and concurrency

Provider connection create/update/credential-reference replacement/test/activate/disable/retire and device provider bind/rebind/lifecycle changes write safe `tracking_audit_event` facts in the same transaction as committed state. Device create/update/lifecycle and Vehicle association audit behavior remains intact. Facts contain only action, logical target, safe lifecycle/result category, actor, Tenant and time—never credential references, secrets, raw configuration, external device references, payloads, coordinates, signatures or nonces. Authenticated forbidden/not-found management commands are captured best-effort after denial without changing the denied response; telemetry packets remain unaudited.

Optimistic versions continue to reject stale provider/binding/device mutations. RETIRED remains terminal. Credential rotation affects the next execution without stale-reference overwrite; disable-versus-test and retire-versus-update rely on the same version predicate, and cross-Tenant rebind fails before partial state. Existing deterministic matrices remain Tracking 9/9, binding 4/4, coordinator 8/8 and Flespi cutover 6/6.

## Verification

- Literal MockMvc security and signed-ingress matrix: 20/20 PASS.
- Focused Flespi endpoint/secret tests: 15/15 PASS.
- Provider-connection and device-binding PostgreSQL acceptance: 19/19 PASS using only `transport_logistics_acceptance`; Flyway V1→V76 PASS.
- Complete Tracking regression: 105/105 PASS.
- Full Maven: 1,506 tests, 0 failures, 0 errors, 15 skipped; BUILD SUCCESS in 08:49.
- Architecture/Modulith/table ownership/provider containment: 49/49 PASS.
- Checkstyle: 0 violations. SpotBugs: 0 findings.
- PMD: no unsuppressed CS07 findings; direct repository-wide check reports 95 pre-existing unrelated findings.
- Controlled PostgreSQL-backed Chromium: 11/11 PASS in 37.3 seconds; sustained 374.5 msg/s and burst 1,039.9 msg/s. Two preceding unchanged complete runs passed all 10 functional cases but missed the burst threshold at 945.6 and 837.1 msg/s; no assertion was weakened.
- `git diff --check`: PASS.
- `DEVELOPMENT_DATABASE_AUTHORITATIVE_EVIDENCE = NO`; the development database was not used or modified.

## Scope exclusions and next task

Physical FMC130/flespi evidence remains mandatory. US-49 must not start. Next: `US-48-PLUGGABLE-DEVICE-ONBOARDING-CS08-FRONTEND-PROVIDER-CONNECTIONS-001`.
