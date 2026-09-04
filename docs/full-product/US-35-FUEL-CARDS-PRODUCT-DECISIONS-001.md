# US-35 Manage Fuel Cards Product Decisions

**Task:** `US-35-FUEL-CARDS-PRODUCT-DECISIONS-001`

**Decision:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`

**Date:** 2026-09-04

**Owner:** Fuel

**Current Flyway head:** V63; implementation selects the next free forward version

**Program accounting:** unchanged at 68 / 87 complete and 19 / 87 remaining

**Next task:** `US-35-FUEL-CARDS-IMPLEMENTATION-001`

## Source intent and scope

The authoritative actor is the Fuel Manager. US-35 must issue and restrict Fuel Cards, import and validate provider transactions, reconcile them with Fuel and Trip facts, identify suspected misuse, block or restrict a card where required, and preserve audit evidence. The acceptance criteria require cards to be issuable and restrictable, imported transactions to be reconcilable, and suspected misuse to produce a review condition.

Fuel owns the Fuel Card master, local lifecycle and restrictions, current Driver or Vehicle binding plus immutable binding history, imported provider transaction facts, local reconciliation, deterministic card-policy indicators, and card-specific audit/history. The external provider remains authoritative for the actual card account, provider authorization, settlement, monetary ledger, provider transaction identity, merchant metadata, and any supplied account balance.

US-35 is not payment processing, banking, card issuing/acquiring, accounts payable, expense reimbursement, general ledger, cash settlement, a generic fraud platform, or a Fuel-exception investigation lifecycle.

## Existing-story boundaries

- US-31 remains the owner of internal operational Fuel Issues. A card transaction never becomes a bunker issue or mutates an issue.
- US-32 remains the owner of accepted external Fuel Purchases and their economic values. An imported card transaction is immutable provider evidence until explicitly linked to one existing same-Tenant Fuel Purchase; import never auto-creates or duplicates a Fuel Purchase.
- US-34 remains the owner of Fuel cost allocation and variance; importing or reconciling a card fact does not allocate cost automatically.
- US-36 bunker inventory remains independent. Card use creates no tank stock movement.
- US-37 remains unchanged. Reconciled card transactions are recorded as `SOURCE_PARITY / FUTURE_EXTENSION`; they are not a new accepted US-37 source in this story.
- US-38 owns investigation, fraud/theft determination, correction approval, exception resolution, escalation, and disciplinary or financial action. US-35 produces review indicators only.
- US-78 receives no automatic case from US-35. A future US-38 decision may publish a minimized `OperationalExceptionFactV1`.
- US-46 payroll and US-47 billing/accounting posting are excluded.

## Card identity and protected data

The internal card ID is UUID. A card records an Organization-owned provider logical UUID, provider-neutral alias, opaque provider card reference, masked display identifier, optional last four digits, and expiry month/year. Provider identity is validated through a published Organization contract; there is no Organization repository/entity import or physical cross-module foreign key.

The application stores no PAN, CVV, PIN, magnetic-stripe data, provider credential, account balance, or provider authorization secret. The opaque provider reference is persisted only because imports must resolve the card; it is never returned by a read API, shown in UI/audit, or logged. Responses expose the masked identifier and last four digits only. This is a limited card-reference model and makes no claim to cardholder-data processing or PCI DSS scope certification.

Within a Tenant and provider, the opaque provider card reference is the unique business identity. Masked values and last four digits are never uniqueness keys.

## Lifecycle, expiry, and provider synchronization

The exact local lifecycle is `DRAFT`, `ACTIVE`, `SUSPENDED`, `BLOCKED`, `EXPIRED`, and `CANCELLED`.

- Issue creates `DRAFT`.
- `DRAFT -> ACTIVE` requires a non-expired card, one valid binding, and valid restrictions.
- `DRAFT -> CANCELLED` abandons an unactivated card.
- `ACTIVE -> SUSPENDED -> ACTIVE` is the reversible temporary restriction path.
- `ACTIVE` or `SUSPENDED -> BLOCKED` is immediate and requires a reason. Lost or stolen always uses this path.
- `SUSPENDED` or `BLOCKED -> CANCELLED` is final and reasoned.
- Any non-cancelled card is operationally expired after the final instant of its expiry month in the Tenant timezone. Expired cards cannot activate or resume; the effective state is `EXPIRED` even before a persisted transition catches up.
- `BLOCKED`, `EXPIRED`, and `CANCELLED` never return to `ACTIVE`. Replacement means a new card.

There is no arbitrary status PATCH. Expiry is enforced on every command and import evaluation using server time. A future persisted expiry transition may reuse US-81's Tenant-aware scheduling pattern; US-35 creates no second scheduler.

All states are local controls. With no provider adapter selected, `ACTIVE` never means provider-activated and `BLOCKED` never means externally blocked. Provider synchronization status is `NOT_CONFIGURED`; the UI must say “Blocked locally” and must not imply provider confirmation.

## Binding and restrictions

A card has exactly one active binding at a time: either `VEHICLE` or `DRIVER`, never both and never multiple. Reassignment is allowed with optimistic versioning and a required reason. It closes the current binding and appends a new immutable history row; historical responsibility is never overwritten.

Vehicle bindings use a minimal Fleet-published lookup and require a same-Tenant active Vehicle. Driver bindings use a minimal Fleet/Driver-published lookup and require a same-Tenant active Driver. Cross-Tenant or unknown IDs return safe not-found. Fuel never imports Fleet/Driver repositories or entities.

The exact Phase 1 controls are:

- maximum amount per transaction;
- maximum total amount per Tenant calendar day;
- maximum total amount per Tenant calendar month;
- maximum quantity per Tenant calendar day;
- an allow-list of fuel types;
- an allow-list of provider station/site references; and
- enforcement of the card's one current Vehicle or Driver binding when a corresponding provider fact is supplied.

Monetary values use positive `BigDecimal` and one ISO-4217 currency per card restriction set. No FX conversion exists. Quantity is positive decimal litres. Period boundaries use server time and the Tenant timezone. Station restrictions compare an exact normalized provider station reference; they do not create a merchant-management model. Department/pool cards, multiple simultaneous holders, and generic policy expressions are not approved.

Because the minimum adapter imports facts after purchase, local limits do not claim to reject provider authorization. A transaction above a limit is retained and receives `LIMIT_EXCEEDED / REVIEW_REQUIRED`. Application commands can prevent an invalid local activation or restriction configuration, but only the external provider can prevent an external purchase.

## Inbound transaction contract and evidence tier

US-35 owns an authenticated Fuel import endpoint rather than expanding Integration. US-73 is accepted only for outbound `FILE_JSON_V1`; it provides no inbound file capability and is not reused or modified.

The minimum evidence tier is `CONTROLLED_PROVIDER_FIXTURE`. Acceptance uploads a real provider-neutral canonical UTF-8 JSON file through the authorized endpoint and exercises real parsing, validation, hashing, PostgreSQL persistence, deduplication, reconciliation, and review evaluation. It does not prove provider authenticity, transport from a named provider, signature validation, settlement, or external card blocking. A real provider sandbox or a new shared Integration inbound capability requires a later approved decision.

The canonical schema is `FUEL_CARD_TRANSACTIONS_V1`. Its envelope contains `schemaVersion`, `providerBatchId`, `generatedAt`, and `transactions`. Each transaction contains:

- `providerTransactionId`;
- `providerCardReference`;
- `transactionKind` (`PURCHASE` or `REVERSAL`);
- `originalProviderTransactionId` for a reversal;
- `transactionTimestamp` and optional `postedTimestamp`;
- optional safe provider station/site reference;
- `fuelType`, positive `quantityLitres`, positive `unitPrice`, positive `totalAmount`, and ISO-4217 `currency`;
- optional provider-supplied Vehicle reference, Driver reference, and Trip UUID; and
- provider status limited to `POSTED` or `REVERSED` consistently with transaction kind.

Provider Vehicle/Driver strings are evidence only and never establish Tenant or a domain identity. A Trip UUID is validated through a published same-Tenant Trip contract before it can support reconciliation.

Only `application/json` UTF-8 is accepted. The maximum file size is 1 MiB and the maximum batch is 1,000 transactions. The server ignores the client filename, rejects BOM/invalid encoding, unknown schema versions, unexpected properties, malformed or non-positive numbers, unsupported currencies/fuel types/statuses, invalid timestamps, missing reversal references, duplicate IDs inside one file, and trailing content. The JSON is parsed as data, never executed, and is discarded after the atomic transaction; no antivirus claim is needed for this bounded non-persisted JSON-only path.

The application stores normalized facts, the SHA-256 file/payload hash, batch identity, and safe validation outcomes. It stores neither the original file nor arbitrary raw provider bodies. If future policy requires original-file retention, US-83 owns content and Fuel stores only a logical document reference.

## Idempotency, transaction state, and reversals

Batch idempotency is `(tenant_id, provider_id, provider_batch_id)` plus payload hash. The same identity and hash returns the existing batch result. The same identity with a different hash is `FUEL_CARD_IMPORT_CONFLICT`. A unique `(tenant_id, provider_id, file_hash)` prevents the same file under another batch ID from duplicating transactions.

Transaction idempotency is `(tenant_id, provider_id, provider_transaction_id)`. Exact replay returns the existing immutable record. A duplicate identity with a different canonical hash is a terminal integrity conflict and leaves the first fact unchanged.

Local transaction state is `IMPORTED`, `REVIEW_REQUIRED`, `RECONCILED`, `REJECTED`, or `REVERSED`; it is distinct from provider status. Imported provider values are immutable. A provider reversal is a new immutable `REVERSAL` transaction with its own provider transaction ID and a same-provider reference to the original. It never deletes or edits the original. A same-ID payload/status rewrite is a conflict, not a reversal.

A transaction received for a suspended, blocked, expired, or cancelled local card is retained and marked `CARD_INACTIVE / REVIEW_REQUIRED`.

## Reconciliation and review indicators

Reconciliation links one imported purchase transaction to at most one existing same-Tenant US-32 Fuel Purchase. It may also retain a validated logical Trip reference as supporting attribution. Import does not create a Fuel Purchase. The existing Fuel Purchase remains the economic source of truth; the card transaction remains provider evidence, eliminating duplicate economic facts.

Automatic matching may only suggest one candidate when provider, card, currency, fuel type, quantity, total amount, validated binding, and an exact date/time-window rule all agree. The initial tolerance is the same Tenant calendar day; no fuzzy score or opaque matching exists. Zero or multiple candidates remain `REVIEW_REQUIRED`.

An authorized operator explicitly matches, unmatches, or rejects with the current version and a required reason. Reconciliation changes append history and never edit provider facts. One purchase cannot be concurrently reconciled to two non-reversed card purchases, and one card purchase cannot have two active reconciliations. Reversal of a reconciled transaction retains history and moves the pair to review for explicit disposition; it does not reverse US-32 automatically.

The exact deterministic review indicators are:

- `BINDING_MISMATCH`;
- `FUEL_TYPE_NOT_ALLOWED`;
- `STATION_NOT_ALLOWED`;
- `LIMIT_EXCEEDED` with the violated limit type;
- `CARD_INACTIVE` for suspended, blocked, expired, or cancelled use;
- `TRANSACTION_INTEGRITY_CONFLICT`; and
- `REVERSAL_REVIEW_REQUIRED`.

US-35 does not freeze an “unusual repeated use” threshold because the sources provide no defensible interval or count. Indicators use `REVIEW_REQUIRED`, `CARD_POLICY_DEVIATION`, and the codes above. They never assert fraud, theft, criminal misuse, culpability, or disciplinary outcome. Indicator acknowledgement may record that a Fuel Manager reviewed it, but investigation and resolution belong to US-38.

## Tenant, authorization, segregation, and audit

Every card, restriction, binding, batch, transaction, reconciliation, indicator, and audit row carries immutable `tenant_id`. HTTP Tenant authority comes from `CurrentTenant`; imports use the authenticated request's trusted Tenant execution context. Payload/provider references never establish Tenant. All repository operations, uniqueness, child relationships, indexes, and concurrency checks are Tenant-qualified.

The minimal permissions are:

- `FUEL_CARD_VIEW` for masked cards, transactions, imports, and indicators;
- `FUEL_CARD_MANAGE` for issue, mutable draft metadata, binding, restrictions, activate, suspend/resume, and cancel;
- `FUEL_CARD_BLOCK` for lost/stolen/policy blocking;
- `FUEL_CARD_IMPORT` for canonical batch import; and
- `FUEL_CARD_RECONCILE` for match, unmatch, reject, and reversal disposition.

`FUEL_CARD_AUDIT_VIEW` is not added: the existing authorized audit capability remains the audit read boundary. The application enforces one targeted segregation rule: the actor who imported a transaction cannot manually reconcile or reject that transaction. Card creation and activation do not require two people because both are local controls and do not activate a provider account. No generic ABAC or approval engine is introduced.

Audit/history records creation, activation, suspension/resume, block, cancel, expiry observation, binding changes, restriction changes, import result, idempotent replay, integrity conflict, reconciliation/unmatch/reject, reversal receipt/disposition, and indicator acknowledgement. Audit contains safe actor/time/action/result, logical IDs, reason codes, and before/after hashes only—never full card reference, PAN, PIN, CVV, credentials, or raw provider payload.

No provider credentials are required for the controlled import. Any future adapter stores only an opaque US-73-style credential reference resolved by infrastructure; no secret belongs in Fuel tables, APIs, UI, logs, or audit.

## Frozen REST API

All external routes are literal `/api/v1` routes and use bounded request DTOs with no `tenantId`:

- `GET /api/v1/fuel/cards` and `POST /api/v1/fuel/cards`;
- `GET /api/v1/fuel/cards/{cardId}`;
- `PUT /api/v1/fuel/cards/{cardId}` for draft alias/expiry metadata only, with version;
- `POST /api/v1/fuel/cards/{cardId}/activate`, `/suspend`, `/resume`, `/block`, and `/cancel`;
- `POST /api/v1/fuel/cards/{cardId}/bindings` and `GET /api/v1/fuel/cards/{cardId}/bindings`;
- `PUT /api/v1/fuel/cards/{cardId}/restrictions` with version and reason;
- `GET /api/v1/fuel/cards/{cardId}/history`;
- `POST /api/v1/fuel/card-imports` as multipart with exactly one JSON part, and `GET /api/v1/fuel/card-imports` plus `GET /api/v1/fuel/card-imports/{batchId}`;
- `GET /api/v1/fuel/card-transactions` and `GET /api/v1/fuel/card-transactions/{transactionId}`; and
- `POST /api/v1/fuel/card-transactions/{transactionId}/match`, `/unmatch`, and `/reject`.

Card and batch lists and transaction/history lists default to 20 and cap at 100. Safe card filters are status, provider ID, binding type/ID, expiry range, and review-required. Safe transaction filters are card/provider ID, transaction date range, local status, reconciliation status, indicator code, and review-required. Safe sort keys are explicit display/business fields only: created time, masked identifier, expiry, transaction/posted time, amount, and status. No arbitrary property or SQL filter exists.

There is no generic status PATCH, raw transaction edit, transaction/card delete, arbitrary upload, external-block claim, manual provider retry, payment action, secret read, or raw provider-payload route.

Stable error codes include `FUEL_CARD_NOT_FOUND`, `FUEL_CARD_CONFLICT`, `FUEL_CARD_INVALID_STATE`, `FUEL_CARD_EXPIRED`, `FUEL_CARD_BINDING_INVALID`, `FUEL_CARD_RESTRICTION_INVALID`, `FUEL_CARD_IMPORT_INVALID`, `FUEL_CARD_IMPORT_TOO_LARGE`, `FUEL_CARD_IMPORT_CONFLICT`, `FUEL_CARD_TRANSACTION_NOT_FOUND`, `FUEL_CARD_TRANSACTION_CONFLICT`, and `FUEL_CARD_RECONCILIATION_INVALID`, returned through the existing standard API error envelope.

## Frontend decision

The feature lives under Fuel in the existing `AppLayout`. It provides pageable card list/detail, masked card display, issue/draft edit, local lifecycle actions, one current Driver/Vehicle binding with history, restrictions, expiry, import/upload history, immutable transaction detail, reconciliation actions, deterministic review indicators, and authorized audit/history access.

The UI distinguishes local lifecycle from provider state and displays `Provider synchronization not configured`. It contains no PAN/PIN/CVV, raw reference, credential, provider balance, settlement, payment approval, bank account, credit account, general-ledger, fraud verdict, or US-38 investigation screen.

## Persistence and performance expectation

Implementation is expected to add one forward migration after inspecting the then-current Flyway head; V64 is not reserved. Fuel owns the following Tenant-scoped tables:

- `fuel_card` for identity, local lifecycle, masked metadata, current version, and provider logical ID;
- `fuel_card_binding_history` for immutable effective-dated assignment history;
- `fuel_card_restriction` for the versioned current restriction set and allowed fuel types;
- `fuel_card_import_batch` for bounded import identity/hash/count/outcome;
- `fuel_card_transaction` for immutable normalized provider facts and local state;
- `fuel_card_reconciliation_history` for append-only match/unmatch/reject/reversal disposition;
- `fuel_card_transaction_indicator` for deterministic review evidence; and
- `fuel_card_audit_event` for append-only safe action evidence.

Same-module relationships use Tenant-consistent composite foreign keys. Vehicle, Driver, Trip, Organization/provider, Integration, Document, and Fuel Purchase references remain UUID logical references with no cross-module physical foreign key. Mutable cards/restrictions and reconciliation commands use optimistic versions; transaction source columns and history rows are append-only.

Tenant-leading indexes cover card status/provider reference hash/current binding/expiry, batch provider identity/hash/created time, transaction provider ID/card/date/status/reconciliation/review state, active reconciliation, indicators, and audit history. Imports are atomic and capped at 1,000 records/1 MiB; lists are pageable; candidate matching is Tenant/card/date bounded. No unbounded provider file or full-history in-memory scan is allowed.

Financial/audit retention duration remains `RETENTION_POLICY_EXTERNAL_TO_US35`. There is no destructive delete or purge endpoint.

## Events and future contracts

P1-01 is `NONE` for US-35 because no current cross-module consumer requires a durable event. US-35 does not pre-build a speculative US-38 anomaly event or modify `OperationalExceptionFactV1`. Audit and Reporting may later consume explicitly approved minimized projections/contracts and may never read Fuel tables directly.

## Frozen verification and acceptance

Implementation must include domain tests for lifecycle, expiry, binding, restrictions, currency, idempotency, immutable provider facts, reversal, reconciliation, indicators, and actor segregation; application/controller security tests must use literal `/api/v1/...` URLs.

PostgreSQL acceptance uses only `transport_logistics_acceptance` and proves clean Flyway current-head migration, Tenant uniqueness, transaction/batch dedupe, conflicting replay rejection, Tenant-consistent child relationships, append-only transaction/history behavior, optimistic races, and reconciliation integrity. Deterministic concurrency tests cover competing bindings, restriction version conflict, block versus activate, duplicate batch/file, duplicate/conflicting transaction, double reconciliation, and reversal versus reconciliation without sleep-based assertions.

The real PostgreSQL-backed Chromium journey must prove: authorized card creation; masked-only display; valid same-Tenant Vehicle or Driver binding; restriction configuration; local activation; real canonical JSON parsing/import; exact replay without duplicates; valid reconciliation to an existing Fuel Purchase; binding mismatch and over-limit review; retained blocked/expired transaction; conflicting duplicate failure; before/after source-value equality; Tenant B non-inference; and limited-user management/import/reconciliation denial.

The full acceptance also requires complete Maven verification, architecture/Modulith, Checkstyle, PMD, SpotBugs, TypeScript, Vitest, production build, changed-file lint, classified global pre-existing lint debt, and `git diff --check`.

## Final decision

All US-35 product gates are frozen. Implementation may proceed without changing story accounting. Any need for full cardholder data, named provider authentication, synchronous provider authorization/blocking, a shared inbound Integration capability, merchant policy, multiple active bindings, arbitrary fraud scoring, auto-created Fuel Purchases, US-38 investigation, Operations case publication, or a new retention duration requires a new explicit decision.
