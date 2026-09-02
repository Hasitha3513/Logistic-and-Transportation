# US-68 Last-Mile Exceptions — Frozen Product Decisions

**Status:** `PRODUCT_DECISIONS_FROZEN / IMPLEMENTATION_NOT_STARTED`  
**Authority:** US-68 use-case/activity/sequence diagrams; current US-59, US-60, US-62, US-63–US-67 source and contracts.  
**Primary actor:** Last-Mile Planner.

## Scope and Ownership

US-68 is a Delivery-owned planner workflow for classifying a last-mile disruption and routing it to the already-owned operational capability. It is **not** a second generic exception aggregate, failed-attempt engine, redelivery scheduler, notification service, route optimiser, telematics feature, or customer self-service flow.

| Requirement outcome | Frozen owner and action |
| :--- | :--- |
| Rider no-show / replacement | Rider and Batch assignment capabilities; planner uses the existing assignment/reassignment commands. No Rider medical, licence, or availability internals are exposed. |
| Multiple attempts | US-59 attempt history and US-60 scheduling decide the next attempt/RTO; US-68 only presents the decision context. |
| Address not found | Record an actual failure under US-59 where applicable, then use US-62 `WRONG_ADDRESS` investigation and existing Location capability for correction. |
| Access restriction | US-59 `ACCESS_RESTRICTED` failed attempt after a failed field attempt. A temporary obstacle is an operational note only until an attempt fails. |
| Contactless delivery | US-57 POD rules; US-68 does not add a contactless proof flow. |
| Cash dispute | US-59 `DOCUMENT_OR_PAYMENT_ISSUE` plus escalation where needed; no settlement ledger is introduced. |

US-62 remains the sole specialized `DeliveryExceptionCase` owner (damage, wrong address, partial delivery, OTP mismatch, refusal). US-78 remains the future cross-module incident authority. US-69 owns customer communications and US-70 customer self-service.

## Lifecycle, Batch, ETA, and Resolution Rules

- A report alone does not change `DeliveryOrder` status, remove it from a dispatched batch, resequence a batch, or create a failed attempt. The remaining batch continues; no TSP/VRP or automatic rerouting is allowed.
- Only an actual unsuccessful field attempt invokes US-59 and its existing `FAILED_ATTEMPT`, `ESCALATED`, or `RETURN_TO_BASE` transitions. US-60 alone schedules a redelivery.
- A Rider no-show may be resolved by an authorized reassignment; it does not itself mark an order failed. Planner-controlled assignment uses the existing Rider/Batch RBAC and capacity rules.
- US-62 wrong-address resolution controls corrected-location semantics. Correct address but blocked access is never treated as `WRONG_ADDRESS`.
- Existing US-59 `delivery_escalation` is reused. No new escalation engine is permitted.
- Existing US-67 cache invalidation is consumed when a real assignment, membership, destination, or batch-status command changes ETA inputs. US-68 does not calculate ETA. A delay note alone is not an ETA fact.
- Zone geometry, serviceability, slot capacity, and reservation state are not changed because one stop has an obstruction.
- Existing POD rules prevail: a temporary non-terminal obstruction does not globally block POD; a failed attempt cannot coexist with final POD; specialized US-62 POD blocks remain type-specific.

## Data, Privacy, and Security

No new entity, table, permission, or API family is justified by this decision. The implementation may compose the established APIs and expose a Delivery-order detail workflow; it must not create `LastMileException` persistence merely to duplicate `delivery_attempt` or `delivery_exception_case`.

If a future implementation proves a new durable fact is necessary, it must re-open this decision and, only if the directory head is still V57, propose (not assume) `V58__delivery_last_mile_exceptions_us68.sql`. It must have Delivery ownership, tenant_id, optimistic versioning, same-tenant composite FKs, and indexes appropriate to `(tenant_id, delivery_order_id)` and active status. Existing V48/V51 tables already enforce the relevant same-tenant links and duplicate-active-case rule.

