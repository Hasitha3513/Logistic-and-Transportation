# Transport & Logistics Spring Modulith (Java 21)

Production-oriented modular-monolith skeleton rebuilt from the uploaded Swagger Codegen server surface.

## Architecture

- Java 21
- Spring Boot 3.2.12
- Spring Modulith 1.2.12
- Hexagonal architecture inside each module
- Spring Web + Jakarta Validation
- Spring Data JPA + PostgreSQL/H2
- MapStruct + Lombok
- Flyway migrations
- Virtual threads enabled

Modules: `identity`, `organization`, `fleet`, `routing`, `trip`, `reporting`, `system`.

The **domain** and **application** packages contain plain Java only. Framework annotations are confined to `infrastructure`. Application services are wired through module `@Configuration` classes. This deliberately resolves the common contradiction between “application service = @Service” and “application layer has no framework dependencies” in favor of strict hexagonal isolation.

## Run

```bash
./mvnw clean test
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

For PostgreSQL:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/transport_logistics
export DB_USERNAME=transport
export DB_PASSWORD=change-me
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

## API surface preserved from upload

Auth: `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/me`  
Users/Roles: `/users/**`, `/roles/**`  
Organization: `/customers/**`, `/departments/**`, `/locations/**`, `/projects/**`  
Fleet: `/drivers/**`, `/vehicles/**`, `/vehicle-categories/**`, `/vehicle-types/**`  
Routing: `/routes/**`  
Trips: `/trips/**` including approval, dispatch, assignment, start/complete/close/cancel transitions  
Reporting: `/dashboard/operations`, `/reports/**`  
System: `/health`

## Important production notes

The uploaded artifact did not contain the original OpenAPI YAML, only generated Java code. Therefore route coverage is reconstructed from the generated API interfaces. DTO schemas are modernized into Java records where practical. Authentication endpoints are contract stubs and intentionally do **not** constitute production JWT security. Replace the development token behavior with your identity provider/JWT implementation before internet exposure. Driver licenses and vehicle documents preserve endpoints but need dedicated persistence tables if they are Phase-1 persisted data.
