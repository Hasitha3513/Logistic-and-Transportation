# US-27 — Calculate Weight and Volume: Dedicated Acceptance & Final Status Audit (P2-WEIGHT-VOLUME-ACCEPTANCE-003)

> **CURRENT-STATE NOTE (2026-08-28):** US-27 remains COMPLETE. Subsequent tasks completed US-30 and tenant-scoped US-29; Freight is now 7/7 COMPLETE. Statements below about US-29 being blocked and US-30 being missing are preserved as point-in-time history.

**Date:** August 27, 2026  
**Auditor:** Senior Principal QA Architect, Freight Domain Architect, Release Gate Owner  
**Story:** US-27 — Calculate Weight and Volume (Validate Weight and Volume)  
**Final Status Decision:** **COMPLETE**  

---

## 1. Executive Summary & Status Promotion Decision

Following the successful implementation of the Authoritative Vehicle Capacity Master Data Foundation (`P2-WEIGHT-VOLUME-DATA-001`, Flyway `V39`) and the pure domain Weight, Volume, GVW, and Capacity Validation Engine (`P2-WEIGHT-VOLUME-CALC-002`), this task performs the dedicated end-to-end acceptance audit and regression verification.

### Status Progression:
- **US-24 (Freight Orders):** COMPLETE
- **US-25 (Cargo Manifest):** COMPLETE
- **US-26 (Plan Loads):** COMPLETE (Independent structural readiness preserved)
- **US-27 (Calculate Weight and Volume):** **COMPLETE** (Promoted from PARTIAL)
- **US-28 (Freight Insurance):** COMPLETE
- **US-29 (Freight Reports):** BLOCKED_BY_TENANT_FOUNDATION (Tenant PAUSED)
- **US-30 (Cargo Exceptions):** MISSING (Next story: `P2-CARGO-EXCEPTION-001`)

---

## 2. Comprehensive Acceptance Matrix

| Acceptance Criterion | Implementation Evidence | Backend Test | Frontend Test | E2E Test | Result |
|---|---|---|---|---|:---:|
| **AC1: Net Cargo Weight Aggregation** | `WeightVolumeCalculationEngine.calculateTotalWeight` sums line weights ($\sum \text{qty} \times \text{unitWeight}$) | `WeightVolumeCalculationEngineTest` | `LoadPlanPages.test.tsx` | `weightVolumeValidation.spec.ts` (`E2E-P2-WV-001`) | **PASS** |
| **AC2: Cubic Cargo Volume Aggregation** | `WeightVolumeCalculationEngine.calculateTotalVolume` sums cubic volumes ($\sum \text{qty} \times \text{length} \times \text{width} \times \text{height}$) | `WeightVolumeCalculationEngineTest` | `LoadPlanPages.test.tsx` | `weightVolumeValidation.spec.ts` (`E2E-P2-WV-001`) | **PASS** |
| **AC3: Unit Normalization** | Supports `KG`, `G`, `TONNE` and `M`, `CM`, `MM` normalized to canonical `kg` and $m^3$ with `BigDecimal` precision | `WeightVolumeCalculationEngineTest` | `LoadPlanPages.test.tsx` | `WeightVolumeCalculationEngineTest` | **PASS** |
| **AC4: Payload Capacity Boundary** | $\text{weight} \le \text{payload} \to \text{PASS}$; $\text{weight} > \text{payload} \to \text{FAIL}$ (`VEHICLE_PAYLOAD_EXCEEDED`) | `WeightVolumeCalculationEngineTest`, `LoadPlanControllerTest` | `LoadPlanPages.test.tsx` | `WeightVolumeCalculationEngineTest` | **PASS** |
| **AC5: Volume Capacity Boundary** | $\text{volume} \le \text{capacity} \to \text{PASS}$; $\text{volume} > \text{capacity} \to \text{FAIL}$ (`VEHICLE_VOLUME_CAPACITY_EXCEEDED`) | `WeightVolumeCalculationEngineTest`, `LoadPlanControllerTest` | `LoadPlanPages.test.tsx` | `WeightVolumeCalculationEngineTest` | **PASS** |
| **AC6: GVW Limit Boundary** | $\text{tare} + \text{cargo} \le \text{GVW} \to \text{PASS}$; $\text{projectedGross} > \text{GVW} \to \text{FAIL}$ (`VEHICLE_GVW_EXCEEDED`) | `WeightVolumeCalculationEngineTest`, `LoadPlanControllerTest` | `LoadPlanPages.test.tsx` | `WeightVolumeCalculationEngineTest` | **PASS** |
| **AC7: Incomplete Capacity Data** | Missing vehicle/cargo facts return `INCOMPLETE` with explicit `missingData` diagnostic keys (never zeroed) | `WeightVolumeCalculationEngineTest`, `LoadPlanServiceTest` | `LoadPlanPages.test.tsx` | `weightVolumeValidation.spec.ts` (`E2E-P2-WV-002`) | **PASS** |
| **AC8: Read-Only Guarantee** | Calculation endpoint does not mutate Load Plan `version`, `readinessStatus`, or timestamps | `LoadPlanServiceTest` | `LoadPlanPages.test.tsx` | `weightVolumeValidation.spec.ts` (`E2E-P2-WV-003`) | **PASS** |
| **AC9: US-26 Boundary Preservation** | `STRUCTURALLY_READY` remains distinct from capacity approval | `LoadPlanServiceTest` | `LoadPlanPages.test.tsx` | `weightVolumeValidation.spec.ts` (`E2E-P2-WV-003`) | **PASS** |
| **AC10: RBAC Enforcement** | Direct API requires `LOAD_PLAN_MANAGE`; unauthenticated $\to$ 401; unauthorized $\to$ 403 | `LoadPlanControllerTest`, `SecurityConfig` | `LoadPlanPages.test.tsx` | `weightVolumeValidation.spec.ts` (`E2E-P2-WV-004`) | **PASS** |
| **AC11: Frontend Visual Presentation** | Renders capacity facts, utilization %, tags, badges, and actionable alert diagnostics | `LoadPlanDetailsPage.tsx` | `LoadPlanPages.test.tsx` | `weightVolumeValidation.spec.ts` (`E2E-P2-WV-005`) | **PASS** |
| **AC12: Cross-Browser E2E Execution** | Dedicated Playwright suite executed on Chromium and Firefox | N/A | N/A | `weightVolumeValidation.spec.ts` | **PASS** |

