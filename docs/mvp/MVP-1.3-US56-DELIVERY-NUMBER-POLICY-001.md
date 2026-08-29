# US-56 Delivery Order Number Policy

**Task:** `MVP-1.3-US56-DELIVERY-NUMBER-POLICY-001`  
**Date:** 2026-08-29  
**Status:** COMPLETE  
**Production implementation:** None  
**US-56:** `READY_FOR_IMPLEMENTATION`

## 1. Source Evidence

The DOCX requires a Delivery Order record but does not define its number. The frozen Delivery contract defines `DeliveryNumber` as a tenant-scoped business key and requires `UNIQUE (tenant_id, delivery_number)`. Existing `FO-`, `CM-`, `CEX-`, fuel and insurance generators are bounded-context-specific `EXISTING_MODULE_CONVENTION`; no reusable platform numbering standard exists. They establish evidence for server authority, immutable human-readable references, database-backed allocation and gap tolerance, but do not authorize a Delivery format.

## 2. Frozen Policy

### Generation authority

`SERVER_GENERATED` (`ARCHITECTURAL_DECISION`). The client never supplies or changes the authoritative Delivery number. The Delivery application requests a number from a Delivery-owned outbound generator port during create. External/customer references, if later approved, remain separate fields.

### Prefix and format

`PREFIX = DEL` (`PRODUCT_DECISION`). Exact format:

```text
DEL-YYYY-NNNNNN
```

- Uppercase ASCII only; regex `^DEL-[0-9]{4}-[0-9]{6}$`.
- Hyphen separators, four-digit calendar year, six-digit zero-padded sequence.
- Exact length and maximum length: 15 characters.
- Canonical comparison is case-sensitive; lowercase/mixed-case representations are invalid.
- Examples: first `DEL-2026-000001`; normal `DEL-2026-004287`; final width value `DEL-2026-999999`.

### Scope, year and reset

Counter scope is `PER_TENANT_PER_YEAR` (`PRODUCT_DECISION`). The year is the calendar year in the authoritative Tenant's configured IANA time zone—not UTC, browser time, request input or fiscal year. The counter begins at `1` independently for each `(tenant_id, calendar_year)` and resets by using a new yearly counter row/key.

Examples:

- Tenant A 2026 first: `DEL-2026-000001`; Tenant B 2026 first: `DEL-2026-000001`. This is valid because uniqueness is tenant-scoped.
- Tenant A after `DEL-2026-000125`: next is `DEL-2026-000126`.
- Tenant A first allocation after its local midnight on 2027-01-01: `DEL-2027-000001`.
- An in-flight request uses the tenant-local year determined once at allocation; it does not change mid-transaction.

At `999999`, the next allocation for that Tenant/year fails with `DELIVERY_NUMBER_SEQUENCE_EXHAUSTED`. It never wraps, widens or reuses a number.

### Concurrency and gaps

`ATOMIC_ALLOCATION_REQUIRED` (`ARCHITECTURAL_DECISION`). A future adapter must use a database-atomic counter operation keyed by `(tenant_id, calendar_year)` (for example row locking or atomic upsert/update-returning). `SELECT MAX(...) + 1` is prohibited. Concurrent creates in one scope receive distinct increasing values; different Tenant/year scopes do not contend logically.

`GAPS_ALLOWED` (`PRODUCT_DECISION`). Rollback, failure, timeout or collision recovery may consume a value. Consumed values are never reused, and the number series is not evidence that every lower value became a successful order. No legal/accounting gapless requirement exists.

### Retry and idempotency

- Technical retry inside the create operation may allocate again after a serialization/deadlock/collision failure; it must never reuse a number unless the original transaction is known to have rolled back its allocation atomically.
- Ordinary client resubmission is a new create and receives a new number if successful.
- `NO_EXPLICIT_IDEMPOTENCY_IN_US56` (`ARCHITECTURAL_DECISION`). No `Idempotency-Key`, request table or generic framework is introduced. Separate successful requests create separate Delivery Orders. UI retry must not claim exactly-once behavior.
- Future explicit idempotency requires a separate contract and would be scoped by Tenant plus endpoint plus key with payload-mismatch conflict semantics.

### Collision and uniqueness

The final guard is `UNIQUE (tenant_id, delivery_number)`. A collision never overwrites. The application may transparently retry atomic allocation up to three total allocation attempts; exhaustion of those attempts maps to `DELIVERY_NUMBER_ALLOCATION_FAILED` without exposing SQL details. Sequence exhaustion is not retried.

### Mutability and ownership

`DeliveryNumber` is an immutable, persistence-independent Delivery domain value object. It is generated through an outbound `DeliveryNumberGenerator` port owned by Delivery. Conceptual input is authoritative `tenantId` plus the already-resolved tenant-local calendar year; output is a validated `DeliveryNumber`. Domain/application code does not call PostgreSQL or a clock/time-zone provider directly.

No update command, DTO, UI control, lifecycle transition or audit operation can renumber an order. Successful Delivery Order creation audit/event records the allocated number; counter allocation itself creates no separate business audit event.

## 3. API and Persistence Impact

| Contract surface | `deliveryNumber` |
| :--- | :--- |
| Create request | NO |
| Create response | YES |
| Detail response | YES |
| List response | YES |
| Update request | NO |

