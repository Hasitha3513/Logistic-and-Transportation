# P2-02: Route Optimization and Performance Analytics

## Delivery status

- **Task:** P2-02
- **Stories:** US-20 Optimize Routes; US-22 Analyze Route Performance
- **Status:** COMPLETE
- **Module owner:** `routing`
- **Dependency:** P2-01 route revisions and the existing organization/trip module APIs

## Route optimization

`RouteOptimizer` is a framework-free deterministic heuristic. It fixes origin and destination, builds an initial stop order with nearest neighbour, then applies a bounded 2-opt improvement. The algorithm accepts at most 50 stops, rejects duplicate locations or invalid/non-finite distance data, and never returns a route longer than the original sequence.

Distances come from organization-owned `Location` latitude/longitude through the public `LocationLookup` contract and `RouteDistancePort`. The adapter uses Haversine distance. Missing coordinates produce the stable `ROUTE_OPTIMIZATION_DATA_UNAVAILABLE` business error; the implementation does not fabricate or hash fallback distances.

Optimization is a preview until explicitly applied. Apply recomputes the canonical proposal, rejects a stale or altered sequence, updates the route, and writes the next immutable route revision inside one infrastructure-provided transaction.

## Performance analytics

`RoutePerformanceCalculator` reports:

- total and completed trip counts;
- planned and average actual distance, with absolute and percentage variance;
- planned and average actual duration, with absolute and percentage variance;
- on-time and delayed completed-trip counts; and
- average delay minutes.

Routing consumes Trip-owned actual odometers, actual timestamps, statuses and delay-event totals through the exposed provider-neutral `RoutePerformanceTripLookupPort`. `TripRoutePerformanceAdapter` implements that contract without exposing Trip repositories or entities. When dates are omitted, the query uses a bounded 30-day window; explicit ranges must be ordered and cannot exceed 366 days.

## REST and authorization

| Method | Endpoint | Authority | Purpose |
|---|---|---|---|
| POST | `/routes/{id}/optimize` | `ROUTE_UPDATE` | Produce a non-persistent optimization preview |
| POST | `/routes/{id}/apply-optimization` | `ROUTE_UPDATE` | Revalidate and apply the canonical sequence |
| GET | `/routes/{id}/performance` | `ROUTE_VIEW` or `REPORT_VIEW` | Read route performance for an optional date range |

Existing authorities are reused to preserve least privilege and avoid a data migration solely for an equivalent operation. Authentication and authority checks execute before controller/domain mutation. Errors use the existing `ApiError` and correlation-ID handling.

## Frontend

The route details experience includes:

- an Ant Design optimizer modal with ordered before/after stop sequences, distance and duration comparison, and explicit apply confirmation;
- an optimizer action visible only to `ROUTE_UPDATE` actors for eligible active multi-stop routes; and
- a performance section backed by the real API with loading, error and no-history states.

React does not calculate optimization or performance business results.

## Persistence and migrations

P2-02 adds no schema migration. Preview and analytics are computed; applying an optimization uses P2-01 route revision storage. The validated Flyway range remains V1–V30.

## Verification

| Gate | Result |
|---|---|
| `./mvnw clean test` | **PASS** — 756 discovered, 0 failures, 0 errors, 22 conditionally skipped |
| Spring Modulith and architecture rules | **PASS** — 22/22 |
| `./mvnw verify` | **PASS** — executable jar produced; same 756-test result |
| `npm run lint` | **PASS** — 0 warnings |
| `npm test` | **PASS** — 191/191 Vitest tests |
| `npm run build` | **PASS** — production bundle generated |
| Focused route-intelligence Playwright | **PASS** — 18/18 executions across Chromium, Firefox and WebKit |
| Full Playwright regression | **PASS** — 228/228 executions (76 logical scenarios × 3 browsers) |

Coverage includes algorithm edge cases and determinism, service behavior, web mapping, 401/403/success authorization, persistence-backed performance queries, frontend rendering/error behavior, and Chromium/Firefox/WebKit workflows for preview, apply/revision, and analytics.

## Explicit limitations

- No real-time traffic, turn-by-turn navigation, commercial map provider, or road-restriction feed is introduced.
- Estimated duration scales the existing route duration by distance; it is not a live travel-time matrix.
- Geographic delay-hotspot clustering and average-speed visualization require richer telemetry and are deferred beyond this slice.