- Notes use the established operational limits (up to 1,000 characters for exception descriptions/resolution notes; US-59 contact notes up to 500). Store only operational facts.
- Never persist raw gate/door/access codes, PINs, passwords, OTPs, customer credentials, precise live location, or copied contact details. Use masked/reference/status facts only.
- Evidence is not required for an access/no-show/attempt workflow. When existing US-62 evidence is required or voluntarily attached, reuse `DeliveryEvidenceStoragePort` and its image validation; no new binary storage or geotagging.
- `ONLINE_ONLY_FOR_US68`: no new offline queue/outbox. Existing US-58 offline POD remains its own scope.
- Tenant and actor are server-derived from the authenticated execution context. Request `tenantId`, reporter/resolver/escalator IDs, and cross-tenant resource IDs are non-authoritative and must fail closed with safe 404/403 semantics.

## RBAC, API, Concurrency, and Events

US-68 reuses the smallest existing permission catalogue:

| Action | Required authority |
| :--- | :--- |
| View planner context/attempt and exception history | `DELIVERY_FAIL_VIEW` and/or `DELIVERY_EXCEPTION_VIEW` for the underlying resource |
| Record an actual failed attempt or contact fact | `DELIVERY_FAIL_RECORD` |
| Create/investigate/resolve US-62 case | existing `DELIVERY_EXCEPTION_CREATE`, `MANAGE`, `RESOLVE` |
| Escalate | existing `DELIVERY_FAIL_ESCALATE` or `DELIVERY_EXCEPTION_ESCALATE` as applicable |
| Replace/reassign Rider | `DELIVERY_RIDER_ASSIGN` or `DELIVERY_BATCH_ASSIGN` as applicable |

Method security remains authoritative, routes are authenticated, and no last-mile mega-permission is created. The implementation reuses current `/v1/deliveries/{id}/failed-attempt`, `/escalate`, Rider/Batch assignment, US-62 `/v1/deliveries/{id}/exceptions`, and US-60 redelivery contracts; it adds no duplicate `/last-mile-exceptions` REST family.

All operations retain their owning aggregate transaction and optimistic version checks. POD-finalize versus failure, failure versus redelivery/RTO, and batch completion versus assignment races return `409 CONFLICT` for the loser. Existing active exception duplicate policy remains `(tenant_id, delivery_order_id, exception_type)` for open/investigating US-62 cases. No new domain event is needed: existing owning commands publish their established after-commit events; US-68 emits no SMS, email, push, or provider event.

## UX, Architecture, and Test Contract

The MVP UI is a Last-Mile Planner section in `DeliveryOrderDetailsPage`, with optional batch-context link. It shows current Rider, batch, ETA, prior attempts and active US-62 cases, then routes the operator into the existing failure, exception, assignment, escalation, or redelivery action. It is not a global dashboard, native mobile app, analytics surface, or customer UI.

Implementation must preserve Delivery package ownership and hexagonal ports; no cross-module repositories/JPA graphs/direct SQL. Location, notification, offline, and routing dependencies use existing published ports only. No GPS, telemetry, geofence, route optimisation, or US-69/US-70 leakage is approved.

Required verification includes focused domain/service/controller security tests, PostgreSQL same-tenant/IDOR/duplicate/concurrency coverage, architecture tests, frontend Vitest, real Chromium planner flow, relevant US-56–US-67 regressions, and destructive PostgreSQL execution only against `transport_logistics_acceptance`—never the development database.

## Acceptance State

- US-68 decisions: **FROZEN**
- US-68 implementation: **NOT STARTED**
- MVP 1.4: **5 / 8 COMPLETE**
- Overall: **62 / 87 COMPLETE**
- Deferred: **25 / 87**
- Next task: `MVP-1.4-US68-LAST-MILE-EXCEPTIONS-IMPLEMENTATION-001`
