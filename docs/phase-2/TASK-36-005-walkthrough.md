# TASK-36-005 Walkthrough: Bunker Management PostgreSQL Multi-Threaded Concurrency Hardening & Verification

**Task ID**: `TASK-36-005`  
**Role**: Senior Java/Spring Boot Engineer & Database Concurrency Specialist  
**Execution Date**: 2026-08-18  

---

## 1. Objective & Scope

The objective of **TASK-36-005** was to prove and harden the US-36 Bunker Management inventory concurrency guarantees against a real PostgreSQL database using multi-threaded integration tests.

The verification covered all core concurrency invariants:
1. **Stock Overdraw Prevention**: Concurrent Fuel Issues cannot overdraw bunker stock below zero.
2. **Capacity Overflow Prevention**: Concurrent Bulk Purchase receipts cannot overfill bunker tanks beyond configured capacity.
3. **Receipt Idempotency Under Race Conditions**: Concurrent duplicate purchase receipt calls safely serialize and prevent duplicate stock credits.
4. **Fuel Issue Serialization**: Concurrent duplicate fuel issue execution attempts safely serialize with status transition lock.
5. **Stock Adjustment Consistency**: Concurrent stock adjustment deductions cannot overdraw bunker stock.
6. **Concurrent Receipt vs. Issue Serialization**: Concurrent purchase credit and issue debit operations serialize consistently with matching ledger resulting balance.
7. **Deadlock-Free Bidirectional Inter-Tank Transfers**: Concurrent opposite-direction transfers ($A \rightarrow B$ and $B \rightarrow A$) execute without deadlock due to lexicographical UUID lock ordering.

---

## 2. Hardening & Findings

### Discovery: Hibernate L1 Session Cache Refresh After Pessimistic Locking
During multi-threaded concurrency testing against PostgreSQL, a critical edge case was identified:
- When a service queries an entity prior to acquiring a row lock (or within the same EntityManager session), Hibernate caches the entity state in its First-Level (L1) session cache.
- When `@Lock(LockModeType.PESSIMISTIC_WRITE)` is subsequently executed, PostgreSQL correctly acquires the row-level lock (`SELECT ... FOR UPDATE`), but Hibernate by default would return the cached L1 instance unless explicitly refreshed.
- **Hardening Applied**: In `BunkerTankPersistenceAdapter`, `findByIdForUpdate` and `findActiveByStationAndFuelTypeForUpdate` were hardened with `entityManager.refresh(entity)` immediately upon acquiring the `PESSIMISTIC_WRITE` lock. This guarantees that every thread that acquires the lock is forced to read the authoritative, newly committed column values directly from PostgreSQL.

---

## 3. PostgreSQL Concurrency Test Matrix

All 7 scenarios were implemented in `com.transportlogistics.app.postgresql.BunkerPostgresConcurrencyIntegrationTest`:

| # | Test Method | Scenario & Invariant Proved | Result |
|---|---|---|---|
| 1 | `concurrentFuelIssuesCannotOverdrawBunkerStock` | Two concurrent 80 L and 50 L fuel issues on a 100 L tank. Exactly one succeeds; the second fails with `INSUFFICIENT_BUNKER_STOCK`. Stock is never negative. | **PASSED** |
| 2 | `concurrentPurchaseReceiptsCannotExceedTankCapacity` | Two concurrent 1500 L receipts on an 8000 L / 10000 L capacity tank. Exactly one succeeds; the second fails with `BUNKER_CAPACITY_EXCEEDED`. Final stock is exactly 9500 L. | **PASSED** |
| 3 | `duplicatePurchaseReceiptAttemptIsSafelyIdempotent` | Two concurrent receive attempts for the same Fuel Purchase. Exactly one credits stock and posts to ledger; duplicate is prevented. | **PASSED** |
| 4 | `duplicateFuelIssueAttemptIsSafelySerialized` | Two concurrent issue attempts for the same Fuel Issue. Exactly one succeeds; status transition lock serializes duplicate. | **PASSED** |
| 5 | `concurrentStockAdjustmentsCannotOverdrawStock` | Two concurrent negative adjustments (-700 L and -500 L) on a 1000 L tank. Only one succeeds; stock never drops below zero. | **PASSED** |
| 6 | `concurrentReceiptVsIssueSerializesConsistently` | Concurrent 1000 L purchase receipt and 800 L fuel issue. Both serialize cleanly; stock is conserved (1000 + 1000 - 800 = 1200 L); ledger balance matches tank. | **PASSED** |
| 7 | `interTankTransfersInOppositeDirectionsDoNotDeadlock` | Concurrent opposite-direction transfers (Tank A $\rightarrow$ Tank B 1000 L, and Tank B $\rightarrow$ Tank A 1000 L). Deterministic UUID lock ordering prevents deadlock; total system stock across tanks is conserved (10,000 L). | **PASSED** |

---

## 4. Verification Evidence

### 1. PostgreSQL Concurrency Suite Execution
```
[INFO] Running com.transportlogistics.app.postgresql.BunkerPostgresConcurrencyIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 25.67 s -- in com.transportlogistics.app.postgresql.BunkerPostgresConcurrencyIntegrationTest
[INFO] 
[INFO] Results:
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 2. Full Backend Test Suite Execution
```
[INFO] Results:
[WARNING] Tests run: 312, Failures: 0, Errors: 0, Skipped: 14
[INFO] BUILD SUCCESS
[INFO] Total time:  01:35 min
```

### 3. Frontend Verification
```
 Test Files  11 passed (11)
      Tests  57 passed (57)
   Duration  45.49s

> eslint . --max-warnings=0 (PASSED - 0 errors, 0 warnings)
> tsc -b && vite build (PASSED - built in 24.30s)
```

---

## 5. Artifacts and Status

- **PostgreSQL Concurrency Proof**: **COMPLETE** (7/7 tests passing).
- **Backend Architecture & Modulith Boundary Checks**: **PASSED**.
- **Ready for TASK-36-006**: Frontend UI implementation for Bunker Management.