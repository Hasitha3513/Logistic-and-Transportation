# Adapter‑Generation Convention

## Why the scaffolding folders exist

The project follows a **hexagonal (ports‑and‑adapters) architecture** inside a **Spring Modulith** modular‑monolith.  Each business module declares **outbound ports** (interfaces) for the capabilities it needs from *other* modules.  Implementations of those ports are placed **inside the consuming module** under a well‑known package structure:

```
<com>/transportlogistics/app/<provider>/infrastructure/adapters/out/<consumer>
```

* **`<provider>`** – the module that **owns** the data or behaviour (e.g. `fuel`).
* **`<consumer>`** – the module that **needs** it (e.g. `fleet`).

The folder therefore tells two things at a glance:
1. **Which module provides the port** (`fuel`).
2. **Which module implements it** (`fleet`).

### Example
```
src/main/java/com/transportlogistics/app/fuel/infrastructure/adapters/out/fleet/FleetFuelTripDistanceAdapter.java
```
* `fuel` defines the port `TripDistancePort`.
* `fleet` implements that port in the `FleetFuelTripDistanceAdapter` class.
* The adapter is `@Component` so Spring can inject it where the port is required.

## Folder‑generation rules

| Rule | Description |
|------|-------------|
| **Out‑adapter root** | `src/main/java/com/transportlogistics/app/<provider>/infrastructure/adapters/out` – created automatically when a module defines an outbound port. |
| **Consumer sub‑folder** | One sub‑folder per consumer module (`fleet`, `identity`, `organization`, `trip`, `events`, `persistence`). The folder is generated even if it is empty – it is a placeholder for future adapters. |
| **Persistence sub‑folder** | When a module *exposes* its own repository ports, the concrete JPA adapters live under `.../infrastructure/adapters/out/persistence`. This folder contains the entity classes, Spring Data repositories, and the persistence‑adapter that implements the repository port. |
| **Events sub‑folder** | Used when a module publishes domain events that another module may listen to. The folder holds event‑publisher implementations. |
| **Empty folders** | If no adapter exists yet, the folder is left empty. They can be safely deleted, but keeping them makes it obvious where new adapters should go. |

## Naming conventions

* **Adapter class name** – `<Consumer><DomainConcept><AdapterSuffix>` (e.g. `FleetFuelTripDistanceAdapter`).
* **Port interface name** – placed in the *provider* module under `application/ports/out` (e.g. `TripDistancePort`).
* **Spring component** – adapters are annotated with `@Component` (or `@Service`) so that Spring Modulith discovers them automatically.

## How to add a new adapter
1. **Define the outbound port** in the provider module (`application/ports/out`).
2. **Run the code‑generator** (or create the class manually) inside the provider’s `infrastructure/adapters/out/<consumer>` folder.
3. Implement the port by delegating to the consumer module’s existing services or repositories.
4. Annotate the class with `@Component`.
5. Use the port in the provider’s application service via constructor injection.

## Interaction flow (runtime)
1. Provider module (e.g. **fuel**) injects the port interface (`TripDistancePort`).
2. Spring resolves the injection to the concrete class found in the consumer module (`fleet`).
3. Calls travel from provider → consumer without the provider knowing the concrete implementation.
4. The consumer can remain completely unaware of the provider – only the shared contract (the port) matters.

## When to keep or delete empty folders
* **Keep** if you anticipate future adapters for that consumer – it serves as a visual cue and prevents accidental mis‑placement of new classes.
* **Delete** if you are certain the provider will never need that consumer.  Deleting does not affect any compiled code because the folder contains no source files.

---
*This document lives in `docs/development/adapter-convention.md` and should be referenced by any developer adding new ports or adapters.*
