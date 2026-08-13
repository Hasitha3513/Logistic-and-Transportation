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