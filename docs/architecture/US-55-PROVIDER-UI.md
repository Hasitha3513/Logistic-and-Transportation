# US-55 Provider UI

## Result

`US-55-PROVIDER-UI` is complete at Flyway V100. It adds no migration, permission,
dependency, public API or provider protocol. Accounting remains 73/87 and US-55 remains
`TECHNICALLY_COMPLETE / IMPLEMENTATION_COMPLETE_ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`.

## Implemented journey

The permission-controlled Provider Connections page now presents the governed onboarding states
separately: connection saved, access verified, device bound, same-Tenant Vehicle associated,
polling enabled and telemetry received. It never treats a successful connection test as telemetry.
It uses the existing provider registry and management APIs for Flespi Cloud and Traccar, accepts
only opaque credential references, explains device-side configuration, and links to the existing
DRAFT-first device/binding/association/activation workflow.

Flespi and Traccar expose only their supported optional `overlapSeconds` safe setting. Traccar
private/self-hosted endpoint approval is explicitly deployment-admin managed and has no Tenant UI.
Generic signed-HMAC ingress remains explicitly available as the governed push path and is not
misrepresented as a polling adapter. Discovery remains capability-gated; both installed polling
adapters currently use governed manual external-device identity entry.

Safe actions cover authentication failure, unreachable/invalid configuration, unapproved endpoint
responses, response overflow, missing device/Vehicle preparation, absent telemetry and stale health
facts using only backend-approved categories. Authentication expiry/logout continues to clear the
entire TanStack Query cache. Backend Tenant/RBAC checks remain authoritative.

## Verification

- Provider UI, device onboarding and schema Vitest: 30/30 PASS.
- Complete Vitest: 341/341 PASS across 85 files.
- TypeScript: PASS.
- Production build: PASS (5,274 modules; existing bundle-size advisory only).
- Changed-file ESLint: PASS with zero errors or warnings.
- Architecture/Modulith: 59/59 PASS.
- Real PostgreSQL-backed Chromium provider/device onboarding continuity: 13/13 PASS against
  `transport_logistics_acceptance`; controlled provider fixtures are technical evidence only.
- Physical/provider acceptance: not executed; controlled fixtures are technical evidence only.

## Limitations and next queue

No browser can provision provider credentials, expand the Traccar destination allowlist, configure
a physical device, or prove live telemetry. Those remain deployment/provider/operator actions.
The open acceptance queue remains `US-55-HANDLE-GPS-EDGE-CASES-FINAL-ACCEPTANCE-001`.
The next independent queue is `US-55-HEALTH-RECOVERY`.
