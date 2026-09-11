# Full-Product 87-Story Completion Roadmap

**Task:** `DEFERRED-BACKLOG-REPRIORITIZATION-001`  
**Planning baseline:** 2026-09-03  
**Register:** exactly `US-01..US-87`  
**Current state:** 73 / 87 accepted; 14 / 87 remaining; Waves A and B are COMPLETE / CLOSED; Wave C is active; US-48 is implementation complete with physical acceptance on external-prerequisite hold; US-49 is COMPLETE / ACCEPTED
**Architecture enabler:** `P1-01` COMPLETE; current Flyway head `V80`
**Mode:** planning only; no story is accepted or implemented by this document

## 1. Executive decision

The exact remaining set is confirmed as:

`US-48, US-50, US-51, US-52, US-53, US-54, US-55, US-72, US-76, US-82, US-84, US-85, US-86, US-87`.

This is 14 stories, so the invariant is `73 + 14 = 87`. `US-88`, `US-89`, and `US-90` are undefined and must not be created. The word *deferred* means scheduled into a governed future wave, not permanently abandoned.

The existing roadmap had correct IDs but non-authoritative labels for several remaining stories. This plan restores the DOCX/UML titles and meanings, notably US-35/37/38 and US-48..55. It does not reopen accepted stories or change their acceptance decisions.

**Waves A and B are COMPLETE / CLOSED; Wave C is active.** US-73 and US-78 are accepted. US-35, US-37, US-38, US-46, US-47, and US-49 final acceptance pass. US-48 is technically complete but physical acceptance is `ON_HOLD_EXTERNAL_PREREQUISITE`; US-50 product decisions are frozen and the next task is `US-50-MONITOR-SPEED-CS01-DOMAIN-PORTS-001`.

## 2. Source reconciliation and non-negotiable boundaries

Reviewed sources:

- `docs/requirements/Mind-Map-Trasportation-and-Logistic.txt`;
- `docs/requirements/Traspotation & logistic.docx` (rendered and visually reviewed, with text/acceptance criteria reconciled);
- all ten UML files from `US-01-US-10` through `US-81-US-90`;
- `MVP_ROADMAP.md`;
- `docs/architecture/P1-01-EVENT-CONTRACT-DURABILITY-AND-ENVELOPE-HARDENING.md`;
- `docs/adr/ADR-database-outbox-for-durable-internal-events.md`;
- accepted evidence and current contracts for US-63, US-66, US-67, US-69, US-74, US-77, and US-83;
- current module/package topology and the central architecture, tenancy, RBAC, integration, Transportation, Finance, and HRM records.

Governing conclusions:

1. Every future tenant-owned command, row, event, cache key, job, query, and integration message must carry trusted Tenant identity and prove Tenant A/B isolation.
2. Cross-module access is only through published ports/APIs or registered events. No foreign repository, entity, physical FK, or direct SQL access is planned.
3. P1-01 is reused only for an approved durable cross-module/external family. It is not a global broker and does not confer exactly-once delivery or global ordering.
4. US-46 is a payroll-link/export capability. Payroll/HRMS owns final salary, taxes, pensions, and payment.
5. US-47 owns transport billing, charges, adjustments, cost centres, validation, and finalization; it is not a full general ledger or payment engine.
6. US-48 includes the provider-neutral ingestion foundation: device identity and vehicle association, location/event model, source time, accuracy/freshness, dedupe, out-of-order handling, Tenant identity, and an explicitly approved durability boundary. No vendor is selected by source.
7. US-78 owns the lifecycle after a domain exception exists; the detecting module continues to own detection and business meaning.
8. US-82 starts with governed read models/KPIs and deterministic forecasting. “Predictive” does not authorize opaque ML or automatic operational mutation.
9. US-84 is jointly satisfied by application behavior, deployment/monitoring controls, and tested runbooks. Application code alone cannot claim replication or disaster-recovery guarantees.

## 3. Status and score legend

- **Status:** `READY_FOR_PRODUCT_DECISIONS`, `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`, `BLOCKED_BY_DEPENDENCY`, `BLOCKED_BY_EXTERNAL_SYSTEM`, `READY_FOR_IMPLEMENTATION_AFTER_DECISIONS`, or `LATER_WAVE`.
- **Priority:** P0 unlocks several remaining stories; P1 is important functional completion; P2 is an advanced capability that follows foundations.
- Score order is **operational value / dependency leverage / security risk / integration complexity / implementation complexity**.
- **Common Definition of Ready (DoR-C):** authoritative source, UML, and acceptance criteria understood; product decisions frozen; prerequisites satisfied; architecture/data owner ratified; database/API/event boundaries known; required external credentials/provider/device available; story-specific test strategy and real-evidence level defined. Each row's Ready cell is its required addendum to DoR-C.
- **Common Definition of Done (DoD-C):** implementation complete; focused unit/application tests pass; PostgreSQL acceptance where persistent; architecture and static analysis pass; RBAC/ABAC and Tenant A/B tests pass; frontend tests/build/lint where applicable; real E2E and real sandbox/device evidence where claimed; independent final acceptance recorded; roadmap updated; central-KB sync attempted.

## 4. Authoritative remaining-story register

