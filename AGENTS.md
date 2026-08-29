# Transport & Logistics Backend - Agent Instructions

## 0. Absolute Mandatory Rule: Read-First and Continuous Sync with Central Knowledge Base

Before generating, modifying, refactoring, deleting, or planning any backend, frontend, database, integration, or architecture change, complete this protocol.

### Step 1: Mandatory Read-First Pre-Flight Check

1. **Never assume or guess:** Inspect the external `central-knowledge-base/` before touching code. Resolve its location from the parent workspace level; for this repository it is maintained outside the project repository.
2. **Scan integration and enterprise rules:** Verify existing contracts, models, ownership, lifecycle status, and rules in:
   - `central-knowledge-base/00_CORE_ARCHITECTURE/` for hexagonal architecture, multi-tenancy, shared entities, RBAC, audit compliance, and sequence generation.
   - `central-knowledge-base/01_INTEGRATION_REGISTRY/` for event contracts, API/port interfaces, cross-module dependencies, state machines, and distributed sagas.
   - `central-knowledge-base/02_MODULES_KNOWLEDGE/<current_and_related_modules>.md` for schemas, Phase 1 active use cases, deferred scope, and published/consumed events.
3. **Complete the pre-flight gate:**
   - [ ] The change respects Domain-First Hexagonal Architecture and current module ownership.
   - [ ] `tenant_id` is propagated and isolated across every new or modified tenant-owned domain model, command/query, DTO, event, table, repository operation, cache key, and background process.
   - [ ] The proposed entity, table, field, enum, event, topic, port, API, or use case does not already exist or conflict with another module.
   - [ ] Existing producers, consumers, upstream/downstream modules, public clients, state machines, and sagas affected by a contract change have been identified.
   - [ ] The requested work belongs to `Phase 1: Current MVP Scope` or has been explicitly approved for promotion from deferred scope.

If a required contract is absent, contradictory, or still only `PROPOSED`, stop the affected implementation and report the decision needed. Do not invent missing governance.

### Step 2: Continuous Knowledge Base Auto-Update Protocol

Whenever code is introduced, modified, refactored, renamed, or deleted, update every affected knowledge-base file in the same execution cycle:

- **Database schemas:** Record complete post-migration table definitions, columns, types, nullability, defaults, constraints, indexes, `tenant_id`, internal foreign keys, and external logical references in `02_MODULES_KNOWLEDGE/<module_name>.md`.
- **Domain/integration events:** Register the topic, owner, version, exact JSON payload, tenant envelope, delivery semantics, ordering, and idempotency in `01_INTEGRATION_REGISTRY/event_contracts.md`; update producer and consumer module documents.
- **State machines and sagas:** Update `01_INTEGRATION_REGISTRY/state_machines.md` and `01_INTEGRATION_REGISTRY/sagas_and_workflows.md` whenever lifecycle or distributed-workflow behavior changes.
- **RBAC and permissions:** Update `00_CORE_ARCHITECTURE/rbac_and_permissions.md` and the owning module document for every new or changed security action.
- **Inbound/outbound ports and REST contracts:** Update exact signatures in `01_INTEGRATION_REGISTRY/api_interfaces.md`, update `01_INTEGRATION_REGISTRY/cross_module_dependency_map.md`, and synchronize provider/consumer module documents.
- **Use cases and business rules:** Update the owning module's Phase 1 inbound-port/use-case catalogue, invariants, authorization, lifecycle effects, and event behavior.
- **Continuous end-user documentation:** Whenever a feature, API, or UI workflow is implemented, modified, renamed, or removed, update the corresponding module guide at `03_USER_MANUALS/<module_name>_guide.md` in the same execution cycle. The guide must describe the current user-visible workflow, prerequisites, permissions, navigation, field behavior, validation, lifecycle actions, expected outcomes, and known limitations without documenting unimplemented scope as available.
- **Verification summary:** Every final report must contain the exact heading `### Synchronized Knowledge Base Files:` and list each updated Markdown file relative to `central-knowledge-base/`. If none changed, state `None` and explain why.

### Step 3: Automated Knowledge Base Git Sync

