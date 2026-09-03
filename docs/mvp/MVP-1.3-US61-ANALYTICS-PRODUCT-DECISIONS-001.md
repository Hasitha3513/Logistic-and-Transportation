# Product Decision Freeze: US-61 — Analyze Delivery Performance

**Task ID:** `MVP-1.3-US61-ANALYTICS-PRODUCT-DECISIONS-001`  
**Title:** Freeze Product Decisions and Domain Contract for US-61 — Analyze Delivery Performance  
**User Story:** US-61 — Analyze Delivery Performance  
**Release Band:** `MVP 1.3 — Delivery Operations`  
**Status:** `🔒 PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Date:** 2026-08-31  

---

## 1. Executive Summary & Purpose

This document establishes the frozen product decisions, mathematical formulas, data models, API contracts, RBAC permissions, and architectural boundaries for **US-61 (Analyze Delivery Performance)** in Phase 1.3 (Delivery Operations).

All analytics defined herein are strictly **read-only operational metrics** computed from authoritative delivery entities (`delivery_order`, `proof_of_delivery`, `delivery_attempt`, `delivery_redelivery_schedule`) without mutating business state, executing repair jobs, or leaking cross-module domain boundaries.

---

## 2. Hard Precondition Verification
- **Accepted Milestone Baseline:** US-56, US-57, US-58, US-59, and US-60 are verified and marked `COMPLETE`.
- **MVP 1.3 Delivery Operations Progress:** `5 / 7 COMPLETE`.
- **Overall Application Progress:** `55 / 87 COMPLETE` (30 approved deferments, 2 not started).
- **Latest Accepted Migration:** `V49__delivery_redelivery_us60.sql`.
- **Next Migration Baseline:** `V50` (to seed `DELIVERY_ANALYTICS_VIEW` and supporting indexes).

---

## 3. Authoritative Source Requirement
- **Requirement Source:** `docs/requirements/Traspotation & logistic.docx` and `docs/requirements/US-61-US-70-UseCase-Activity-Sequence-Diagrams.md`.
- **Primary Actor:** Delivery Manager, Dispatcher, Operations Analyst.
- **Authoritative Requirement Text:**
  > *"Analyze delivery success, delays, attempts, and regional performance so delivery operations can be improved."*

---

## 4. Source Classification of Analytical Capabilities

| Capability / Metric | Classification | Description & Rationale |
| :--- | :---: | :--- |
| **Delivery Success Rate** | `SOURCE_DEFINED` | Ratio of delivered orders to terminal completed outcomes. |
| **Delivery Delay & On-Time Performance** | `SOURCE_DEFINED` | Comparison of actual completion timestamp against committed window end. |
| **Delivery Attempt Tracking** | `SOURCE_DEFINED` | Aggregation of failed attempts, distributions, and attempts-to-delivery ratios. |
| **Regional Performance Breakdown** | `SOURCE_DEFINED` | Grouping delivery success, volumes, and punctuality by destination location/region. |
| **Failure Reason Distribution** | `ALREADY_FROZEN` | Aggregates factual failure taxonomy established in US-59. |
| **Redelivery Performance** | `ALREADY_FROZEN` | Aggregates redelivery volume and reschedule history established in US-60. |
| **Multi-Tenancy Isolation** | `ARCHITECTURAL_CONSTRAINT` | Derived strictly from `CurrentTenant`; no cross-tenant leakage. |
| **Read-Only Invariant** | `ARCHITECTURAL_CONSTRAINT` | US-61 never mutates orders, PODs, attempts, or schedules. |
| **Contractual SLA Penalty Tables** | `OUT_OF_SCOPE` | External contractual penalty engines are outside MVP 1.3. |
| **Cross-Module Enterprise Analytics** | `OUT_OF_SCOPE` | Executive cross-module blending belongs strictly to US-82. |
| **Customer-Facing Tracking Portal** | `OUT_OF_SCOPE` | Customer self-service portals belong strictly to US-70. |

---

## 5. Mathematical KPI Catalogue & Frozen Formulas

### 5.1 Order Success Rate (Canonical)
- **Formula:**
  $$\text{Order Success Rate (\%)} = \left( \frac{\text{Count of Orders with Status } DELIVERED}{\text{Terminal Completed Orders } (DELIVERED + RETURN\_TO\_BASE)} \right) \times 100$$
- **Terminal Denominator:** Only orders that reached an absolute terminal operational outcome (`DELIVERED` or `RETURN_TO_BASE`) within the period are included.
- **Active Exclusions:** In-flight orders (`DRAFT`, `READY_FOR_ASSIGNMENT`, `FAILED_ATTEMPT`, `ESCALATED`) are excluded from the completed outcome denominator to avoid artificially deflating success rates during active operations.
- **Zero Denominator Rule:** If `terminal completed orders == 0`, the metric returns `null` (`NOT_APPLICABLE`), displayed as `N/A` (never a false `0.0%`).

### 5.2 First-Attempt Success Rate
- **Formula:**
  $$\text{First-Attempt Success Rate (\%)} = \left( \frac{\text{Orders with Status } DELIVERED \text{ AND } \text{failedAttemptCount} = 0}{\text{Total Delivered Orders}} \right) \times 100$$
- **Purpose:** Measures the proportion of delivered orders completed without any intervening failed attempt.

### 5.3 On-Time Delivery Rate & Delay Calculation
- **Authoritative Completion Timestamp:** `proof_of_delivery.accepted_at` (or `finalized_at`), NOT mutable `updated_at`.
- **Authoritative Committed Window End:**
  - For standard initial deliveries: `delivery_order.window_end`.
  - For redelivered orders with an active confirmed schedule: `delivery_redelivery_schedule.scheduled_end_time` of the latest `CONFIRMED` schedule.
- **On-Time Condition:** $\text{completionTimestamp} \le \text{committedWindowEnd}$.
- **On-Time Rate Formula:**
  $$\text{On-Time Delivery Rate (\%)} = \left( \frac{\text{Delivered Orders where } \text{completionTimestamp} \le \text{committedWindowEnd}}{\text{Total Delivered Orders}} \right) \times 100$$
- **Late Delivery Rate Formula:**
  $$\text{Late Delivery Rate (\%)} = \left( \frac{\text{Delivered Orders where } \text{completionTimestamp} > \text{committedWindowEnd}}{\text{Total Delivered Orders}} \right) \times 100$$
- **Average Delivery Delay (Minutes):**
  $$\text{Average Delay (Minutes)} = \frac{\sum \max\left(0, \text{durationInMinutes}(\text{completionTimestamp} - \text{committedWindowEnd})\right)}{\text{Count of Late Delivered Orders}}$$
  - For orders delivered on-time or early, delay is `0`.
  - For non-delivered or `RETURN_TO_BASE` orders, delay is `null` (`NOT_APPLICABLE`).

### 5.4 Attempt Distribution & Failure Metrics
- **Total Failed Attempts:** Exact count of `delivery_attempt` rows in the period.
- **Average Failed Attempts per Order:** $\frac{\text{Total Failed Attempts}}{\text{Total Delivery Orders in Period}}$.
- **Attempt Distribution Breakdown:**
  - 0 Failed Attempts (Delivered directly on first run).
  - 1 Failed Attempt.
  - 2 Failed Attempts.
  - 3+ Failed Attempts.
- **Failure Reason Breakdown:** Count and percentage of failed attempts grouped by `failure_reason` (`CUSTOMER_UNAVAILABLE`, `WRONG_ADDRESS`, `CUSTOMER_REFUSED`, `ACCESS_RESTRICTED`, `DAMAGED_CARGO`, `DOCUMENT_OR_PAYMENT_ISSUE`, `OTHER`).

### 5.5 Redelivery Metrics
- **Total Redelivered Orders:** Count of distinct delivery orders having at least one `delivery_redelivery_schedule` in the period.
- **Redelivery Rate:** $\frac{\text{Total Redelivered Orders}}{\text{Total Delivery Orders in Period}} \times 100$.
- **Redelivery Success Rate:** $\frac{\text{Redelivered Orders that reached } DELIVERED}{\text{Total Completed Redelivery Orders } (DELIVERED + RETURN\_TO\_BASE)} \times 100$.

### 5.6 Return-to-Base (RTO) Rate
- **Formula:**
  $$\text{Return-to-Base Rate (\%)} = \left( \frac{\text{Count of Orders with Status } RETURN\_TO\_BASE}{\text{Terminal Completed Orders } (DELIVERED + RETURN\_TO\_BASE)} \right) \times 100$$

---

## 6. Regional Dimension & Location Resolution
- **Grouping Key:** `delivery_order.destination_location_id`.
- **Location Resolution:** Resolved through `OrganizationLookupPort` to obtain location `name`, `code`, `city`, and `stateProvince`.
- **Unclassified Fallback:** If location metadata is missing or deleted, records are aggregated under region `"UNCLASSIFIED"` rather than silently dropped.

---

## 7. Time, Period, and Timezone Contract
- **Storage:** UTC `TIMESTAMP WITH TIME ZONE` across all tables.
- **Business Date Bucketing:** Daily, weekly, and monthly aggregations use the tenant's operating timezone (`Asia/Colombo` default / tenant-configurable).
- **Default Query Range:** Last 30 days (`LocalDate.now().minusDays(30)` to `LocalDate.now()`).
- **Maximum Query Range:** 365 days (1 year) to protect database resources from unbounded scans.
- **Trend Buckets Supported:** `DAY`, `WEEK`, `MONTH`.

---

## 8. REST API Surface Contract

Base path: `/api/v1/deliveries/analytics`

### 8.1 Summary KPI Endpoint
- **Method / Path:** `GET /api/v1/deliveries/analytics/summary`
- **Permission:** `DELIVERY_ANALYTICS_VIEW`
- **Query Parameters:** `from` (ISO LocalDate), `to` (ISO LocalDate), `serviceType` (optional), `priority` (optional).
- **Response Schema:**
  ```json
  {
    "period": {
      "from": "2026-08-01",
      "to": "2026-08-31"
    },
    "totalOrders": 120,
    "activeOrders": 15,
    "terminalCompletedOrders": 105,
    "deliveredOrders": 98,
    "returnedToBaseOrders": 7,
    "orderSuccessRate": 93.33,
    "firstAttemptSuccessRate": 78.57,
    "onTimeDeliveredOrders": 90,
    "lateDeliveredOrders": 8,
    "onTimeDeliveryRate": 91.84,
    "lateDeliveryRate": 8.16,
    "averageDelayMinutes": 34.5,
    "totalFailedAttempts": 28,
    "averageFailedAttemptsPerOrder": 0.23,
    "redeliveredOrders": 18,
    "redeliveryRate": 15.00,
    "redeliverySuccessRate": 83.33,
    "returnToBaseRate": 6.67
  }
  ```

### 8.2 Failure Reasons Breakdown Endpoint
- **Method / Path:** `GET /api/v1/deliveries/analytics/failures`
- **Permission:** `DELIVERY_ANALYTICS_VIEW`
- **Response Schema:**
  ```json
  [
    {
      "failureReason": "CUSTOMER_UNAVAILABLE",
      "count": 14,
      "percentage": 50.0,
      "redeliveryEligibleCount": 14,
      "returnToBaseCount": 0,
      "escalatedCount": 0
    },
    {
      "failureReason": "WRONG_ADDRESS",
      "count": 6,
      "percentage": 21.43,
      "redeliveryEligibleCount": 6,
      "returnToBaseCount": 0,
      "escalatedCount": 0
    },
    {
      "failureReason": "CUSTOMER_REFUSED",
      "count": 4,
      "percentage": 14.29,
      "redeliveryEligibleCount": 0,
      "returnToBaseCount": 4,
      "escalatedCount": 0
    }
  ]
  ```

### 8.3 Regional Performance Breakdown Endpoint
- **Method / Path:** `GET /api/v1/deliveries/analytics/regions`
- **Permission:** `DELIVERY_ANALYTICS_VIEW`
- **Response Schema:**
  ```json
  [
    {
      "destinationLocationId": "3d246c64-3598-4256-aaa8-f6584b233fb1",
      "locationCode": "LOC-CMB-01",
      "locationName": "Colombo Central Depot",
      "city": "Colombo",
      "totalOrders": 45,
      "deliveredOrders": 42,
      "returnedToBaseOrders": 3,
      "orderSuccessRate": 93.33,
      "onTimeDeliveredOrders": 39,
      "onTimeDeliveryRate": 92.86,
      "averageDelayMinutes": 22.0,
      "failedAttemptCount": 8
    }
  ]
  ```

### 8.4 Time-Series Trends Endpoint
- **Method / Path:** `GET /api/v1/deliveries/analytics/trends`
- **Permission:** `DELIVERY_ANALYTICS_VIEW`
- **Query Parameters:** `from`, `to`, `granularity` (`DAY`, `WEEK`, `MONTH`, default `DAY`).
- **Response Schema:**
  ```json
  [
    {
      "bucketDate": "2026-08-01",
      "totalCreated": 10,
      "delivered": 8,
      "failedAttempts": 2,
      "returnedToBase": 1,
      "onTimeDelivered": 7,
      "lateDelivered": 1
    }
  ]
  ```

---

## 9. Architecture & Module Boundaries

- **Module Ownership:** Owned within the `delivery` bounded context (`com.transportlogistics.app.delivery`).
- **Public Port for Reporting Module:** Exposes public read interface `DeliveryReportingQuery` at root `com.transportlogistics.app.delivery.DeliveryReportingQuery` allowing the centralized `reporting` module to consume tenant-scoped summaries without violating modular monolith boundaries.
- **Persistence / Query Execution:** Executed via dedicated Spring Data JPA projection queries and optimized native JDBC queries in `DeliveryAnalyticsPersistenceAdapter` targeting `delivery_order`, `proof_of_delivery`, `delivery_attempt`, and `delivery_redelivery_schedule`.
- **Consistency Model:** Transactionally live queries (`READ_COMMITTED` current state). No materialized views or delayed caches required for Phase 1.3 operational volume.

---

## 10. Multi-Tenancy & Security Hardening

1. **Server-Side Tenant Resolution:** `tenant_id` is extracted strictly from `CurrentTenant.get()` / `TenantExecutionContext`.
2. **Client Hint Rejection:** Client-supplied tenant query parameters or headers are ignored.
3. **Cross-Tenant IDOR Protection:** Filter queries validate that requested `locationId` or `customerId` belong to the same active tenant.
4. **RBAC:**
   - Permission: `DELIVERY_ANALYTICS_VIEW`.
   - Seeded into `app_permission` and granted to roles: `ADMIN`, `DISPATCHER`, `DELIVERY_MANAGER`, `VIEWER`.
5. **PII Protection:** Analytics payloads contain only operational facts, timestamps, counts, rates, and IDs. Customer names, phone numbers, signatures, and photographic evidence are never queried or returned.

---

## 11. Frontend UX Specification

- **Navigation:** Accessible under **Delivery > Delivery Analytics** (`/deliveries/analytics`) in the application menu.
- **Components:**
  1. **Analytics Header & Date Range Picker:** Preset options (`Last 7 Days`, `Last 30 Days`, `This Month`) + custom range (max 365 days).
  2. **Executive KPI Cards:**
     - Order Success Rate (with delivered vs RTO breakdown).
     - On-Time Delivery Rate (with on-time vs late count and average delay minutes).
     - First-Attempt Success Rate.
     - Redelivery Rate & Redelivery Success Rate.
  3. **Trend Line/Bar Visualizations:** Daily delivery volumes (Created vs Delivered vs Failed).
  4. **Failure Reason Analysis:** Interactive distribution chart & breakdown table.
  5. **Regional Punctuality Table:** Destination depot/city comparison table with sorting.

---

## 12. Verification & Acceptance Gate Matrix

| Test ID | Verification Scope | Target Component | Expected Result |
| :--- | :--- | :--- | :--- |
| `VM61-01` | Zero data / empty dataset | Application Service / API | 200 OK with `null` rates and 0 counts. |
| `VM61-02` | On-time delivery calculation | Service & PostgreSQL | Order delivered before window end produces 100% on-time rate and 0 min delay. |
| `VM61-03` | Late delivery calculation | Service & PostgreSQL | Order delivered 45 min after window end produces 0% on-time, 45 min average delay. |
| `VM61-04` | Return-to-Base handling | Service & PostgreSQL | RTO order included in completed outcome denominator, excluded from delay. |
| `VM61-05` | Active in-flight order exclusion | Service & PostgreSQL | `READY_FOR_ASSIGNMENT` order excluded from completed outcome denominator. |
| `VM61-06` | Redelivered order on-time check | Service & PostgreSQL | Evaluated against latest confirmed redelivery schedule window end. |
| `VM61-07` | Multi-tenant query isolation | PostgreSQL Acceptance | Tenant A metrics strictly exclude Tenant B deliveries and attempts. |
| `VM61-08` | RBAC authorization | Security Controller | `DELIVERY_ANALYTICS_VIEW` granted; unauthorized users receive 403 Forbidden. |
| `VM61-09` | Playwright E2E Dashboard | Chromium E2E | Loads KPI cards, changes date filters, verifies rendered chart and tables. |

---

## 13. Migration & Schema Expectation
- **Migration File:** `V50__delivery_analytics_us61.sql`.
- **Expected Operations:**
  - Seed permission `DELIVERY_ANALYTICS_VIEW` into `app_permission` and assign to role templates.
  - Create composite indexes on `delivery_order (tenant_id, status, created_at)` and `delivery_attempt (tenant_id, attempt_timestamp, failure_reason)` if query optimization requires them.

---

## 14. Implementation Readiness & Next Action
- **Status:** `🔒 PRODUCT_DECISIONS_FROZEN`
- **Immediate Next Task:** `MVP-1.3-US61-ANALYTICS-IMPLEMENTATION-001` (Implement US-61 Delivery Performance Analytics, Flyway V50, backend services, REST endpoints, React UI, and full regression verification).