| ID | Authoritative title | Expected owning module | Current status | Priority | Wave | Value / leverage / security / integration / implementation | Dependencies | External dependency | Product decisions required | Migration likely | API impact | Frontend impact | Security impact | Architecture risk | Parallelizable with | Blocked by | Definition of Ready | Definition of Done |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| US-35 | Manage Fuel Cards | `fuel` | COMPLETE | P1 | B | H/H/H/H/H | US-31/32/34/36, Fleet, Driver, Organization, Identity, Tenancy, Audit; US-73 outbound capability inspected but not reused | CONTROLLED_PROVIDER_FIXTURE only; named provider remains future | Accepted: limited card reference; local lifecycle; one Vehicle/Driver binding; limits; canonical JSON import; immutable transactions; deterministic reconciliation/review; no payment/fraud engine | V64; V65 canonical Bunker ledger order | frozen operator card/import/reconciliation API implemented | OPERATOR UI implemented | five permissions; Tenant; masked data; importer/reconciler SoD | Fuel owns local control/evidence only; provider owns financial authority; no foreign persistence | US-46 decisions | NONE | Final acceptance PASS after ledger-order and sample-ledger remediations; Maven 1,335/0/0/15; Chromium 6/6; zero post-startup mismatches | COMPLETE; `US-35-FUEL-CARDS-FINAL-ACCEPTANCE-001.md` |
| US-37 | Analyze Fuel Performance | `fuel` (publishes `FuelPerformanceQuery`) | COMPLETE | P1 | B | H/M/M/M/M | US-31/32/33/34/36, Driver, Trip, Fleet, Reporting, Tenant | NONE | Accepted: distance/engine-hour metrics; 7/30/90/custom≤365-day windows; explicit quality; same-vehicle prior-period baseline; compatible peers; deterministic 20% deviation and repeated 30% possible-leakage indicators; non-punitive language; no ML | V63 permission seed only; no analytics table | six read-only `/fuel/performance` query routes | DASHBOARD | `FUEL_PERFORMANCE_VIEW`; Tenant; privacy-controlled driver performance | On-demand bounded Fuel-owned analytics; published bulk contracts only; no foreign SQL/raw mutation/ranking/event/export/US-38 action | NONE | NONE | Final acceptance PASS: Maven 1,310/0/0/15, architecture 46/46, frontend 262/262, Chromium 6/6 with 101 rows, PostgreSQL V1–V63 | COMPLETE; raw Fuel data unchanged; `US-37-FUEL-PERFORMANCE-FINAL-ACCEPTANCE-001.md` |
| US-38 | Handle Fuel Exceptions | `fuel` | COMPLETE | P1 | B | H/M/H/M/H | US-31/32/33/34/35/36/37, US-78 intake, Audit, Notification | NONE for Phase 1; accepted local facts and controlled Fuel Card fixture | Accepted six categories/lifecycle, immutable owner corrections and durable handoff | V66/V67 | Frozen `/api/v1/fuel/exceptions` route set; internal identities non-public | OPERATOR UI under Fuel/AppLayout | five permissions; Tenant; requester/approver and US-35 SoD | Fuel/Operations ownership; P1-01 shared outbox only | US-46/47 decisions | NONE | Final acceptance PASS: focused 21/21, concurrency 9/9, regression 112/112, Maven 1,356/0/0/15, architecture 46/46, Chromium 6/6 | COMPLETE; historical source mutation NO; `US-38-FUEL-EXCEPTIONS-FINAL-ACCEPTANCE-001.md` |
| US-46 | Process Driver Payroll Link | `driver` within Fleet (operational payroll-input owner) | COMPLETE | P1 | B | H/H/H/H/H | Driver, Trip, Audit, Identity, Tenancy, P1-01, US-73; Scheduling only after a separately accepted duty-fact contract | Controlled US-73 `FILE_JSON_V1`; no live HRMS | Four source-backed categories; explicit batch inputs; cutoff; immutable release/correction; provisional net input only | V68–V71 | frozen API/UI/event family accepted | OPERATOR UI under Driver/AppLayout | four permissions; Tenant; preparer/approver SoD; minimized financial/Driver data | Payroll/HRMS owns final salary/tax/pension/payment; Finance owns posting; HRM blueprint remains PROPOSED | US-35, US-37, US-73, P1-01 | NONE | Final acceptance PASS: PostgreSQL 18/18, races 9/9, Maven 1,380/0/0/15, architecture 46/46, Chromium 7/7 | COMPLETE; `US-46-DRIVER-PAYROLL-LINK-FINAL-ACCEPTANCE-001.md` |
| US-47 | Manage Transport Billing | dedicated `billing` bounded context; Finance owns tax invoice/ledger/AR/posting/payment | COMPLETE | P1 | B | H/H/H/H/H | Closed Trip and explicit Freight completed/closed owner facts; Customer/Organization; Audit; US-72 gate; P1-01; US-73 | Controlled `FILE_JSON_V1`; no live accounting system or acknowledgement | Per-source `TransportBillingRecord`; explicit base/surcharge/penalty/credit lines; supplied tax facts; required cost-centre allocation; approval/finalization/reversal; durable idempotency | V72 | exact `/api/v1/billing/records` command/query family | OPERATOR UI under AppLayout | five permissions; preparer/approver SoD; Tenant; minimized financial/tax data | Finance boundary preserved; no foreign persistence, tax engine, ERP, or Customer duplication | NONE | NONE | Final acceptance PASS: focused 17/17, concurrency 9/9, Maven 1,398/0/0/15 in 06:38, architecture 46/46, static/frontend gates, Chromium 7/7, V1→V72 | COMPLETE; `US-47-TRANSPORT-BILLING-FINAL-ACCEPTANCE-001.md` |
| US-48 | Track Vehicles Live | dedicated `tracking` bounded context | IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM | P0 | C | H/H/H/H/H | Fleet vehicle identity, optional Trip assignment, Identity, Tenancy, Audit, P1-01, US-73 configuration reference | Physical GPS device and real provider payload required for final acceptance | Signed HTTP JSON; effective-dated device link; WGS84 source/receipt time; exact accuracy/freshness/trust/dedupe/order/retention rules | V73–V76 | external ingestion + bounded tracking queries; 15-second polling | MINIMAL_FOUNDATION | Three permissions; Tenant; precise location; signed device/provider auth | CS01–CS10 technically complete; pluggable supported-adapter runtime accepted | US-49–55 consume frozen technical foundations without acceptance inheritance | Physical FMC130/live Flespi prerequisites for final acceptance | CS10 PostgreSQL 47/47, Tracking Java 112/112, Chromium 24/24 | External capture ON_HOLD; real provider/device evidence and DoD-C still pending |
| US-49 | Manage Geofences | `tracking` | COMPLETE / ACCEPTED | P1 | C | H/M/H/M/H | US-48 position stream, Organization active-location lookup, Notification, Audit | NONE after telemetry exists | FROZEN: DEPOT/CUSTOMER_SITE/UNAUTHORIZED_ZONE; polygon-only; two-position confirmation; no dwell; independent overlap; unauthorized HIGH | V77 persistence; V78 permission seed; V79 Notification catalogue seed; V80 claim/candidate index hardening | implemented `/api/v1/tracking/geofences` management/query family; `VehicleGeofenceTransitionedV1` publishes durably through P1-01 and resolves V79 Tenant Dispatcher IN_APP notifications idempotently | OPERATOR UI complete | three narrow permissions enforced at HTTP and use-case boundaries; Tenant; minimized location evidence | Published Tenant-aware Organization location lookup only; logical location IDs; no US-63 ownership/persistence reuse | US-50, US-51, US-52 | frozen US-48 trusted WGS84/Vehicle/source-time contracts | Final acceptance PASS: focused 76/76, Maven 1,595/0/0/15, architecture 52/52, Vitest 299/299, Chromium 7/7 | Accepted independently; no US-48 acceptance inheritance |
| US-50 | Monitor Speed | `tracking` | PRODUCT_DECISIONS_FROZEN / READY_FOR_IMPLEMENTATION | P1 | C | H/M/H/M/H | US-48 normalized speed/trust/Vehicle/source time; Trip source-time attribution; Notification; Audit | Real provider/device speed fidelity for final evidence | route-config then Tenant fallback; no legal-limit claim; zero tolerance; two-sample episode; ten-minute repeat | V81 likely | `/api/v1/tracking/speed-monitoring`; `VehicleSpeedingDetectedV1` proposed for CS05 | OPERATOR UI | three narrow permissions; Tenant; minimized Driver/location data | Tracking detects; Trip attributes; Driver owns violation/discipline; Notification owns delivery | US-49, US-51, US-52 | frozen technical US-48 speed contract; no acceptance inheritance | CS01 Domain + Ports next | Final acceptance requires verified physical-source speed or remains externally blocked; missing speed stays UNKNOWN |
| US-51 | Monitor Idle Time | `tracking` | BLOCKED_BY_REQUIRED_TELEMETRY_CAPABILITY | P1 | C | M/M/M/M/M | US-48 engine/movement telemetry, Fuel, Fleet, Reporting | Device must expose engine state or approved proxy | idle threshold; engine-state source; gap handling; fuel-waste estimate and label | YES | query/config API + events | OPERATOR UI | RBAC; Tenant; driver/location data | Missing telemetry must remain unknown, never zero; Fuel consumes published estimate only | US-49, US-50, US-52 | Current FLESPI does not advertise IGNITION and no accepted alternate source exists | Engine signal/proxy, assumptions, gaps and owner contracts must be frozen before implementation | Engine-on/nonmovement duration and qualified fuel estimate pass; unknown telemetry shown; DoD-C |
| US-52 | Monitor Route Deviations | `tracking` | TECHNICAL_DEPENDENCY_SATISFIED / READY_FOR_PRODUCT_DECISIONS | P1 | C | H/H/H/M/H | US-48, Routing planned-route contract, Trip assignment, Audit, Notification, US-80 approvals | NONE after telemetry/route exists | corridor tolerance; severity; planned-route version; approval/override; optional fuel impact | YES | deviation command/query API + events | OPERATOR UI | RBAC/ABAC; Tenant; driver/location; override audit | Routing owns plan; Tracking owns comparison; approval never rewrites route silently | US-49, US-50, US-51 | frozen US-48 trusted-position/source-time and published Routing contracts | Plan/version/tolerance/severity/approval and event contracts frozen | Planned-versus-actual severity and audited approval pass; stale positions excluded; independent DoD-C |
| US-53 | Replay Journeys | `tracking` | TECHNICAL_DEPENDENCY_SATISFIED / READY_AFTER_EARLIER_WAVE_C_DECISIONS | P2 | C | M/M/H/M/H | US-48 retained history; US-49..52 overlays; Trip; Audit | Storage capacity/retention infrastructure | retention; sampling; timezone/source time; stop algorithm; evidence export; legal access | NO beyond US-48 unless decisions require projections | historical query/export API | OPERATOR UI | RBAC/ABAC; Tenant; location history/retention | One immutable history source; overlays reference events rather than recreate them | US-55 after core overlays | frozen US-48 immutable history; producer outputs for optional overlays | History/retention/access/stop/overlay behavior and performance target frozen | Core replay can proceed without physical US-48 acceptance; overlays follow accepted producers |
| US-54 | View Tracking Dashboard | `tracking` | BLOCKED_BY_US49_TO_US53_PRODUCERS | P2 | C | H/L/H/M/M | US-48..53, Reporting conventions, Notification | Map tile/geospatial service if selected | widgets; freshness; heat-map privacy; refresh/load budget; alert aggregation | NO | aggregate read API | DASHBOARD | RBAC/ABAC; Tenant; location aggregation/privacy | Must consume producer contracts; dashboard contains no detector | NONE in same wave once producers stable | US-49..53 producer contracts | Widget/source/freshness/privacy/performance decisions and APIs frozen | Fleet overview/exceptions/alerts/heat maps derive from authoritative data and label stale state; own E2E required |
| US-55 | Handle GPS Edge Cases | `tracking` | BLOCKED_BY_REQUIRED_TELEMETRY_AND_PRODUCT_DECISIONS | P1 | C | H/H/H/H/H | US-48 ingestion/trusted state, US-78 intake, Notification, Audit | Real device/provider fault simulation and unsupported signal sources | loss/tamper/spoof/battery/delayed taxonomy; trust policy; restoration; escalation | YES | exception/status APIs + events | OPERATOR UI | RBAC; Tenant; location/device security; evidence | Edge policy must protect US-48 trusted state and preserve source timestamp | US-49..52 after US-48 | Signal-loss/delay contracts exist; tamper/spoof/battery signals are not established | Required signals and product semantics must be frozen before implementation | Partial technical support cannot satisfy all six fault classes or story readiness |
| US-72 | Enforce Compliance | justified `compliance` decision/evidence context; domains own facts | BLOCKED_BY_DEPENDENCY | P1 | D | H/H/H/H/H | Fleet, Driver, Freight/Hazmat, US-47 billing, US-83 retention, US-75 Audit, Identity, US-80 Workflow | Regulatory/tax policy authority; optional rules feed | jurisdictions; effective dating; domain check catalogue; allow/restrict/block; override/appeal; evidence/retention | YES | compliance decision API + published query ports/events | OPERATOR UI | RBAC/ABAC/SoD; Tenant; driver/medical/cargo/tax data | Avoid one expression-language “god engine”; explicit typed checks consume published facts | US-76 product decisions | US-47 tax facts for full source acceptance; policy authority | Jurisdictions/checks/owners/effects/override/evidence and legal sign-off frozen | Vehicle/driver/cargo/hazmat/tax/regional/retention decisions are auditable and block/flag correctly; DoD-C |
| US-73 | Manage External Integrations | justified `integration` bounded context | COMPLETE | P0 | A | H/H/H/H/H | Identity, Tenancy, Audit, US-75, P1-01; domain-owned mappings | Controlled filesystem sandbox; no vendor | ACCEPTED: governed outbound JSON file; environment-backed credential-reference port; versioned declarative mapping; five-attempt retry; read-only reconciliation | YES | eight operator endpoints; internal durable probe contract; no public inbound API | OPERATOR UI | RBAC/SoD; Tenant; secret/path/payload redaction; sensitive types rejected | Connectivity/reliable exchange only; no business meaning; reuses P1-01, no cloned outbox | US-78 decisions | Final acceptance PASS: V61, focused 24/24, regression 40/40, Maven 1,276/0/0/15 in 05:04, architecture 44/44, Chromium 6/6 | COMPLETE — decision, implementation, technical closure, and hostile final acceptance cover owner, adapter, security, persistence, API/UI, and controlled sandbox | 3/3 source AC and DoD-C PASS; only FILE_EXCHANGE/FILE_JSON_V1/OUTBOUND accepted |
| US-76 | Support Mobile Operations | `offlinesync` for sync state; frontend mobile feature owns client state | BLOCKED_BY_DEPENDENCY | P1 | D | H/M/H/H/H | US-71, US-74, US-57/58, Driver/Trip/Delivery/Rider, Notification/US-77, US-73 | Mobile browser/device, push provider if push is accepted | PWA versus native; roles/workflows; device binding; camera/signature; background limits; push; health signals | UNKNOWN UNTIL DECISIONS | existing domain APIs plus mobile/session/device extensions | MOBILE UI | RBAC/ABAC; Tenant; device auth; offline secrets/PII/POD; session controls | Mobile is an adapter, never a second backend; current responsive SPA alone is insufficient for source acceptance | US-72 implementation | product decision; device/push path | Delivery channel and role matrix, offline/security/device/background/push boundaries and physical-device matrix frozen | Driver/dispatcher/delivery permitted workflows, offline recovery, evidence capture, device/health behavior and physical-device E2E pass; DoD-C |
| US-78 | Manage Operational Exceptions | ratified `operations` hybrid exception-lifecycle context | COMPLETE | P0 | A | H/H/H/M/H | actual accepted Routing US-22 and Delivery US-62 first; future Trip/Cargo/Driver/Fuel/Tracking detectors, US-75 Audit, US-77 Notification, US-80 Workflow, US-81 Scheduling, P1-01 | NONE | ACCEPTED: hybrid aggregate; durable typed intake; seven-category/four-severity triage; role/user assignment; fixed SLA; escalation; action/RCA/closure/reopen; external retention policy | V62 | 17 central lifecycle routes + typed durable intake | OPERATOR UI | seven narrow RBAC permissions; contextual checks; high/critical SoD; Tenant/privacy | Operations owns lifecycle only; domains retain detection/evidence/correction; no foreign access or generic engine | NONE | US-37 product decisions | FINAL ACCEPTANCE PASS — focused 41/41, concurrency 6/6, PostgreSQL 3/3, regressions 84/84, Maven 1,296/0/0/15 in 05:06, architecture 46/46, Chromium 6/6; `US-78-OPERATIONAL-EXCEPTIONS-FINAL-ACCEPTANCE-001.md` | COMPLETE — Routing + Delivery traverse one lifecycle; dedupe/Tenant/SoD/SLA/Notification/history and scope containment pass |
| US-82 | Use Operational Analytics | `reporting` | LATER_WAVE | P2 | E | H/M/H/H/H | accepted operational modules; US-37, US-47, US-54, US-78, US-85; Fleet maintenance facts; P1-01 only for approved feeds | NONE for deterministic baseline; model platform only if later approved | KPI catalogue; freshness; read-model feeds; forecast methods; confidence; recommendation governance; predictive scope | UNKNOWN UNTIL DECISIONS | analytics/query/export APIs | DASHBOARD | RBAC/ABAC; Tenant; cross-domain aggregation; driver/customer/financial data | Read models consume contracts; predictions are labeled and never auto-write domains | US-84, US-87 after data contracts | stable producer contracts and data-quality gates | KPI lineage, deterministic model baseline, confidence/freshness, privacy and performance frozen | Authorized actual KPIs plus clearly labeled forecasts/risk/recommendations pass; no automatic override; DoD-C |
| US-84 | Handle Global System Failures | `system` technical operations | LATER_WAVE | P2 | E | H/H/H/H/H | P1-01, US-71, US-73, US-78, US-85, deployment/monitoring platform | Infrastructure/monitoring and controlled fault environment | supported degraded modes; RTO/RPO claims; lag/backlog/clock signals; operator actions; recovery verification | UNKNOWN UNTIL DECISIONS | health/operations API, mostly internal | OPERATOR UI | privileged RBAC/SoD; Tenant-aware diagnostics; secrets/log privacy | Split code, platform config, and runbooks; never claim multi-region DR without infrastructure proof | US-82, US-87 | monitoring/fault-injection capability and US-73/78 | Failure catalogue, responsibility matrix, observability, degraded-mode safety, RTO/RPO and fault plan frozen | Outage/lag/dependency/backlog/clock scenarios produce truthful incidents and verified recovery; runbooks/fault tests; DoD-C |
| US-85 | Protect Data Integrity | `system` orchestration; each domain owns its validators/corrections | BLOCKED_BY_DEPENDENCY | P1 | E | H/H/H/M/H | all domain owner contracts, US-71, US-48 for GPS/trip mismatch, US-78, US-75 Audit, US-80 Workflow | NONE | invariant catalogue; scan cadence/scope; quarantine; correction ownership; false positives; historical verification | YES | integrity run/findings APIs + owner validation ports | OPERATOR UI | RBAC/SoD; Tenant; broad sensitive-data exposure | System may orchestrate but never scan foreign tables directly or perform generic cross-domain repair | US-84 decisions, US-87 | US-48 for complete mismatch scope; published validators | Owner-by-owner invariant/correction contracts, scan budget, quarantine and permissions frozen | Duplicate/orphan/odometer/GPS-trip/master-data detection, safe quarantine and audited owner correction pass; DoD-C |
| US-86 | Handle Operational Disruptions | `operations` | BLOCKED_BY_DEPENDENCY | P2 | E | H/M/H/H/H | US-78, US-81, Routing, Fleet, Trip, Delivery, US-48/52, Notification, US-72 | Optional weather/border/advisory feed | disruption types/scope/time; constraint ownership; affected-operation projection; replan approval; recovery | YES | disruption command/query API + domain planning ports/events | OPERATOR UI | RBAC/ABAC/SoD; Tenant; location/resource/customer impact | Coordinate through contracts; no cross-module mega-transaction or generic repository | NONE at final wave | US-78 plus tracking/routing/scheduling contracts | Constraint/replan/approval/history/communication owners and deterministic scenario set frozen | Disaster/restriction/strike/border/demand scenarios identify effects, preserve history and safely replan/restore; DoD-C |
| US-87 | Detect User Risk | `identity` | LATER_WAVE | P2 | E | H/M/H/H/H | US-74, US-75, US-80; domain override/audit signals; US-78 investigation; US-82 optional analytics | Identity provider for real MFA/SSO action if claimed | deterministic signals; severity; thresholds; false-positive review; enforcement matrix; privacy/retention; explainability | YES | security admin API + risk-signal/enforcement contracts | OPERATOR UI | highest: identity/session, behavioral data, fraud, MFA/lockout, SoD, Tenant | Start with explainable rules; no opaque ML; missing-field validation remains at domain boundary | US-82 deterministic analytics, US-84 | frozen enforcement/IdP boundary; audited signal catalogue | Signals/actions, review/appeal, retention, safe lockout and IdP test path frozen | Overrides/shared-login/fraud/delay produce explainable events; mandatory fields fail at source; configured reauth/restrict actions and audit pass; DoD-C |

