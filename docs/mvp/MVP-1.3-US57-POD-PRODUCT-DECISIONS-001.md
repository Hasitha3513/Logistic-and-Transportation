# MVP 1.3 US-57 — Proof-of-Delivery Product Decisions

**Task:** `MVP-1.3-US57-POD-PRODUCT-DECISIONS-001`  
**Date:** 2026-08-29  
**Status:** `PRODUCT_DECISIONS_COMPLETE`  
**Production implementation:** `NOT_STARTED`  
**Migration baseline:** Flyway `V46`; a future US-57 schema change must use the next available forward migration, expected `V47`

## 1. Authority

Authority was applied in this order:

1. `docs/requirements/Traspotation & logistic.docx`
2. `docs/mvp/MVP-1.3-DELIVERY-OPERATIONS-CONTRACT-001.md` and accepted US-56 decisions
3. accepted US-56 implementation and final-acceptance evidence
4. `docs/requirements/US-51-US-60-UseCase-Activity-Sequence-Diagrams.md`
5. roadmap, status, traceability and central knowledge-base documents
6. explicit product/architecture decisions in this document

US-56 remains `COMPLETE`. US-57 remains `NOT_STARTED`. This document freezes decisions only and authorizes no production code, migration, endpoint, event producer, or UI.

## 2. Source Findings

- `SOURCE_DEFINED`: US-57 actor is Rider/Courier and its purpose is to capture proof that establishes delivery completion.
- `SOURCE_DEFINED`: the supported primary evidence types are signature, photo and barcode.
- `SOURCE_DEFINED`: signature/photo/barcode are conditional evidence types; the source explicitly warns against treating all three as universally mandatory.
- `SOURCE_DEFINED`: timestamp is part of the common POD workflow. Geo-tag is captured where available.
- `SOURCE_DEFINED`: configured required proof must validate before successful completion.
- `SOURCE_DEFINED`: US-58, not US-57, owns offline signature/photo capture, secure queueing/synchronization, quality controls, retake controls and consent controls.
- `ALREADY_FROZEN`: Delivery is a Tenant-owned Spring Modulith boundary and uses server-side `CurrentTenant` / `TenantExecutionContext`.
- `ALREADY_FROZEN`: US-56 implements only `DRAFT` and `READY_FOR_ASSIGNMENT`; no assignment target or assignment columns exist.
- `ARCHITECTURAL_CONSTRAINT`: binary evidence must be accessed through a provider-neutral outbound port; Delivery domain/application code must not depend on filesystem, cloud SDK, multipart, JPA blob, or another module's internals.

## 3. Unresolved Source Gaps

The source does not define a universal evidence combination, barcode authority, signature representation, file limits, MIME types, exact timestamp authority, geolocation precision, retention period, malware service, POD record cardinality, correction model, exact API, or the minimum lifecycle extension needed after US-56. Sections below freeze explicit product/architecture decisions for those gaps.

## 4. Frozen Decision Summary

| Area | Classification | Frozen decision |
| :--- | :--- | :--- |
| Operating mode | `DEFERRED_TO_US58` | US-57 is online-only; offline evidence capture/replay belongs to US-58. |
| Primary evidence | `PRODUCT_DECISION` | At least one of `SIGNATURE`, `PHOTO`, or `BARCODE` is required; any combination is allowed. |
| Timestamp | `PRODUCT_DECISION` | Server acceptance time is authoritative UTC instant; optional device capture time is informational/audit-only. |
| Geo-tag | `SOURCE_DEFINED` + `PRODUCT_DECISION` | Capture when available; absence does not block US-57 completion. |
| Storage | `ARCHITECTURAL_CONSTRAINT` | Delivery owns metadata and a Delivery-owned outbound storage port owns binary storage abstraction. |
| POD cardinality | `PRODUCT_DECISION` | One POD aggregate per Delivery Order; one mutable draft followed by one immutable final record. |
| Delivery completion | `SOURCE_DEFINED` + `PRODUCT_DECISION` | Finalizing valid POD atomically changes the Delivery Order to `DELIVERED`. |
| Current eligible state | `PRODUCT_DECISION` | Current US-57 may finalize only a `READY_FOR_ASSIGNMENT` order; this is an explicit transitional path, not an assignment claim. |
| Retention | `ARCHITECTURAL_CONSTRAINT` | External organizational policy; US-57 hard-codes no retention duration and exposes no deletion. |
| Public event | `OUT_OF_SCOPE` | No cross-module/public event is approved in US-57; internal audit/business facts only. |

