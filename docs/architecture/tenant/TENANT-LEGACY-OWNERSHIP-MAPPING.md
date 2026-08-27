# TENANT-LEGACY-OWNERSHIP-AUTHORITY-001 — Legacy Ownership Mapping

**Discovery date:** 2026-08-27  
**Status:** **BLOCKED_RUNTIME_DATABASE_UNAVAILABLE**  
**Certification:** **NOT READY FOR BUSINESS APPROVAL**  
**Backfill authorization:** **NOT AUTHORIZED**

## 1. Scope and limitations

Migrations V1–V41, application configuration, bootstrap code, sample datasets, tests, and governance documents were inspected. No production database export or authoritative business-owner record was supplied. Docker inspection failed because the local socket is unavailable, and no PostgreSQL client is installed. Runtime row, orphan, duplicate, and relationship-conflict counts therefore remain unknown.

The committed datasets are explicitly mock/development assets. They prove test scenarios only, not legal ownership or production contents.

### TENANT-LEGACY-EVIDENCE-002 connectivity result

- Host source: `DB_URL`, defaulting to JDBC PostgreSQL at `localhost:5432`; Compose uses service host `postgres:5432`.
- Database-name source: `POSTGRES_DB` / JDBC URL, default `transport_logistics`.
- Username source: `DB_USERNAME` or Compose `POSTGRES_USER`.
- Connection mechanism: Spring JDBC/Flyway; project-supported inspection path is Docker Compose `psql`.
- Reachability: **not established**. Direct TCP socket access is prohibited in the execution environment, Docker socket access was denied both normally and through the approval path, and no local `psql` executable is installed.
- Runtime PostgreSQL/Flyway version, applied/pending/failed/unexpected migrations, and every runtime reconciliation count remain **UNKNOWN**.

Safe prerequisite: provide Docker socket access to the current user and a running project `postgres` service, then run read-only queries through `docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"`; alternatively provide a read-only database connection plus `psql`, or a sanitized query export containing Flyway history and all required inventories. Do not place secrets in the command line or evidence artifact.

## 2. Canonical owner and Tenant

**CANONICAL_OWNER_NOT_PROVEN**

No authoritative source identifies one legal entity as owner of all historical data. Customers/vendors are business master data, not Tenant candidates. Repository/database names, local usernames, Sri Lankan sample addresses, LKR/USD examples, and an operational notification time zone are not business authority.

| Field | Certified value | Status |
|---|---|---|
| Tenant UUID | **REQUIRED** | Generate only after owner approval |
| Tenant code | **REQUIRED** | No authoritative code |
| Tenant name | **REQUIRED** | Legal operator not proven |
| Currency | **REQUIRED** | Scenario currencies are not a Tenant default |
| Time zone | **REQUIRED** | Operational config is not approved Tenant data |
| Initial status | **REQUIRED** | No approved bootstrap record |

No UUID was generated and no single-owner mapping is proposed.

## 3. Legacy users

Production migrations seed no users. Actual users exist only in runtime data, which was unavailable.

| Source | Identity | Classification | Expected Tenant | Disposition |
|---|---|---|---|---|
| `postgresql-sample-data.sql` | Four fixed active users: `system.admin`, `ops.manager`, `fuel.officer`, `freight.planner` | DEMO_USER | UNKNOWN | Exclude/quarantine unless explicitly adopted |
| `h2-phase1.sql` | Three fixed active sample users | TEST/DEMO_USER | UNKNOWN | Never map silently |
| `LocalIdentityBootstrap` | Environment username/email, random UUID, `LOCAL_MVP_ADMIN` | BOOTSTRAP_ADMIN | UNKNOWN | Inventory each actual environment |
| Test sources | Random/fixed test users | TEST_USER | none | Exclude from production mapping |

| Measure | Result |
|---|---:|
| Actual users / active users | **UNKNOWN / UNKNOWN** |
| Certified business users | **0** |
| Safely mapped active users | **0** |
| Users requiring inventory/authority | **UNKNOWN** |

Exactly-one-active-membership cannot be certified without an actual user inventory and canonical Tenant.

## 4. Tenant-owned schema inventory

The migration chain defines 64 tables. Fifty-seven are tenant-owned business tables under the frozen classification. For every row below, the actual row count, orphan count, and conflicts are `UNKNOWN`; certification is `BLOCKED_OWNER_AND_RUNTIME`.