## 5. Wave plan

### Wave A — Integration and exception-control foundations

**Stories:** US-73, US-78.  
**Goal:** establish two narrow platform contracts that unblock reliable external exchange and cross-domain exception lifecycle without absorbing domain logic.  
**Prerequisites:** P1-01, Tenancy, Identity/RBAC, Audit, Workflow, Notification.  
**Parallel work:** US-73 and US-78 product decisions may run in parallel; implementations remain separate.  
**Serial dependencies:** freeze contracts before any downstream story consumes them.  
**Product decisions:** minimum US-73 acceptance adapter; secret/mapping/idempotency model; US-78 aggregate versus read-model decision and typed intake.  
**Architecture risks:** duplicated outbox, generic integration swamp, generic exception god object, foreign repositories.  
**Expected migrations:** both likely.  
**Integration dependencies:** one real sandbox or governed file path for US-73; none for US-78.  
**Test strategy:** unit/application; replay/idempotency; secret redaction/SSRF/webhook security; Tenant A/B PostgreSQL; architecture/static analysis; admin UI; real sandbox/file and browser E2E; failure injection.  
**Exit gate:** both stories independently accepted and published contracts registered; no domain meaning moved into either context.

### Wave B — Fuel control and financial links

**Stories:** US-37, US-35, US-38, US-46, US-47.  
**Goal:** finish the authoritative Fuel family and expose validated transport financial facts without building payroll, ledger, or payment engines.  
**Prerequisites:** Wave A contracts, accepted Fuel/Driver/Trip/Freight/Delivery/Organization facts.  
**Parallel work:** US-37, US-35, US-46, and US-47 decision tracks can run concurrently; US-37 implementation is independent of US-35.  
**Serial dependencies:** US-35 before full US-38; US-47 billing facts before US-72 tax-compliance acceptance.  
**Product decisions:** provider/file boundaries, calculations/cutoffs/corrections, monetary/tax rules, billing ownership.  
**Architecture risks:** cross-module SQL, Finance/HRM scope leakage, historical price mutation, fraud accusations from weak indicators.  
**Expected migrations:** US-35/38/46/47 yes; US-37 depends on read-model decision.  
**Integration dependencies:** fuel card, HRMS/payroll, and accounting endpoints only where selected; deterministic file adapters remain valid minimums if frozen.  
**Test strategy:** domain calculations; import replay; PostgreSQL money/concurrency; SoD/Tenant A/B; contract tests; sandbox/file exchange; operator E2E; full regression.  
**Exit gate:** all five accepted, all source transactions traceable, raw operational records immutable, and external-system ownership explicit.