## 5. Evidence Model

`ProofOfDelivery` is a Tenant-owned entity associated directly with one Delivery Order. `PodEvidence` is Tenant-owned metadata under the POD aggregate.

Evidence types:

- `SIGNATURE`: a raster representation of a recipient's drawn signature.
- `PHOTO`: an image supporting the physical delivery outcome.
- `BARCODE`: a normalized scanned/entered value matching the Delivery Order number.
- Timestamp and geolocation are POD metadata, not independent file evidence rows.

A POD is valid only when it contains at least one valid primary evidence item and all supplied items pass their type-specific validation.

## 6. Required Evidence Rules

- Minimum primary evidence count: one of signature, photo, or barcode.
- Allowed combinations: any non-empty subset of the three types.
- Maximum: one signature, three photos and one barcode.
- Timestamp: always assigned by the server on final acceptance.
- Geo-tag: accepted when available; missing GPS never fabricates coordinates and does not block completion.
- Empty evidence, a reference-only payload without durable stored content, or any invalid supplied evidence blocks finalization.
- US-57 has no tenant/customer/service-type evidence-policy engine. Such conditional configuration requires separate product authority.

## 7. Signature Policy

- Signature is required only when it is the selected primary evidence or part of the submitted combination.
- When signature evidence is supplied, `signerName` is required, trimmed, and limited to 200 characters.
- `signerRelationship` is optional descriptive text limited to 100 characters; US-57 defines no identity-verification catalogue.
- Representation is a raster image, not biometric data or authoritative stroke/vector telemetry.
- Supported detected MIME types: `image/png` and `image/jpeg`.
- Maximum object size: 2 MiB.
- A draft signature may be replaced before finalization. A finalized signature is immutable.
- US-57 does not perform biometric verification, compare identity documents, or prove that the named signer is the legal recipient.
- Consent capture/versioning and offline signature behavior belong to US-58.

## 8. Photo Policy

- Photo is optional unless it is the chosen primary evidence.
- Minimum when used: one. Maximum: three.
- Supported detected MIME types: `image/jpeg` and `image/png`.
- Maximum object size: 10 MiB per photo.
- US-57 accepts browser camera capture or a user-selected image file; camera-only enforcement is not required.
- EXIF is not trusted as timestamp or location authority. Storage must strip nonessential EXIF metadata before durable acceptance where the storage adapter can do so safely.
- US-57 validates decodability and type/size only. It defines no minimum dimensions, blur detection, computer vision, quality scoring or retake workflow; those quality/retake controls belong to US-58.

## 9. Barcode Policy

- The barcode represents the immutable server-generated Delivery Order number, not a package, consignment, customer reference, OTP, or generated proof token.
- Accepted value is trimmed, converted to uppercase, and must match the target order's `DEL-YYYY-NNNNNN` value exactly.
- Maximum input length is 64 characters; control characters are rejected.
- Only one barcode evidence value may exist per POD. Repeated scans of the same value do not create duplicate evidence.
- A mismatch returns a validation error and blocks finalization.
- US-57 does not generate or print barcodes. Manual browser entry is an allowed fallback when no scanner integration exists.

## 10. Timestamp Policy

- `acceptedAt` is mandatory and is generated by the server clock when finalization succeeds. It is persisted as an unambiguous UTC instant (`TIMESTAMPTZ` / ISO-8601 offset response).
- `deviceCapturedAt` is optional, supplied by the browser/device, retained only for audit context, and never controls status, ordering, authorization or the authoritative completion time.
- `createdAt` and `updatedAt` follow standard server audit rules.
- Frontend-local time strings are display values only.

