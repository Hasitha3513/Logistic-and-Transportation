# Vehicle Reading Source Identity Constraint Remediation

**Task:** `FOUNDATION-VEHICLE-READING-SOURCE-IDENTITY-CONSTRAINT-REMEDIATION-001`
**Status:** COMPLETE
**Migration:** `V84__vehicle_reading_source_identity_constraint.sql`

## Decision and implementation

Fleet remains the owner of the append-only Vehicle Reading ledger. An original externally sourced reading is
uniquely identified within a Tenant by `tenant_id`, `vehicle_id`, `reading_type`, `source_type`, and
`source_reference_id`. Manual/source-less readings and correction rows are outside that uniqueness boundary.

V84 first fails deterministically if an eligible schema contains duplicate original source identities. It does not
delete, merge, or rewrite historical data. An eligible schema then receives the partial unique index
`uq_vehicle_reading_source_identity`. Historical migrations V1–V83 remain unchanged, and no persistence mapping,
REST API, event contract, permission, frontend behavior, dependency, or module boundary changes.

## Regression coverage

- Equivalent source retries return the original application fact without a second event or row.
- PostgreSQL rejects a database-level duplicate even when application validation is bypassed.
- The same external reference remains valid for another source type and another Tenant.
- Multiple source-less manual readings remain valid.
- Flyway validates and migrates both V83→V84 and an empty PostgreSQL schema V1→V84.
- The production migration-head assertion advances from stale V17 to V84.
- The Offline Sync index baseline now names its two V29 indexes plus the intentional V44 Tenant index.
- Twenty-two additional PostgreSQL acceptance assertions that treated V83 as the repository head now assert V84;
  migration-specific assertions remain unchanged.

## Verification evidence

- Vehicle Reading domain/application tests: 10 tests, 0 failures, 0 errors, 0 skipped.
- Focused PostgreSQL production/offline invariant tests: 15 tests, 0 failures, 0 errors, 0 skipped.
- Affected PostgreSQL migration-head regression group: 108 tests, 0 failures, 0 errors, 0 skipped.
- Architecture and Spring Modulith verification: 55 tests, 0 failures, 0 errors, 0 skipped.
- Complete `./mvnw -B clean test`: 1,669 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`
  in 13:46.
- Every accepted PostgreSQL command used only `transport_logistics_acceptance`. Flyway validated V83→V84 and
  a clean V1→V84 migration path.

## Rollback

Application rollback is to deploy the prior application version while leaving V84 in place; the additive index is
compatible with existing reads and valid writes. Removing the invariant requires a separately reviewed forward
migration and is not part of this remediation.