### Wave C — GPS and telematics

**Stories:** US-48, US-49, US-50, US-51, US-52, US-55, US-53, US-54.  
**Goal:** create one trustworthy, provider-neutral location history and build detectors, forensics, and dashboard projections on it.  
**Prerequisites:** US-73; Fleet/Trip/Routing published identities/contracts; P1-01 durability decision per approved event family.  
**Parallel work:** US-49/50/52 product decisions may proceed against the frozen technical US-48 contract while physical acceptance is on hold; US-53 follows earlier Wave C decisions for overlays; US-54 is last. US-51 and full US-55 remain blocked on required telemetry/product decisions.
**Serial dependencies:** frozen technical US-48 → {US-49,50,52} → US-53 → US-54; US-51/55 rejoin only after their missing signal decisions. Physical US-48 acceptance remains a separate mandatory gate.
**Product decisions:** provider/device/protocol, trust/freshness/order/retention, map/road sources, thresholds and privacy.  
**Architecture risks:** vendor leakage, Tenant/location exposure, false certainty from stale telemetry, unbounded writes, duplicated detection logic.  
**Expected migrations:** US-48..52 and US-55 likely; US-53/54 should reuse source/history unless an approved projection is needed.  
**Integration dependencies:** GPS provider/device; optional road/map service.  
**Test strategy:** deterministic protocol contracts; duplicate/out-of-order/property tests; PostgreSQL volume/index tests; location/RBAC/Tenant security; architecture/static/frontend; load/latency; real device/provider and eight-story Chromium journey.  
**Exit gate:** eight independent acceptances, real-source evidence, stated freshness/retention, and 8/8 tracking suite PASS.