## 11. Geolocation Policy

- Latitude and longitude must be supplied together or both omitted.
- Latitude range: `-90` through `90`. Longitude range: `-180` through `180`.
- `accuracyMeters` is optional and, when supplied, must be finite and greater than zero.
- Geolocation capture time may reuse optional `deviceCapturedAt`; server `acceptedAt` remains authoritative.
- Browser geolocation is accepted as device-reported evidence and is not guaranteed to be spoof-resistant.
- Location permission denial, timeout or unavailable capability produces a visible warning but does not block POD when another required evidence item is valid.
- No geofence, destination-radius check or location-masking algorithm is introduced. US-49 geofencing remains deferred.

## 12. Storage Ownership and Upload Strategy

- Delivery owns POD/evidence business metadata, validation and storage references.
- No implemented Document module/public storage contract exists. US-57 therefore uses the preserved Delivery-owned `DeliveryEvidenceStoragePort`, expanded during implementation with store/read operations and provider-neutral content metadata.
- The first adapter may use repository-approved local storage, but domain/application code must remain provider-neutral. No S3, MinIO or cloud dependency is authorized by this decision.
- Binary uploads use multipart through the application API because it is the smallest secure pattern compatible with the current web application. Multipart types remain inside the inbound adapter.
- Downloads/previews stream through an authenticated Delivery API. Public or durable browser-facing storage URLs are forbidden.
- Storage keys are generated server-side. Original filenames never become paths or storage keys.

## 13. Conceptual Persistence Fields

### `proof_of_delivery`

| Field | Decision |
| :--- | :--- |
| `id`, `tenant_id`, `delivery_order_id` | `REQUIRED` |
| `status` (`DRAFT`, `FINALIZED`) | `REQUIRED` |
| `accepted_at`, `accepted_by` | `REQUIRED` on finalization |
| `device_captured_at` | `OPTIONAL`, audit-only |
| `latitude`, `longitude`, `accuracy_meters` | `OPTIONAL`, paired-coordinate rules |
| `signer_name`, `signer_relationship` | `OPTIONAL`; signer name required when signature exists |
| `version`, standard create/update audit columns | `REQUIRED` |
| consent flag/text/version | `DEFERRED_TO_US58` |
| deletion timestamp | `NOT_REQUIRED`; US-57 exposes no deletion |

### `pod_evidence`

| Field | Decision |
| :--- | :--- |
| `id`, `tenant_id`, `proof_of_delivery_id`, `evidence_type` | `REQUIRED` |
| `storage_reference` | `REQUIRED` for signature/photo; absent for barcode |
| `barcode_value` | `REQUIRED` only for barcode |
| `detected_content_type`, `content_length`, `sha256_checksum` | `REQUIRED` for signature/photo |
| `original_filename` | `OPTIONAL`, display/audit only; never trusted as a path |
| `capture_source` (`CAMERA`, `FILE`, `SCANNER`, `MANUAL`) | `REQUIRED` |
| `created_by`, `created_at` | `REQUIRED` |
| EXIF payload | `NOT_REQUIRED` |

## 14. File Integrity and Content Safety

- SHA-256 checksum, detected MIME type and content length are required for each binary object.
- Validate magic bytes/decoded image content; do not trust filename extension or browser-declared MIME type.
- Reject malformed images, unsupported content, unsafe filenames and size-limit violations before they become accepted evidence.
- Cryptographic signing, watermarking and biometric validation are out of scope.
- No approved malware-scanning service exists. Malware scanning is `PLATFORM_DEFERRED`; US-57 does not invent one. If platform scanning later becomes mandatory, unscanned content must remain quarantined and cannot finalize.

## 15. Privacy, Consent and Data Classification