| Module | Table | Ownership source / deterministic rule after approval | Primary exception risk |
|---|---|---|---|
| Organization | `customer` | approved Tenant | customer must not become Tenant |
| Organization | `department` | approved Tenant | unknown owner |
| Organization | `location` | approved Tenant | potentially shared location |
| Organization | `project` | same Tenant as department | missing/mismatched department |
| Organization | `vendor` | approved Tenant | vendor must not become Tenant |
| Fleet | `vehicle_category` | approved Tenant | legacy catalogue intent ambiguous |
| Fleet | `vehicle_type` | category Tenant | missing/mismatched category |
| Fleet | `vehicle` | approved Tenant; category/type must match | reference conflict |
| Fleet | `vehicle_document` | vehicle Tenant | orphan vehicle |
| Fleet | `vehicle_reading` | vehicle Tenant | actor/reference conflict |
| Fleet | `vehicle_meter_reset` | vehicle Tenant | actor/orphan conflict |
| Fleet | `maintenance_schedule` | vehicle Tenant | orphan vehicle |
| Fleet | `lubricant_log` | vehicle Tenant; vendor must match | cross-module mismatch |
| Driver | `driver` | approved Tenant | unknown workforce owner |
| Driver | `driver_license` | driver Tenant | orphan driver |
| Driver | `driver_exception` | driver Tenant | orphan driver |
| Driver | `driver_violation` | driver Tenant; trip must match | cross-module mismatch |
| Driver | `driver_medical_record` | driver Tenant | orphan driver |
| Driver | `driver_drug_test` | driver Tenant | orphan driver |
| Routing | `route` | approved Tenant; locations must match | cross-module mismatch |
| Routing | `route_stop` | route/location Tenant | reference conflict |
| Routing | `route_revision` | route Tenant | orphan route |
| Routing | `route_revision_stop` | revision/location Tenant | reference conflict |
| Routing | `route_disruption` | route Tenant | orphan route |
| Trip | `trip` | approved Tenant; all logical refs must match | multi-module conflict |
| Trip | `trip_status_history` | trip Tenant | orphan trip |
| Trip | `trip_dispatch` | trip Tenant | orphan trip |
| Trip | `trip_operational_event` | trip Tenant | orphan trip |
| Fuel | `fuel_station` | approved Tenant | unknown owner |
| Fuel | `fuel_limit_policy` | approved Tenant; scoped refs match | reference conflict |
| Fuel | `fuel_issue` | approved Tenant; vehicle/station/trip/actors match | multi-module conflict |
| Fuel | `fuel_issue_history` | issue Tenant | orphan issue |
| Fuel | `fuel_price` | station Tenant | orphan station |
| Fuel | `fuel_purchase` | approved Tenant; vendor/station/actors match | reference conflict |
| Fuel | `fuel_purchase_history` | purchase Tenant | orphan purchase |
| Fuel | `bunker_tank` | station Tenant | orphan station |
| Fuel | `bunker_stock_movement` | tank Tenant; source must match | source conflict |
| Fuel | `bunker_dip_reading` | tank Tenant | orphan tank |
| Fuel | `bunker_stock_adjustment` | tank Tenant | orphan tank |
| Freight | `freight_order` | approved Tenant; customer/locations match | cross-module conflict |
| Freight | `freight_order_line` | order Tenant | orphan order |
| Freight | `cargo_manifest` | order Tenant | orphan order |
| Freight | `cargo_manifest_item` | manifest/order-line Tenant | conflicting parents |
| Freight | `load_plan` | manifest Tenant; vehicle must match | cross-module conflict |
| Freight | `load_plan_item_placement` | plan/item Tenant | conflicting parents |
| Freight | `freight_insurance_policy` | order/manifest Tenant | conflicting parents |
| Freight | `freight_insurance_claim` | policy/order Tenant | conflicting parents |
| Freight | `freight_insurance_settlement` | claim Tenant | orphan claim |
| Freight | `cargo_exception` | order Tenant; optional manifest/trip match | cross-module conflict |
| Freight | `cargo_exception_history` | exception Tenant | orphan exception |
| Notification | `notification_rule` | approved Tenant | unknown owner |
| Notification | `notification` | event/recipient Tenant | mixed recipient/event |
| Notification | `notification_rule_policy` | rule Tenant | orphan rule |
| Notification | `notification_rule_quiet_day` | rule/policy Tenant | orphan parent |
| Notification | `notification_rule_execution` | rule/event Tenant | idempotency conflict |
| Notification | `notification_delivery_attempt` | notification Tenant | orphan notification |
| Offline | `offline_sync_operation` | actor membership and aggregate Tenant must agree | operation ID proves no ownership |