### Wave D — Compliance and field mobility

**Stories:** US-72, US-76.  
**Goal:** enforce explicit cross-domain compliance decisions and deliver secure field workflows through a selected mobile channel.  
**Prerequisites:** Wave B billing facts, US-71/74/75/77/80/81/83, Wave A integration contracts.  
**Parallel work:** product decisions can overlap; implementation may proceed in parallel after shared security contracts freeze.  
**Serial dependencies:** US-72 requires US-47 for complete tax/billing scope; US-76 requires selected device/push/integration boundaries.  
**Product decisions:** jurisdiction/rule catalogue and effects; PWA/native decision; role workflows; device binding; offline data protection; push/background limitations.  
**Architecture risks:** generic rules engine, exposure of medical/compliance data, mobile second backend, platform-specific claims without device evidence.  
**Expected migrations:** US-72 yes; US-76 unknown until decisions.  
**Integration dependencies:** regulatory authority/data where required; real mobile devices; push provider if included.  
**Test strategy:** typed policy rules; cross-domain contract tests; PostgreSQL audit; ABAC/SoD/Tenant A/B; offline conflict/recovery; device security; accessibility; physical-device and Chromium E2E.  
**Exit gate:** both independently accepted with jurisdiction and supported-device limits stated truthfully.

### Wave E — Analytics, integrity, resilience, disruption, and user risk

