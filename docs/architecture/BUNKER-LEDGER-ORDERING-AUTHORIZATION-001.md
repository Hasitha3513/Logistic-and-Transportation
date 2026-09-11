# Canonical Bunker Ledger Ordering Authorization

**Task:** `BUNKER-LEDGER-ORDERING-AUTHORIZATION-001`  
**Classification:** Non-story architecture / technical governance  
**Decision:** `APPROVED`  
**Date:** 2026-09-05  
**Owner:** Fuel  
**Current Flyway head:** V64  
**Accounting:** unchanged at 68 / 87 accepted and 19 / 87 remaining

## Problem and rejected alternatives

`BunkerPostgresConcurrencyIntegrationTest.concurrentReceiptVsIssueSerializesConsistently` exposed that the current movement table has no canonical serialized ledger order. `occurredAt` is business/source time and may be backdated. `createdAt` is server audit/persistence time captured before the authoritative Tank lock and does not prove mutation or commit order. The identifier is a random UUID with no temporal meaning.

Consequently, ordering by `occurredAt`, `createdAt`, random UUID, or any combination of them can provide deterministic presentation but cannot prove that the first row represents the final serialized Tank balance. Those alternatives are rejected as the canonical ledger order. Business timestamps must not be altered to manufacture uniqueness.

## Canonical decision

Each `bunker_stock_movement` has a `ledger_sequence BIGINT` with these semantics:

- canonical monotonic serialized stock-mutation order scoped to `(tenant_id, tank_id)`;
- assigned only while the existing authoritative Tank pessimistic write lock is held;
- persisted in the same ACID transaction as stock validation, Tank balance update, and movement insertion;
- the greatest committed sequence identifies the latest serialized mutation and its `resulting_balance_liters` must equal the Tank balance at that mutation;
- `occurred_at` remains business/source occurrence time and `created_at` remains server persistence/audit time;
- internal only: clients cannot supply it and the existing REST contract is unchanged.

The approved allocation strategy is a bounded Tenant/Tank `MAX(ledger_sequence) + 1` query executed after obtaining the Tank write lock. The first movement is 1. The supporting descending index avoids an unbounded scan, and the common Tank lock prevents concurrent allocation races. No global sequence, identity, serial, distributed allocator, or `bunker_tank` counter is authorized.

Every stock-mutating path—opening balance, purchase receipt, Fuel Issue, adjustment, and both sides of a transfer—must use the same allocator. Transfer continues to lock both Tanks in stable UUID order, then allocates independently within each Tenant/Tank ledger. Sequence is not an external idempotency key. Rollback commits neither movement nor sequence; no-gap ordering is expected under this locked `MAX+1` design.

## Schema and query authorization

One forward migration is authorized, using V65 only if it remains the next free version. V1–V64 are immutable. Authorized work is limited to:

- add `ledger_sequence BIGINT` to `bunker_stock_movement`;
- deterministic fail-closed legacy backfill;
- set the column `NOT NULL` after validation;
- add uniqueness on `(tenant_id, tank_id, ledger_sequence)`;
- add a Tenant-leading latest-first index `(tenant_id, tank_id, ledger_sequence DESC)`;
- remove an older movement-order index only if inspection proves it redundant after the new index.

All repository/API operations meaning latest movement, latest-first ledger, or current ledger tail must order primarily by `ledger_sequence DESC`. An optional `id DESC` tie-break is defensive only; uniqueness means it cannot determine business order.

## Legacy backfill and fail-closed policy

Legacy rows receive a deterministic **migration canonical order**, not a claim about their original commit order. Within each Tenant/Tank, backfill order is `occurred_at ASC, created_at ASC, id ASC`, assigning contiguous values beginning at 1. The migration must not change timestamps, quantity, movement type, resulting balance, actor, reference, delete/combine rows, or synthesize movements.

Before `NOT NULL` and constraints are finalized, migration must fail closed when any of the following exists:

- a movement has no owning Tank or its Tenant differs from the Tank Tenant;
- deterministic canonicalization produces null, duplicate, non-positive, or non-contiguous sequence values;
- a Tank has movements but the highest canonicalized row's resulting balance differs from `bunker_tank.current_stock_liters`;
- a Tank has non-zero stock but no movement history, so the authoritative current balance cannot be reconciled to a ledger tail;
- existing logical duplicate/idempotency violations or invalid balance/quantity facts prevent safe canonicalization.

Historical data that fails these checks requires separately governed reconciliation; the migration must not invent accounting facts to pass.

## Required remediation evidence

The authorized remediation must prove, without sleep-based correctness assumptions:

- same `occurredAt` with different serialized order;
- backdated `occurredAt` does not change ledger order;
- concurrent receipt versus issue, two receipts, and two permitted issues;
- rollback leaves no movement/sequence;
- latest movement balance equals final Tank stock;
- sequences are monotonic per Tank and independent across Tanks and Tenants;
- duplicate Tenant/Tank sequence is rejected;
- legacy backfill and every fail-closed condition;
- clean PostgreSQL migration V1 through the new head using only `transport_logistics_acceptance`;
- focused Bunker/Fuel regressions, complete Maven, architecture, static analysis, and `git diff --check`.

## Scope and next task

This authorization changes no Fuel Card behavior, API, permission, event, scheduler, module dependency, or story accounting. US-35 remains `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED`.

Next task: `US-35-FUEL-CARDS-ACCEPTANCE-REMEDIATION-002`.
