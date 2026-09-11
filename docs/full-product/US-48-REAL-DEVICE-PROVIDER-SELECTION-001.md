# US-48 Real Device and Provider Selection

**Task:** `US-48-REAL-DEVICE-PROVIDER-ACQUISITION-001`  
**Research date:** 2026-09-09  
**Decision:** Select Teltonika FMC130 + customer-supplied LTE SIM + flespi for the one-device acceptance pilot  
**US-48:** `IMPLEMENTATION_COMPLETE / ACCEPTANCE_BLOCKED_EXTERNAL_SYSTEM`  
**Accounting:** unchanged at 72 / 87 complete; 15 / 87 remaining  
**Flyway:** V74; no V75 authorized

## Recommendation

Purchase one **Teltonika FMC130** and connect it to a **flespi** developer account. The FMC130 is an installable vehicle tracker with LTE Cat 1, GNSS, ignition/input support and Teltonika Codec 8/8E records. Its server address, port and TCP/UDP transport are configurable; TLS is supported. flespi supplies the commercial provider hop: it receives the physical device's native protocol, produces provider-generated normalized JSON, retains device messages, and exposes them through token-authorized REST, MQTT and webhook/stream facilities. The US-48 edge adapter would authenticate flespi, map its JSON to the already-frozen normalized Tracking command, and invoke the existing signed production ingress without placing vendor types in the domain.

This combination best fits a one-device engineering acceptance because the hardware is sold singly and the flespi Free plan is intended for development and permits up to ten connected devices. The current observed retail range is approximately **€57.52–€104.05 plus shipping**; SIM/data cost is separate. The flespi developer tier is **€0**, while its published commercial tier starts around **€130/month for 1,000+ devices**. Prices, cellular bands, tax, shipping and availability must be reconfirmed for Sri Lanka before ordering.