**Stories:** US-85, US-84, US-87, US-82, US-86.  
**Goal:** use the now-complete operational data/contracts to detect integrity and risk, operate safely through technical failures and real-world disruption, and provide governed analytics.  
**Prerequisites:** Waves A-D; US-48 for GPS/trip integrity; US-78 for incidents; stable operational read contracts.  
**Parallel work:** US-85/84/87 decision tracks may overlap; US-82 read-model design may start after producer contracts freeze.  
**Serial dependencies:** US-85 baseline before recovery certification; US-84 recovery integrates integrity verification; US-86 follows US-78 plus routing/tracking/scheduling; US-82 accepts only after data lineage/quality gates.  
**Product decisions:** invariants/corrections; degraded modes and evidence claims; deterministic risk/analytics methods; disruption authority and replan approvals.  
**Architecture risks:** foreign-table scanners, overclaimed DR, opaque ML, automated punitive action, distributed mega-transactions.  
**Expected migrations:** US-85/86/87 yes; US-84/82 unknown.  
**Integration dependencies:** monitoring/fault environment; IdP for real MFA actions; optional advisory feeds; no ML provider required for baseline.  
**Test strategy:** deterministic unit/property tests; Tenant A/B PostgreSQL; lineage/privacy/security; fault injection and recovery reconciliation; performance; dashboards; real disruption scenarios and full E2E regression.  
**Exit gate:** five independent acceptances and overall 87/87, with every infrastructure claim linked to executable evidence/runbooks.

## 6. Required module contracts

| Consumer | Provider | Required public contract; no implementation access |
|---|---|---|
| Integration | domain owners | Versioned outbound message specifications and inbound mapping ports; domains own business validation |
| Operations exceptions | all detecting domains | Typed, minimized exception facts with logical source ID, Tenant, severity candidate, event identity and time |
| Fuel analytics/exceptions | Fleet, Driver, Trip | Tenant-scoped identity/usage summaries, never repositories or tables |
| Driver payroll link | Trip, Scheduling | Completed work/allowance/overtime/deduction facts; HRMS owns settlement |
| Billing | Trip, Freight, Delivery, Fuel, Organization | Completed/billable facts and customer logical references; Finance owns posting/payment |
| Tracking | Fleet, Trip, Routing, Organization | Vehicle-device eligibility, active assignment, planned route/version, logical site references |
| Compliance | Fleet, Driver, Freight, Billing, Documents | Typed compliance facts and decision request/result contracts |
| Mobile | domain inbound ports, Identity, Offline Sync | Existing authorized commands/queries, device/session policy, idempotent offline operations |
| Reporting/analytics | every contributing domain | Purpose-built Tenant-scoped read contracts or owner-maintained projections |
| Integrity | domain owners | Domain-owned invariant check and corrective-command contracts; no generic foreign-row mutation |
| User risk | Audit/domain signals, Identity | Minimized behavioral/security signals and explicit enforcement port |

