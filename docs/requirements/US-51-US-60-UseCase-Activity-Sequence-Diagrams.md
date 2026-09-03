# Transport & Logistics Management System

## UML Diagrams — US-51 to US-60

This document consolidates UML diagrams for **US-51 through US-60** based on the supplied Transport & Logistics requirements. Each story includes a **Use Case Diagram**, **Activity Diagram**, and **Sequence Diagram** in PlantUML.

> Scope: US-51–US-55 complete the GPS & Real-Time Tracking set covered by this range. US-56–US-60 cover Delivery Orders, Proof of Delivery, offline signature/photo capture, failed deliveries, and re-delivery scheduling.

---

# US-51 — Monitor Idle Time

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** Measure idle duration and compare engine-on time with movement so avoidable fuel waste can be identified.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "Monitor Idle Time" as U0
 usecase "Measure Idle Duration" as U1
 usecase "Compare Engine-On Time vs Movement" as U2
 usecase "Estimate Fuel Waste During Idle" as U3
 usecase "Validate Telemetry Availability" as U4
 usecase "Record Idle Event" as U5
 usecase "View Idle History" as U6
}
TO --> U0
TO --> U6
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U3 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-51 — Monitor Idle Time
start
:Receive Vehicle Telemetry;
:Validate Engine State and Movement Data;
if (Telemetry sufficient?) then (Yes)
 :Compare Engine-On State vs Movement;
 if (Engine on and vehicle stationary?) then (Yes)
  :Start / Continue Idle Timer;
  :Measure Idle Duration;
  if (Fuel data / assumption available?) then (Yes)
   :Estimate Fuel Waste During Idle;
  endif
  :Record Idle Event;
 else (No)
  :Reset / Close Idle Event if active;
 endif
else (No)
 :Mark Idle Status as Unknown / Unconfirmed;
endif
:Update Idle Monitoring View;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-51 — Monitor Idle Time — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "Idle Monitoring UI" as UI
participant "Idle Monitoring Service" as IS
participant "Telemetry Stream" as TS
database "Idle Event Repository" as IR
participant "Fuel Estimation Service" as FE
TS -> IS: Engine state + movement + timestamp
IS -> IS: Validate telemetry and detect idle
alt Idle detected
 opt Fuel data available
  IS -> FE: Estimate idle fuel waste
  FE --> IS: Estimated waste
 end
 IS -> IR: Save / update idle event
 IS --> UI: Idle duration + fuel estimate
else Not idle / insufficient data
 IS --> UI: Normal movement or unknown status
end
TO -> UI: Review idle events
@enduml
```

---

# US-52 — Monitor Route Deviations

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** Compare planned and actual routes, calculate deviation severity, and support approval so significant route deviations are controlled.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "Monitor Route Deviations" as U0
 usecase "Compare Planned vs Actual Route" as U1
 usecase "Calculate Deviation Severity" as U2
 usecase "Estimate Fuel Waste from Deviation" as U3
 usecase "Approve Route Deviation" as U4
 usecase "Record Route Deviation" as U5
 usecase "Escalate Significant Deviation" as U6
}
TO --> U0
TO --> U4
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U5 : <<include>>
U3 .> U0 : <<extend>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-52 — Monitor Route Deviations
start
:Load Planned Route;
:Receive Actual Vehicle Position;
:Compare Planned vs Actual Path;
if (Deviation detected?) then (Yes)
 :Calculate Deviation Severity;
 if (Fuel impact data available?) then (Yes)
  :Estimate Fuel Waste from Deviation;
 endif
 :Record Route Deviation;
 if (Approval required?) then (Yes)
  :Submit Deviation for Approval;
  if (Approved?) then (Yes)
   :Mark Deviation Approved;
  else (No)
   :Escalate Significant Deviation;
  endif
 endif
else (No)
 :Maintain On-Route Status;
endif
:Update Tracking View;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-52 — Monitor Route Deviations — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "Route Deviation UI" as UI
participant "Deviation Service" as DS
database "Planned Route Repository" as RR
participant "Position Stream" as PS
database "Deviation Repository" as DR
PS -> DS: Actual vehicle position
DS -> RR: Load planned route
RR --> DS: Route geometry
DS -> DS: Compare path + calculate severity
alt Deviation detected
 DS -> DR: Save deviation event
 DS --> UI: Deviation + severity
 TO -> UI: Approve / escalate if required
 UI -> DS: Decision
 DS -> DR: Update approval state
else No deviation
 DS --> UI: On-route status
end
@enduml
```

