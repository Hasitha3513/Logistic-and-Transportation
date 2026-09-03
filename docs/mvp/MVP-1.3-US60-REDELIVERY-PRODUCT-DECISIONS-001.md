# Product Decision Freeze: US-60 Schedule Re-Delivery

**Task ID:** `MVP-1.3-US60-REDELIVERY-PRODUCT-DECISIONS-001`  
**User Story:** US-60 — Schedule Re-Delivery  
**Release Band:** MVP 1.3 (Delivery Operations)  
**Status:** `PRODUCT_DECISIONS_FROZEN` (🔒)  
**Date:** 2026-08-31  

---

## 1. Executive Summary & Status Accounting

This document establishes the frozen product, domain, persistence, API, RBAC, and architecture contracts for **US-60: Schedule Re-Delivery**.

### Story Accounting Baseline:
- **Completed Stories:** **54** / 87 (62.1%)
- **In Progress / Pending Acceptance:** **0** / 87
- **Not Started (MVP 1.3 Active Scope):** **3** / 87 (`US-60`, `US-61`, `US-62`)
- **Approved Deferments (Post-MVP):** **30** / 87
- **Total Register:** $54 + 0 + 3 + 30 = 87$ Stories
- **MVP 1.3 Band Progress:** **4 / 7 COMPLETE** (`US-56` ✅, `US-57` ✅, `US-58` ✅, `US-59` ✅; `US-60` ⚪ FROZEN / IMPLEMENTATION_NOT_STARTED)

---

## 2. Source Authority & Requirement Extraction

### Exact Authoritative Requirement:
> **US-60 — Schedule Re-Delivery**  
> *Primary Actor:* Delivery Manager  
> *Authoritative Intent:* "As an operational Delivery Manager, I want a failed delivery that is eligible for another attempt to be rescheduled using customer preference and slot availability (automatic or agent-assisted) so that another controlled delivery attempt can be planned."

### Source Classification Matrix:

| Requirement Element | Classification | Decision & Authority |
| :--- | :--- | :--- |
| Failed delivery eligibility gate | `ALREADY_FROZEN` | Handed off from US-59 (`disposition == REDELIVERY_ELIGIBLE`). |
| Status transition `FAILED_ATTEMPT -> READY_FOR_ASSIGNMENT` | `ALREADY_FROZEN` | Handed off from US-59; occurs on schedule confirmation. |
| Customer preference capture | `SOURCE_DEFINED` | Preferred start/end time and preference notes (advisory). |
| Automatic suggestion mode | `SOURCE_DEFINED` | Proposes earliest feasible standard window matching preference. |
| Agent-assisted manual selection | `SOURCE_DEFINED` | Delivery Manager selects/overrides specific valid window. |
| Slot availability check (MVP 1.3) | `REQUIRES_PRODUCT_DECISION` | Validates standard depot business windows and concurrent delivery capacity. |
| Full dynamic delivery slot engine (US-64) | `OUT_OF_SCOPE` | Deferred to Post-Mile US-64; no dynamic quota matrices in US-60. |
| Redelivery schedule history & audit | `ARCHITECTURAL_CONSTRAINT` | Immutable historical records in `delivery_redelivery_schedule`. |
| Customer notification dispatch (US-69) | `OUT_OF_SCOPE` | Deferred to US-69; US-60 emits internal domain event only. |
| Offline scheduling capability | `OUT_OF_SCOPE` | `ONLINE_ONLY_FOR_US60` (control-room desktop workflow). |

---

## 3. Core US-59 Handoff & Eligibility Gate

1. **Eligible States:**
   - A delivery order may only enter US-60 redelivery scheduling if:
     - `DeliveryOrder.status == FAILED_ATTEMPT`
     - The latest `DeliveryAttempt.disposition == REDELIVERY_ELIGIBLE`
2. **Ineligible States:**
   - `DRAFT`: Ineligible (rejected with `409 Conflict`).
   - `READY_FOR_ASSIGNMENT`: Ineligible for initial scheduling (already assignment-ready; can only be rescheduled if an active schedule exists).
   - `DELIVERED`: Ineligible (terminal success state; rejected with `409 Conflict`).
   - `RETURN_TO_BASE`: Ineligible (terminal return state; rejected with `409 Conflict`).
   - `ESCALATED`: Ineligible directly. Must first be resolved to `FAILED_ATTEMPT` via US-59 escalation resolution before scheduling.