The external `central-knowledge-base/` is a dedicated Git repository whose canonical remote is `https://github.com/Hasitha3513/central-knowledge-base.git`.

Whenever any task-scoped file is modified or created inside `central-knowledge-base/`, including architecture, integration, module, and `03_USER_MANUALS/` documentation, the agent MUST immediately complete an atomic stage, commit, and normal push from that repository before declaring the task complete. This synchronization is automatic and must not be deferred to a developer or replaced with manual instructions when authenticated Git access is available:

```bash
git -C central-knowledge-base add .
git -C central-knowledge-base commit -m "docs(<module_name>): update schemas, events, and integration contracts"
git -C central-knowledge-base push origin main
```

For a task that adds or updates end-user manuals together with other synchronized system documentation, use the commit message `docs(manuals): add user guide and sync system documentation`. For narrower changes, use an accurate Conventional Commit message that describes the task-scoped documentation.

- Resolve `central-knowledge-base/` from the parent workspace when it is external to the main project; do not assume it is nested inside the application repository.
- Inspect the knowledge-base repository status and diff before staging. Commit only synchronized knowledge-base changes belonging to the current task; never absorb unrelated user changes into the commit.
- Use an accurate Conventional Commit scope and message when the update is narrower than the example.
- If the repository is missing, is not a Git checkout, has an unexpected remote or branch, contains overlapping uncommitted work, rejects the commit, fails to push, or encounters a merge conflict, stop the Git-sync step and report the condition under `Unresolved Risks` in the final report. Do not overwrite, reset, force-push, or silently resolve unrelated changes.

#### Persistent Auto-Sync Reconciliation Rules

- Auto-sync is required when a successful implementation, acceptance, closure, architecture, governance, migration, product decision, or approved deferment materially changes authoritative project state. It is not required for exploratory analysis, failed attempts, local debugging, formatting-only work, or test refactoring with no product or architecture effect.
- Before editing the knowledge base, inspect the actual application branch, HEAD, status, diff, migrations, tests, implementation, closure documents, and roadmap. Verified implementation and accepted decisions take precedence over stale documentation; never invent future schemas, APIs, lifecycle states, permissions, integrations, or completion status.
- Before synchronization, run a safe fetch and inspect the KB branch, HEAD, `origin/main`, worktree, and divergence. If local `main` is behind with no divergence, update only with `git pull --ff-only origin main`. Never reset, clean, rebase, force-push, or silently resolve unrelated changes.
- Classify a dirty KB worktree as current-task, pre-existing related, pre-existing unrelated, or unknown. Preserve all pre-existing work and stage only task-scoped files. Stop with `BLOCKED_KB_WORKTREE_AMBIGUOUS` when ownership cannot be determined safely.
- Prevent duplicate synchronization: first verify whether `origin/main` already contains the required authoritative state or an existing valid local task-scoped commit. If remote state is current, create no commit and report `KB_SYNC_ALREADY_CURRENT`. If a valid local commit is ahead, push it instead of recreating it.
- After committing, push normally to `origin main`. If HTTPS authentication fails, inspect the configured remote, credential helper, and `gh auth status` without exposing or storing credentials. Use an existing secure authenticated mechanism when available; never invent credentials or store plaintext tokens.
- If no authenticated mechanism can push, report `BLOCKED_GOVERNANCE_SYNC_AUTHENTICATION` with the KB commit SHA, branch, remote, divergence, failed command, and minimum required user action. Do not rerun implementation or downgrade an already accepted story: report `PRODUCT_ACCEPTANCE = PASS` separately from `GOVERNANCE_REMOTE_SYNC = BLOCKED_AUTHENTICATION`.
- After a successful push, fetch and require `HEAD == origin/main`, divergence `0 0`, and remote containment of `HEAD` on `origin/main` before reporting `KB_SYNC = COMPLETE`.
- Continue the parent task after successful KB synchronization. Do not create a separate manual-push ceremony or ask the user to rerun acceptance.
- Story accounting must remain arithmetically consistent with the authoritative register `US-01` through `US-87`; do not create `US-88`, `US-89`, or `US-90` without explicit product authority.