Conceptual persistence:

- `delivery_order.delivery_number VARCHAR(15) NOT NULL`.
- `UNIQUE (tenant_id, delivery_number)` remains the final guard.
- A Delivery-owned allocation store keyed by `tenant_id UUID` and `calendar_year INTEGER`, holding the last allocated value, with a unique/primary key over both fields.
- Tenant identity comes only from `CurrentTenant` / `TenantExecutionContext`; tenant time zone comes from authoritative Tenant data.
- No Flyway SQL is created by this decision task.

## 4. Failure Semantics

| Condition | Result |
| :--- | :--- |
| Tenant context/time zone unavailable | Fail closed; `TENANT_CONTEXT_REQUIRED` / tenant-safe server error |
| Atomic allocator unavailable | `DELIVERY_NUMBER_ALLOCATION_FAILED`; no fabricated number or order |
| Sequence exhausted | `DELIVERY_NUMBER_SEQUENCE_EXHAUSTED`; operator action required |
| Collision | Up to three allocation attempts, then `DELIVERY_NUMBER_ALLOCATION_FAILED` |
| Invalid generated representation | `DELIVERY_NUMBER_ALLOCATION_FAILED`; never persisted |
| Client repeats create | New create/new number; no exactly-once promise |

## 5. Test Contract

- Format: prefix, four-digit year, six-digit padding, length 15, first/normal/999999 examples and invalid representation rejection.
- Tenant/year: independent Tenant A/B counters, same-year increment, tenant-local year boundary and reset.
- Concurrency: concurrent allocation within one scope is unique; no `MAX+1`; different scopes remain independent.
- Exhaustion: 999999 succeeds, next fails without wrap.
- Gaps/retry: consumed values need not be reused; technical retry and client resubmission follow the frozen rules.
- Collision: database unique constraint is final guard; bounded retry and sanitized error.
- Immutability/API: create request and update request cannot set the number; responses expose it; updates preserve it.
- Persistence: tenant-scoped uniqueness permits the same formatted number in different Tenants but rejects it twice in one Tenant.

## 6. Decision Matrix

| Decision | Final policy | Provenance | Rationale | Implementation impact |
| :--- | :--- | :--- | :--- | :--- |
| Generation authority | `SERVER_GENERATED` | `ARCHITECTURAL_DECISION` | Protect authoritative business key | Generator port; no request field |
| Prefix | `DEL` | `PRODUCT_DECISION` | Stable Delivery vocabulary | Constant and validation |
| Exact format | `DEL-YYYY-NNNNNN`, uppercase ASCII, 15 chars | `PRODUCT_DECISION` | Readable deterministic reference | VO, schema and UI |
| Sequence width | Six digits, zero-padded | `PRODUCT_DECISION` | Fixed representation through 999999 | Formatter and validation |
| Sequence scope | Per Tenant per year | `PRODUCT_DECISION` | Tenant-independent number series | Scoped counter key |
| Year authority | Tenant-local calendar year from authoritative configured time zone | `PRODUCT_DECISION` | Operationally meaningful boundary | Tenant time-zone lookup |
| Reset behavior | New counter scope when Tenant-local calendar year changes | `PRODUCT_DECISION` | Predictable annual series | Counter key rotation |
| Starting value | 1 (`000001`) | `PRODUCT_DECISION` | Defined first value | Counter initialization |
| Exhaustion | Fail after 999999 | `ARCHITECTURAL_DECISION` | Never wrap/collide | Explicit error |
| Concurrency | Database-atomic allocation | `ARCHITECTURAL_DECISION` | Unique under concurrency | Counter adapter; prohibit MAX+1 |
| Gap policy | Allowed; never reuse | `PRODUCT_DECISION` | No gapless legal requirement | Rollback may consume values |
| Technical retry | May retry internally and allocate afresh without duplicate persistence | `ARCHITECTURAL_DECISION` | Preserve transactional correctness | Infrastructure retry boundary |
| Client retry | Independent resubmission is a new create and may receive a new number | `ARCHITECTURAL_DECISION` | No implicit payload deduplication | UX prevents accidental repeats |
| Idempotency | None explicit in US-56 | `ARCHITECTURAL_DECISION` | Avoid unapproved framework | No idempotency header/store |
| Collision handling | Tenant unique guard; max three attempts | `ARCHITECTURAL_DECISION` | Defense in depth | Sanitized failure |
| Mutability | Immutable | `PRODUCT_DECISION` | Stable traceability | No update field/control |
| Tenant uniqueness | `UNIQUE(tenant_id, delivery_number)` | `SOURCE_REQUIREMENT` | Frozen schema contract | Database constraint |
| API exposure | Response/list/detail only | `ARCHITECTURAL_DECISION` | Server authority | DTO rules |
| Domain ownership | Delivery value object + outbound port | `ARCHITECTURAL_DECISION` | Hexagonal isolation | Provider-neutral contract |
| Database allocation requirement | Atomic Delivery-owned persistence mechanism keyed by Tenant/year | `ARCHITECTURAL_DECISION` | Safe scoped concurrency | Forward Flyway migration and adapter in R3 |

No implementation-critical Delivery-number ambiguity remains.