- Delivery/order identifiers and barcode values are business operational data.
- Signer name, signature, photo, timestamp and location are potential personal data; signature and photo are sensitive proof evidence.
- Only same-Tenant users with `DELIVERY_POD_CAPTURE` may create/finalize. Only same-Tenant users with `DELIVERY_POD_VIEW` may view metadata or stream evidence.
- List/detail responses must not expose public storage references, filesystem paths or object keys.
- Evidence views/downloads must be audited with actor, Tenant, POD/evidence ID and time.
- US-57 records no consent flag or text. Source assigns consent controls to US-58; an online consent requirement needs separate explicit product/legal authority.
- Frontend caches must not persist binary evidence or signed/private URLs beyond the active view.

## 16. Retention and Deletion

- No authoritative retention duration exists. `RETENTION_POLICY_EXTERNAL_TO_US57` is frozen.
- US-57 does not hard-code 30-day, annual or seven-year retention and provides no user deletion endpoint.
- Finalized POD/evidence may be removed only by a future authorized retention/privacy process that preserves a tombstone and audit trail. That process is outside US-57.

## 17. Immutability, Corrections and Audit

- Draft POD metadata/evidence may be added, replaced or removed by an authorized capture user while the optimistic version matches.
- Finalization makes the POD and all attached evidence immutable.
- Finalized metadata, binary evidence and barcode values cannot be edited, replaced or deleted by US-57.
- A future correction requires an explicitly authorized replacement/supersession record; no correction endpoint is introduced now.
- Required audit actions: `POD_DRAFT_CREATED`, `POD_EVIDENCE_ADDED`, `POD_EVIDENCE_REPLACED`, `POD_FINALIZED`, `POD_VIEWED`, `POD_EVIDENCE_DOWNLOADED`.
- Audit entries contain Tenant, actor, target IDs, time, evidence type and checksum where applicable; they never contain binary content.

## 18. POD and Delivery Lifecycle

POD lifecycle:

```text
DRAFT -> FINALIZED
```

- One POD exists per Delivery Order. Concurrent creation is protected by tenant-scoped uniqueness and optimistic locking.
- A draft may be resumed online. A finalized POD rejects all mutation and additional evidence.
- US-57 adds `DELIVERED` to the production Delivery lifecycle only when the implementation task is accepted.
- Transitional US-57 completion path: `READY_FOR_ASSIGNMENT -> DELIVERED` during POD finalization.
- This transitional path does not assert assignment, rider ownership, trip execution, or `OUT_FOR_DELIVERY`. Those facts remain unavailable after US-56 and must not be fabricated.
- `DRAFT` Delivery Orders are ineligible. Already `DELIVERED` orders reject duplicate finalization.
- When a later story introduces an authoritative assignment/execution model, POD eligibility must be narrowed to that active execution state through a separately approved forward change.

## 19. Completion Semantics and Failure Atomicity

- Finalizing a valid POD atomically persists final POD metadata and changes the Delivery Order to `DELIVERED` with the same authoritative `acceptedAt` instant.
- Binary evidence must already exist durably and pass integrity checks before finalization begins.
- External/object storage and database writes are not claimed as a distributed atomic transaction. A storage failure returns `503`, leaves the Delivery Order unchanged, and leaves the POD in a retryable draft state.
- Database finalization failure leaves durable evidence unaccepted; cleanup/reconciliation may remove unreferenced draft objects after a safe grace period defined by operations, without deleting finalized evidence.
- No POD may appear finalized when any required evidence is missing, unreadable or unverified.

## 20. API Contract

All routes use the existing controller convention `/v1/deliveries`; deployment may expose `/api/v1/deliveries`. Requests never accept `tenantId`.

| Method and path | Permission | Responsibility | Success |
| :--- | :--- | :--- | :--- |
| `POST /v1/deliveries/{deliveryId}/proof` | `DELIVERY_POD_CAPTURE` | Create the single draft POD with optional device time/geo/signer metadata and Delivery `version` | `201` draft detail |
| `GET /v1/deliveries/{deliveryId}/proof` | `DELIVERY_POD_VIEW` | Return POD metadata and authorized evidence summaries, never storage paths/public URLs | `200` |
| `POST /v1/deliveries/{deliveryId}/proof/evidence` | `DELIVERY_POD_CAPTURE` | Multipart upload of one signature/photo or JSON barcode value with POD `version` | `201` evidence summary |
| `DELETE /v1/deliveries/{deliveryId}/proof/evidence/{evidenceId}` | `DELIVERY_POD_CAPTURE` | Remove draft evidence only, with POD `version` | `204` |
| `GET /v1/deliveries/{deliveryId}/proof/evidence/{evidenceId}/content` | `DELIVERY_POD_VIEW` | Authenticated same-Tenant streaming/preview | `200` |
| `POST /v1/deliveries/{deliveryId}/proof/finalize` | `DELIVERY_POD_CAPTURE` | Validate evidence, finalize POD and atomically mark Delivery `DELIVERED`; requires Delivery and POD versions | `200` final POD/Delivery result |