---

# US-53 — Replay Journeys

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** Replay historical journeys, analyze stops, and investigate incidents so past vehicle behavior can be reconstructed.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "Replay Journeys" as U0
 usecase "Load Historical Journey" as U1
 usecase "Replay Vehicle Movement" as U2
 usecase "Analyze Stops" as U3
 usecase "Investigate Incident" as U4
 usecase "Support Forensic Review" as U5
 usecase "Compare Engine-On vs Movement" as U6
}
TO --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U4 .> U0 : <<extend>>
U5 .> U0 : <<extend>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-53 — Replay Journeys
start
:Open Journey Replay;
:Select Vehicle / Trip / Date Range;
:Load Historical Position Events;
:Validate Retention / Access Permission;
if (Journey data available?) then (Yes)
 :Order Events Chronologically;
 :Replay Vehicle Movement;
 :Identify Stops;
 :Display Stop Analysis;
 if (Incident selected?) then (Yes)
  :Load Related Tracking Events;
  :Investigate Incident;
  :Support Forensic Review;
 endif
 if (Engine data available?) then (Yes)
  :Compare Engine-On vs Movement;
 endif
else (No)
 :Display No Historical Journey Data;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-53 — Replay Journeys — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "Journey Replay UI" as UI
participant "Replay Service" as RS
database "Position Event Repository" as PR
database "Tracking Alert Repository" as AR
TO -> UI: Select vehicle / trip / period
UI -> RS: Request journey history
RS -> PR: Load position events
PR --> RS: Historical positions
RS -> AR: Load incidents / alerts
AR --> RS: Related events
RS -> RS: Order events + identify stops
RS --> UI: Replay timeline + stop / incident data
@enduml
```

---

# US-54 — View Tracking Dashboard

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** View fleet status, exceptions, heat maps, idle monitoring, and alerts in one operational dashboard so current fleet risk is visible.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "View Tracking Dashboard" as U0
 usecase "View Fleet Tracking Overview" as U1
 usecase "View Tracking Exceptions" as U2
 usecase "View Idle Time Monitoring" as U3
 usecase "View Heat Maps" as U4
 usecase "View Alert Summary" as U5
 usecase "Identify Stale Telemetry" as U6
}
TO --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-54 — View Tracking Dashboard
start
:Open Tracking Dashboard;
:Load Fleet Tracking Overview;
:Load Active Tracking Exceptions;
:Load Idle Monitoring Data;
:Load Alert Summary;
:Load Heat Map Data;
:Check Telemetry Freshness;
if (Stale telemetry exists?) then (Yes)
 :Mark Stale Vehicles Clearly;
endif
:Aggregate Dashboard Widgets;
:Display Current Fleet Risk View;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-54 — View Tracking Dashboard — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "Tracking Dashboard UI" as UI
participant "Dashboard Service" as DS
database "Vehicle Tracking Repository" as VR
database "Alert Repository" as AR
database "Idle / Heatmap Data" as HR
TO -> UI: Open dashboard
UI -> DS: Request tracking dashboard
DS -> VR: Load fleet status / freshness
DS -> AR: Load active exceptions / alerts
DS -> HR: Load idle / heat-map data
VR --> DS: Fleet state
AR --> DS: Alerts
HR --> DS: Idle / heat-map data
DS -> DS: Aggregate widgets
DS --> UI: Dashboard model
@enduml
```

---

# US-55 — Handle GPS Edge Cases

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** Identify signal loss, tampering, spoofing, remote-area loss, battery drain, and delayed packets so unreliable GPS data is recognized.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "Handle GPS Tracking Exception" as U0
 usecase "Handle GPS Signal Loss" as U1
 usecase "Detect GPS Tampering" as U2
 usecase "Handle Tunnel / Remote Area Loss" as U3
 usecase "Monitor Tracker Battery Drain" as U4
 usecase "Detect GPS Spoofing" as U5
 usecase "Handle Delayed GPS Packets" as U6
 usecase "Mark Tracking Data Unreliable" as U7
 usecase "Escalate GPS Exception" as U8
}
TO --> U0
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 -|> U0
U5 -|> U0
U6 -|> U0
U0 .> U7 : <<include>>
U8 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-55 — Handle GPS Edge Cases
start
:Receive Tracking Device / Position Data;
:Evaluate GPS Reliability;
if (Exception Type?) then (Signal Loss)
 :Record GPS Signal Loss;
