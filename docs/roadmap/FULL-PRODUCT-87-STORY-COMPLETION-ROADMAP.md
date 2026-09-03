# Full-Product 87-Story Completion Roadmap

**Task:** `DEFERRED-BACKLOG-REPRIORITIZATION-001`  
**Planning baseline:** 2026-09-03  
**Register:** exactly `US-01..US-87`  
**Current state:** 66 / 87 accepted; 21 / 87 remaining; US-78 implementation complete and acceptance pending
**Architecture enabler:** `P1-01` COMPLETE; Flyway head `V61`
**Mode:** planning only; no story is accepted or implemented by this document

## 1. Executive decision

The exact remaining set is confirmed as:

`US-35, US-37, US-38, US-46, US-47, US-48, US-49, US-50, US-51, US-52, US-53, US-54, US-55, US-72, US-76, US-78, US-82, US-84, US-85, US-86, US-87`.

This is 21 stories, so the invariant is `66 + 21 = 87`. `US-88`, `US-89`, and `US-90` are undefined and must not be created. The word *deferred* means scheduled into a governed future wave, not permanently abandoned.

The existing roadmap had correct IDs but non-authoritative labels for several remaining stories. This plan restores the DOCX/UML titles and meanings, notably US-35/37/38 and US-48..55. It does not reopen accepted stories or change their acceptance decisions.

**US-73 — Manage External Integrations** is independently accepted and COMPLETE. Its frozen boundary provides one governed outbound JSON-file adapter with controlled-sandbox evidence and provider-neutral reliability without duplicating the P1-01 outbox. Acceptance does not claim that payroll, billing/accounting, fuel-card, telematics, DMS/OCR, push/webhook, or other named ecosystems are connected. US-78 implementation is technically complete; the current Wave A focus is its independent technical closure and final acceptance.

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

- **Status:** `READY_FOR_PRODUCT_DECISIONS`, `BLOCKED_BY_DEPENDENCY`, `BLOCKED_BY_EXTERNAL_SYSTEM`, `READY_FOR_IMPLEMENTATION_AFTER_DECISIONS`, or `LATER_WAVE`.
- **Priority:** P0 unlocks several remaining stories; P1 is important functional completion; P2 is an advanced capability that follows foundations.
- Score order is **operational value / dependency leverage / security risk / integration complexity / implementation complexity**.
- **Common Definition of Ready (DoR-C):** authoritative source, UML, and acceptance criteria understood; product decisions frozen; prerequisites satisfied; architecture/data owner ratified; database/API/event boundaries known; required external credentials/provider/device available; story-specific test strategy and real-evidence level defined. Each row's Ready cell is its required addendum to DoR-C.
- **Common Definition of Done (DoD-C):** implementation complete; focused unit/application tests pass; PostgreSQL acceptance where persistent; architecture and static analysis pass; RBAC/ABAC and Tenant A/B tests pass; frontend tests/build/lint where applicable; real E2E and real sandbox/device evidence where claimed; independent final acceptance recorded; roadmap updated; central-KB sync attempted.

## 4. Authoritative remaining-story register

