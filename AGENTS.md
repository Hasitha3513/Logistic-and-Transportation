
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

Do not introduce direct dependencies between modules unless allowed by
the application architecture.

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

### Application

Contains:

- use cases
- commands
- queries
- application services
- orchestration
- transaction boundaries

Application may depend on Domain.

### Infrastructure

Contains adapters for:

- JPA
- PostgreSQL
- external APIs
- event infrastructure

### Web

Contains:

- REST controllers
- request DTOs
- response DTOs
- REST mapping

Controllers must not contain business rules.

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

Never delete or weaken tests merely to make implementation pass. Never disable failing tests without explicit approval. Never add broad exclusions to hide failures. Fix implementation rather than manipulating verification.

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