Primary evidence: [FMC130 documentation](https://wiki.teltonika-gps.com/index.php?title=FMC130), [FMC130 first-start/server configuration](https://wiki.teltonika-gps.com/view/FMC130_First_Start), [FMC130 LTE/GNSS datasheet](https://wiki.teltonika-gps.com/images/b/b5/DS-FMC130.pdf), [flespi platform](https://flespi.com/), [flespi REST API](https://flespi.com/rest-api), [flespi limits](https://flespi.com/en/docs/restrictions), and [current FMC130 retail comparison](https://www.idealo.de/preisvergleich/OffersOfProduct/208470864_-gps-tracker-finder-auto-schwarz-fmc130-teltonika.html).

## Candidate matrix

| Rank | Hardware | Provider/platform | Approximate pilot cost | Delivery/API class | Authentication | Source timestamp | Accuracy | Adapter | One-device practical | Recommendation |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| 1 | Teltonika FMC130 | flespi | €58–€104 hardware; SIM extra; Free developer platform | MQTT + REST + webhook/stream | Scoped flespi token; TLS; separate device channel | YES | Device GNSS specification `<3 m`; emitted accuracy field must be confirmed in captured flespi payload | 1 | YES | **SELECT** |
| 2 | Digital Matter Oyster3 Cellular | Telematics Guru | Public reseller example $149.95; SIM/platform pricing by partner | API/platform integration; outbound webhook availability must be confirmed | Account/API credentials over TLS | YES (`DateUTC`) | Device ~2 m CEP; API field mapping must be confirmed | 1–2 | YES, sample units available | Strong battery-powered alternative, but partner onboarding/pricing is less transparent |
| 3 | Geotab GO9 | MyGeotab | Indicative $58–$80 hardware or bundled plan; roughly $17–$29/month in published public-contract examples | REST feed/history; effectively polling for telemetry | MyGeotab credentials/session/token controls | YES | UNKNOWN in standard location record | 1–2 | POSSIBLE through reseller | Excellent API and fleet data, slower procurement and recurring contract dependency |
| 4 | Queclink GV57MG | Wialon Hosting/provider | Quote/reseller dependent; SIM and Wialon subscription extra | Real-time update channel + REST-like Remote API | Scoped Wialon access token | YES (`t` message time; `rt` registration time) | NOT DOCUMENTED in standard Wialon position object | 1–2 | POSSIBLE, provider/reseller needed | Good protocol/platform breadth; commercial onboarding and exact hardware cost are uncertain |
| 5 | Samsara VG54/VG34 | Samsara cloud | Quote required; typically bundled hardware/subscription | REST polling/feed; high-rate streaming may require Kafka/enterprise arrangement | Scoped bearer API token; webhook IP allowlisting for supported events | YES | UNKNOWN from public core GPS examples | 2 | LOW–MEDIUM | Secure and capable, but excessive cost/contract and integration scope for one acceptance unit |

Cost figures are planning estimates, not quotes. Digital Matter explicitly supports small sample purchases but directs buyers to partners; its [Oyster3 page](https://www.digitalmatter.com/devices/oyster3/) documents LTE-M/NB-IoT, GNSS and sample availability, while this [public single-unit listing](https://www.lonestartracking.com/tracking-devices/oyster3-4g-5g-waterproof-battery-powered-gps-tracking-device/) provides the $149.95 observation. Geotab documents the [GO9 LTE device](https://support.geotab.com/go-devices/go9/doc/go9-document), its [Device API](https://developers.geotab.com/myGeotab/apiReference/objects/Device/index.html), and reseller pricing model in its [FAQ](https://www.geotab.com/faq/). Queclink documents [GV57MG LTE Cat M1/NB2 hardware](https://www.queclink.com/wp-content/uploads/2021/10/GV57MG-20211027.pdf); Wialon documents [tokens](https://help.wialon.com/en/api/user-guide/getting-an-access-token), [message time versus registration time](https://help.wialon.com/en/api/user-guide/data-format/messages), and [real-time update consumption](https://help.wialon.com/en/api/user-guide/api-reference/events). Samsara documents its [vehicle gateways and REST telemetry](https://developers.samsara.com/docs/rest-api-overview), [five-second telemetry feed](https://developers.samsara.com/docs/telematics), and [bearer-authorized webhook management](https://developers.samsara.com/reference/postwebhooks).

## Weighted compatibility scores

Scores are 1 (poor/unknown) through 5 (excellent) based on current public evidence. “Lock-in” is scored higher when lock-in is lower.

| Candidate | Device | Docs | Push/API | Real-time | Source time | Accuracy | Security | Integration | One unit | Developer | Cost | Lock-in | Scale | Total / 65 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| FMC130 + flespi | 5 | 5 | 5 | 5 | 5 | 4 | 4 | 5 | 5 | 5 | 5 | 4 | 5 | **62** |
| Oyster3 + Telematics Guru | 4 | 4 | 3 | 4 | 5 | 4 | 4 | 3 | 4 | 3 | 3 | 3 | 5 | **49** |
| GO9 + MyGeotab | 4 | 5 | 3 | 4 | 5 | 2 | 5 | 4 | 3 | 4 | 2 | 2 | 5 | **48** |
| GV57MG + Wialon | 4 | 4 | 4 | 5 | 5 | 2 | 4 | 4 | 3 | 3 | 3 | 3 | 5 | **49** |
| Samsara gateway + cloud | 5 | 5 | 3 | 5 | 5 | 2 | 5 | 3 | 2 | 3 | 1 | 1 | 5 | **45** |

## Top-three assessment

### 1. Teltonika FMC130 + flespi

- **Fit:** readily purchasable hard-wired LTE tracker; physical GNSS/ignition telemetry; protocol parser, normalized messages, REST/MQTT and webhook capacity; free development tier.
- **Expected effort:** roughly 2–4 engineering days after hardware/SIM activation for device channel configuration, payload capture, Level-1 adapter mapping, secrets/TLS configuration and acceptance rehearsal. This is an estimate, not a vendor commitment.
- **Risks:** select the FMC130 regional modem variant only after verifying local LTE bands; installation needs vehicle power and should be fused correctly; a public TLS endpoint is required; flespi's precise webhook delivery/authentication behavior and emitted accuracy/message identity fields must be validated with the real device capture.
- **Procurement:** buy one FMC130 from an authorized/local telemetry reseller or a reputable single-unit retailer; acquire a data SIM with compatible LTE bands and APN details; register a free flespi account and create a Teltonika channel/device.
- **Adapter:** Level 1. A Tracking inbound adapter maps flespi JSON and performs controlled outbound signing to the existing production endpoint. No domain/schema/public-human-API change.

### 2. Digital Matter Oyster3 + Telematics Guru

- **Fit:** rugged LTE-M/NB-IoT physical GNSS unit with real regional cloud and explicit source `DateUTC`; good for a non-powered test asset.
- **Expected effort:** roughly 3–6 engineering days, dominated by partner/account provisioning and confirming outbound data access.
- **Risks:** battery-oriented reporting may be less convenient for rapid LIVE/disconnect/reconnect testing; pricing and Telematics Guru partner access are quote-based; the publicly documented third-party API is primarily inbound to TG, so outward webhook/API rights must be confirmed before purchase.
- **Procurement/signup:** request a sample and regional partner from Digital Matter; obtain an LTE-M/NB-IoT SIM if not bundled; request Telematics Guru API/outbound integration access in writing.
- **Adapter:** Level 1 if outbound API/webhook is granted, otherwise Level 2.

### 3. Queclink GV57MG + Wialon Hosting provider

- **Fit:** compact waterproof LTE-M/NB-IoT tracker and mature provider API. Wialon distinguishes message time `t` from registration time `rt`, directly supporting US-48 source/receipt semantics.
- **Expected effort:** roughly 3–6 engineering days after a Wialon provider account and unit are active.
- **Risks:** local reseller/platform subscription and exact protocol onboarding must be arranged; accuracy is not explicit in the standard public Wialon position object; real-time integration is session/update-channel oriented rather than a simple position webhook.
- **Procurement/signup:** request a single GV57MG from a Queclink distributor and a one-unit Wialon Hosting plan from a local Wialon partner; obtain a least-privilege token.
- **Adapter:** Level 1–2 depending on whether the chosen Wialon provider exposes a straightforward outbound stream.

## Security and privacy review

For the selected route, device-to-flespi transport should use Teltonika TLS where the selected firmware/channel supports it. The adapter must use a narrowly scoped flespi ACL token held only in the approved runtime secret store. The token must never be committed, inserted in `tracking_provider_binding`, printed, placed in screenshots, or returned by an API. Rotate it after acceptance if it was exposed during manual setup. Restrict the token to the selected channel/device and read/stream operations; restrict any webhook source by signature/token and, where stable addresses are published, network allowlisting.

flespi documents isolated namespaces, ACL tokens and REST/MQTT APIs, but the final security review must verify the exact Free-account region, data hosting, retention, deletion, credential rotation and webhook authentication settings visible in the created account. Public evidence supports 30-day/3 GB log retention limits for Free accounts, but that must not be treated as US-48's legal retention policy. Device IMEI/serial, coordinates and provider tokens are sensitive operational data; documentation and final evidence must use masked identifiers.

## Integration plan

```text
Teltonika FMC130 physical tracker
        ↓ Teltonika Codec 8/8E over cellular TLS
Customer data SIM / LTE network
        ↓
flespi Teltonika channel and device
        ↓ normalized provider-generated JSON via MQTT/webhook/REST
Provider-specific Tracking edge adapter (Level 1)
        ↓ existing HMAC canonical request
POST /api/integration/v1/tracking/positions
        ↓ existing provider binding / Tenant / nonce / association chain
PostgreSQL V74 Tracking state in transport_logistics_acceptance
        ↓
US-48 operator UI
```

The edge adapter is a separately authorized follow-up if flespi cannot emit the application's exact HMAC contract. It belongs in the Tracking inbound-adapter layer, may translate only external authentication/payload representation, and must not add provider fields to the domain/database, route packets through US-73 persistence, alter Tenant authority, or create V75. This research task does not authorize implementing it.

## Procurement and activation checklist

- [ ] Confirm Sri Lankan operator coverage and supported LTE bands; select the matching FMC130 regional SKU.
- [ ] Obtain a written single-unit hardware quote, stock status, warranty, shipping cost and lead time.
- [ ] Order one FMC130 from an authorized/reputable supplier.
- [ ] Obtain a low-volume data SIM and record APN details outside Git.
- [ ] Install with correct fused vehicle power/ground and safe ignition input, or use a suitable bench harness.
- [ ] Create a flespi developer account and confirm its region/data handling.
- [ ] Create one Teltonika channel and one device using the masked IMEI/identifier.
- [ ] Configure APN, flespi server/port, TCP/TLS and a short acceptance reporting interval.
- [ ] Drive/move the physical unit and verify real messages in flespi Toolbox.
- [ ] Capture one sanitized provider-generated JSON schema sample; do not store token, full IMEI or unnecessary coordinates in Git.
- [ ] Confirm source timestamp, coordinates, device identity, message identity/sequence, accuracy, speed, heading and ignition field availability from the real capture.
- [ ] Confirm MQTT/webhook/REST delivery behavior, retry/replay controls and rate limits.
- [ ] Create a least-privilege flespi token in the approved secret manager and test rotation.
- [ ] Obtain a public HTTPS acceptance endpoint with valid DNS/certificate/firewall configuration.
- [ ] Authorize and implement only the required Level-1 inbound adapter if direct canonical HMAC delivery is unavailable.
- [ ] Configure the existing Tracking provider binding with an opaque provider key and credential reference.
- [ ] Register the physical device and associate it with a same-Tenant acceptance Vehicle.
- [ ] Use only `transport_logistics_acceptance`; never use the development database as authoritative evidence.
- [ ] Run `US-48-LIVE-VEHICLE-TRACKING-EXTERNAL-ACCEPTANCE-PREPARATION-001`.
- [ ] Then rerun `US-48-LIVE-VEHICLE-TRACKING-FINAL-ACCEPTANCE-001`.

## Remaining blockers and go/no-go checkpoints

US-48 remains blocked until the device is physically obtained, the SIM and flespi account are active, real payload fields are captured, provider delivery can reach the TLS acceptance endpoint, and the authentication bridge is approved and operational. Before ordering, require a **GO** on regional LTE bands and a **GO** that the account exposes real device messages via MQTT/webhook/REST. After the first capture, require a separate **GO** that the adapter is Level 1 or 2; if satisfying the provider contract would require new domain semantics, a public API change, V75, or a weakened trust boundary, stop and request a new product/architecture decision.

Selection is not acquisition and does not satisfy `REAL_DEVICE_REAL_PROVIDER`. No story accounting changes, and US-49 must not start.