Tenant-safe missing or cross-Tenant IDs use `404`. Standard authentication/authorization and error envelopes apply. Candidate business codes are `POD_NOT_FOUND`, `POD_ALREADY_EXISTS`, `POD_ALREADY_FINALIZED`, `POD_REQUIRED_EVIDENCE_MISSING`, `POD_EVIDENCE_INVALID`, `POD_EVIDENCE_LIMIT_EXCEEDED`, `POD_MEDIA_TYPE_UNSUPPORTED`, `POD_FILE_TOO_LARGE`, `POD_BARCODE_MISMATCH`, `POD_LOCATION_INVALID`, `POD_DELIVERY_STATE_INELIGIBLE`, `POD_VERSION_CONFLICT`, and `POD_STORAGE_UNAVAILABLE`.

## 21. RBAC

The smallest frozen permission set is:

- `DELIVERY_POD_CAPTURE`: create/resume a draft, add/replace/remove draft evidence, and finalize POD. This is a Rider operational permission and does not grant general Delivery mutation.
- `DELIVERY_POD_VIEW`: view POD metadata and stream/download evidence. This is separate because proof contains personal and location data.

No `UPDATE`, `DELETE`, or separate `FINALIZE` permission is introduced. Backend enforcement and same-Tenant filtering are mandatory; frontend visibility is advisory.

## 22. Tenant Isolation and Security Controls

- Resolve Tenant exclusively from `CurrentTenant` / `TenantExecutionContext` and active server-side membership.
- Every POD/evidence row, repository operation, storage key namespace, audit record and cache key is Tenant-scoped.
- Direct-ID lookup is constrained by `(tenant_id, delivery_id/pod_id/evidence_id)` and must not leak cross-Tenant existence.
- Server-generated opaque storage keys prevent path traversal. Original filenames are sanitized display metadata only.
- Content sniffing/decoding, limits and checksum validation mitigate MIME spoofing and malformed/oversized uploads.
- Barcode values are treated as data, length-bounded, normalized and never evaluated or interpolated into queries.
- Browser geolocation is evidence, not an anti-spoof guarantee.
- Evidence streaming requires authorization on every request and uses restrictive content-disposition/content-type headers.

## 23. US-57 / US-58 Boundary

`US57_ONLINE_ONLY` and `US58_OFFLINE_EXTENSION` are frozen.

US-57 owns online POD draft/finalization, primary evidence validation, authoritative server timestamp, optional device-reported geo-tag, tenant/privacy enforcement and the minimal completion transition.

US-58 owns offline queueing, store-and-forward, retry/idempotent replay, sync conflicts, offline signature/photo capture, photo quality, retake behavior, offline consent controls and offline evidence synchronization through US-71.

US-57 does not attach POD to the existing offline queue. US-58 may extend the US-57 model without weakening finalized evidence immutability or Tenant validation.

## 24. Frontend Contract and Device Fallbacks

- Add a responsive **Proof of Delivery** section to Delivery Details only during implementation.
- Show current POD state, evidence summaries, privacy notice, add/replace/remove draft actions and finalization confirmation according to permissions.
- Signature: browser canvas may produce a raster image; file upload fallback is allowed.
- Photo: browser camera/file chooser may be used; file upload fallback is allowed when camera access is unavailable.
- Barcode: scanner integration is optional; manual entry is required as fallback.
- Geolocation: request browser location once; denial/timeout displays a non-blocking warning and permits finalization with valid primary evidence.
- Map backend field errors into the form. Storage failure, stale version, unsupported media and barcode mismatch must remain distinguishable.
- No local/offline evidence persistence, background sync, quality/retake or consent UI is introduced by US-57.

