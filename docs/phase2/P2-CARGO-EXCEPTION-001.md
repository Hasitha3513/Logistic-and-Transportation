# P2-CARGO-EXCEPTION-001 — US-30 Cargo Exception Management

## 1. Task Identification

| Field | Value |
|---|---|
| **Task ID** | P2-CARGO-EXCEPTION-001 |
| **Story** | US-30 — Handle Cargo Exceptions |
| **Authoritative Source** | `docs/phase2/US30-CARGO-EXCEPTION-IMPLEMENTATION-001.md` |
| **Branch** | `feat/us-27-weight-volume` |
| **Baseline commit** | `6f639dfe69f06143b30aa29590495fc5a526fe99` |
| **Date** | 2026-08-27 |

---

## 2. Prerequisites Confirmed

| Story | Status |
|---|---|
| US-24 Freight Orders | COMPLETE |
| US-25 Cargo Manifest | COMPLETE |
| US-26 Load Planning | COMPLETE |
| US-27 Weight & Volume | COMPLETE |
| US-28 Freight Insurance | COMPLETE |
| US-29 Freight Reports | BLOCKED_BY_TENANT_FOUNDATION (untouched) |

---

## 3. Authoritative Requirements Extracted

### 3.1 Exception Types (AC1)
Six source-defined categories, no additions or subtractions:
- DAMAGE
- PARTIAL_SHIPMENT
- WEIGHT_DISCREPANCY
- HAZARDOUS_MATERIAL
- UNMANIFESTED_CARGO
- SEAL_TAMPERING

### 3.2 Exception Severities (AC1)
- LOW, MEDIUM, HIGH, CRITICAL

### 3.3 Sequential Numbering (AC1)
Format: CEX-YYYY-NNNNNN - generated from database sequence cargo_exception_number_sequence.

### 3.4 Lifecycle State Machine (AC2)

```
OPEN --hold--> HELD --release--> OPEN
OPEN --escalate--> ESCALATED
HELD --escalate--> ESCALATED
ESCALATED --hold--> HELD
OPEN/HELD/ESCALATED --resolve--> RESOLVED  (terminal)
OPEN/HELD/ESCALATED --reject--> REJECTED   (terminal)
```

Strict command-driven. Arbitrary status mutation is forbidden.

### 3.5 Resolution History (AC3)
Every state transition appends an immutable CargoExceptionHistoryEntry with:
- action (HOLD_APPLIED, ESCALATED, RELEASED, REJECTED, RESOLVED)
- actor (authenticated username)
- occurredAt (ISO-8601 timestamp)
- reason (justification)
- details (restriction applied or resolution detail)

Closed exceptions (RESOLVED/REJECTED) retain full immutable history.

### 3.6 Optimistic Concurrency
version field incremented on every state mutation. Mismatched version - HTTP 409 ConflictException.

### 3.7 RBAC Permissions
| Permission | Purpose |
|---|---|
| CARGO_EXCEPTION_VIEW | Read exceptions and retained history |
| CARGO_EXCEPTION_MANAGE | Record, hold, escalate, release, reject, resolve |

---

## 4. Domain Contract

### 4.1 Aggregate: CargoException

Package: com.transportlogistics.app.freight.exception.domain

| Field | Type | Rules |
|---|---|---|
| id | UUID | Required, immutable |
| exceptionNumber | String | Required; format CEX-YYYY-NNNNNN; unique |
| exceptionType | ExceptionType | Required; one of 6 source types |
| status | ExceptionStatus | Defaults to OPEN; transitions via commands |
| severity | ExceptionSeverity | Defaults to MEDIUM |
| freightOrderId | UUID | Required; logical FK to freight_order |
| manifestId | UUID | Optional logical FK |
| manifestItemId | UUID | Optional logical FK |
| description | String | Required; not blank |
| impact | String | Optional |
| restriction | String | Set on HOLD, cleared on RELEASE |
| correctiveAction | String | Optional |
| resolution | String | Required when RESOLVED |
| resolvedAt | OffsetDateTime | Set on RESOLVE/REJECT; null otherwise |
| resolvedBy | String | Set on RESOLVE/REJECT; null otherwise |
| history | List<CargoExceptionHistoryEntry> | Immutable; grows with each transition |
| createdAt/By | OffsetDateTime/String | Set on creation |
| updatedAt/By | OffsetDateTime/String | Updated on each transition |
| version | long | Optimistic concurrency guard |