| ID | Authoritative title | Expected owning module | Current status | Priority | Wave | Value / leverage / security / integration / implementation | Dependencies | External dependency | Product decisions required | Migration likely | API impact | Frontend impact | Security impact | Architecture risk | Parallelizable with | Blocked by | Definition of Ready | Definition of Done |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| US-35 | Manage Fuel Cards | `fuel` | READY_FOR_PRODUCT_DECISIONS | P1 | B | H/H/H/H/H | US-31/32/34, Fleet, Driver, Organization, Identity, Tenancy, Audit, P1-01; US-73 for real provider/import | Fuel-card provider or governed file feed | card lifecycle; limits; vehicle/driver binding; import/reconciliation; fraud rule; provider versus file acceptance | YES | new operator API + external import adapter | OPERATOR UI | RBAC; Tenant; financial/card data; credentials; fraud evidence | Do not turn cards into bunker inventory or a payment engine | US-37, US-46 | provider acceptance path; US-73 only for live exchange | Card/provider/file boundary, reconciliation/idempotency, owner, schema/API, permissions and provider test path frozen | Issue/restrict/import/reconcile/misuse AC pass; replay-safe provider/file evidence; DoD-C |
| US-37 | Analyze Fuel Performance | `fuel` (publishes reporting projection) | READY_FOR_IMPLEMENTATION_AFTER_DECISIONS | P1 | B | H/M/M/M/M | US-31/32/33/34/36, Driver, Trip, Fleet, Reporting, Tenant | NONE for deterministic baseline | metric windows; missing-data policy; thresholds; leakage wording; read-model freshness | UNKNOWN UNTIL DECISIONS | new analytics query API | DASHBOARD | RBAC; Tenant; driver performance data | Analytics must not query foreign tables or alter raw fuel facts | US-35, US-46 | decisions only; not US-35 | Metrics and owner contracts frozen; source completeness/data-quality fixtures available | Vehicle/driver comparisons and non-accusatory anomaly flags pass; raw data unchanged; DoD-C |
| US-38 | Handle Fuel Exceptions | `fuel` | BLOCKED_BY_DEPENDENCY | P1 | B | H/M/H/M/H | US-31/32/33/34/35/36, US-78 workflow contract, Audit, Notification | Fuel-card feed for card-misuse case | exception taxonomy/lifecycle; correction approval; stock/cost effects; evidence/retention; US-78 handoff | YES | new command/query APIs | OPERATOR UI | RBAC/SoD; Tenant; financial/fraud evidence | Partial delivery before US-35 would omit required card misuse and violate source completeness | US-47 after US-35 contract | US-35; US-78 public intake contract | All six cases and correction/approval boundaries frozen; card-misuse facts available | Theft/wrong reading/price/emergency/card/negative-balance paths pass without corrupting history; DoD-C |
| US-46 | Process Driver Payroll Link | `driver` (operational payroll-input owner) | BLOCKED_BY_EXTERNAL_SYSTEM | P1 | B | H/H/H/H/H | Driver, Trip, Scheduling/US-81, Audit, Identity, Tenancy, US-73 | Payroll/HRMS sandbox or signed file exchange | earning/allowance/overtime/deduction rules; cutoff; corrections; export format; settlement-authorization boundary | YES | operator API + external export API/file | OPERATOR UI | RBAC/SoD; Tenant; driver PII; financial data; credentials | Never implement salary/tax/payment; HRM blueprint remains PROPOSED | US-35, US-37 | frozen HRMS/file contract and sample | Rules, ownership, mapping, reconciliation, permission, and sandbox/file evidence frozen | Calculations trace to driver/trip; replay/correction safe; final settlement finance-authorized; real exchange evidence; DoD-C |
| US-47 | Manage Transport Billing | justified new `billing` bounded context; Finance owns ledger/payment | READY_FOR_PRODUCT_DECISIONS | P1 | B | H/H/H/H/H | Trip, Freight, Delivery, Customer/Organization, Fuel, Audit, US-73; US-72 for tax/compliance gate | Accounting/ERP optional for acceptance, required before claiming live posting | billable event; currency/tax; surcharge/penalty; cost centre; invoice versus bill-finalization boundary; reversals; Finance handoff | YES | new billing APIs + external accounting port | OPERATOR UI | RBAC/SoD; Tenant; customer/financial/tax data; credentials | New context is justified by independent financial lifecycle; ARB must ratify it and forbid foreign-table reads | US-38, US-46 | product/ownership decision; external posting if included | Billing-versus-ledger boundary, monetary rules, ownership, event/API, permissions and acceptance mode frozen | Cost/surcharge/penalty/cost-centre/finalization/audit pass; no operational aggregate ownership leak; DoD-C |
| US-48 | Track Vehicles Live | justified new `tracking` bounded context | READY_FOR_PRODUCT_DECISIONS | P0 | C | H/H/H/H/H | Fleet vehicle identity, Trip, Routing, Organization, Identity, Tenancy, Audit, P1-01, US-73 | GPS provider/device for real acceptance | protocol(s); device registry/link; source timestamps; accuracy/freshness; dedupe/order; retention; live definition; durable-ingestion need | YES | external ingestion + tracking query/stream API | DASHBOARD | RBAC/ABAC; Tenant; precise location; device credentials | High-volume stream and location lifecycle justify a distinct owner; no vendor types in ports | US-55 decisions; provider contract work | US-73 minimum adapter governance; device/provider availability for real acceptance | Provider-neutral contract, owner, retention, stale/order rules, load target, credentials and real-device/sandbox plan frozen | Current/last-known/connectivity/accuracy/staleness pass under duplicates/out-of-order events; real provider/device evidence; DoD-C |
| US-49 | Manage Geofences | `tracking` | BLOCKED_BY_DEPENDENCY | P1 | C | H/M/H/M/H | US-48 position stream, Organization depot/customer-site references, Notification, Audit | NONE after telemetry exists | geometry/types; entry/exit hysteresis; dwell; unauthorized policy; alert recipients | YES | new geofence API + internal event contract | OPERATOR UI | RBAC; Tenant; location/zone sensitivity | Use logical site IDs; do not duplicate US-63 delivery-zone ownership | US-50, US-51, US-52 | accepted US-48 stream | Geometry and transition semantics frozen; authoritative site/location contracts available | Valid entry/exit and unauthorized events, invalid polygon rejection, Tenant/device isolation and real-stream E2E pass; DoD-C |
| US-50 | Monitor Speed | `tracking` | BLOCKED_BY_DEPENDENCY | P1 | C | H/M/H/M/H | US-48, Routing road-rule facts, Driver, Notification, Audit | Road-rule source if road-specific acceptance is selected | threshold precedence; road-rule source; tolerance; repeats; driver attribution | YES | rule/config API + alert queries/events | OPERATOR UI | RBAC; Tenant; driver/location data; disciplinary sensitivity | Tracking detects; Driver owns violation/discipline | US-49, US-51, US-52 | US-48 and road-rule decision | Threshold/source/repeat semantics and Driver contract frozen | Threshold and repeat alerts identify available vehicle/time/location; no automatic discipline; DoD-C |
| US-51 | Monitor Idle Time | `tracking` | BLOCKED_BY_DEPENDENCY | P1 | C | M/M/M/M/M | US-48 engine/movement telemetry, Fuel, Fleet, Reporting | Device must expose engine state or approved proxy | idle threshold; engine-state source; gap handling; fuel-waste estimate and label | YES | query/config API + events | OPERATOR UI | RBAC; Tenant; driver/location data | Missing telemetry must remain unknown, never zero; Fuel consumes published estimate only | US-49, US-50, US-52 | US-48 engine-state capability | Engine signal/proxy, assumptions, gaps and owner contracts frozen | Engine-on/nonmovement duration and qualified fuel estimate pass; unknown telemetry shown; DoD-C |
| US-52 | Monitor Route Deviations | `tracking` | BLOCKED_BY_DEPENDENCY | P1 | C | H/H/H/M/H | US-48, Routing planned-route contract, Trip assignment, Audit, Notification, US-80 approvals | NONE after telemetry/route exists | corridor tolerance; severity; planned-route version; approval/override; optional fuel impact | YES | deviation command/query API + events | OPERATOR UI | RBAC/ABAC; Tenant; driver/location; override audit | Routing owns plan; Tracking owns comparison; approval never rewrites route silently | US-49, US-50, US-51 | US-48 and published Routing contract | Plan/version/tolerance/severity/approval and event contracts frozen | Planned-versus-actual severity and audited approval pass; stale positions excluded; DoD-C |
| US-53 | Replay Journeys | `tracking` | BLOCKED_BY_DEPENDENCY | P2 | C | M/M/H/M/H | US-48 retained history; US-49..52 overlays; Trip; Audit | Storage capacity/retention infrastructure | retention; sampling; timezone/source time; stop algorithm; evidence export; legal access | NO beyond US-48 unless decisions require projections | historical query/export API | OPERATOR UI | RBAC/ABAC; Tenant; location history/retention | One immutable history source; overlays reference events rather than recreate them | US-55 after core overlays | US-48 retention; overlays for complete source experience | History/retention/access/stop/overlay behavior and performance target frozen | Replay/stops/incidents and retention/access pass at representative volume; DoD-C |
| US-54 | View Tracking Dashboard | `tracking` | BLOCKED_BY_DEPENDENCY | P2 | C | H/L/H/M/M | US-48..53, Reporting conventions, Notification | Map tile/geospatial service if selected | widgets; freshness; heat-map privacy; refresh/load budget; alert aggregation | NO | aggregate read API | DASHBOARD | RBAC/ABAC; Tenant; location aggregation/privacy | Must consume producer contracts; dashboard contains no detector | NONE in same wave once producers stable | US-48..53 accepted contracts | Widget/source/freshness/privacy/performance decisions and APIs frozen | Fleet overview/exceptions/alerts/heat maps derive from authoritative data and label stale state; real E2E; DoD-C |
| US-55 | Handle GPS Edge Cases | `tracking` | BLOCKED_BY_DEPENDENCY | P1 | C | H/H/H/H/H | US-48 ingestion/trusted state, US-78 intake, Notification, Audit | Real device/provider fault simulation | loss/tamper/spoof/battery/delayed taxonomy; trust policy; restoration; escalation | YES | exception/status APIs + events | OPERATOR UI | RBAC; Tenant; location/device security; evidence | Edge policy must protect US-48 trusted state and preserve source timestamp | US-49..52 after US-48 | US-48; US-78 public intake for central lifecycle | Trust taxonomy, stale/restoration rules, simulations, and exception handoff frozen | All six fault classes, delayed source time, trusted-state protection and recovery pass with provider/device evidence; DoD-C |
| US-72 | Enforce Compliance | justified `compliance` decision/evidence context; domains own facts | BLOCKED_BY_DEPENDENCY | P1 | D | H/H/H/H/H | Fleet, Driver, Freight/Hazmat, US-47 billing, US-83 retention, US-75 Audit, Identity, US-80 Workflow | Regulatory/tax policy authority; optional rules feed | jurisdictions; effective dating; domain check catalogue; allow/restrict/block; override/appeal; evidence/retention | YES | compliance decision API + published query ports/events | OPERATOR UI | RBAC/ABAC/SoD; Tenant; driver/medical/cargo/tax data | Avoid one expression-language “god engine”; explicit typed checks consume published facts | US-76 product decisions | US-47 tax facts for full source acceptance; policy authority | Jurisdictions/checks/owners/effects/override/evidence and legal sign-off frozen | Vehicle/driver/cargo/hazmat/tax/regional/retention decisions are auditable and block/flag correctly; DoD-C |
| US-73 | Manage External Integrations | justified `integration` bounded context | COMPLETE | P0 | A | H/H/H/H/H | Identity, Tenancy, Audit, US-75, P1-01; domain-owned mappings | Controlled filesystem sandbox; no vendor | ACCEPTED: governed outbound JSON file; environment-backed credential-reference port; versioned declarative mapping; five-attempt retry; read-only reconciliation | YES | eight operator endpoints; internal durable probe contract; no public inbound API | OPERATOR UI | RBAC/SoD; Tenant; secret/path/payload redaction; sensitive types rejected | Connectivity/reliable exchange only; no business meaning; reuses P1-01, no cloned outbox | US-78 decisions | Final acceptance PASS: V61, focused 24/24, regression 40/40, Maven 1,276/0/0/15 in 05:04, architecture 44/44, Chromium 6/6 | COMPLETE — decision, implementation, technical closure, and hostile final acceptance cover owner, adapter, security, persistence, API/UI, and controlled sandbox | 3/3 source AC and DoD-C PASS; only FILE_EXCHANGE/FILE_JSON_V1/OUTBOUND accepted |
| US-76 | Support Mobile Operations | `offlinesync` for sync state; frontend mobile feature owns client state | BLOCKED_BY_DEPENDENCY | P1 | D | H/M/H/H/H | US-71, US-74, US-57/58, Driver/Trip/Delivery/Rider, Notification/US-77, US-73 | Mobile browser/device, push provider if push is accepted | PWA versus native; roles/workflows; device binding; camera/signature; background limits; push; health signals | UNKNOWN UNTIL DECISIONS | existing domain APIs plus mobile/session/device extensions | MOBILE UI | RBAC/ABAC; Tenant; device auth; offline secrets/PII/POD; session controls | Mobile is an adapter, never a second backend; current responsive SPA alone is insufficient for source acceptance | US-72 implementation | product decision; device/push path | Delivery channel and role matrix, offline/security/device/background/push boundaries and physical-device matrix frozen | Driver/dispatcher/delivery permitted workflows, offline recovery, evidence capture, device/health behavior and physical-device E2E pass; DoD-C |
| US-78 | Manage Operational Exceptions | ratified `operations` hybrid exception-lifecycle context | IMPLEMENTATION_COMPLETE / ACCEPTANCE_PENDING | P0 | A | H/H/H/M/H | actual accepted Routing US-22 and Delivery US-62 first; future Trip/Cargo/Driver/Fuel/Tracking detectors, US-75 Audit, US-77 Notification, US-80 Workflow, US-81 Scheduling, P1-01 | NONE | IMPLEMENTED: hybrid aggregate; durable typed intake; seven-category/four-severity triage; role/user assignment; fixed SLA; escalation; action/RCA/closure/reopen; external retention policy | V62 | 17 central lifecycle routes + typed durable intake | OPERATOR UI | seven narrow RBAC permissions; contextual checks; high/critical SoD; Tenant/privacy | Operations owns lifecycle only; domains retain detection/evidence/correction; no foreign access or generic engine | NONE | independent technical closure and final acceptance | IMPLEMENTATION PASS — Maven 1,296/0/0/15, architecture 49/49, Chromium 6/6; `US-78-OPERATIONAL-EXCEPTIONS-IMPLEMENTATION-001.md` | Routing + Delivery traverse one lifecycle; dedupe/Tenant/SoD/SLA/Notification/history implementation evidence passes; independent acceptance pending |
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
**Parallel work:** after US-48, US-49/50/51/52 and US-55 can proceed in parallel; US-53 follows retained history and overlays; US-54 is last.  
**Serial dependencies:** US-48 → {US-49,50,51,52,55} → US-53 → US-54.  
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
2. `US-78-OPERATIONAL-EXCEPTIONS-PRODUCT-DECISIONS-001` — COMPLETE; next: implementation → closure → acceptance.
3. US-37 product decisions → implementation → closure → acceptance.
4. US-35 product decisions → implementation → closure → acceptance (may overlap item 3).
5. US-38 product decisions → implementation → closure → acceptance (implementation waits for US-35 and US-78 contracts).
6. US-46 product decisions → implementation → closure → acceptance (may overlap Fuel work once US-73 contract freezes).
7. US-47 product/ARB decisions → implementation → closure → acceptance.
8. US-48 product/architecture decisions → implementation → closure → real provider/device acceptance.
9. US-49 product decisions → implementation → closure → acceptance.
10. US-50 product decisions → implementation → closure → acceptance (parallel with item 9 after US-48).
11. US-51 product decisions → implementation → closure → acceptance (parallel with items 9-10 after US-48).
12. US-52 product decisions → implementation → closure → acceptance (parallel with items 9-11 after US-48).
13. US-55 product decisions → implementation → closure → fault-simulated/provider acceptance (parallel after US-48).
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
| 8 | US-48 | 73 / 87 |
| 9 | US-49 | 74 / 87 |
| 10 | US-50 | 75 / 87 |
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

`US-78-OPERATIONAL-EXCEPTIONS-IMPLEMENTATION-001`

It must implement the frozen hybrid Operations lifecycle with Routing and Delivery intake, without taking detector ownership or inventing cross-module repository access.
