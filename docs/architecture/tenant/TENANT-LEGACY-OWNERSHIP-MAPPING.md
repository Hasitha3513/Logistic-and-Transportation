# TENANT-LEGACY-OWNERSHIP-AUTHORITY-001 — Legacy Ownership Mapping

**Discovery date:** 2026-08-27  
**Status:** **SUPERSEDED_BY_CLEAN_INITIALIZATION_DECISION**  
**Certification:** **LEGACY_RECONCILIATION_NOT_APPLICABLE**  
**Backfill authorization:** **LEGACY_BACKFILL_NOT_APPLICABLE**

## Clean-initialization decision — TENANT-CLEAN-INITIALIZATION-DECISION-001

**Decision date:** 2026-08-28  
**Result:** **CLEAN TENANT INITIALIZATION AUTHORIZED**

The authoritative decision declares that no recoverable legacy production
database requires preservation. Both discovered PostgreSQL databases are empty
new-environment targets, no credible backup exists, and the legacy preservation
and backfill path is retired.

- Legacy database: **NOT FOUND**
- Legacy preservation: **NOT REQUIRED**
- Legacy reconciliation: **NOT APPLICABLE**
- Legacy backfill: **NOT REQUIRED / NOT EXECUTED**
- Clean tenant-aware initialization for `CLTS-LK`: **AUTHORIZED**
- Canonical Tenant: `4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a`, `CLTS-LK`,
  Ceylon Logistics & Transport Solutions (Pvt) Ltd, `LKR`, `Asia/Colombo`,
  `ACTIVE`

Historical discovery and failed reconciliation evidence below is preserved for
audit traceability but no longer represents a prerequisite to backfill, because
there is no legacy backfill. No schema, row, migration, or application code was
changed by this decision.

## Database recovery discovery — TENANT-RUNTIME-DATABASE-RECOVERY-001

**Discovery date:** 2026-08-28  
**Mode:** Read-only database identification  
**Result:** **LEGACY_DATABASE_NOT_FOUND**

### Configuration paths checked

| Source | Host | Port | Database | Username | Profile / purpose | Reachability |
|---|---|---:|---|---|---|---|
| `.env`, `.env.docker.example`, `compose.yml` | Compose service `postgres` / published `localhost` | 5432 | `transport_logistics` | `transport_app` | Docker PostgreSQL runtime | Reachable in both Docker contexts; empty |
| `application-postgres.yml` | `localhost` default | 5432 | `transport_logistics` | `transport_app` default | Spring `postgres` profile | Same empty runtime endpoint |
| IntelliJ data source and history | `localhost` | 5432 | `transport_logistics` | `transport_app` | Local development | Same endpoint; no alternate database recorded |
| Default application/run scripts | Local detection | 5432 | PostgreSQL above or embedded H2 fallback | Configured environment | Development convenience | No additional persistent PostgreSQL source |

No alternate host, port, database, Compose project, deployment configuration,
or historical profile was found.

### Docker evidence

| Context | PostgreSQL container | Volume | Created | Size | Public tables | Flyway history | Classification |
|---|---|---|---|---:|---:|---|---|
| `desktop-linux` | `transport-logistics-postgres-1` (`postgres:16-alpine`, stopped) | `transport-logistics_postgres-data` | 2026-08-27 12:03 UTC | 70.28 MB | 0 | Absent | `EMPTY_SCHEMA` |
| `default` | `transport-logistics-postgres-1` (`postgres:16-alpine`, healthy) | `transport-logistics_postgres-data` | 2026-08-27 07:28 +05:30 | 48.13 MB | 0 | Absent | `EMPTY_SCHEMA` |

Both containers belong to Compose project `transport-logistics`, mount their
context-local volume at `/var/lib/postgresql/data`, and expose only the expected
PostgreSQL environment variable names. Secret values were not recorded.

The live `default` cluster contains only connectable databases `postgres` and
`transport_logistics`; each has zero public base tables and no
`flyway_schema_history`. The host PostgreSQL service is inactive and no other
PostgreSQL container, volume, or database was found.

### Backup and old-project search

- Accessible home, Documents, Downloads, Desktop, repository, and Git-history
  searches found no `.dump`, `.backup`, database `.bak`, SQL archive, or credible
  transport/logistics database backup.
- The only project SQL assets are Flyway migrations V1–V42 and explicitly
  non-authoritative PostgreSQL/H2 sample data. Build-output copies are identical
  application resources, not dumps.
- Only one repository checkout and its parent directory were found. IntelliJ
  history records the same `localhost:5432/transport_logistics` endpoint.
- Browser/profile databases, IDE metadata, plugin examples, and unrelated
  archives were rejected as non-candidates.

### Recovery decision

No live database or backup meets the authoritative fingerprint: substantial
application tables, identity/Freight/Fleet/Trip/Fuel tables, and Flyway history.
The current empty databases are not authoritative, and schema recreation would
not recover historical records.