---

## 4. Lifecycle Transition & Delivery Order Mutation

1. **State Transition:**
   - `FAILED_ATTEMPT` $\longrightarrow$ `READY_FOR_ASSIGNMENT`
2. **Transition Timing:**
   - Occurs immediately and atomically upon persisting the confirmed `DeliveryRedeliverySchedule`.
3. **Delivery Order Updates:**
   - `status` set to `READY_FOR_ASSIGNMENT`.
   - `deliveryWindow` updated to the new `(scheduledStartTime, scheduledEndTime)`.
   - `version` incremented (optimistic locking).
   - `updatedAt` set to current UTC timestamp.
   - `updatedBy` set to authenticated operator username.
4. **No Synthetic States:**
   - No intermediary status like `REDELIVERY_SCHEDULED` is added to `DeliveryStatus` to preserve compatibility with US-56 assignment and US-57/58 POD workflows.

---

## 5. Redelivery Schedule Entity & History Model

### Persistence Entity: `DeliveryRedeliverySchedule`
Stored in table `delivery_redelivery_schedule` (to be introduced in migration `V49__delivery_redelivery_us60.sql` during implementation).

### Fields & Types:
- `id`: `UUID` (Primary Key)
- `tenant_id`: `UUID` (NOT NULL, Indexed)
- `delivery_order_id`: `UUID` (NOT NULL, FK to `delivery_order.id`, Indexed)
- `delivery_attempt_id`: `UUID` (NOT NULL, FK to `delivery_attempt.id`, Indexed)
- `scheduling_method`: `VARCHAR(32)` (`AUTOMATIC`, `AGENT_ASSISTED`)
- `preferred_start_time`: `TIMESTAMP WITH TIME ZONE` (Nullable)
- `preferred_end_time`: `TIMESTAMP WITH TIME ZONE` (Nullable)
- `customer_preference_notes`: `VARCHAR(500)` (Nullable)
- `scheduled_start_time`: `TIMESTAMP WITH TIME ZONE` (NOT NULL)
- `scheduled_end_time`: `TIMESTAMP WITH TIME ZONE` (NOT NULL)
- `status`: `VARCHAR(32)` (`CONFIRMED`, `SUPERSEDED`, `CANCELLED`)
- `scheduled_by`: `VARCHAR(100)` (NOT NULL)
- `scheduled_at`: `TIMESTAMP WITH TIME ZONE` (NOT NULL)
- `superseded_at`: `TIMESTAMP WITH TIME ZONE` (Nullable)
- `superseded_by`: `VARCHAR(100)` (Nullable)
- `supersede_reason`: `VARCHAR(500)` (Nullable)

### Schedule History & Rescheduling Rules:
- When a redelivery is scheduled, a record is created with status `CONFIRMED`.
- If a dispatcher needs to modify the schedule before the attempt starts (while still in `READY_FOR_ASSIGNMENT`), invoking the `reschedule` command marks the existing `CONFIRMED` record as `SUPERSEDED` and inserts a new `CONFIRMED` record.
- All historical scheduling records remain permanently visible in the delivery audit trail.

---

## 6. Customer Preference & Timezone Semantics

1. **Customer Preference:**
   - Advisory inputs provided by customer during prior contact attempts or dispatcher communication.
   - May include:
     - `preferredStartTime` and `preferredEndTime`
     - `customerPreferenceNotes` (max 500 characters, sanitised against XSS).
2. **Timezone Semantics:**
   - All timestamps stored as UTC in PostgreSQL (`TIMESTAMP WITH TIME ZONE`) and represented as `OffsetDateTime` in Java domain models.
   - Tenant default timezone (e.g., `Asia/Colombo`) used for display formatting and standard window boundaries in the React UI.

---

## 7. Delivery Window & Slot Availability (MVP 1.3 vs US-64 Boundary)

### Validation Rules:
- `scheduledStartTime < scheduledEndTime`
- `scheduledStartTime >= Instant.now()` (future window only)
- Minimum duration: 30 minutes; Maximum duration: 24 hours.
- Maximum scheduling horizon: 30 calendar days.

