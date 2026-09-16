# US-55 CS01 — Domain Trust and Quality Contracts

## Verdict

`PASS / CS01_COMPLETE`

US-55 now has framework-neutral Tracking domain contracts for coordinate validity, accuracy, source-time
ordering, freshness/connectivity, impossible movement, optional battery/tamper evidence, reliability state and
GPS-exception episode lifecycle. This change set adds ports but deliberately does not add adapters, persistence,
Flyway, APIs, permissions, Kafka/Redis wiring, event publication or frontend behavior.

## Implemented contract

- WGS84 coordinate bounds and seven-digit normalized precision are fail-closed.
- `(0,0)` is retained as untrusted `NULL_ISLAND_SUSPECT` evidence.
- Accuracy boundaries are exact: good through 100 m, low through 1,000 m, unusable through 10,000 m; absence is
  UNKNOWN and invalid values are rejected.
- Clock skew, future, delayed, late, retained-history and deterministic equal-source-time ordering are explicit.
- Impossible movement uses the frozen equal-time conflict and 2 km / 10 minute / 250 km/h rules.
- Trusted live promotion and detector eligibility are separate and fail closed.
- Battery warning/critical and rapid-drain classification do not independently disqualify position trust.
- Exception episodes deduplicate repeated evidence, never downgrade severity, require a review reason, preserve
  evidence, enforce two-point recovery and keep resolved episodes immutable.
- Tenant identity is mandatory on observations, episodes and every repository/use-case port operation.

## Verification

| Gate | Result |
| --- | --- |
| Focused GPS edge domain tests | 12 / 12 PASS |
| Architecture, Modulith and focused domain | 71 / 71 PASS |
| Complete backend (`transport_logistics_acceptance`) | 1,850 / 1,850 PASS; 0 failures, 0 errors |
| Flyway | V95 validated; no migration added |
| Checkstyle | PASS; 0 violations |
| SpotBugs | PASS |
| PMD | Existing global 32 unused-import findings only; none in CS01 files |
| Dependency analysis | PASS with existing repository dependency warnings |
| Docker Compose validation | PASS |
| `git diff --check` | PASS |

The first discarded full-suite invocation used sandbox-restricted sockets. A second discarded invocation selected
the repository default database name and was stopped when that was observed. Authoritative complete-suite
evidence is only the explicit `DB_URL=jdbc:postgresql://localhost:5433/transport_logistics_acceptance` run.

## Security and architecture

The domain package imports no Spring, JPA, Hibernate, Jackson, web or persistence types. No raw payload,
credential, signature or personal-data field is introduced. Ports carry Tenant identity explicitly, and no
cross-module persistence access exists.

## Remaining scope

CS01 does not establish canonical telemetry signal compatibility. The exact next controlled queue is:

`US-55-HANDLE-GPS-EDGE-CASES-CS02-CANONICAL-SIGNAL-CONTRACT-001`

Accounting remains 73 / 87 and Flyway remains V95.
