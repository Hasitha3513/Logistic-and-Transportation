# P2-06-R1 — Manage Freight Insurance (US-28)

## 1. Requirement & Scope Summary

- **Story**: US-28 — Manage Freight Insurance
- **Goal**: Implement source-aligned Freight Insurance policies, claims, assessments, authorized approval/rejection/dispute workflows, and settlement history.
- **Package**: `com.transportlogistics.app.freight.insurance`
- **Permissions**: `CARGO_INSURANCE_VIEW`, `CARGO_INSURANCE_MANAGE` (assigned to `ADMIN`, `DISPATCHER`, `OPERATIONS_MANAGER`)

---

## 2. Invariants & Business Logic

1. **Policy Association & Validity**:
   - Each policy is linked to a valid `freightOrderId`.
   - Coverage limits, premiums, and deductibles are non-negative `BigDecimal` amounts with explicit ISO currency codes (e.g., `USD`).
   - Claim filing requires an `ACTIVE` policy whose validity window covers the claim incident date.

2. **Sequential Numbering**:
   - Policies generate identifiers matching `POL-YYYY-NNNNNN`.
   - Claims generate identifiers matching `CLM-YYYY-NNNNNN`.
   - Settlements generate references matching `STL-YYYY-NNNNNN-XXX`.

3. **Claim Workflow State Machine**:
   - Initial state: `OPEN`.
   - `OPEN` / `UNDER_REVIEW` / `DISPUTED` -> **Assess** (`assessedAmount`, notes) -> `UNDER_REVIEW`.
   - `UNDER_REVIEW` / `OPEN` -> **Approve** (with valid `assessedAmount > 0`) -> `APPROVED`.
   - `OPEN` / `UNDER_REVIEW` -> **Reject** (with reason) -> `REJECTED`.
   - `REJECTED` / `UNDER_REVIEW` -> **Dispute** (with reason) -> `DISPUTED`.
   - `APPROVED` (or partially settled) -> **Record Settlement** (`settlementAmount`).
     - Settlement amount + existing settlements cannot exceed `assessedAmount` (over-settlement prevented).
     - When cumulative settlements reach `assessedAmount`, claim transitions to `SETTLED`.

4. **Optimistic Concurrency & Audit**:
   - Version checking on all mutating commands; throws `ConflictException` (HTTP 409) on mismatch.
   - Comprehensive auditable settlement records retain `settledAmount`, `currencyCode`, `settlementNotes`, `settledBy`, and `settledAt`.

---

## 3. REST API Endpoints

### Policies
- `POST /api/v1/freight/insurance/policies` — Create policy
- `GET /api/v1/freight/insurance/policies` — List policies
- `GET /api/v1/freight/insurance/policies/{id}` — Get policy by ID
- `GET /api/v1/freight/insurance/policies/by-order/{freightOrderId}` — Get policy by Freight Order ID
- `PUT /api/v1/freight/insurance/policies/{id}` — Update policy coverage / status

### Claims
- `POST /api/v1/freight/insurance/claims` — File claim
- `GET /api/v1/freight/insurance/claims` — List claims
- `GET /api/v1/freight/insurance/claims/{id}` — Get claim by ID
- `GET /api/v1/freight/insurance/claims/by-policy/{policyId}` — Get claims by Policy ID
- `POST /api/v1/freight/insurance/claims/{id}/assess` — Assess claim amount & notes
- `POST /api/v1/freight/insurance/claims/{id}/approve` — Authorize claim for payout
- `POST /api/v1/freight/insurance/claims/{id}/reject` — Reject claim with reason
- `POST /api/v1/freight/insurance/claims/{id}/dispute` — Dispute claim with reason
- `POST /api/v1/freight/insurance/claims/{id}/settlements` — Record tranche settlement

---

## 4. Verification & Testing

- **Backend Unit Tests**: `FreightInsuranceDomainTest` (100% pass)
- **Backend Service Tests**: `FreightInsuranceServiceTest` (100% pass)
- **Backend Controller Tests**: `FreightInsuranceControllerTest` (100% pass)
- **Backend Integration Tests**: `FreightInsurancePersistenceIntegrationTest` (100% pass)
- **Architecture Tests**: `ApplicationModulesTest`, `HexagonalLayerArchitectureTest`, `ModuleBoundaryArchitectureTest`, `LombokUsageArchitectureTest` (100% pass)
- **Frontend Vitest Tests**: `InsurancePages.test.tsx` (100% pass)
- **Playwright E2E Tests**: `freightInsurance.spec.ts` (`E2E-P2-INS-001` to `E2E-P2-INS-007`)
