# Canonical Tenant Owner Approval

**Related task:** `TENANT-LEGACY-EVIDENCE-002`  
**Status:** **DRAFT — BUSINESS OWNER APPROVAL REQUIRED**  
**Effect:** This document does not authorize migration or backfill until completed and approved.

## Canonical Tenant

Legal Operating Entity: **REQUIRED**

Tenant Code: **REQUIRED**

Tenant Name: **REQUIRED**

Default Currency: **REQUIRED**

Default Time Zone: **REQUIRED**

Initial Status: `ACTIVE`

## Historical Data Ownership

Select exactly one:

- [ ] All production historical business records belong to this Tenant.
- [ ] Historical records contain multiple owners and require deterministic per-record mapping.

If multiple owners are selected, attach the authoritative owner list and mapping rule for every affected record/table:

**Evidence/mapping reference:** REQUIRED

## Demo/Test Data

Select and document the disposition applicable to each matching runtime record:

- [ ] Exclude
- [ ] Quarantine
- [ ] Explicitly adopt

**Record list and evidence:** REQUIRED

## User Membership Authority

Attach the actual runtime user inventory and certify exactly one active Tenant membership for each active business user. Explicitly classify bootstrap, service, demo, test, inactive, and unknown users.

**User mapping reference:** REQUIRED

## Approval

Approved By: **REQUIRED**

Authority: **REQUIRED**

Approval Date: **REQUIRED**

Evidence Reference: **REQUIRED**

Signature / Approval Record: **REQUIRED**

## Approval declaration

By approving this document, the named authority certifies the canonical Tenant identity and the stated historical-ownership/test-data dispositions as inputs to a separately authorized migration plan. Approval does not itself execute or authorize unreviewed database changes.

### Synchronized Knowledge Base Files:

None. This is an unsigned approval template; it contains no approved Tenant identity or ownership facts.