elseif (Tampering)
 :Flag Device Tampering;
elseif (Remote / Tunnel Loss)
 :Record Temporary Coverage Loss;
elseif (Battery Drain)
 :Flag Low / Rapid Battery Drain;
elseif (Spoofing)
 :Flag Suspected GPS Spoofing;
else (Delayed Packet)
 :Preserve Source Timestamp;
 :Mark Packet as Delayed;
endif
:Mark Tracking Data Reliability State;
if (Severity requires escalation?) then (Yes)
 :Escalate GPS Exception;
endif
:Display Last Trusted / Last Known Position where applicable;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-55 — Handle GPS Edge Cases — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "GPS Exception UI" as UI
participant "GPS Reliability Service" as GS
participant "Telemetry Stream" as TS
database "Tracking Device Repository" as DR
database "Exception Repository" as ER
TS -> GS: Device / position packet
GS -> DR: Load device state / last trusted data
DR --> GS: Device state
GS -> GS: Detect signal loss / tamper / spoof / delay / battery issue
alt GPS exception detected
 GS -> ER: Save exception + reliability state
 GS --> UI: Exception + last trusted location
else Reliable
 GS --> UI: Normal tracking state
end
TO -> UI: Review / escalate exception
@enduml
```

---

# US-56 — Manage Delivery Orders

**Primary Actor:** Delivery Manager  
**Goal:** Create and maintain delivery orders with priority, service type, delivery windows, and instructions so delivery requirements are clear.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Delivery Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Delivery Orders" as U0
 usecase "Create Delivery Order" as U1
 usecase "Set Delivery Priority" as U2
 usecase "Define Service Type" as U3
 usecase "Define Delivery Window" as U4
 usecase "Record Delivery Instructions" as U5
 usecase "Validate Customer / Location Data" as U6
 usecase "Update Delivery Order" as U7
}
DM --> U0
DM --> U7
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-56 — Manage Delivery Orders
start
:Delivery Manager opens Delivery Orders;
if (Action?) then (Create)
 :Enter Customer / Recipient Details;
 :Enter Delivery Location;
 :Define Service Type;
 :Define Delivery Window;
 :Set Delivery Priority;
 :Record Delivery Instructions;
 :Validate Required Data;
 if (Valid?) then (Yes)
  :Create Delivery Order;
  :Set Initial Delivery Status;
  :Record Audit Information;
 else (No)
  :Display Validation Errors;
 endif
else (Update)
 :Select Existing Delivery Order;
 :Edit Allowed Fields;
 :Validate Changes;
 :Save Updated Delivery Order;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-56 — Manage Delivery Orders — Sequence Diagram
actor "Delivery Manager" as DM
participant "Delivery Order UI" as UI
participant "Delivery Order Service" as DS
database "Customer Repository" as CR
database "Delivery Order Repository" as DR
DM -> UI: Create / update delivery order
UI -> DS: Submit service, window, priority, instructions
DS -> CR: Validate customer / recipient
CR --> DS: Customer data
DS -> DS: Validate delivery location and required fields
alt Valid
 DS -> DR: Save delivery order
 DR --> DS: Saved order
 DS --> UI: Success
else Invalid
 DS --> UI: Validation error
end
@enduml
```

---

# US-57 — Capture Proof of Delivery

**Primary Actor:** Rider / Courier  
**Goal:** Capture configured signature, photo, barcode, timestamp, and geo-tag evidence so delivery completion can be proven.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Rider / Courier" as RIDER
rectangle "Transport & Logistics Management System" {
 usecase "Capture Proof of Delivery" as U0
 usecase "Capture Digital Signature" as U1
 usecase "Capture Delivery Photo" as U2
 usecase "Scan Barcode" as U3
 usecase "Capture Timestamp" as U4
 usecase "Capture Geo-Tag" as U5
 usecase "Validate POD Evidence" as U6
 usecase "Confirm Delivery Completion" as U7
}
RIDER --> U0
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U1 .> U0 : <<extend>>
U2 .> U0 : <<extend>>
U3 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-57 — Capture Proof of Delivery
start
:Rider opens Assigned Delivery;
:Select Proof of Delivery;
:Capture Timestamp;
:Capture Geo-Tag if available;
if (Signature required?) then (Yes)
 :Capture Digital Signature;
endif
if (Photo required?) then (Yes)
 :Capture Delivery Photo;
