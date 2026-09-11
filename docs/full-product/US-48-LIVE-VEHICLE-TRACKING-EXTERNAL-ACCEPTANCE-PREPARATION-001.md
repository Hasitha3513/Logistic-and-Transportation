# US-48 Live Vehicle Tracking External Acceptance Preparation

**Task:** `US-48-LIVE-VEHICLE-TRACKING-EXTERNAL-ACCEPTANCE-PREPARATION-001`  
**Result:** `PREPARATION_COMPLETE`  
**Real capture:** `NOT_EXECUTED / BLOCKED_EXTERNAL_SYSTEM`  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Flyway:** V76; no V77  
**Accounting:** 72 / 87 complete; 15 / 87 remaining

This is an operator runbook, not real-provider evidence. Complete every prerequisite with one physical
Teltonika FMC130 and genuine Flespi-generated telemetry before executing the capture task. Controlled
fixtures, simulator messages, REST-injected Flespi messages, screenshots from another device, and copied
historical results do not satisfy this gate.

## 1. Hardware and external-service prerequisites

- [ ] Physical FMC130 is on hand; record only a masked IMEI/serial in evidence.
- [ ] Device SKU/LTE bands are compatible with the selected Sri Lankan operator.
- [ ] Safe fused power/test bench, antennas and device documentation are available.
- [ ] Activated data SIM, confirmed APN (and username/password only if required), outbound data and
      adequate LTE/GNSS coverage are available.
- [ ] Device has GNSS visibility and produces a fresh physical position.
- [ ] Active Flespi account, Teltonika channel and FMC130 device exist.
- [ ] The Flespi device identity is bound to the physical tracker identity and messages are visible in
      Flespi **Logs & Messages**.
- [ ] A least-privilege ACL token can read only the acceptance device and its messages; the master token
      is not used by the application.
- [ ] `transport_logistics_acceptance` is available and positively identified. The development database
      must not be used as authoritative or destructive acceptance evidence.

