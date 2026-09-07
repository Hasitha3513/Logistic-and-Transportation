# US-46 Technical Remediation Authorization

**Task:** `US-46-TECHNICAL-REMEDIATION-AUTHORIZATION-001`  
**Classification:** Non-story technical governance  
**Decision:** `APPROVED`  
**Date:** 2026-09-07  
**Current Flyway head:** V70  
**Authorized migration:** V71 (verified free)  
**Story accounting:** unchanged at 70 / 87 complete and 17 / 87 remaining  
**Next task:** `US-46-DRIVER-PAYROLL-LINK-TECHNICAL-REMEDIATION-001`

## Decision

One forward-only migration, V71, is authorized for the minimum Driver-owned persistence needed to make worker-mapping commands durably idempotent. V1–V70 remain immutable. V71 may extend `driver_payroll_worker_mapping` or add one same-module, tenant-scoped mapping-command idempotency table containing the idempotency key, deterministic canonical request hash, original safe result identity/snapshot, actor/audit facts, and replay metadata. It must use tenant-leading uniqueness and cannot contain unrelated schema changes.

For `PUT /api/v1/drivers/{driverId}/payroll-worker-mapping`, the same Tenant, key, and canonical frozen-field request must replay the original safe result without changing mapping version, `updatedAt`, actor/time audit facts, history, or worker reference. Reuse of the same Tenant/key with a different canonical request must fail with a deterministic conflict. A genuinely new key may mutate the mapping only with the expected optimistic version.

## Authorized technical remediation

The remediation must also close these already-frozen implementation gaps without introducing another public route, permission, outbox, or product semantic:

- transition `EXPORT_REQUESTED -> EXPORTED` only from safe Integration file-delivery/hash evidence, with no HRMS acknowledgement or settlement meaning;
- enforce the 32-KiB canonical payload limit during validation, while retaining a defensive export check;
- prove the complete correction lifecycle and supersede the original only after successful correction delivery if `SUPERSEDED` is retained;
- preserve released batch, line, mapping snapshot, source Trip/Driver, Integration configuration, and history immutability;
- expand Tenant A/B denial for foreign Trip, mapping, batch approval/export/history, and correction source;
- add deterministic PostgreSQL proofs for DB immutability, outbox commit/rollback atomicity, optimistic conflicts, Integration dedupe, and the nine-race concurrency matrix;
- strengthen real Chromium evidence for mapping replay/conflict, mapping snapshot, validation-time size failure, full correction export, delivered `EXPORTED`, Tenant denial, and exact private file/hash contents.

The existing `DriverPayrollInputExportRequestedV1`, shared `DurableEventPublisher`, `integration_outbox_event`, `integration-outbound-exchange`, four permissions, frozen API, Trip published contract, controlled `FILE_JSON_V1`, and Payroll/HRMS authority remain unchanged.

## Explicit exclusions

This authorization permits no payroll/salary/tax/pension/payslip/payment/ledger/employee/attendance engine, automatic punitive deduction, live HRMS, webhook, SFTP, inbound acknowledgement, manual retry route, foreign repository/SQL/FK, distributed transaction, new top-level module, public API, permission, or story accounting change.

## Verification required after remediation

Acceptance evidence must use only `transport_logistics_acceptance`, migrate cleanly V1→V71, run the complete focused backend/security/PostgreSQL/concurrency suite, Driver/Trip/US-73/P1-01/Identity/Tenancy/Audit regressions, full Maven verification, architecture/static/frontend gates, strengthened real Chromium, and `git diff --check`. Technical closure and final acceptance remain separate later tasks.