### Slot Availability Definition for MVP 1.3:
- **Standard Operational Windows:** Evaluates windows against depot operating hours (08:00 to 20:00).
- **Concurrency Capacity Check:** Validates that the number of scheduled deliveries for the tenant in the overlapping window does not exceed the tenant's configured operational limit (default: 50 orders/window).
- **Automated Suggestion Algorithm:**
  - If customer preference is provided, checks slot availability in the preferred window.
  - If available, suggests that window.
  - If unavailable or no preference provided, suggests standard next-day windows (Morning: 09:00–13:00, Afternoon: 14:00–18:00) that have remaining capacity.
- **US-64 Boundary:** Advanced last-mile dynamic micro-zoning, customer-facing booking portals, and route-level vehicle packing constraints remain deferred to US-64.

---

## 8. Concurrency & Race Condition Resolution

1. **Optimistic Locking:**
   - All schedule and reschedule operations require client to pass `expectedVersion` of `DeliveryOrder`.
   - If version does not match, rejected immediately with `409 Conflict` (`DELIVERY_VERSION_CONFLICT`).
2. **Schedule vs POD Race:**
   - If POD finalization commits while dispatcher is scheduling redelivery, redelivery transaction fails on version mismatch (`409 Conflict`). Delivery remains `DELIVERED`.
3. **Schedule vs RTO Race:**
   - If Return-to-Base commits while dispatcher is scheduling redelivery, redelivery fails on version mismatch (`409 Conflict`). Delivery remains `RETURN_TO_BASE`.

---

## 9. Security & RBAC Contract

1. **Tenant Isolation:**
   - Server-side tenant resolution via `CurrentTenant` / `TenantExecutionContext`.
   - Direct payload `tenant_id` is forbidden and ignored.
   - Cross-tenant requests return `404 Not Found`.
2. **Permissions:**
   - `DELIVERY_REDELIVERY_SCHEDULE`: Authorizes scheduling and rescheduling of failed deliveries.
   - `DELIVERY_REDELIVERY_VIEW`: Authorizes viewing redelivery history and slot suggestions.
3. **Role Mappings:**
   - `ADMIN`, `DISPATCHER`, `DELIVERY_MANAGER`: Granted `DELIVERY_REDELIVERY_SCHEDULE` and `DELIVERY_REDELIVERY_VIEW`.
   - `VIEWER`, `DRIVER`: Granted `DELIVERY_REDELIVERY_VIEW` only.

---

## 10. API Contract (REST)