Every newly consumed durable contract must use the P1-01 envelope and shared `DurableEventPublisher` only after its producer, consumer, payload, retry, idempotency, privacy, retention, and version policy are approved.

## 7. Per-story rollback strategy

| Story | High-level rollback |
|---|---|
| US-35 | Disable provider/import adapter and card commands; retain imported audit rows; correct schema only through a forward migration. |
| US-37 | Disable analytics feature/query projection and rebuild derived data; never alter source fuel rows. |
| US-38 | Disable automated blocking/escalation while retaining case history; use forward fixes/compensating corrections. |
| US-46 | Disable export destination/batch release; retain immutable batches; send corrective/reversal batch. |
| US-47 | Disable finalization/posting adapter; retain drafts; reverse finalized financial effects rather than delete. |
| US-48 | Isolate provider adapter and freeze last trusted state; retain raw accepted events per policy; forward-fix schema. |
| US-49 | Disable individual geofence/rule or alert publication; preserve transition history. |
| US-50 | Disable faulty speed rule/source adapter; preserve alerts as superseded, not erased. |
| US-51 | Disable estimation/rule and show unknown; recompute derived intervals from trusted telemetry. |
| US-52 | Disable detector/approval action; keep route plan and recorded deviations immutable. |
| US-53 | Disable replay endpoint/export; rebuild derived stops/overlays without removing source points. |
| US-54 | Feature-flag widgets/heat maps and fall back to source lists; no producer mutation. |
| US-55 | Disable suspect detector only with explicit safe fallback to stale/untrusted display; never promote uncertain data. |
| US-72 | Disable a policy version only through authorized replacement; default fail-closed for mandatory controls; forward migration only. |
| US-73 | Disable/isolate endpoint, rotate credentials, pause retries, and preserve message state for reconciliation. |
| US-76 | Disable mobile feature/channel or push adapter; retain server commands and queued data; use compatible client rollback. |
| US-78 | Disable automatic assignment/escalation while retaining intake/manual lifecycle; compensate, never delete cases. |
| US-82 | Feature-flag model/widget, fall back to actual-only KPIs, and rebuild derived projections. |
| US-84 | Exit degraded mode only through verified runbook; isolate failing dependency and reconcile queued work. |
| US-85 | Disable faulty scanner/auto-quarantine, retain findings, and reverse corrections through owner commands. |
| US-86 | Deactivate temporary constraints through audited commands and restore the last approved plan; retain history. |
| US-87 | Disable a risky detector/action independently, preserve events, unlock through audited break-glass policy, and rotate IdP configuration if needed. |

No rollback edits an applied Flyway migration or destructively removes operational history.

## 8. Source-parity dependencies from accepted stories

These are `SOURCE_PARITY_DEPENDENCY` records, not defects in accepted MVP contracts and not permission to reopen those stories.

| Accepted story | Broader source language | Natural future dependency/closure route |
|---|---|---|
| US-63 | dynamic zones, capacity, temporary override, micro-hub | US-86 may consume temporary zone constraints; US-82 may analyze capacity. Micro-hub breadth remains for the final parity audit if no registered story closes it. |
| US-66 | proximity clustering and intelligent grouping | US-48/52 provide trusted position/route facts; US-82 may evaluate clustering efficiency. Accepted batching remains unchanged. |
| US-67 | current position, available traffic, recalculation, customer update | US-48 supplies trusted live position, US-52 route deviation, US-73 provider integration; accepted heuristic ETA remains valid until an approved adapter is selected. |
| US-69 | SMS/app/email, OTP/delay, provider delivery failure | US-76 can provide customer/field app-push channel; US-73 can host provider adapters. OTP remains a separately governed Delivery security capability. |
| US-74 | SSO, MFA, device authentication, session controls, privileged monitoring | US-73 provides enterprise IdP connectivity, US-76 device channel, and US-87 risk-triggered enforcement. Accepted current security is not reduced. |
| US-77 | push, in-app, webhook, broad templates/escalation/quiet hours | US-73 provides webhook/channel adapters and US-76 provides mobile push. Existing accepted rule engine remains owner. |
| US-83 | OCR and external DMS | US-73 provides provider-neutral DMS/OCR connectivity. Document remains owner of version, retention, permissions, and original association. |

## 9. Exact execution queue

Every story follows `PRODUCT DECISIONS → IMPLEMENTATION → TECHNICAL CLOSURE → INDEPENDENT FINAL ACCEPTANCE`. Parallel-safe entries share a group, but acceptance and accounting occur in the listed order.

