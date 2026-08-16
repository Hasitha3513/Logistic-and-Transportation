# PostgreSQL Integration Verification

## Environment

- Java: 21.0.7
- Database image: `postgres:16.4-alpine`
- Local verification engine: Docker Desktop 29.7.2, API 1.55
- Test framework: JUnit 5, Spring Boot Test, and Testcontainers 2.0.3
- CI prerequisite: a Docker-compatible container runtime. No external PostgreSQL service or fixed localhost port is required.

On Windows Docker Desktop installations whose active engine is not exposed through the default pipe, Maven can be run with `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine`. Linux CI agents should use their normal Docker socket.

## Testcontainers Setup

`PostgreSqlIntegrationTest` is the reusable base for production-like database tests. It starts PostgreSQL dynamically and supplies the JDBC URL, username, password, driver, Flyway, and Hibernate validation properties through `@DynamicPropertySource`. Credentials and mapped ports are not hard-coded.

The PostgreSQL suite is tagged `postgres` but is not excluded from the default Maven lifecycle. Its class name matches Surefire's normal test discovery, so `mvn clean verify` executes it together with the existing H2 suite.

Testcontainers 2.0.3 and its renamed `testcontainers-postgresql` artifact are used because the Spring Boot-managed older Testcontainers client was rejected by the current Docker 29 API. Existing H2 tests remain unchanged and continue to provide fast application-level coverage.

## Flyway Result

An empty PostgreSQL database was migrated successfully from `V1` through `V14`. The verification asserts that all 14 migrations are applied, successful, and at version 14. V14 adds the Fleet-owned vehicle-reading foundation and its PostgreSQL partial source-identity index.

Flyway 9.22.3 emits a compatibility warning because PostgreSQL 16 is newer than the highest PostgreSQL version tested by that Flyway release. Migration and validation succeed, but upgrading Flyway should be handled as a separate dependency-compatibility change.

## JPA Validation

The PostgreSQL Spring context starts with `spring.jpa.hibernate.ddl-auto=validate`. Hibernate validates the Flyway-created schema instead of creating or updating it. This proves the current entity mappings and migrated PostgreSQL schema are compatible at startup.

The current entities do not use `@Version`, so optimistic locking is not present. Concurrency-sensitive workflows rely on explicit pessimistic row locks and transactional application services.

## Vehicle Assignment Concurrency

Two real transactions attempt to assign the same vehicle to overlapping trips at the same time. The vehicle row is locked pessimistically, exactly one assignment succeeds, the other reports a conflict, and only one overlapping allocation remains.

Overlap protection is application-enforced while holding the resource lock. PostgreSQL does not currently have a range exclusion constraint that independently rejects overlapping vehicle allocations.

## Driver Assignment Concurrency

Two real transactions attempt to assign the same driver to overlapping trips. The driver row is locked pessimistically, exactly one assignment succeeds, the other reports a conflict, and only one overlapping assignment remains.

As with vehicles, overlap protection is application-enforced under a pessimistic lock; there is no database range exclusion constraint.

## Trip Lifecycle Concurrency

Concurrent duplicate dispatch calls serialize on the trip row and persist one dispatch transition and one dispatch audit record. Concurrent duplicate start calls likewise persist one start transition and one start audit record. The losing transaction is rejected by lifecycle state validation after it obtains the lock.

The verified transaction isolation is PostgreSQL's default `READ COMMITTED`, supplemented by explicit pessimistic locks on the affected trip or resource rows.

## Fuel Issue Constraints

Concurrent fuel issue creation obtains unique voucher numbers from the PostgreSQL sequence. A number consumed in a rolled-back transaction is not reused, which is the expected PostgreSQL sequence behavior. The database unique constraint rejects a duplicate voucher number.

## Fuel Purchase Constraints

Concurrent fuel purchase creation obtains unique purchase numbers from the PostgreSQL sequence. Rolled-back sequence values are not reused, and the database unique constraint rejects duplicate purchase numbers.

The vendor invoice database constraint is exact and case-sensitive: an exact duplicate for the same vendor is rejected, while a differently cased value can be stored by PostgreSQL. The application repository intentionally performs a case-insensitive existence check and therefore prevents that variant through the normal use case. Direct SQL or a future alternate write path could bypass this application policy.

## Price Overlap Behavior

Fuel price effective-period overlap is enforced by the application repository query. Overlapping inclusive periods are detected and adjacent non-overlapping periods are allowed. PostgreSQL has no exclusion constraint for this rule, so direct database writes can bypass it.

## Foreign Key Verification

The suite verifies PostgreSQL rejects invalid critical references, including trip resource references and fuel issue/purchase station, vehicle, driver, and vendor references. It also exercises migrated uniqueness for business numbers, permission names, and ordered route stops.

## Transaction Rollback

A deliberately failing fuel purchase audit insert causes the earlier purchase insert in the same service transaction to roll back. Neither the purchase nor its history record remains, demonstrating atomic rollback across the aggregate and audit write.

## Known Database-Level Gaps

- Vehicle and driver scheduling overlap rules depend on application checks plus pessimistic locks; PostgreSQL exclusion constraints are absent.
- Fuel price period overlap is application-only.
- Vendor invoice uniqueness is case-sensitive in the database while the application policy is case-insensitive.
- No persistence entity currently uses optimistic version columns.
- Flyway 9.22.3 warns that PostgreSQL 16 is newer than its officially tested maximum of PostgreSQL 15.
- The repository has no CI workflow configuration, so CI must be configured separately with Docker available and `mvn clean verify` as the gate.

Standard Spring Data pagination and JPQL date-overlap queries ran successfully against PostgreSQL. The only PostgreSQL-specific native operations identified are the fuel number generators' `nextval(...)` sequence calls, which were exercised directly by the concurrency and rollback tests.

The recommended release decision after a green full build is to accept PostgreSQL verification as the database gate and proceed to the next planned slice. The gaps above should be tracked explicitly; this verification work does not begin US-33 or add business behavior.