### Knowledge Base Repository Push Policy

- The main project codebase preserves the rule: do not commit or push unless explicitly requested.
- The external `central-knowledge-base/` repository is the sole standing exception: task-scoped documentation updates there must be committed and pushed to `origin main` automatically upon completion.
- This exception does not authorize committing application code, modifying unrelated knowledge-base files, force-pushing, bypassing branch protection, or including pre-existing unrelated changes.

### Step 4: Mandatory Traceability & Roadmap Sync (MVP_ROADMAP.md)

Whenever a User Story or Task is implemented, verified, or deferred, the agent MUST update `MVP_ROADMAP.md` in the project root:
- Update the relevant checkbox (`[x]`) and status indicators (`✅` COMPLETE, `🟡` IN PROGRESS / ACCEPTANCE PENDING, `🔴` BLOCKED, `⏸` DEFERRED).
- Update the `Immediate Next Queue (What to build next)` section in the same execution cycle.
- Reconcile progress metrics across the executive dashboard and detailed phase breakdown.
- Ensure `MVP_ROADMAP.md` accurately reflects the authoritative state of repository deliverables.

Code, knowledge-base synchronization, and `MVP_ROADMAP.md` updates form one atomic deliverable. Missing synchronization makes the task incomplete.

## Architecture

This application uses:

- Java 21+
- Spring Boot
- Spring Modulith
- Maven
- PostgreSQL
- Flyway
- Spring Data JPA
- MapStruct
- Jakarta Validation
- OpenAPI / Swagger
- Spring Security with JWT
- JUnit 5
- Mockito
- Testcontainers

Architecture style:

- Modular Monolith
- Domain-First Hexagonal Architecture (Ports and Adapters)
- Domain Driven Design principles

## Module Rules

Business modules include:

- identity
- masterdata
- fleet
- driver
- route
- trip
- notification
- reporting
- shared

Do not create microservices.

Do not introduce direct dependencies between modules unless allowed by the application architecture.

**Cross-Module Communication:**
- Use Spring Application Events for inter-module communication to maintain decoupling.
- Do not inject application services or repositories from one business module into another.
- Module B must never execute SQL queries, JPA joins, entity relationships, or repository calls against tables owned by Module A.
- Direct database joins and physical foreign keys across module boundaries are prohibited. Store cross-module references as UUID primitives documented as logical foreign keys.
- Register every domain or integration event crossing a module boundary in `central-knowledge-base/01_INTEGRATION_REGISTRY/event_contracts.md` and the producer/consumer module documents.

**The `shared` Module:**
- The `shared` module is strictly for cross-cutting technical concerns (e.g., Base Entities, Global Exception Handler, generic utilities).
- NEVER use the `shared` module as a dumping ground for feature-specific code or domain business logic.

[ADDED IN V2] **Utility Functions Extraction:**
- Extract truly common, cross-cutting utility functions (e.g., generic date manipulation, string hashing) to a dedicated `utils` package inside the `shared` module.
- Avoid creating generic "god" utility classes (e.g., `GeneralUtils`). Use cohesive, specific names (e.g., `DateTimeUtils`, `StringSanitizer`).
- Module-specific utility logic must remain inside its own module and must NOT be leaked into the `shared` module.

## Layer Rules

Each business module should follow:

domain/
application/
infrastructure/
web/

### Domain

Must not depend on:

- Spring MVC
- JPA
- Hibernate
- HTTP
- PostgreSQL
- controllers

Domain contains:

- aggregates
- entities
- value objects
- domain services
- business rules
- domain events
- repository ports
- domain exceptions (e.g., business rule violations)

### Application

Contains:

- use cases
- commands
- queries
- application services
- orchestration
- transaction boundaries
- MapStruct mappers (mapping Domain models to Application DTOs if necessary)

Application may depend on Domain.

### Infrastructure

Contains adapters for:

- JPA
- PostgreSQL
- external APIs
- event infrastructure
- technical and infrastructure exceptions

### Web

Contains:

- REST controllers
- request DTOs
- response DTOs
- REST mapping
- MapStruct mappers (mapping Request DTOs to Application Commands/Queries, and Domain models to Response DTOs)

Controllers must not contain business rules.
Domain entities must NEVER leak directly into REST responses. Always use MapStruct to map them to Response DTOs.

[ADDED IN V2] **Web Layer Packaging Rules:**
Do not place Request/Response DTOs or Mappers directly inside the `controllers` package. The `web` layer must follow this strict sub-package structure:
- `web/controllers/` (REST Controllers only)
- `web/dto/request/` (Incoming Request Payloads)
- `web/dto/response/` (Outgoing Response Payloads)
- `web/mappers/` (MapStruct interfaces for Web <-> Application/Domain translation)

## Database

Use PostgreSQL.

All schema changes must use Flyway migrations.

Never rely on Hibernate ddl-auto to create production tables.

Use UUID identifiers unless an existing module explicitly uses another strategy.

## APIs

Base path:

/api/v1

Use:

- GET for reads
- POST for creation and business commands
- PATCH for partial updates
- DELETE for deactivation/deletion where appropriate

Trip lifecycle changes must use explicit command endpoints such as:

POST /trips/{tripId}/submit
POST /trips/{tripId}/approve
POST /trips/{tripId}/assign-vehicle
POST /trips/{tripId}/dispatch

Do not expose generic arbitrary status mutation endpoints.

## Errors

Use the existing global exception handler.

Standard API errors should contain:

- timestamp
- status
- error
- code
- message
- path
- correlationId
- fieldErrors

## Testing

For every implementation:

- add unit tests for domain rules
- add application service tests
- add repository integration tests where appropriate
- add controller/API tests for critical endpoints

Run:

mvn test

and when appropriate:

mvn verify

before considering a task complete.

## Implementation Constraints

Do not:

- restructure unrelated modules
- rename unrelated classes
- introduce microservices
- add libraries without justification
- bypass domain/application layers
- expose JPA entities directly through REST
- place business logic in controllers
- place business logic in repositories

Prefer minimal production-ready changes.

Before changing existing architecture, explain why.

# AI Coding Agent Governance

## AI Coding Agent Operating Rules

### 1. Authority
Existing production code, tests, ADRs, architecture, module boundaries, database migrations, and repository conventions are authoritative. Agents must adapt to the repository. Agents must not redesign the repository unless explicitly instructed.

### 2. Scope
NEVER delete, replace, rewrite, rename, move, or refactor code outside the explicit scope of the current task. Do not modify an unrelated module simply because doing so would make implementation easier.

### 3. Existing Code Protection
Do not remove code, tests, comments, validation, security checks, audit behavior, concurrency controls, or database constraints unless the task explicitly requires it. If code appears unnecessary but its purpose is unclear: DO NOT DELETE IT. Report it.

### 4. Minimal Change Principle
Prefer the smallest implementation that satisfies the requirement. Prefer additive changes over destructive rewrites where both approaches are reasonable. Do not perform opportunistic cleanup.

### 5. Coding Style
Preserve existing formatting, naming, package organization, design patterns, architecture, dependency injection style, error handling, and test conventions. Do not reformat unrelated files. Do not rename existing classes/methods merely because another name seems better.

### 6. Architecture
Respect existing ADRs, Spring Modulith boundaries, Hexagonal Architecture / Ports and Adapters, domain ownership, application boundaries, and persistence boundaries. Do not introduce cross‑module coupling. Do not move domain ownership without explicit architectural approval.

### 7. Database
Existing Flyway migrations are immutable. NEVER modify an already‑applied historical migration. Create a new forward migration only when schema change is explicitly required. Do not rename/drop tables or columns outside explicit scope.

### 8. Tests
Never delete or weaken tests merely to make implementation pass. Never disable failing tests without explicit approval. Never add broad exclusions to hide failures. Fix implementation rather than manipulating verification. Exception: Tests may be replaced only if the task explicitly requires an architectural refactor and a human approves the change.

### 9. Security
Do not remove or weaken authentication, authorization, permission checks, validation, tenant/organization boundaries, audit logging, or sensitive‑data protection without explicit approval.

