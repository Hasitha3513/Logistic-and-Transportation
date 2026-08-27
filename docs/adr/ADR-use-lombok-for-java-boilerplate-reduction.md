# ADR: Use Lombok for Controlled Java Boilerplate Reduction

## Status
Accepted

## Context
The Transport & Logistics Management System is a Java 21 / Spring Boot 3.2.12 modular monolith following Hexagonal Architecture and Domain-Driven Design principles.
The codebase contains substantial mechanical Java boilerplate in JPA entities and Spring-managed adapters/controllers. At the same time, domain models, domain events, and many DTOs are already implemented as clean, immutable Java records.

Previous attempts to manage boilerplate without Lombok led to hundreds of lines of repetitive getters, setters, and default constructors across 27 JPA entities.

## Decision
We introduce Project Lombok in a strictly controlled architectural capacity to eliminate mechanical boilerplate while preserving encapsulation and architectural integrity.

### Allowed Annotations
- `@Getter` — on JPA entities and immutable component fields where read access is required.
- `@Setter` — on JPA entities where mutation through setters is intentionally part of the existing persistence/entity design.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — on JPA entities to satisfy JPA requirements while discouraging uninitialized instantiation outside persistence mechanisms. Compatible visibility (e.g. package-private or public) is retained where existing callers require it.
- `@RequiredArgsConstructor` — on Spring-managed `@RestController` and `@Component` adapter classes with final dependency fields, replacing mechanical constructor injection.

### Conditionally Allowed (Requires Justification)
- `@Builder` — only on complex DTOs or value objects where construction requires telescoping arguments and cannot be expressed as a record with compact constructors.
- `@Value` — for immutable value objects where Java records cannot be used.
- `@ToString` — only where class-level string representations are needed, always with sensitive field exclusions (`exclude = {"password", "secret", "token"}`).

### Prohibited by Default
- `@Data` — strictly prohibited on JPA entities and domain classes due to implicit mutable equals/hashCode/toString behaviors and loss of encapsulation.
- `@EqualsAndHashCode` on JPA entities — prohibited to prevent broken entity equality semantics in Hibernate session/proxy contexts.
- Class-level `@ToString` on JPA entities — prohibited to prevent accidental lazy loading initialization and performance degradation.
- Blind `@Setter` on domain aggregates — prohibited. Business state transitions (e.g., `trip.complete()`, `vehicle.allocate()`, `fuelIssue.authorize()`) must never be replaced with generic setters.
- `@AllArgsConstructor` merely to bypass intentional constructor validation.
- Converting existing Java records to Lombok classes — prohibited. Java records remain the primary immutable data representation.

### Architectural Principle
> **"Lombok may remove syntax, but must not remove business encapsulation."**

Lombok annotations must never bypass domain validation rules or replace explicit domain lifecycle methods.

## Consequences
- Mechanical boilerplate in JPA entities and Spring adapters is dramatically reduced (~600+ lines removed).
- Domain rules, API contracts, database schema, Flyway migrations, and Spring Modulith boundaries remain 100% unchanged.
- JaCoCo and toolchains are configured via `lombok.config` to recognize generated code.
