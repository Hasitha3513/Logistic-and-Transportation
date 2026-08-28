# Canonical Tenant Owner Approval

**Related task:** `TENANT-LEGACY-RUNTIME-RECONCILIATION-001`  
**Status:** **CANONICAL DECISION APPROVED — RUNTIME RECONCILIATION REQUIRED**  
**Decision date:** 2026-08-28  
**Effect:** Canonical identity and policy are frozen. Backfill is only conditionally
authorized and cannot be implemented until every reconciliation gate passes.

## Canonical Tenant

Tenant UUID: `4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a`

Legal Operating Entity: **Ceylon Logistics & Transport Solutions (Pvt) Ltd**

Tenant Code: `CLTS-LK`

Tenant Name: **Ceylon Logistics & Transport Solutions (Pvt) Ltd**

Default Currency: `LKR`

Default Time Zone: `Asia/Colombo`

Initial Status: `ACTIVE`

## Historical Data Ownership

All existing production/business operational data currently stored in the
Transport & Logistics system belongs to `CLTS-LK`, except records explicitly
identified during reconciliation as `DEMO`, `TEST`, `GLOBAL_REFERENCE`,
`SHARED`, or `UNKNOWN`.

`UNKNOWN` and `SHARED` records must not be automatically backfilled. They
require explicit classification before tenant enforcement.

## Demo/Test Data

Demo and test records must be identified during runtime reconciliation and must
not automatically be treated as production tenant data. Their final disposition
must be recorded before tenant enforcement.

## User Membership Authority

Existing approved operational users belong to `CLTS-LK`. Runtime reconciliation
must first distinguish operational users from system, service, demo, test, and
unknown users. System/service users require separate classification; unknown
users prevent the membership gate from passing.

## Global Reference Policy

Generic reference data such as countries, units of measure, and other approved
shared master data may remain global where architecture permits. Absence of
`tenant_id` does not by itself establish global status; each exception requires
an explicit classification.

## Backfill Authorization

**CONDITIONALLY AUTHORIZED.** Authorization advances to
`AUTHORIZED_FOR_IMPLEMENTATION` only after read-only reconciliation proves:

- unmapped rows = 0;
- multi-mapped rows = 0;
- unresolved shared rows = 0;
- unresolved user memberships = 0;
- orphan tenant relationships = 0;
- relationship conflicts = 0.

No database change or backfill is authorized by this document alone.

## Approval

Decision source: User-authored task directive
`TENANT-LEGACY-RUNTIME-RECONCILIATION-001`, received 2026-08-28.

Business Owner: **NAMED SIGN-OFF NOT SUPPLIED**

Enterprise Architect: **NAMED SIGN-OFF NOT SUPPLIED**

Security / Identity Owner: **NAMED SIGN-OFF NOT SUPPLIED**

Data Governance Owner: **NAMED SIGN-OFF NOT SUPPLIED**

Engineering / Release Owner: **NAMED SIGN-OFF NOT SUPPLIED**

Approval Date: 2026-08-28

Evidence Reference: `TENANT-LEGACY-RUNTIME-RECONCILIATION-001`, Section 1

Signature / Approval Record: User-authored explicit canonical decision in the
task directive. No organizational named-signatory records were supplied; this
fact must not be inferred or fabricated.

## Approval declaration

The user-authored directive freezes the canonical Tenant identity and conditional
ownership policy as inputs to runtime reconciliation. It does not execute a
migration, authorize unresolved records, or permit tenant backfill before every
gate above passes.

### Synchronized Knowledge Base Files:

None. This synchronization records the user-approved project decision locally;
no central knowledge-base contract was changed in this task.