### 10. Public Contracts
Do not change REST endpoints, HTTP methods, request/response schemas, JSON property names, public interfaces, or domain events unless explicitly required.

### 11. Dependencies
Do not upgrade Java, Spring Boot, React, replace libraries, introduce frameworks, or remove dependencies unless explicitly requested.

### 12. File Modification
Before modifying a file: inspect it; understand its purpose; identify why it must change; confirm it is inside task scope. If completing the task requires files outside declared scope: STOP that part of implementation and report the required expansion.

### 13. Diff Discipline
Before declaring completion: inspect `git status`, `git diff --stat`, and `git diff`. Confirm there are no unrelated changes.

### 14. Required Final Report
Every non‑trivial agent task must report: files changed, created, deleted; tests changed; APIs changed; database changes; architecture changes; dependencies changed; verification commands executed; test results; unresolved risks.

### 15. Human Authority
Agents propose and implement scoped changes. Humans approve architecture and merge decisions. No agent‑generated change is considered approved merely because tests pass.

### 16. Traceability & Roadmap Synchronization
Whenever a User Story or Task is implemented, verified, or deferred, agents MUST update `MVP_ROADMAP.md` in the project root within the same execution cycle:
- Synchronize story checkboxes (`[x]`), status indicators (`✅`, `🟡`, `🔴`, `⏸`), and the `Immediate Next Queue` section.
- Reconcile progress metrics across executive dashboard and phase breakdowns.
- Never declare a feature or release-band task complete without updating `MVP_ROADMAP.md`.

## Mandatory Stop Conditions

The agent MUST stop the affected implementation and report the issue when:

1. A requested change conflicts with an existing ADR.
2. Completing the task requires modifying a module explicitly outside task scope.
3. Completing the task requires changing a public API not mentioned in the task.
4. Completing the task requires rewriting an existing Flyway migration.
5. Completing the task requires deleting an existing test.
6. Completing the task requires weakening authentication or authorization.
7. Completing the task requires breaking an existing module boundary.
8. The expected diff becomes materially larger than the declared task scope.
9. The agent cannot explain the purpose of code it intends to delete.
10. Existing tests contradict the requested behavior and the conflict cannot be resolved from requirements.
11. The repository contains uncommitted user changes that would be overwritten.
12. A destructive operation such as mass delete, rename, move, or replacement becomes necessary.

In these situations:

- DO NOT improvise.
- DO NOT silently choose an architectural direction.
- Report the conflict, affected files/modules, why scope expansion appears necessary, and recommended options.

---

## AI Agent Governance Workflow (Brief)

1. Human defines task and creates a Task Scope Manifest.
2. Agent reads `AGENTS.md`, relevant ADRs, and the manifest.
3. Agent establishes baseline (branch, commit, `git status`).
4. Agent produces an implementation plan and seeks approval if needed.
5. Agent implements only within the declared scope.
6. Agent runs required verification commands.
7. Agent reviews the actual Git diff and ensures compliance.
8. Human reviews diff, runs CI, and approves merge.

---

Before changing existing architecture, explain why.

# Architecture Governance — Feature-First Hexagonal Architecture

## Mandatory Architecture

All new and modified backend Fleet code must follow Feature-First / Domain-First Hexagonal Architecture, Ports and Adapters, and the existing Spring Modulith modular-monolith boundary.

## Fleet Feature Map

Fleet remains the single top-level business module. Its internal business features are:

- `fleet/vehiclemaster`
- `fleet/category`
- `fleet/document`
- `fleet/allocation`
- `fleet/fuellubricant`
- `fleet/runninglog`
- `fleet/maintenance`

Fleet edge cases are policies inside their owning features. Do not implement a separate `edgecases` CRUD package.

## Dependency Direction

The required direction is `domain <- ports/application <- adapters`.

Domain:

- no Spring, JPA, Hibernate, Jackson, web, persistence, or adapter imports;
- only the Java standard library and approved pure Fleet domain primitives.

Ports:

- framework-neutral;
- no persistence implementation or HTTP types;
- depend only on domain or provider-neutral application types.