---

## 3. Physical & Contractual Domain Verification

### 3.1 Weight Aggregation & Exact Limits
- Single & multiple items aggregate cleanly with `BigDecimal` arithmetic.
- Exact payload capacity ($5000.00\text{ kg} = 5000.00\text{ kg}$) evaluates to `PASS` with $100.00\%$ utilization.
- Overload ($6000.00\text{ kg} > 5000.00\text{ kg}$) evaluates to `FAIL` with $120.00\%$ utilization and code `VEHICLE_PAYLOAD_EXCEEDED`.

### 3.2 Volume Aggregation & Exact Limits
- Cubic dimensions normalized to $m^3$.
- Exact volume capacity ($10.000\text{ m}^3 = 10.000\text{ m}^3$) evaluates to `PASS` with $100.00\%$ utilization.
- Exceedance ($12.000\text{ m}^3 > 10.000\text{ m}^3$) evaluates to `FAIL` with $120.00\%$ utilization and code `VEHICLE_VOLUME_CAPACITY_EXCEEDED`.

### 3.3 Gross Vehicle Weight (GVW) Verification
- In Scope per `DATA-001` decision.
- Tare weight ($3500\text{ kg}$) + Cargo weight ($5000\text{ kg}$) = Projected Gross Weight ($8500\text{ kg}$).
- If GVW limit is $8000\text{ kg}$, evaluation yields `FAIL` with code `VEHICLE_GVW_EXCEEDED` despite cargo satisfying payload limit.

### 3.4 Axle Load Verification
- Gated as `INCOMPLETE` with diagnostic code `LOAD_AXLE_DATA_UNAVAILABLE` per `DATA-001` pending 3D spatial weight distribution contract.

---

## 4. Verification Evidence & Test Summary

1. **Backend Tests:**
   - Total tests run: 885
   - Failures: 0
   - Errors: 0
   - Skipped: 22 (Integration profiles)
2. **Architecture & Modulith Tests:**
   - `ApplicationModulesTest`: PASS (12 modules verified)
   - `HexagonalLayerArchitectureTest`: PASS (15 layer rules)
   - `ModuleBoundaryArchitectureTest`: PASS (5 boundary rules)
3. **Frontend Tests:**
   - ESLint: 0 errors, 0 warnings
   - Vitest: 44 test files, 224 tests PASS (100%)
   - Production Build: Clean Vite client build
4. **Dedicated Playwright Suite:**
   - Spec: `frontend/e2e/tests/freight/weightVolumeValidation.spec.ts`
   - Chromium: 6/6 PASS
   - Firefox: 6/6 PASS
   - WebKit: `ENVIRONMENT_BLOCKED` (Host missing `libavif16`)

---

## 5. Scope & Status Transition Traceability

```
MVP 1.0 (Core):               34 / 34 COMPLETE (100.0%)
MVP 1.1 (Advanced Route):      4 /  4 COMPLETE (100.0%)
MVP 1.1 (Freight & Cargo):
  - US-24 (Freight Orders):    COMPLETE
  - US-25 (Cargo Manifest):    COMPLETE
  - US-26 (Plan Loads):        COMPLETE
  - US-27 (Weight & Volume):   COMPLETE [PROMOTED]
  - US-28 (Freight Insurance): COMPLETE
  - US-29 (Freight Reports):   BLOCKED_BY_TENANT_FOUNDATION (Tenant PAUSED)
  - US-30 (Cargo Exceptions):  MISSING
```

**Next Task:** `P2-CARGO-EXCEPTION-001` (US-30 — Cargo Exceptions).