Runtime ownership reconciliation therefore remains blocked. The next decision
must either supply/recover the historical database or formally declare that no
legacy production data requires preservation and authorize a clean tenant-aware
initialization. No database was started, restored, initialized, migrated,
repaired, or modified by this recovery task.

## Runtime reconciliation addendum — TENANT-LEGACY-RUNTIME-RECONCILIATION-001

**Reconciliation date:** 2026-08-28  
**Mode:** Read-only database audit  
**Result:** **BLOCKED**

Read-only inspection covered both PostgreSQL containers available through the
machine's Docker contexts:

- Docker Desktop Compose volume `transport-logistics_postgres-data`;
- system Docker volume `transport-logistics_postgres-data` (Compose-normalized
  volume name `transport-logistics_postgres-data` / daemon listing
  `transport-logistics_postgres-data` as exposed by its context).

Both connections reached PostgreSQL 16.15 as `transport_app` in database
`transport_logistics`, schema `public`. Neither application startup nor Flyway
was executed.

Each inspected database contains **zero public base tables**. Neither has
`flyway_schema_history`, identity tables, or tenant-owned business tables.
Consequently, neither instance is the expected legacy V1–V42 runtime database.
Starting the backend or applying migrations was deliberately avoided because
this task forbids database changes.

The canonical decision is synchronized as `CLTS-LK`, UUID
`4f8b6a3b-2c1e-4d89-9a72-f9e4c5b3671a`, legal name
`Ceylon Logistics & Transport Solutions (Pvt) Ltd`, currency `LKR`, time zone
`Asia/Colombo`, and status `ACTIVE`. The ownership and backfill authorization
remain conditional on successful runtime reconciliation. Runtime emptiness
cannot satisfy those gates.

### Per-table runtime certification

Every expected tenant-owned table is blocked because the table is absent from
the inspected runtime schema; the approved policy therefore cannot be evaluated
against actual legacy records.

| Module | Expected tables | Runtime status | Certification |
|---|---|---|---|
| Organization | `customer`, `department`, `location`, `project`, `vendor` | All absent | **BLOCKED** |
| Fleet | `vehicle_category`, `vehicle_type`, `vehicle`, `vehicle_document`, `vehicle_reading`, `vehicle_meter_reset`, `maintenance_schedule`, `lubricant_log` | All absent | **BLOCKED** |
| Driver | `driver`, `driver_license`, `driver_exception`, `driver_violation`, `driver_medical_record`, `driver_drug_test` | All absent | **BLOCKED** |
| Routing | `route`, `route_stop`, `route_revision`, `route_revision_stop`, `route_disruption` | All absent | **BLOCKED** |
| Trip | `trip`, `trip_status_history`, `trip_dispatch`, `trip_operational_event` | All absent | **BLOCKED** |
| Fuel | `fuel_station`, `fuel_limit_policy`, `fuel_issue`, `fuel_issue_history`, `fuel_price`, `fuel_purchase`, `fuel_purchase_history`, `bunker_tank`, `bunker_stock_movement`, `bunker_dip_reading`, `bunker_stock_adjustment` | All absent | **BLOCKED** |
| Freight | `freight_order`, `freight_order_line`, `cargo_manifest`, `cargo_manifest_item`, `load_plan`, `load_plan_item_placement`, `freight_insurance_policy`, `freight_insurance_claim`, `freight_insurance_settlement`, `cargo_exception`, `cargo_exception_history` | All absent | **BLOCKED** |
| Notification | `notification_rule`, `notification`, `notification_rule_policy`, `notification_rule_quiet_day`, `notification_rule_execution`, `notification_delivery_attempt` | All absent | **BLOCKED** |
| Offline | `offline_sync_operation` | Absent | **BLOCKED** |

### Required reconciliation gates

| Gate | Observed result | Decision |
|---|---|---|
| Unmapped rows = 0 | Cannot be evaluated against absent legacy tables | **BLOCKED** |
| Multi-mapped rows = 0 | Cannot be evaluated without legacy records and ownership paths | **BLOCKED** |
| Unresolved shared records = 0 | The conditional policy exists, but no runtime records are available to classify | **BLOCKED** |
| Unresolved user membership = 0 | `app_user` is absent, so operational/system/service classifications cannot be evaluated | **BLOCKED** |
| Orphan tenant references = 0 | Tenant columns/tables are absent; no legacy relationships can be audited | **BLOCKED** |

No row was inserted, updated, deleted, or backfilled. No migration was applied.
US-29 remains `BLOCKED_BY_TENANT_FOUNDATION`.

The sections below preserve the 2026-08-27 discovery baseline for audit history.
Where they refer to unavailable Docker access or a missing canonical decision,
the 2026-08-28 runtime reconciliation addendum above supersedes them. Their
unresolved row-level findings remain applicable because no legacy schema was
available to audit.

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