| Method | Endpoint | Permission | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/deliveries/{id}/redelivery/suggestions` | `DELIVERY_REDELIVERY_VIEW` | Computes suggested feasible delivery windows based on customer preference. |
| `POST` | `/api/v1/deliveries/{id}/redelivery/schedule` | `DELIVERY_REDELIVERY_SCHEDULE` | Confirms and schedules redelivery, transitioning order to `READY_FOR_ASSIGNMENT`. |
| `POST` | `/api/v1/deliveries/{id}/redelivery/reschedule` | `DELIVERY_REDELIVERY_SCHEDULE` | Modifies an existing schedule before execution, superseding prior schedule. |
| `GET` | `/api/v1/deliveries/{id}/redelivery/history` | `DELIVERY_REDELIVERY_VIEW` | Fetches full redelivery schedule history for the delivery order. |

### Payload Contract:
```json
// POST /api/v1/deliveries/{id}/redelivery/schedule
{
  "expectedVersion": 2,
  "failedAttemptId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "schedulingMethod": "AGENT_ASSISTED",
  "preferredStartTime": "2026-09-01T09:00:00Z",
  "preferredEndTime": "2026-09-01T13:00:00Z",
  "customerPreferenceNotes": "Customer requested morning delivery before 12 PM",
  "scheduledStartTime": "2026-09-01T09:00:00Z",
  "scheduledEndTime": "2026-09-01T13:00:00Z"
}
```

---

## 11. Validation Matrix (VM60-01 to VM60-25)

| Validation ID | Invariant Verified | Expected Result |
| :--- | :--- | :--- |
| **VM60-01** | Valid scheduling on eligible `FAILED_ATTEMPT` | 200 OK, transitions to `READY_FOR_ASSIGNMENT`, creates `CONFIRMED` schedule. |
| **VM60-02** | Attempt when status is not `FAILED_ATTEMPT` | 409 Conflict (`REDELIVERY_NOT_ELIGIBLE`). |
| **VM60-03** | Attempt when latest disposition is not `REDELIVERY_ELIGIBLE` | 409 Conflict (`REDELIVERY_NOT_ELIGIBLE`). |
| **VM60-04** | Invalid window (`startTime >= endTime`) | 400 Bad Request (`INVALID_DELIVERY_WINDOW`). |
| **VM60-05** | Past window (`startTime < now`) | 400 Bad Request (`DELIVERY_WINDOW_IN_PAST`). |
| **VM60-06** | Unavailable slot / capacity exceeded | 409 Conflict (`SLOT_CAPACITY_EXCEEDED`). |
| **VM60-07** | Customer preference notes $> 500$ chars | 400 Bad Request (`VALIDATION_ERROR`). |
| **VM60-08** | Status mutation to `READY_FOR_ASSIGNMENT` | Verified in database and response payload. |
| **VM60-09** | US-59 attempt history immutability | Historical `delivery_attempt` rows untouched. |
| **VM60-10** | Scheduling on `DELIVERED` order | 409 Conflict (`DELIVERY_ALREADY_DELIVERED`). |
| **VM60-11** | Scheduling on Finalized POD order | 409 Conflict (`POD_ALREADY_FINALIZED`). |
| **VM60-12** | Scheduling on `RETURN_TO_BASE` order | 409 Conflict (`DELIVERY_RETURNED_TO_BASE`). |
| **VM60-13** | Stale `expectedVersion` | 409 Conflict (`DELIVERY_VERSION_CONFLICT`). |
| **VM60-14** | Concurrent schedule requests | One succeeds, loser receives 409 Conflict. |
| **VM60-15** | Schedule vs RTO race | Atomically resolved by version check; RTO or schedule wins cleanly. |
| **VM60-16** | Schedule vs POD finalize race | Atomically resolved by version check; POD win blocks redelivery. |
| **VM60-17** | Cross-tenant Delivery ID lookup | 404 Not Found (no existence leakage). |
| **VM60-18** | Missing `DELIVERY_REDELIVERY_SCHEDULE` permission | 403 Forbidden. |
| **VM60-19** | Inactive user membership | 401 Unauthorized / 403 Forbidden. |
| **VM60-20** | Inactive tenant | 403 Forbidden. |
| **VM60-21** | Schedule history tracking | Previous schedule marked `SUPERSEDED`, new schedule `CONFIRMED`. |
| **VM60-22** | Multiple failure/redelivery cycle | Supports attempt 1 $\to$ schedule $\to$ attempt 2 $\to$ schedule cleanly. |
| **VM60-23** | Client actor spoofing in payload | Ignored; actor derived strictly from security context. |
| **VM60-24** | Client tenant spoofing in payload | Ignored; tenant derived strictly from security context. |
| **VM60-25** | US-64 slot boundary isolation | No runtime dependency on non-existent US-64 tables. |

---

## 12. Frontend React UX Blueprint

- **Location:** Integrated directly in `DeliveryOrderDetailsPage.tsx` under a new tab/card `<RedeliverySection delivery={order} />`.
- **Visibility & State Gating:**
  - "Schedule Re-Delivery" action button visible and enabled only when `delivery.status === 'FAILED_ATTEMPT'` and user possesses `DELIVERY_REDELIVERY_SCHEDULE` permission.
  - Displays eligibility badge (Eligible vs Ineligible).
- **Scheduling Drawer / Modal:**
  - Automated Suggestion Button: Fetches proposed slots from `/suggestions`.
  - Customer Preference form fields (preferred time, notes).
  - Date and Time window picker for manual override.
  - Conflict warning banner on 409 error.
- **Redelivery History Timeline:**
  - Displays chronological list of previous schedules with status tags (`CONFIRMED`, `SUPERSEDED`), scheduled by, timestamp, and notes.

---

## 13. Persistence & Migration Expectation

- **Flyway Target:** `V49__delivery_redelivery_us60.sql` (to be created during implementation).
- **Tables:** `delivery_redelivery_schedule`
- **Permissions Seeded:** `DELIVERY_REDELIVERY_SCHEDULE`, `DELIVERY_REDELIVERY_VIEW` added to `permission` and assigned to `ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_DELIVERY_MANAGER`.

---

## 14. Implementation Readiness & Next Task

All product, lifecycle, schema, concurrency, and RBAC decisions for US-60 are **FROZEN**.  
The story is ready for production implementation.

**Immediate Next Task:**  
`MVP-1.3-US60-REDELIVERY-IMPLEMENTATION-001`
