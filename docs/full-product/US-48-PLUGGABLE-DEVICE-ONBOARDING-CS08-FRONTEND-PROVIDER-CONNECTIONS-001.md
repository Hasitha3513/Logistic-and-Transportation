# US-48 CS08 Provider Connections Frontend — Technical Evidence

## Result

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS08-FRONTEND-PROVIDER-CONNECTIONS-001` is complete. US-48 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`; accounting remains 72/87 complete with 15 remaining and Flyway remains V76. No backend API, migration, permission, event, device-onboarding workflow, US-49 work, application commit or application push was introduced.

## Route, navigation and layout

The existing Tracking navigation now has **Live Vehicles**, **Devices**, and permission-controlled **Provider Connections** children. `/tracking/provider-connections` renders the enterprise management page, while `/tracking/devices` opens the existing device tab. `AppLayout` remains the sole owner of the global header, breadcrumb, page title and shell; the Provider Connections page adds none of those elements and uses a standard table toolbar without floating actions or redundant card nesting.

## Provider connection experience

The page uses the existing CS06 routes and backend-supplied provider descriptors. It provides bounded server pagination, recoverable loading/error/empty states, safe details, truthful capability tags, DRAFT-first create, optimistic update, Test Connection, activate, disable and permanent retire actions. RETIRED rows expose details only. Discovery is rendered only when the selected installed adapter reports `DISCOVERY`, and its bounded masked preview never performs CS09 device onboarding.

React Hook Form and Zod validate the user-facing CS06 bounds: display/alias lengths, HTTPS endpoint, five-to-86,400-second polling, page size 1–500, and an object-only safe configuration capped at 8 KiB with string values and secret-like keys rejected. Because CS06 exposes no structured safe-configuration field schema, a bounded JSON object editor is used. Backend adapter validation and SSRF protection remain authoritative.

Existing credential references are never returned or rendered. Create/replacement accepts only an opaque resolver reference through a password-style input, edit starts blank, and blank update preserves the backend-held reference. UI, query keys, local storage, URLs, notifications and safe error mapping expose no stored reference, provider token, authorization header, SQL detail, raw provider body or Axios object.

## Verification

- Focused Provider Connections/validation Vitest: 18/18 PASS.
- Complete frontend Vitest: 283/283 PASS across 66 files.
- TypeScript project check: PASS.
- Production build: PASS (5,229 modules transformed); the existing bundle-size advisory remains non-blocking.
- Changed-file ESLint: PASS with zero CS08 findings.
- Repository-wide ESLint: 71 pre-existing findings, all in unrelated Delivery analytics/batches/orders/riders/slots/zones files; zero CS08 findings.
- Provider Connections real PostgreSQL-backed Chromium journey: 10/10 PASS against `transport_logistics_acceptance`.
- Complete Tracking Chromium: 21/21 PASS in 51.9 seconds; sustained 258.5 msg/s and burst 1,024.1 msg/s. One preceding unchanged run passed 20/21 but measured the existing burst gate at 966.5 msg/s; no assertion was changed.
- `git diff --check`: PASS.

## Next task

`US-48-PLUGGABLE-DEVICE-ONBOARDING-CS09-FRONTEND-DEVICE-ONBOARDING-001`