## 25. Validation Matrix

| Scenario | Result |
| :--- | :--- |
| Valid POD with at least one valid primary evidence | `ACCEPT` / `200` finalize |
| Missing all primary evidence | `422` |
| Missing signer name or invalid signature image | `422` |
| Oversized photo/signature | `413` (or existing envelope equivalent) |
| Unsupported/detected media mismatch or malformed image | `415` / `422` |
| Invalid or mismatched barcode | `422` |
| Duplicate signature/barcode or more than three photos | `409` / `422` |
| Coordinates incomplete or outside range | `422` |
| Location unavailable/permission denied | Accept with warning when primary evidence is valid |
| Delivery/POD/evidence missing or cross-Tenant | `404` |
| Delivery `DRAFT` or another ineligible state | `409` |
| Duplicate POD/finalization | `409` |
| Stale Delivery or POD version | `409` |
| Evidence storage unavailable | `503`; Delivery remains unchanged and POD not finalized |
| Malware scan failure | Not active in US-57; future scanner must reject/quarantine |
| Unauthenticated | `401` |
| Missing capture/view permission | `403` |

## 26. Architecture and Event Boundary

- Dependency direction remains `domain <- ports/application <- adapters`.
- `ProofOfDelivery`, evidence policy and finalization rules are framework-free Delivery domain behavior.
- Multipart, browser data, persistence and concrete file storage remain adapters.
- No direct access to Document, Offline Sync, Organization, Trip, Freight or another module's repositories/tables/services is permitted.
- US-57 records internal provider-neutral `ProofOfDeliveryFinalized` and `DeliveryCompleted` business facts through an outbound event/audit port. No public integration event or external consumer is approved; public registration is deferred until an actual consumer and exact delivery contract exist.
- US-61 may later read source facts but US-57 implements no analytics, dashboards or KPI formulas.

## 27. Future Migration and Acceptance Contract

The implementation is expected to create forward-only `V47` after rechecking the migration head. It must not edit V1–V46. Expected schema includes Tenant-scoped `proof_of_delivery`, `pod_evidence`, indexes/uniqueness, audit/version columns, a `DELIVERED` Delivery status check extension, and seeds for `DELIVERY_POD_CAPTURE` and `DELIVERY_POD_VIEW`.

Future acceptance must cover:

- domain evidence/cardinality/finalization/immutability rules;
- Tenant A/B isolation and tenant-safe direct-ID behavior;
- both permissions and unauthorized streaming protection;
- signature/photo/barcode, timestamp and geolocation validation;
- size, MIME, malformed image, checksum, unsafe filename and barcode-injection controls;
- optimistic concurrency and duplicate finalization;
- storage failure without false completion;
- PostgreSQL V1–V47 migration and tenant-scoped uniqueness;
- frontend validation, permission visibility, evidence preview and distinct errors;
- Chromium successful online POD capture plus missing-evidence and authorization negative coverage.

US-58 offline scenarios are prohibited from the US-57 acceptance claim.

## 28. Explicit Exclusions and Readiness

Excluded: offline capture/sync, quality scoring, retake and consent (US-58); failed attempts (US-59); re-delivery (US-60); analytics (US-61); Delivery exceptions/OTP mismatch (US-62); Last-Mile assignment/tracking (US-63–70); geofencing (US-49); mobile apps (US-76); barcode generation; identity/biometric verification; cryptographic proof signing; malware-service implementation; retention/deletion workflow; public POD event publication.

All implementation-critical US-57 decisions are frozen. `MVP-1.3-US57-PROOF-OF-DELIVERY-001` is authorized to implement only this online POD scope. US-57 remains `NOT_STARTED` until production implementation and acceptance succeed. MVP 1.3 remains `1/7 COMPLETE`; overall completion remains `51/87`.