Application:

- no Spring, JPA, Hibernate, Jackson, or adapter implementation dependencies;
- orchestrates domain behavior through inbound and outbound ports.

Adapters:

- framework dependencies are allowed;
- implement or invoke ports;
- map external representations to and from domain types.

## Persistence Rule

A JPA entity is not a domain entity. JPA models belong only under `adapters/outbound/persistence` and must be mapped explicitly:

`JpaEntity <-> persistence mapper <-> Domain`

Domain and application code must never receive JPA entity types.

## Web Rule

REST controllers, request/response DTOs, validation annotations, and web mappers belong only under `adapters/inbound/web`. Controllers depend on inbound ports and must never access a JPA repository directly.

## Transactions

Do not place Spring transaction annotations inside pure domain or application packages. Define atomicity from use-case needs and use the repository-approved infrastructure transaction adapter/proxy mechanism.

## Events

Domain and application code must not use `ApplicationEventPublisher` directly. Publish provider-neutral events through outbound ports and map them in a Spring event adapter.

## Spring Modulith

Fleet remains one top-level Spring Modulith module. Feature packages are internal slices. Do not expose adapter implementations as public module contracts.

## Database

Never modify historical Flyway migrations. Package-only architecture refactors must not create database migrations.

## API Compatibility

Architecture refactors must preserve existing REST paths, methods, JSON contracts, status/error semantics, authorization, and event behavior unless a change is explicitly approved.

## Refactor Workflow

Before changing a feature:

1. Inspect current classes and behavior.
2. Identify internal and external consumers.
3. Create or update the current-to-target mapping.
4. Add or confirm characterization tests.
5. Extract the pure domain.
6. Define focused inbound and outbound ports.
7. Extract framework-free application services.
8. Move or create the web adapter.
9. Move or create the persistence adapter and mapper.
10. Run focused tests.
11. Run architecture and Spring Modulith tests.
12. Run the full regression suite and inspect the complete diff.

## Git Safety

- preserve pre-existing changes;
- prefer `git mv` for moves;
- never use destructive reset to perform a refactor;
- do not commit or push the main project unless explicitly requested; the only standing exception is the task-scoped `central-knowledge-base/` documentation sync required by Section 0.

## Agent Stop Conditions

Stop instead of guessing when business semantics are unclear, an API contract or migration would need to change, a public module contract would break, status semantics conflict, transaction behavior would change, or tests cannot prove compatibility.

## Fleet Feature-Specific Rules

### Vehicle Master

Fleet Dashboard, Company Fleet, Rental Fleet, QR, Vehicle Status, Available, Allocated, Maintenance, Out of Service, and Retired are use cases or policies—not separate Java packages.

### Fleet Categories

Vehicle Type, Capacity Class, Usage, Ownership Classification, and Special Equipment belong to the cohesive Category feature when supported by the existing model.

### Vehicle Documents

RC, Insurance, Permit, Fitness, Emission, Lease, Expiry, Renewal, missing-document detection, and version history belong to the cohesive Document feature.

### Allocation

Matching, Calendar, Reservation, Conflict, Overbooking, Priority, Replacement, and Approval belong to the cohesive Allocation feature and its policies.

### Fuel/Lubricant

Fuel entry, Lubricant entry, Consumption, Refill, Vendor, and Trends belong to Fleet usage history and must not duplicate the separate Fuel business module.

### Running Log

Kilometres, engine hours, trip usage, idle time, and validation belong to Running Log. Preserve offline Vehicle Reading integration.

### Maintenance

Preventive-maintenance triggers, Breakdown, schedule dependencies, and allocation blocking belong to Fleet Maintenance Linkage, not a speculative full Maintenance ERP module.

# Frontend Architecture Governance

## Feature-First Frontend

Frontend code must be organized by business domain, then feature, then the smallest useful set of `api`, `hooks`, `components`, `pages`, `types`, `validation`, and `utils` folders. Do not create empty feature folders or broad cross-domain feature containers.

## Fleet Feature Map

Fleet frontend features are:

- `features/fleet/vehicleMaster`
- `features/fleet/category`
- `features/fleet/document`
- `features/fleet/allocation`
- `features/fleet/fuelLubricant`
- `features/fleet/runningLog`
- `features/fleet/maintenance`

Fleet edge-case UX belongs to the owning feature. Do not create a `FleetEdgeCasesPage` or an equivalent catch-all feature.

## Global Layout

`AppLayout` is the single owner of the sidebar, top header, breadcrumb, route page title, and global content spacing. Child pages must not duplicate that application chrome. Feature pages may render section headings that do not repeat the current route title.

## Server State

Use TanStack Query for backend server state. Feature-owned query keys and hooks live with the feature. Do not replace an existing query boundary with ad-hoc `useEffect` fetching, and preserve established cache keys during controlled moves.

## Forms

Use React Hook Form with Zod as the client validation source and Ant Design controls as the visual layer. Map backend field errors into React Hook Form. Backend validation remains authoritative. Do not maintain a second independent client validation system.

## API

Feature API clients live within the owning feature and use the shared Axios client. Do not create a giant cross-domain API module. API clients must preserve existing backend paths, methods, payloads, and error contracts during architecture refactors.

## RBAC

Centralized navigation metadata and action visibility may use actual backend permission constants. Hide inaccessible children and hide an empty parent menu. Never invent permissions or treat frontend visibility as authorization; the backend remains authoritative.

## Routes

Preserve current public routes and deep links during architecture refactors. Route changes require explicit approval and compatibility redirects.

## Dependencies

Do not introduce Refine, Ant Design Pro Components, Tailwind CSS, Radix UI, another UI framework, or another state-management framework without explicit architecture approval.

## Test Safety

Do not weaken Playwright selectors or assertions to hide refactor regressions. Prefer accessible locators such as `getByRole`, `getByLabel`, and `getByText`. Preserve current E2E intent, RBAC coverage, offline Vehicle reading behavior, and backend contract verification.

# Enterprise Suite Architecture & Integration Rules

## Multi-Tenancy Rules

- Every new or modified tenant-owned domain entity, database table, DTO, command/query, event, repository operation, cache key, and background process must carry or enforce `tenant_id`.
- Resolve tenant context at the inbound adapter and pass it explicitly into use cases.
- Never execute an unscoped query against tenant-owned data.

## Database Schema Documentation

For every module-owned table, maintain an exact data dictionary in `central-knowledge-base/02_MODULES_KNOWLEDGE/<module_name>.md` using this standard format:

#### Table: `<table_name>`

- **Purpose:** Brief description
- **Primary Key:** `id` (UUID)
- **Multi-Tenant Key:** `tenant_id` (UUID, Indexed)

| Column Name | Data Type | Nullable | Default | Constraints / Logical FK | Description |
| :----------- | :---------- | :------- | :------------------ | :-------------------------- | :---------------------- |
| `id` | UUID | NO | gen_random_uuid() | PRIMARY KEY | Unique ID |
| `tenant_id` | UUID | NO | - | Logical FK -> `tenants(id)` | Tenant Scope Identifier |
| `status` | VARCHAR(50) | NO | 'PENDING' | CHECK (status IN (...)) | Lifecycle State |
| `created_at` | TIMESTAMPTZ | NO | NOW() | - | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NO | NOW() | - | Update timestamp |
| `deleted_at` | TIMESTAMPTZ | YES | NULL | INDEXED with `tenant_id` | Soft-delete timestamp |

Cross-module IDs such as `trip_id`, `vehicle_id`, and `driver_id` must be UUID primitives without physical cross-module database constraints. Direct cross-module SQL, JPA joins, entity relationships, and repositories remain forbidden.

## MVP Scope & Phased Governance

- Module documents must distinguish `Phase 1: Current MVP Scope` from `Phase 2: Post-MVP / Future Roadmap`.
- Implement only Phase 1 scope unless deferred work is explicitly promoted.
- Mark deferred schema fields and advanced capabilities clearly; do not create migrations or production code for them prematurely.
- Record cross-module dependencies as `Active (MVP)` or `Planned (Post-MVP)` in the dependency map.