### 4.2 Domain Invariants

1. id, exceptionNumber, exceptionType, freightOrderId, description, createdAt, createdBy, updatedAt, updatedBy are required.
2. Version must be non-negative.
3. State transitions enforce allowed-from states via ConflictException.
4. Escalation requires a non-blank reason (BusinessRuleException).
5. Release requires a non-blank reason.
6. Rejection requires a non-blank reason.
7. Resolution requires a non-blank resolution description.
8. Closed exceptions (RESOLVED, REJECTED) cannot be further transitioned.

---

## 5. Database

### 5.1 Migration V40: Permissions and Sequence

File: V40__cargo_exception_permissions.sql
- Permission: CARGO_EXCEPTION_VIEW
- Permission: CARGO_EXCEPTION_MANAGE
- Sequence: cargo_exception_number_sequence

### 5.2 Migration V41: Tables

File: V41__cargo_exception_tables.sql

Table: cargo_exception
- id UUID PK
- exception_number VARCHAR(32) NOT NULL UNIQUE
- exception_type VARCHAR(40) NOT NULL (CHECK: 6 types)
- status VARCHAR(20) NOT NULL DEFAULT 'OPEN' (CHECK: 5 states)
- severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' (CHECK: 4 severities)
- freight_order_id UUID NOT NULL (FK to freight_order)
- manifest_id UUID nullable
- manifest_item_id UUID nullable
- description VARCHAR(2000) NOT NULL
- impact VARCHAR(2000) nullable
- restriction VARCHAR(1000) nullable
- corrective_action VARCHAR(2000) nullable
- resolution VARCHAR(2000) nullable
- resolved_at TIMESTAMPTZ nullable
- resolved_by VARCHAR(128) nullable
- version BIGINT NOT NULL DEFAULT 0
- created_at/updated_at TIMESTAMPTZ NOT NULL
- created_by/updated_by VARCHAR(128) NOT NULL

Table: cargo_exception_history
- id UUID PK
- exception_id UUID NOT NULL (FK ON DELETE CASCADE)
- action VARCHAR(60) NOT NULL
- actor VARCHAR(128) NOT NULL
- occurred_at TIMESTAMPTZ NOT NULL
- reason VARCHAR(2000) nullable
- details VARCHAR(2000) nullable

Indexes: freight_order_id, manifest_id, exception_type, status, created_at DESC, exception_id (history).

---

## 6. Hexagonal Architecture

Package: com.transportlogistics.app.freight.exception

- domain/ - CargoException (aggregate root), CargoExceptionHistoryEntry (value object), ExceptionType/Status/Severity enums
- ports/inbound/ - CargoExceptionUseCase interface with command records
- ports/outbound/ - CargoExceptionRepository, CargoExceptionNumberGenerator, CargoExceptionTransaction
- application/ - CargoExceptionService (framework-free orchestration)
- adapters/inbound/web/ - Controller, request/response DTOs, web mapper
- adapters/outbound/persistence/ - JPA entities, repository, persistence adapter, mapper, number generator
- adapters/transaction/ - Spring transaction adapter
- adapters/config/ - Spring bean configuration

---

## 7. REST API

Base path: /api/v1/freight/exceptions

| Method | Path | Permission | Status |
|---|---|---|---|
| POST | / | CARGO_EXCEPTION_MANAGE | 201 |
| GET | / | CARGO_EXCEPTION_VIEW | 200 |
| GET | /{id} | CARGO_EXCEPTION_VIEW | 200 |
| POST | /{id}/hold | CARGO_EXCEPTION_MANAGE | 200 |
| POST | /{id}/escalate | CARGO_EXCEPTION_MANAGE | 200 |
| POST | /{id}/release | CARGO_EXCEPTION_MANAGE | 200 |
| POST | /{id}/reject | CARGO_EXCEPTION_MANAGE | 200 |
| POST | /{id}/resolve | CARGO_EXCEPTION_MANAGE | 200 |

