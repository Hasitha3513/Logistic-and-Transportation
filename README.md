# Transport & Logistics Spring Modulith

Phase 1 Transport & Logistics Management System implemented as a Java 21 modular monolith with a React/Ant Design operator frontend.

Release boundary: Phase 1 ends at Flyway V10. The current feature branch also contains the separately requested post-Phase-1 US-31 Fuel Issue slice in the `fuel` module at V11; do not use current HEAD as a pure Phase 1 tag target.

## Architecture

- Java 21, Spring Boot 3.2.12, Spring Modulith 1.2.12, Maven
- Hexagonal architecture with plain Java domain/application layers
- Spring Security bearer JWT, BCrypt, rotating hashed refresh tokens, and business permissions
- Spring Data JPA, PostgreSQL/H2, Flyway, MapStruct, and Jakarta Validation
- React, TypeScript, Vite, Ant Design, TanStack Query, React Hook Form, Zod, and Axios

Modules: `identity`, `organization`, `fleet`, `routing`, `trip`, `fuel`, `reporting`, `system`, and `shared`.

The architectural flow is controller → input port → application service → domain → output port → persistence adapter → JPA repository. Cross-module operations use public application/module interfaces; JPA repositories are not shared between modules.

## Implemented Phase 1 scope

- JWT login, current user, refresh rotation/revocation, logout, users, roles, and business permissions
- Vehicle categories/types, vehicle master, persisted documents, and structured availability
- Driver master, persisted licences, and structured availability
- Routes with ordered stops, distance, duration, filtering, and active state
- Trip creation/editing, route/resource assignment, strict lifecycle commands, dispatch revalidation, and append-only history
- Permission-aware React/Ant Design dashboard and operator management screens
- Basic reporting endpoints; operational metric aggregation remains limited

Trip logs, running logs, persisted maintenance records, and advanced reporting are documented limitations.

## Backend

The application listens on port 8080 with servlet context path `/api`.

```bash
mvn clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

If the repository wrapper shim reports that Maven is unavailable, use Maven 3.9.x installed on the workstation.

PostgreSQL requires external secrets:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/transport_logistics
export DB_USERNAME=transport_app
export DB_PASSWORD=change-me
export JWT_SECRET=replace-with-production-secret
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

No PostgreSQL credentials have committed defaults. Flyway owns the schema; V1-V10 cover Phase 1 and V11 adds the Phase 2 US-31 Fuel Issue schema, permissions, audit history, and indexes.

## Frontend

```bash
cd frontend
npm ci
npm run lint
npm run test
npm run build
npm run dev
```

The development frontend uses `/api`, proxied by Vite to `http://localhost:8080`.

## Docker Compose

The Compose stack runs PostgreSQL, the backend, and the production-built frontend behind an Nginx `/api` reverse proxy.

```powershell
Copy-Item .env.docker.example .env
# Edit .env and fill POSTGRES_PASSWORD, JWT_SECRET, and APP_ADMIN_PASSWORD.
docker compose up --build -d
docker compose ps
```

Open `http://localhost:5173` and sign in with `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` from `.env`. The administrator bootstrap is opt-in and restricted to the explicit `docker` profile. Flyway creates the PostgreSQL schema automatically; the database is persisted in the `postgres-data` volume.

```powershell
docker compose logs -f backend frontend
docker compose down
```

`docker compose down` preserves PostgreSQL data. Use `docker compose down --volumes` only when you intentionally want to delete the local database and start again from an empty schema.

## URLs

- Backend: `http://localhost:8080`
- API base: `http://localhost:8080/api`
- Health: `http://localhost:8080/api/health`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- Frontend: `http://localhost:5173`

## API areas

- Authentication: `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/me`
- Identity: `/users/**`, `/roles/**`
- Organization: `/customers/**`, `/departments/**`, `/locations/**`, `/projects/**`
- Fleet: `/drivers/**`, `/vehicles/**`, `/vehicle-categories/**`, `/vehicle-types/**`
- Routing: `/routes/**`
- Trips: `/trips/**`, including submit, approve/reject, route/resource assignment, dispatch, start, complete, close, cancel, and history
- Reporting: `/dashboard/operations`, `/reports/**`
- System: `/health`

The detailed frozen Phase 1 endpoint inventory is in `docs/phase-1-api-contract.md`.

## Production notes

- Supply PostgreSQL and JWT secrets through the deployment secret store.
- Local identity/sample-data bootstraps are H2-only, disabled by default, and require explicit environment variables.
- Use Java 21 for release verification. Newer JDKs may produce ArchUnit compatibility warnings.
- A production release requires a clean PostgreSQL V1-V10 migration and PostgreSQL concurrency evidence. See `docs/phase-1-release-readiness.md`.
- See `docs/local-development.md` for opt-in local administrator and sample-data setup. No password is committed.