1. `US-73-EXTERNAL-INTEGRATIONS-FINAL-ACCEPTANCE-001` — COMPLETE; accounting advanced to 66 / 87.
2. `US-78-OPERATIONAL-EXCEPTIONS-FINAL-ACCEPTANCE-001` — COMPLETE; accounting advanced to 67 / 87 and Wave A closed.
3. `US-37-FUEL-PERFORMANCE-FINAL-ACCEPTANCE-001` — COMPLETE; accounting advanced to 68 / 87 and Wave B remains open.
4. `US-35-FUEL-CARDS-FINAL-ACCEPTANCE-001-RERUN-2` — COMPLETE; accounting advanced to 69 / 87 and Wave B remains open.
5. `US-38-FUEL-EXCEPTIONS-FINAL-ACCEPTANCE-001` — COMPLETE; accounting advanced to 70 / 87 and Wave B remains open.
6. US-46 product decisions → implementation → closure → acceptance (may overlap Fuel work once US-73 contract freezes).
7. `US-47-TRANSPORT-BILLING-FINAL-ACCEPTANCE-001` — COMPLETE; accounting advanced to 72 / 87 and Wave B closed.
8. US-48 product/architecture/technical implementation — COMPLETE; real provider/device acceptance is `ON_HOLD_EXTERNAL_PREREQUISITE` and resumes only when external facts change.
9. `US-49-MANAGE-GEOFENCES-FINAL-ACCEPTANCE-001` — COMPLETE; accounting advanced to 73 / 87 independently of US-48 physical acceptance.
10. US-50 product decisions — COMPLETE; next CS01 domain/ports → V81 persistence → evaluation → API/RBAC → Notification → frontend → concurrency/performance → closure → acceptance.
11. US-51 remains `BLOCKED_BY_REQUIRED_TELEMETRY_CAPABILITY`; resume product decisions only when an accepted engine-state source/proxy can be governed.
12. US-52 product decisions → implementation → closure → acceptance (parallel with items 9-10 against frozen technical US-48 contracts).
13. US-55 remains `BLOCKED_BY_REQUIRED_TELEMETRY_AND_PRODUCT_DECISIONS`; resume only after tamper/spoof/battery signal sources and full fault semantics can be governed.
14. US-53 product decisions → implementation → closure → acceptance.
15. US-54 product decisions → implementation → closure → complete tracking-dashboard acceptance.
16. US-72 product/compliance decisions → implementation → closure → acceptance.
17. US-76 mobile-channel decisions → implementation change sets → closure → physical-device acceptance (decision work may overlap item 16).
18. US-85 product/architecture decisions → owner-specific implementation change sets → closure → acceptance.
19. US-84 resilience responsibility/RTO-RPO decisions → application/infrastructure/runbook change sets → closure → fault-injection acceptance.
20. US-87 user-risk/security decisions → implementation change sets → closure → acceptance.
21. US-82 analytics decisions → read-model/KPI/prediction change sets → closure → acceptance.
22. US-86 disruption decisions → coordination change sets → closure → full disruption acceptance.
23. `FULL-SOURCE-PARITY-AUDIT-001` (non-story governance gate).
24. Resolve explicitly approved parity findings without inventing IDs.
25. `FULL-PLATFORM-END-TO-END-ACCEPTANCE-001`.

Broad stories should use small technical change sets such as `US-48-CS01` (device/association), `US-48-CS02` (ingestion/trust), and `US-48-CS03` (live projection/UI), while retaining one story-level final acceptance.

## 10. Exact 65-to-87 accounting progression

Architecture tasks, change sets, parity audits, and platform acceptance do not increment story count.

| Acceptance order | Story accepted | Result |
|---:|---|---:|
| Baseline | — | 65 / 87 |
| 1 | US-73 | 66 / 87 |
| 2 | US-78 | 67 / 87 |
| 3 | US-37 | 68 / 87 |
| 4 | US-35 | 69 / 87 |
| 5 | US-38 | 70 / 87 |
| 6 | US-46 | 71 / 87 |
| 7 | US-47 | 72 / 87 |
| 8 | US-49 | 73 / 87 |
| 9 | US-50 | 74 / 87 |
| 10 | US-48 | 75 / 87 |
| 11 | US-51 | 76 / 87 |
| 12 | US-52 | 77 / 87 |
| 13 | US-55 | 78 / 87 |
| 14 | US-53 | 79 / 87 |
| 15 | US-54 | 80 / 87 |
| 16 | US-72 | 81 / 87 |
| 17 | US-76 | 82 / 87 |
| 18 | US-85 | 83 / 87 |
| 19 | US-84 | 84 / 87 |
| 20 | US-87 | 85 / 87 |
| 21 | US-82 | 86 / 87 |
| 22 | US-86 | 87 / 87 |

## 11. Post-87 governance gates

### `FULL-SOURCE-PARITY-AUDIT-001`

After 87/87, compare the mind map, rendered DOCX, all UML, implementation, migrations, APIs/events, user manuals, and acceptance records. Classify every difference as implemented, accepted narrower contract, defect, duplicate, obsolete, or `OUTSIDE_CURRENT_87_STORY_REGISTER`. The audit cannot silently reopen an accepted story or create a story ID.

Capabilities outside the current register include a full Maintenance Management product beyond US-07 linkage, Workshop operations, maintenance Work Orders, Job Cards, Parts Inventory, and Inspection Management. The requirements narrative explicitly keeps mechanic assignment, parts consumption, work orders, and workshop execution outside US-07. They remain `OUTSIDE_CURRENT_87_STORY_REGISTER` unless separately authorized after the audit.

### `FULL-PLATFORM-END-TO-END-ACCEPTANCE-001`

After 87/87 and disposition of parity findings, execute a real Tenant-isolated operational journey spanning identity/security, master data, fleet/driver, route/trip, freight, fuel, billing/payroll exchange, delivery/last-mile, telematics, mobile/offline, compliance, exceptions, notifications, documents, audit/reporting, resilience, and recovery. Require PostgreSQL/Flyway-current, architecture/static analysis, frontend, real browser/device/provider, performance, fault-recovery, and no cross-Tenant leakage.

## 12. Next executable task

Exactly one queue head is authorized by this roadmap:

`US-50-MONITOR-SPEED-CS01-DOMAIN-PORTS-001`

US-48 external acceptance remains on hold until physical FMC130/live Flespi prerequisites change. US-49 is COMPLETE / ACCEPTED at V80. US-50 product decisions are frozen without US-48 acceptance inheritance, accounting change or V81 creation; begin its framework-neutral domain and ports slice.
