# US30-CARGO-EXCEPTION-IMPLEMENTATION-001 — Handle Cargo Exceptions (US-30)

## 1. Requirement & Scope Summary

- **Story**: US-30 — Handle Cargo Exceptions
- **Task**: P2-CARGO-EXCEPTION-001
- **Goal**: Implement source-aligned Cargo Exception aggregate, state machine, restrictions, resolution history, RBAC, API, React frontend, and E2E acceptance.
- **Package**: `com.transportlogistics.app.freight.exception`
- **Flyway Migrations**:
  - `V40__cargo_exception_permissions.sql`: Permissions `CARGO_EXCEPTION_VIEW`, `CARGO_EXCEPTION_MANAGE` and sequence `cargo_exception_number_sequence`
  - `V41__cargo_exception_tables.sql`: Tables `cargo_exception` and `cargo_exception_history`
- **Permissions**: `CARGO_EXCEPTION_VIEW`, `CARGO_EXCEPTION_MANAGE`

---

## 2. Invariants & Business Logic

1. **Exception Classification (AC1)**:
   - Exception Types: `DAMAGE`, `PARTIAL_SHIPMENT`, `WEIGHT_DISCREPANCY`, `HAZARDOUS_MATERIAL`, `UNMANIFESTED_CARGO`, `SEAL_TAMPERING`
   - Severities: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
   - Sequential Numbering: `CEX-YYYY-NNNNNN` generated from dedicated database sequence.
   - Cross-module link to `freight_order` via UUID primitive (verified via `FreightOrderLookup` inbound port).

2. **Lifecycle State Machine & Operational Restrictions (AC2)**:
   - Initial State: `OPEN`
   - Allowed Transitions:
     - `OPEN` -> `HOLD` -> `HELD` (captures active movement/dispatch restriction)
     - `HELD` -> `RELEASE` -> `OPEN` (clears restriction upon safety/inspection sign-off)
     - `OPEN` / `HELD` -> `ESCALATE` -> `ESCALATED`
     - `ESCALATED` -> `HOLD` -> `HELD`
     - `OPEN` / `HELD` / `ESCALATED` -> `REJECT` -> `REJECTED` (closed terminal state)
     - `OPEN` / `HELD` / `ESCALATED` -> `RESOLVE` -> `RESOLVED` (closed terminal state with resolution notes)
   - Strict command-driven lifecycle endpoints. Arbitrary status mutation is forbidden.

3. **Retained Resolution History (AC3)**:
   - Every state transition or workflow command appends an immutable `CargoExceptionHistoryEntry` recording:
     - `action` (`HOLD_APPLIED`, `ESCALATED`, `RELEASED`, `REJECTED`, `RESOLVED`)
     - `actor` (authenticated user)
     - `occurredAt` (timestamp)
     - `reason` (rationale / justification)
     - `details` (restrictions applied or resolution details)
   - When closed (`RESOLVED` or `REJECTED`), full historical timeline is retained and non-destructive.

4. **Optimistic Concurrency**:
   - `version` field incremented on state mutations; mismatched versions return HTTP 409 `ConflictException`.

---

## 3. REST API Endpoints

- `GET /v1/freight/exceptions` — List exceptions with optional filters (`freightOrderId`, `manifestId`, `type`, `status`) and pagination (`CARGO_EXCEPTION_VIEW`)
- `GET /v1/freight/exceptions/{id}` — Get exception details and history (`CARGO_EXCEPTION_VIEW`)
- `POST /v1/freight/exceptions` — Record new exception (`CARGO_EXCEPTION_MANAGE`)
- `POST /v1/freight/exceptions/{id}/hold` — Apply hold and movement restriction (`CARGO_EXCEPTION_MANAGE`)
- `POST /v1/freight/exceptions/{id}/escalate` — Escalate to management (`CARGO_EXCEPTION_MANAGE`)
- `POST /v1/freight/exceptions/{id}/release` — Release hold (`CARGO_EXCEPTION_MANAGE`)
- `POST /v1/freight/exceptions/{id}/reject` — Reject exception (`CARGO_EXCEPTION_MANAGE`)
- `POST /v1/freight/exceptions/{id}/resolve` — Resolve exception with outcome (`CARGO_EXCEPTION_MANAGE`)

---

## 4. Verification & Testing

- **Domain Unit Tests**: `CargoExceptionTest` (20 tests, 100% pass)
- **Application Service Tests**: `CargoExceptionServiceTest` (9 tests, 100% pass)
- **Controller Unit Tests**: `CargoExceptionControllerTest` (10 tests, 100% pass)
- **Persistence Integration Tests**: `CargoExceptionPersistenceIntegrationTest` (1 test, 100% pass)
- **Security & Bootstrap Integration Tests**: `LocalIdentityBootstrapIntegrationTest` (100% pass)
- **Modulith & ArchUnit Architecture Tests**: `ApplicationModulesTest`, `HexagonalLayerArchitectureTest`, `ModuleBoundaryArchitectureTest`, `LombokUsageArchitectureTest` (25 tests, 100% pass)
- **Frontend Vitest Unit Tests**: `CargoExceptionPages.test.tsx` (3 tests, 100% pass)
- **Playwright E2E Tests**: `cargoExceptions.spec.ts` (8 scenarios across Chromium & Firefox, 100% pass)
  - `E2E-P2-CEX-001`: Record damage exception against freight order (AC1)
  - `E2E-P2-CEX-002`: Record partial shipment and seal tampering exceptions (AC1)
  - `E2E-P2-CEX-003`: Apply operational hold and restriction on hazardous material (AC2)
  - `E2E-P2-CEX-004`: Escalate unmanifested cargo exception to management (AC2)
  - `E2E-P2-CEX-005`: Release hold after safety clearance and verify state transition (AC2)
  - `E2E-P2-CEX-006`: Resolve cargo exception with resolution outcome and verify retained audit history (AC3)
  - `E2E-P2-CEX-007`: Reject invalid exception and confirm closed state (AC3)
  - `E2E-P2-CEX-008`: UI flow: browse list, filter by type/status, navigate to detail (AC1, AC2, AC3)