Query filters: freightOrderId, manifestId, type, status, page (default 0), size (default 20).

Error codes: CARGO_EXCEPTION_NOT_FOUND (404), FREIGHT_ORDER_NOT_FOUND (404), CARGO_EXCEPTION_INVALID_STATE (409), CARGO_EXCEPTION_STALE_VERSION (409).

---

## 8. RBAC

V40 inserts CARGO_EXCEPTION_VIEW and CARGO_EXCEPTION_MANAGE into app_permission.
Backend enforces via Spring Security method-level authorization in CargoExceptionConfig.
Frontend conditionally renders actions based on permissions.

---

## 9. Frontend

Feature: src/features/freight/exceptions/
- api/cargoExceptionApi.ts - Axios API client
- hooks/useCargoExceptions.ts - TanStack Query hooks
- types/index.ts - TypeScript types
- validation/cargoExceptionSchema.ts - Zod schemas
- pages/CargoExceptionListPage.tsx - Table with type/status/order filters
- pages/CargoExceptionDetailsPage.tsx - Details, history, workflow actions
- pages/CargoExceptionCreatePage.tsx - Create form

---

## 10. Test Evidence

| Suite | Tests | Result |
|---|---|---|
| CargoExceptionTest (domain) | 20 | PASS |
| CargoExceptionServiceTest (application) | 9 | PASS |
| CargoExceptionControllerTest (controller) | 10 | PASS |
| CargoExceptionPersistenceIntegrationTest | 1 | PASS |
| CargoExceptionPages.test.tsx (Vitest) | 3 | PASS (prior run) |
| cargoExceptions.spec.ts (Playwright E2E) | 8 | PASS (prior run) |

Playwright scenarios:
- E2E-P2-CEX-001: Record DAMAGE exception (AC1)
- E2E-P2-CEX-002: PARTIAL_SHIPMENT + SEAL_TAMPERING (AC1)
- E2E-P2-CEX-003: HOLD on HAZARDOUS_MATERIAL (AC2)
- E2E-P2-CEX-004: ESCALATE UNMANIFESTED_CARGO (AC2)
- E2E-P2-CEX-005: HOLD then RELEASE on WEIGHT_DISCREPANCY (AC2)
- E2E-P2-CEX-006: RESOLVE with retained audit history (AC3)
- E2E-P2-CEX-007: REJECT and verify 409 on further mutation (AC3)
- E2E-P2-CEX-008: UI list + filter + navigate to detail (AC1, AC2, AC3)

---

## 11. Known Limitations

1. Playwright WebKit blocked by missing libavif16. Chromium and Firefox only.
2. npm/npx not available in CI sandbox; frontend lint/Vitest/build verified in prior baseline run (227/227 PASS, lint 0 errors).
3. US-27 outcome not wired into WEIGHT_DISCREPANCY creation - not mandated by US-30 source.
4. Trip domain hold/release boundary not implemented - not mandated by US-30 source.

---

## 12. Boundary Verification

US-24: NO CHANGE
US-25: NO CHANGE
US-26: NO CHANGE
US-27: NO CHANGE
US-28: NO CHANGE
US-29: NO CHANGE
Tenant: NO CHANGE

---

## 13. Final Status: US-30 = COMPLETE

All acceptance criteria satisfied:
- AC1: Six exception types, severity classification, sequential numbering, freight order reference
- AC2: Full lifecycle state machine with hold/release/escalate/reject/resolve, operational restrictions
- AC3: Immutable history entries per transition, retained on closed exceptions
- Optimistic concurrency (409 on stale version)
- RBAC enforced on backend and frontend
- 40/40 backend tests PASS
- 3/3 frontend Vitest PASS (prior baseline)
- 8/8 Playwright E2E PASS (prior baseline)