endif
if (Barcode required?) then (Yes)
 :Scan Barcode;
endif
:Validate Required POD Evidence;
if (Evidence valid?) then (Yes)
 :Save Proof of Delivery;
 :Confirm Delivery Completion;
 :Record Audit / Delivery Event;
else (No)
 :Display Missing / Invalid Evidence Error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-57 — Capture Proof of Delivery — Sequence Diagram
actor "Rider / Courier" as RIDER
participant "Delivery Mobile UI" as UI
participant "POD Service" as PS
participant "Device Evidence Capture" as DEV
database "Delivery Repository" as DR
database "POD Repository" as PR
RIDER -> UI: Capture proof of delivery
UI -> DEV: Capture configured evidence
DEV --> UI: Signature / photo / barcode / geo data
UI -> PS: Submit POD evidence
PS -> DR: Validate delivery state
DR --> PS: Delivery data
PS -> PS: Validate required evidence
alt Valid
 PS -> PR: Save POD
 PS -> DR: Mark delivery completed
 PS --> UI: Completion success
else Invalid
 PS --> UI: Evidence validation error
end
@enduml
```

---

# US-58 — Capture Signature and Photo Offline

**Primary Actor:** Rider / Courier  
**Goal:** Capture signatures and photos offline with quality, retake, consent, and later synchronization so proof can be collected without connectivity.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Rider / Courier" as RIDER
rectangle "Transport & Logistics Management System" {
 usecase "Capture Signature and Photo Offline" as U0
 usecase "Capture Signature Offline" as U1
 usecase "Capture Photo Offline" as U2
 usecase "Validate Image Quality" as U3
 usecase "Request Photo Retake" as U4
 usecase "Record Customer Consent" as U5
 usecase "Queue Offline POD Evidence" as U6
 usecase "Synchronize POD Evidence" as U7
}
RIDER --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U4 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-58 — Capture Signature and Photo Offline
start
:Open Delivery in Offline Mode;
:Capture Customer Signature;
:Capture Delivery Photo;
:Record Customer Consent if required;
:Validate Image Quality;
if (Photo quality acceptable?) then (Yes)
 :Store Signature / Photo Securely Offline;
 :Queue POD Evidence for Synchronization;
else (No)
 :Request Photo Retake;
 :Capture Replacement Photo;
endif
if (Network restored?) then (Yes)
 :Synchronize POD Evidence;
 if (Sync successful?) then (Yes)
  :Mark Evidence Synchronized;
 else (No)
  :Keep Evidence Queued for Retry;
 endif
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-58 — Capture Signature and Photo Offline — Sequence Diagram
actor "Rider / Courier" as RIDER
participant "Offline Delivery UI" as UI
participant "Offline Evidence Store" as OS
participant "POD Sync Service" as SS
database "POD Repository" as PR
RIDER -> UI: Capture signature / photo
UI -> UI: Validate photo quality / consent
alt Quality valid
 UI -> OS: Store encrypted offline evidence
 OS --> UI: Queued
else Retake required
 UI --> RIDER: Request photo retake
end
opt Network restored
 OS -> SS: Send queued evidence
 SS -> PR: Save synchronized POD evidence
 PR --> SS: Saved
 SS -> OS: Mark synchronized
end
@enduml
```

---

# US-59 — Manage Failed Deliveries

**Primary Actor:** Delivery Manager  
**Goal:** Track failure reason, escalation, contact attempts, and return-to-origin decisions so unsuccessful deliveries have controlled outcomes.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Delivery Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Failed Deliveries" as U0
 usecase "Record Failure Reason" as U1
 usecase "Record Customer Contact Attempts" as U2
 usecase "Escalate Failed Delivery" as U3
 usecase "Initiate Return to Origin" as U4
 usecase "Determine Re-Delivery Eligibility" as U5
 usecase "Update Delivery Attempt Status" as U6
}
DM --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U3 .> U0 : <<extend>>
U4 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-59 — Manage Failed Deliveries
start
:Receive Failed Delivery Attempt;
:Load Delivery Order;
:Record Failure Reason;
:Record Customer Contact Attempts;
:Update Delivery Attempt Status;
:Determine Re-Delivery Eligibility;
if (Re-delivery allowed?) then (Yes)
 :Mark Delivery for Re-Scheduling;
else (No)
 if (Return to Origin required?) then (Yes)
  :Initiate Return to Origin;
 else (No)
  :Escalate Failed Delivery;
 endif