## 5. Global/identity exclusions

| Table | Classification and treatment |
|---|---|
| `app_permission` | GLOBAL permission catalogue; no tenant backfill |
| `app_role` | GLOBAL role templates; no tenant ownership on definition |
| `app_role_permission` | GLOBAL template mapping |
| `notification_template` | GLOBAL system/default templates; customized copies deferred |
| `app_user` | Global credential identity; membership supplies Tenant scope |
| `app_user_role` | Legacy unscoped assignment; migrate only from approved membership/role mapping |
| `refresh_token` | Identity session; runtime inventory and Tenant binding required during implementation |

## 6. Freight discovery

Sample SQL contains mock freight orders, manifests, load plans, and insurance records. It is not production evidence. V41 cargo-exception tables postdate the sample file's declared V1–V38 compatibility.

| Measure | Runtime result |
|---|---:|
| Orders / lines | UNKNOWN / UNKNOWN |
| Manifests / items | UNKNOWN / UNKNOWN |
| Load plans / placements | UNKNOWN / UNKNOWN |
| Policies / claims / settlements | UNKNOWN / UNKNOWN / UNKNOWN |
| Exceptions / history | UNKNOWN / UNKNOWN |
| Mapped | 0 certified |
| Unknown / orphans / conflicts | UNKNOWN / UNKNOWN / UNKNOWN |

Freight mapping is **BLOCKED**; US-29 cannot proceed.

## 7. Exceptional records and multiple owners

| Class | Finding | Required disposition |
|---|---|---|
| Orphans | Runtime counts unavailable | Read-only FK/logical-reference audit, then approved quarantine/manual mapping/delete-candidate classification; delete nothing here |
| Test/demo | Sample SQL and bootstrap are explicitly non-production candidates | Exclude, quarantine, or explicitly adopt each actual record |
| Unknown | All runtime business rows/users are uncertified | Supply database snapshot/count report and owner evidence |
| Shared/multiple owners | Repository neither proves nor excludes multiple legal owners | Certify single owner or provide deterministic per-record rules |

No record is approved for deletion or Tenant assignment.

## 8. Identifier collisions

Current global constraints cover many customer/department/location/project/vendor codes, employee/registration numbers, route/trip/freight/manifest/load-plan/insurance/exception identifiers, and fuel references. They prevent duplicates inside the current namespace where enforced but do not reveal external-source collisions or prove future ownership.

In-database duplicates should be zero for enforced unique columns, but the database was not queried. Tenant-scoped collision readiness remains **BLOCKED_RUNTIME**. No constraint change is authorized.

## 9. Reconciliation

| Measure | Certified result |
|---|---:|
| Tenant-owned tables inventoried | 57 / 57 |
| Actual tenant-owned rows | UNKNOWN |
| Safely mapped rows | 0 certified |
| Unknown/unresolved rows | UNKNOWN |
| Orphans | UNKNOWN |
| Relationship conflicts | UNKNOWN |
| Actual test/demo rows | UNKNOWN |
| Unresolved active memberships | UNKNOWN |

Certification requires unknown ownership, conflicts, and unresolved active memberships to be zero or explicitly approved. Those conditions are not met.

## 10. Evidence required

1. Authoritative record naming the legal operating company.
2. Approved Tenant code/name/currency/time zone/status and recorded canonical UUID.
3. Read-only production or migration-source inventory for all 64 tables.
4. Complete actual-user inventory and classification.
5. Parent/orphan/logical-reference audit for all 57 tenant-owned tables.
6. Approved test/demo/unknown/shared-record dispositions.
7. Identifier collision results and per-record mapping if multiple owners exist.
8. Named approver, authority, date, evidence reference, and signature/approval record.

## 11. Certification

| Field | Value |
|---|---|
| Approver | **REQUIRED** |
| Role/authority | **REQUIRED** |
| Approval date | **REQUIRED** |
| Evidence reference | **REQUIRED** |
| Signature/approval record | **REQUIRED** |

Codex is not the business approver. This artifact remains **BLOCKED** and cannot authorize backfill.

The unsigned canonical-owner template is `docs/architecture/tenant/TENANT-CANONICAL-OWNER-APPROVAL.md`.

### Synchronized Knowledge Base Files:

- `00_CORE_ARCHITECTURE/multi_tenancy_standards.md`

The central standard records that certification is blocked because owner evidence, runtime reconciliation, and formal approval are absent. No commit or push was performed per task constraints.