Official setup references: [FMC130 first start](https://wiki.teltonika-gps.com/view/FMC130_First_Start),
[Flespi Teltonika setup](https://flespi.com/blog/teltonika-device-data-via-api),
[physical tracker quick start](https://flespi.com/kb/quick-start-guide-physical-tracker), and
[Flespi ACL tokens](https://flespi.com/kb/tokens-access-keys-to-flespi-platform).

## 2. FMC130 and Flespi setup

1. In the Teltonika configurator, configure the SIM APN and the unique host/port shown by the real
   Flespi Teltonika channel. Select TCP/UDP and TLS only as supported by that channel/device setup;
   do not copy endpoint values from documentation screenshots. Configure a short, safe acceptance
   reporting interval and reboot the device after changing its server.
2. In Flespi, create a channel using the `teltonika` protocol, then create a device using the actual
   FMC130 device type and physical identifier. Verify genuine tracker traffic in **Logs & Messages**.
3. Create an expiring, least-privilege ACL token restricted to GET access for the selected Flespi device
   and its message/telemetry resources. Do not grant platform administration or unrelated devices.
4. Export the token only in the acceptance application's process environment, for example
   `US48_FLESPI_ACCEPTANCE_TOKEN=<secret>`. Configure the application with the opaque reference
   `env:US48_FLESPI_ACCEPTANCE_TOKEN`. The resolver accepts only `env:[A-Z][A-Z0-9_]{0,126}`.
   Never put the token in SQL, source, Markdown, screenshots, shell history, request bodies, logs or UI.

## 3. Provider connection through the implemented UI

Use an authenticated same-Tenant user with `TRACKING_DEVICE_MANAGE`.

1. Open **Tracking > Provider Connections** and choose **Add Provider Connection**.
2. Enter provider type `FLESPI`, a Tenant-unique display name and provider alias, and a new opaque
   provider key ID. Use endpoint `https://flespi.io` unless an approved provider-controlled
   `https://*.flespi.io` endpoint is required. Only HTTPS, no userinfo/query/fragment, port 443 or the
   default port, and no redirect are accepted.
3. Enter `env:US48_FLESPI_ACCEPTANCE_TOKEN` as the credential reference, not the token. Use poll interval
   5 seconds, page size at most 500, and safe configuration `{}` or `{"overlapSeconds":"300"}`.
4. Save the DRAFT connection, open its details, select **Test Connection**, and require `PASS`.
5. Select **Activate** and confirm lifecycle `ACTIVE`. Confirm that responses and UI expose only
   `credentialConfigured`; the reference and secret must remain absent.

## 4. Device onboarding through the implemented UI

FLESPI does not advertise `DISCOVERY`; manual external identity is mandatory.

1. Open **Tracking > Devices**, select **Add Device**, choose the active Flespi connection, enter the
   real Flespi external device reference accepted by `/gw/devices/{selector}/messages` and returned as
   message `ident` as **External device reference**, optionally
   enter a masked hardware serial reference, and select **Create DRAFT**.
2. Open the DRAFT device. If it is not already bound, select **Bind Provider**, select the active Flespi
   connection, manually enter the same external device reference and select **Bind Provider**.
3. Select **Associate Vehicle** and choose an acceptance Vehicle in the same Tenant. Record the
   effective start time; it must cover the source time of the real point.
4. Require an ACTIVE provider connection, ACTIVE device-provider binding, and current Vehicle
   association. Select **Activate Device** and confirm device lifecycle `ACTIVE`.
5. Enable the existing provider coordinator for the acceptance runtime. Do not add a per-device
   scheduler, legacy Flespi poller or HMAC loopback path.

## 5. Real capture and field-level proof

Generate a new physical point by safely moving the powered FMC130 with GNSS reception. Capture the
provider message in Flespi and the corresponding application record without exposing the token or full
device identity. Execute the companion capture template and prove this implemented mapping:

| Required application fact | Flespi field | Rule |
| :--- | :--- | :--- |
| External identity | `ident` | Must equal the configured external device reference |
| Source timestamp | `timestamp` | Preserved as source time, distinct from receipt time |
| Latitude | `position.latitude` | Numeric WGS84 `[-90,90]` |
| Longitude | `position.longitude` | Numeric WGS84 `[-180,180]` |
| Accuracy | `position.accuracy` | Optional, non-negative metres; otherwise UNKNOWN |
| Speed | `position.speed` | Optional numeric `[0,400]`; preserve observed provider value |
| Heading | `position.direction` | Optional numeric `[0,360)`; 360 is treated as absent |

The current Flespi capability descriptor does **not** advertise ignition, odometer, engine hours,
message identity or sequence. Do not infer or synthesize them. During real execution, record
`NOT_PRESENT_IN_CAPTURE` for an optional fact only after inspecting the genuine message.

Raw payload inspection is temporary acceptance handling outside production persistence. Store only a
sanitized excerpt in the evidence document, mask device identity, remove account/network identifiers,
never retain authorization headers, limit access, and delete any unsanitized capture after review.

## 6. Evidence checklist

- [ ] Photo of the physical FMC130/test setup with IMEI/serial and SIM details masked.
- [ ] Flespi channel/device screenshot showing genuine arrival time and masked identity.
- [ ] Token ACL/expiry screenshot with token value fully hidden.
- [ ] Provider connection list/detail, test PASS and ACTIVE lifecycle screenshots.
- [ ] DRAFT device, ACTIVE binding, same-Tenant Vehicle association and ACTIVE device screenshots.
- [ ] Sanitized genuine Flespi message and field-mapping table.
- [ ] Provider/coordinator sanitized logs proving fetch and normalized ingest without raw payload/secret.
- [ ] Database query output for the same Tenant/device/Vehicle and source time.
- [ ] Live UI evidence for source time, receipt time, WGS84 point, accuracy truth, freshness and
      connectivity.
- [ ] Stale/loss/recovery, disable, rotation, Tenant/RBAC and privacy evidence listed below.

## 7. Safe tenant-scoped PostgreSQL inspection

Run read-only against `transport_logistics_acceptance`. Set psql variables to the acceptance UUIDs; never
select `credential_reference`, raw payloads or unrestricted rows.

```sql
\set tenant_id '00000000-0000-0000-0000-000000000000'
\set device_id '00000000-0000-0000-0000-000000000000'
\set vehicle_id '00000000-0000-0000-0000-000000000000'

SELECT id, provider_key_id, provider_alias, provider_type, display_name, lifecycle,
       test_status, last_tested_at, last_successful_poll_at, last_provider_message_at
FROM tracking_provider_binding WHERE tenant_id = :'tenant_id'::uuid;

SELECT id, lifecycle, last_seen_at, version
FROM tracking_device WHERE tenant_id = :'tenant_id'::uuid AND id = :'device_id'::uuid;

SELECT tracking_device_id, provider_binding_id, lifecycle, watermark_source_timestamp,
       watermark_message_identity, next_poll_at, version
FROM tracking_device_provider_binding
WHERE tenant_id = :'tenant_id'::uuid AND tracking_device_id = :'device_id'::uuid;

SELECT tracking_device_id, vehicle_id, effective_from, effective_to
FROM tracking_vehicle_device_assignment
WHERE tenant_id = :'tenant_id'::uuid AND tracking_device_id = :'device_id'::uuid
ORDER BY effective_from DESC;

SELECT device_id, vehicle_id, provider_alias, source_timestamp, received_at,
       latitude, longitude, horizontal_accuracy_meters, speed_kph, heading_degrees,
       engine_state, trust, quality, ordering_classification
FROM tracking_position
WHERE tenant_id = :'tenant_id'::uuid AND device_id = :'device_id'::uuid
ORDER BY received_at DESC, id DESC LIMIT 20;

SELECT vehicle_id, latest_received_position_id, latest_trusted_position_id,
       last_successful_receipt_at
FROM tracking_vehicle_latest
WHERE tenant_id = :'tenant_id'::uuid AND vehicle_id = :'vehicle_id'::uuid;
```

## 8. Loss, recovery, security and operations

After a confirmed trusted point, pause real device transmission while leaving the application running.
Prove freshness changes from LIVE (source age <=60s) to RECENT (>60s and <=5m) to STALE (>5m), while
connectivity changes independently from CONNECTED (receipt age <=60s) to DEGRADED (>60s and <=5m) to
OFFLINE (>5m). Confirm LAST KNOWN and the original source timestamp remain visible. Restore telemetry and
prove CONNECTED/LIVE recovery without restart.

Disable the device binding and then the provider connection separately; prove polling/ingestion stops and
last-known history remains readable. Reactivate only through supported lifecycle commands. Rotate the
environment secret by exporting a new environment-backed reference, update the connection's replacement
credential reference, test PASS, and prove that the next execution uses it without restart and without any
credential appearing in UI, API, logs, audit or SQL output.

Prove Tenant B cannot read or mutate Tenant A's provider/device/location facts. Prove a same-Tenant user
without `TRACKING_DEVICE_MANAGE` cannot manage connections/devices and a user without `TRACKING_VIEW`
cannot view precise location. Confirm no Driver PII, Customer fleet-location exposure, raw provider
payload, full external device identity or credential appears.

Existing controlled-provider throughput establishes platform capacity only. The real-device run records
FMC130-to-Flespi fidelity, latency and connectivity; its provider arrival rate is not a substitute for the
existing platform throughput gate, and the controlled throughput figures are not physical-source proof.

## 9. Rollback and execution gates

Rollback is operational: disable the Flespi provider connection or provider coordinator, keep the device
and immutable history for audit, and preserve V76. Do not introduce V77, revert historical migrations,
enable the retired singleton Flespi scheduler, create dual polling, or use the signed public ingress as an
internal loopback.

Preparation is PASS when this runbook and both templates are complete, exact current contracts are
recorded, secrets are placeholder-only, and the roadmap/knowledge base point to the capture task.
Real capture remains BLOCKED until the physical FMC130, SIM/connectivity, active Flespi channel/device,
scoped resolvable credential and genuine provider-generated telemetry all exist. Any mapping-only
correction may remain in the Flespi adapter; any need for a migration, public API, permission, Tenant/trust
contract or architecture change is a mandatory stop and requires separate authority.

Execute in this exact order:

1. `US-48-FMC130-FLESPI-EXTERNAL-CAPTURE-001`
2. `US-48-LIVE-VEHICLE-TRACKING-FINAL-ACCEPTANCE-001`
3. Only after final acceptance PASS, update US-48 to COMPLETE and unblock the approved next queue. Do not
   start US-49 before that PASS.