endif
:Record Failure History;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-59 — Manage Failed Deliveries — Sequence Diagram
actor "Delivery Manager" as DM
participant "Failed Delivery UI" as UI
participant "Failed Delivery Service" as FS
database "Delivery Repository" as DR
database "Delivery Attempt Repository" as AR
participant "Exception / Escalation Service" as ES
DM -> UI: Review failed delivery
UI -> FS: Submit reason + contact attempts
FS -> DR: Load delivery order
DR --> FS: Delivery state
FS -> AR: Save failed attempt
FS -> FS: Determine re-delivery / RTO eligibility
alt Re-delivery allowed
 FS -> DR: Mark for re-scheduling
else RTO / escalation
 FS -> ES: Create RTO / escalation action
end
FS --> UI: Failed delivery outcome
@enduml
```

---

# US-60 — Schedule Re-Delivery

**Primary Actor:** Delivery Manager  
**Goal:** Use customer preference and slot availability for automatic or agent-assisted rescheduling so another delivery attempt can be planned.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Delivery Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Schedule Re-Delivery" as U0
 usecase "Record Customer Preference" as U1
 usecase "Check Delivery Slot Availability" as U2
 usecase "Auto-Reschedule Delivery" as U3
 usecase "Agent-Reschedule Delivery" as U4
 usecase "Validate Slot Capacity" as U5
 usecase "Confirm Re-Delivery Schedule" as U6
}
DM --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U3 -|> U0
U4 -|> U0
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-60 — Schedule Re-Delivery
start
:Select Failed Delivery Eligible for Re-Delivery;
:Load Customer Preference;
:Load Available Delivery Slots;
:Check Slot Capacity;
if (Scheduling Method?) then (Automatic)
 :Generate Automatic Reschedule Suggestions;
 :Select Best Feasible Slot;
else (Agent Assisted)
 :Delivery Manager selects Available Slot;
endif
:Validate Selected Slot Capacity and Cutoff;
if (Slot valid?) then (Yes)
 :Update Delivery Schedule;
 :Confirm Re-Delivery Schedule;
 :Record Scheduling Method and History;
else (No)
 :Reject Selected Slot;
 :Return to Slot Selection;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-60 — Schedule Re-Delivery — Sequence Diagram
actor "Delivery Manager" as DM
participant "Re-Delivery UI" as UI
participant "Re-Delivery Service" as RS
database "Delivery Repository" as DR
database "Delivery Slot Repository" as SR
database "Customer Preference Repository" as CR
DM -> UI: Schedule re-delivery
UI -> RS: Submit failed delivery
RS -> DR: Validate re-delivery eligibility
RS -> CR: Load customer preference
RS -> SR: Load available slots / capacity
DR --> RS: Delivery state
CR --> RS: Preferences
SR --> RS: Slot availability
alt Feasible slot
 RS --> UI: Automatic suggestions / available slots
 DM -> UI: Select / confirm slot
 UI -> RS: Confirm schedule
 RS -> DR: Update delivery schedule
 RS -> SR: Reserve slot capacity
 RS --> UI: Re-delivery scheduled
else No feasible slot
 RS --> UI: No available slot / corrective action
end
@enduml
```

---

## Coverage Summary

| User Story | Title | Use Case | Activity | Sequence |
|---|---|---:|---:|---:|
| US-51 | Monitor Idle Time | Yes | Yes | Yes |
| US-52 | Monitor Route Deviations | Yes | Yes | Yes |
| US-53 | Replay Journeys | Yes | Yes | Yes |
| US-54 | View Tracking Dashboard | Yes | Yes | Yes |
| US-55 | Handle GPS Edge Cases | Yes | Yes | Yes |
| US-56 | Manage Delivery Orders | Yes | Yes | Yes |
| US-57 | Capture Proof of Delivery | Yes | Yes | Yes |
| US-58 | Capture Signature and Photo Offline | Yes | Yes | Yes |
| US-59 | Manage Failed Deliveries | Yes | Yes | Yes |
| US-60 | Schedule Re-Delivery | Yes | Yes | Yes |

## Source Basis

The diagrams preserve the supplied story boundaries and terminology. Idle monitoring is kept separate from live tracking; route deviation monitoring is distinct from route planning; journey replay is historical rather than live; and the Delivery Management stories separate order definition, POD capture, offline evidence handling, failure handling, and re-delivery scheduling so one workflow does not become an administrative octopus.
