# Local development

## Prerequisites

- Java 21 (the project compiles on newer JDKs, but Spring Modulith/ArchUnit verification is noisy and incomplete on JDK 26).
- Maven 3.9.x. The repository's current `mvnw` and `mvnw.cmd` are shims and `.mvn/wrapper` is absent, so use an installed `mvn` until the wrapper is repaired.
- Node.js 22 and npm 11 (the verified workstation used Node 22.14.0 and npm 11.6.2).
- PostgreSQL is optional. The default `h2` profile uses an in-memory database and needs no separate database process.

## Backend configuration

The backend listens on `http://localhost:8080` with servlet context path `/api`.

For H2 development, set a non-production JWT secret and opt in to a temporary local identity. The bootstrap is disabled by default, is H2-only, and never contains a committed password.

```powershell
$env:JWT_SECRET = "<choose-a-random-secret-of-at-least-32-bytes>"
$env:DEV_IDENTITY_BOOTSTRAP_ENABLED = "true"
$env:DEV_IDENTITY_USERNAME = "<choose-a-local-username>"
$env:DEV_IDENTITY_PASSWORD = "<choose-a-password-of-at-least-12-characters>"
$env:DEV_IDENTITY_EMAIL = "local.operator@example.test"
$env:DEV_SAMPLE_DATA_ENABLED = "true"
mvn spring-boot:run
```

The first H2 startup applies Flyway migrations V1 through V10 and creates the opt-in local user with the `LOCAL_MVP_ADMIN` role and all Phase 1 business permissions. `DEV_SAMPLE_DATA_ENABLED=true` additionally loads a development-only Phase 1 dataset containing customers, locations, fleet categories and types, vehicles and documents, drivers and licences, routes, and trips spanning the main lifecycle statuses. Both bootstraps are disabled by default and H2-only. H2 is in-memory, so the data and user disappear when the backend stops.

For PostgreSQL, create the database using your normal PostgreSQL administration tooling, then start with environment variables (do not commit their values):

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_URL = "jdbc:postgresql://localhost:5432/transport_logistics"
$env:DB_USERNAME = "<database-user>"
$env:DB_PASSWORD = "<database-password>"
$env:JWT_SECRET = "<production-quality-secret>"
mvn spring-boot:run
```

There is no Docker Compose file in this repository. PostgreSQL must therefore be started locally or supplied externally. The H2-only identity bootstrap does not run for PostgreSQL; provision the first production administrator through an approved deployment/identity provisioning process.

## Backend verification

```powershell
mvn clean test
mvn verify
```

- Health: `http://localhost:8080/api/health`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- Login endpoint: `POST http://localhost:8080/api/auth/login`

Do not paste access or refresh tokens into documentation or source control.

## Frontend configuration and startup

The development frontend listens on `http://localhost:5173`. Its default API base is `/api`, and Vite proxies that path to `http://localhost:8080`. This same-origin development setup does not require permissive backend CORS.

```powershell
Set-Location frontend
npm ci
npm run lint
npm test
npm run build
npm run dev
```

Open `http://localhost:5173/login` and use the username/password supplied through the backend bootstrap environment variables. `.env.example` contains:

```text
VITE_API_BASE_URL=/api
```

Use an absolute `VITE_API_BASE_URL` only when the backend has an explicit, narrowly scoped CORS policy for the deployed frontend origin.

ESLint with TypeScript and React Hooks rules, Vitest, and the TypeScript/Vite production build are the configured frontend checks.

## Stopping services

Press `Ctrl+C` in each terminal running Maven or Vite. For a locally managed PostgreSQL instance, stop it using the same service manager or container command used to start it.

## Troubleshooting

- `mvnw` reports that Maven is missing: install Maven 3.9.x and run `mvn`; the wrapper distribution is not committed.
- Modulith logs `Unsupported class file major version 70`: switch `JAVA_HOME` to JDK 21.
- Browser requests return the Vite HTML page: remove generated `vite.config.js`/`.d.ts` files and restart Vite. These files are now ignored; `vite.config.ts` is authoritative.
- Login fails on fresh H2: confirm all three `DEV_IDENTITY_BOOTSTRAP_*` variables are set before backend startup and the password is at least 12 characters.
- Lists are empty on fresh H2: set `DEV_SAMPLE_DATA_ENABLED=true` before starting the backend; restart it after changing this flag.
- Frontend receives 401 after backend restart: the in-memory refresh token store was recreated. Sign in again.
- Port 8080 or 5173 is busy: stop the existing process or explicitly configure a different port and keep the Vite proxy/API URL aligned.
