# Transport & Logistics Backend - Agent Instructions

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
- Hexagonal Architecture
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
- do not commit or push unless explicitly requested.

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
