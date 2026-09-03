# Transport & Logistics Management System

## UML Diagrams — US-61 to US-70

This document consolidates UML diagrams for **US-61 through US-70** based on the supplied Transport & Logistics requirements. Each story includes a **Use Case Diagram**, **Activity Diagram**, and **Sequence Diagram** in PlantUML.

> Scope: US-61–US-62 complete Delivery Management in this range. US-63–US-68 cover Last-Mile Delivery zones, slots, riders, batching, ETA, and exceptions. US-69–US-70 cover customer notifications and customer self-service.

---

# US-61 — Analyze Delivery Performance

**Primary Actor:** Delivery Manager  
**Goal:** Analyze delivery success, delays, attempts, and regional performance so delivery operations can be improved.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Delivery Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Analyze Delivery Performance" as U0
 usecase "Calculate Delivery Success Rate" as U1
 usecase "Analyze Delivery Delay" as U2
 usecase "Track Delivery Attempts" as U3
 usecase "Analyze Regional Performance" as U4
 usecase "Filter Delivery Analytics" as U5
 usecase "View Delivery Performance Dashboard" as U6
}
DM --> U0
DM --> U6
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-61 — Analyze Delivery Performance
start
:Delivery Manager opens Delivery Analytics;
:Select Reporting Period / Region / Service Type;
:Load Delivery Orders and Attempts;
:Validate Analytics Data Set;
:Calculate Delivery Success Rate;
:Analyze Delivery Delays;
:Calculate Delivery Attempt Trends;
:Analyze Regional Performance;
if (Incomplete records detected?) then (Yes)
 :Flag / Exclude According to Analytics Rules;
endif
:Generate Delivery Performance Summary;
:Display Dashboard / Metrics;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-61 — Analyze Delivery Performance — Sequence Diagram
actor "Delivery Manager" as DM
participant "Delivery Analytics UI" as UI
participant "Delivery Analytics Service" as AS
database "Delivery Order Repository" as DR
database "Delivery Attempt Repository" as AR
DM -> UI: Request delivery analytics
UI -> AS: Submit period / filters
AS -> DR: Load delivery orders
AS -> AR: Load delivery attempts
DR --> AS: Orders
AR --> AS: Attempts
AS -> AS: Calculate success, delay, attempts, regional metrics
AS --> UI: Performance dashboard data
@enduml
```

---

# US-62 — Handle Delivery Exceptions

**Primary Actor:** Delivery Manager  
**Goal:** Handle customer unavailability, wrong address, refusal, partial delivery, damage, and OTP mismatch so exceptional delivery outcomes are recorded correctly.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Delivery Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Handle Delivery Exception" as U0
 usecase "Handle Customer Unavailable" as U1
 usecase "Handle Wrong Address" as U2
 usecase "Handle Delivery Refusal" as U3
 usecase "Handle Partial Delivery" as U4
 usecase "Handle Damaged Delivery" as U5
 usecase "Handle OTP Mismatch" as U6
 usecase "Record Delivery Exception" as U7
 usecase "Escalate / Re-Schedule Delivery" as U8
}
DM --> U0
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
title US-62 — Handle Delivery Exceptions
start
:Receive / Detect Delivery Exception;
:Load Delivery Order and Attempt;
:Classify Exception;
if (Exception Type?) then (Customer Unavailable)
 :Record Customer Unavailable;
elseif (Wrong Address)
 :Record Address Issue;
elseif (Refusal)
 :Record Delivery Refusal;
elseif (Partial Delivery)
 :Record Delivered and Undelivered Items;
elseif (Damaged Delivery)
 :Record Damage Details / Evidence;
else (OTP Mismatch)
 :Prevent Normal POD Completion;
 :Record OTP Mismatch;
endif
:Record Delivery Exception;
if (Re-delivery / escalation required?) then (Yes)
 :Escalate or Route to Re-Delivery Workflow;
endif
:Update Delivery Attempt Status;
:Preserve Exception History;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-62 — Handle Delivery Exceptions — Sequence Diagram
actor "Delivery Manager" as DM
participant "Delivery Exception UI" as UI
participant "Delivery Exception Service" as ES
database "Delivery Repository" as DR
database "Delivery Attempt Repository" as AR
participant "Exception / Re-Delivery Service" as RS
DM -> UI: Review / record exception
UI -> ES: Submit exception type + details
ES -> DR: Load delivery order
ES -> AR: Load delivery attempt
DR --> ES: Delivery state
AR --> ES: Attempt state
ES -> ES: Classify exception + apply rules
ES -> AR: Save exception outcome
opt Re-delivery / escalation needed
 ES -> RS: Create follow-up action
end
ES --> UI: Updated delivery state
@enduml
```

---

# US-63 — Manage Delivery Zones

**Primary Actor:** Last-Mile Planner  
**Goal:** Create dynamic delivery zones, manage capacity, overrides, and micro-hubs so last-mile coverage can adapt to demand.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Last-Mile Planner" as LP
rectangle "Transport & Logistics Management System" {
 usecase "Manage Delivery Zones" as U0
 usecase "Create Dynamic Delivery Zone" as U1
 usecase "Manage Zone Capacity" as U2
 usecase "Override Zone Assignment" as U3
 usecase "Manage Micro-Hub" as U4
 usecase "Validate Zone Geometry" as U5
 usecase "View Zone Coverage" as U6
}
LP --> U0
LP --> U6
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
title US-63 — Manage Delivery Zones
start
:Open Delivery Zone Management;
if (Action?) then (Create / Update Zone)
 :Define Zone Geometry;
 :Set Zone Capacity;
 :Associate Micro-Hub if applicable;
 :Validate Zone Geometry;
 if (Valid?) then (Yes)
  :Save Delivery Zone;
 else (No)
  :Display Zone Validation Error;
 endif
elseif (Override Assignment)
 :Select Delivery / Zone;
 :Record Override Reason;
 :Validate Override Permission;
 :Apply Temporary Zone Override;
else (View)
 :Display Zone Coverage and Capacity;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-63 — Manage Delivery Zones — Sequence Diagram
actor "Last-Mile Planner" as LP
participant "Delivery Zone UI" as UI
participant "Zone Service" as ZS
database "Zone Repository" as ZR
database "Micro-Hub Repository" as MR
LP -> UI: Create / update / override zone
UI -> ZS: Submit geometry, capacity, hub, override
ZS -> ZS: Validate geometry and permissions
opt Micro-hub supplied
 ZS -> MR: Validate micro-hub
 MR --> ZS: Hub state
end
alt Valid
 ZS -> ZR: Save zone / override
 ZR --> ZS: Saved
 ZS --> UI: Updated zone coverage
else Invalid
 ZS --> UI: Validation error
end
@enduml
```

---

# US-64 — Manage Delivery Slots

**Primary Actor:** Last-Mile Planner  
**Goal:** Plan slot capacity, peak hours, cutoffs, and buffers so delivery promises remain feasible.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Last-Mile Planner" as LP
rectangle "Transport & Logistics Management System" {
 usecase "Manage Delivery Slots" as U0
 usecase "Plan Slot Capacity" as U1
 usecase "Define Peak Hours" as U2
 usecase "Define Cutoff Time" as U3
 usecase "Define Operational Buffers" as U4
 usecase "Validate Slot Availability" as U5
 usecase "Prevent Over-Capacity Booking" as U6
}
LP --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-64 — Manage Delivery Slots
start
:Open Delivery Slot Management;
:Select Delivery Zone / Date;
:Define Slot Start and End Time;
:Set Slot Capacity;
:Define Peak-Hour Rules;
:Define Cutoff Time;
:Define Operational Buffers;
:Validate Slot Configuration;
if (Configuration valid?) then (Yes)
 :Save Delivery Slot;
 :Calculate Available Capacity;
 if (Capacity exceeded by booking?) then (Yes)
  :Prevent Over-Capacity Booking;
 endif
else (No)
 :Display Slot Validation Error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-64 — Manage Delivery Slots — Sequence Diagram
actor "Last-Mile Planner" as LP
participant "Delivery Slot UI" as UI
participant "Slot Service" as SS
database "Delivery Zone Repository" as ZR
database "Delivery Slot Repository" as SR
LP -> UI: Create / update delivery slot
UI -> SS: Submit zone, time, capacity, cutoff, buffer
SS -> ZR: Validate zone
ZR --> SS: Zone state
SS -> SS: Validate slot and capacity rules
alt Valid
 SS -> SR: Save slot
 SR --> SS: Saved slot
 SS --> UI: Slot + available capacity
else Invalid / over capacity
 SS --> UI: Validation / capacity error
end
@enduml
```

---

# US-65 — Manage Riders

**Primary Actor:** Last-Mile Planner  
**Goal:** Onboard and manage gig or full-time riders with identity, shifts, and availability so valid riders can be assigned.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Last-Mile Planner" as LP
rectangle "Transport & Logistics Management System" {
 usecase "Manage Riders" as U0
 usecase "Onboard Rider" as U1
 usecase "Manage Gig Rider" as U2
 usecase "Verify Rider Identity" as U3
 usecase "Manage Rider Shift" as U4
 usecase "Track Rider Availability" as U5
 usecase "Update Rider Status" as U6
}
LP --> U0
U0 .> U1 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U2 .> U0 : <<extend>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-65 — Manage Riders
start
:Open Rider Management;
if (Action?) then (Onboard)
 :Enter Rider Details;
 :Select Rider Type;
 :Verify Rider Identity;
 if (Identity valid?) then (Yes)
  :Create Rider Profile;
  :Assign Initial Status;
 else (No)
  :Reject / Hold Rider Onboarding;
 endif
elseif (Shift)
 :Select Rider;
 :Define / Update Rider Shift;
 :Validate Shift Conflicts;
 :Save Rider Shift;
else (Availability)
 :Load Rider Schedule / Current State;
 :Update Rider Availability;
endif
:Record Change History;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-65 — Manage Riders — Sequence Diagram
actor "Last-Mile Planner" as LP
participant "Rider Management UI" as UI
participant "Rider Service" as RS
participant "Identity Verification Service" as IV
database "Rider Repository" as RR
database "Shift Repository" as SR
LP -> UI: Onboard / manage rider
UI -> RS: Submit rider action
opt Onboarding
 RS -> IV: Verify rider identity
 IV --> RS: Verification result
end
opt Shift action
 RS -> SR: Validate / save shift
 SR --> RS: Shift state
end
RS -> RR: Save rider / availability state
RR --> RS: Saved rider
RS --> UI: Rider status
@enduml
```

---

# US-66 — Batch Delivery Orders

**Primary Actor:** Last-Mile Planner  
**Goal:** Cluster deliveries by proximity, rider capacity, and priority so rider workloads are efficient.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Last-Mile Planner" as LP
rectangle "Transport & Logistics Management System" {
 usecase "Batch Delivery Orders" as U0
 usecase "Cluster Deliveries" as U1
 usecase "Batch by Proximity" as U2
 usecase "Validate Rider Capacity" as U3
 usecase "Apply Delivery Priority" as U4
 usecase "Assign Batch to Rider" as U5
 usecase "Validate Batch Feasibility" as U6
}
LP --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U6 : <<include>>
U5 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-66 — Batch Delivery Orders
start
:Select Delivery Zone / Slot / Pending Orders;
:Load Delivery Locations and Priorities;
:Cluster Deliveries;
:Batch Orders by Proximity;
:Load Eligible Riders;
:Validate Rider Capacity;
:Apply Delivery Priority;
:Validate Batch Feasibility;
if (Batch feasible?) then (Yes)
 :Create Delivery Batch;
 if (Rider selected?) then (Yes)
  :Assign Batch to Rider;
 endif
 :Save Batch and Order Relationships;
else (No)
 :Recluster / Reduce Batch;
 :Display Capacity or Feasibility Conflict;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-66 — Batch Delivery Orders — Sequence Diagram
actor "Last-Mile Planner" as LP
participant "Delivery Batching UI" as UI
participant "Batching Service" as BS
database "Delivery Order Repository" as DR
database "Rider Repository" as RR
database "Delivery Batch Repository" as BR
LP -> UI: Create delivery batch
UI -> BS: Submit zone / slot / pending orders
BS -> DR: Load orders + locations + priorities
BS -> RR: Load eligible riders / capacity
DR --> BS: Order data
RR --> BS: Rider data
BS -> BS: Cluster + proximity + priority + capacity validation
alt Feasible
 BS -> BR: Save batch / rider assignment
 BR --> BS: Saved batch
 BS --> UI: Batch result
else Infeasible
 BS --> UI: Capacity / clustering conflict
end
@enduml
```

---

# US-67 — Calculate Last-Mile ETA

**Primary Actor:** Last-Mile Planner  
**Goal:** Calculate and recalculate traffic-adjusted ETA after delays or sequence changes so customers receive realistic arrival times.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Last-Mile Planner" as LP
rectangle "Transport & Logistics Management System" {
 usecase "Calculate Last-Mile ETA" as U0
 usecase "Calculate Traffic-Adjusted ETA" as U1
 usecase "Recalculate ETA After Delay" as U2
 usecase "Recalculate ETA After Sequence Change" as U3
 usecase "Consider Prior Stops" as U4
 usecase "Send ETA Update to Customer" as U5
 usecase "Label ETA Source / Freshness" as U6
}
LP --> U0
U0 .> U1 : <<include>>
U0 .> U4 : <<include>>
U0 .> U6 : <<include>>
U2 .> U0 : <<extend>>
U3 .> U0 : <<extend>>
U5 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-67 — Calculate Last-Mile ETA
start
:Load Delivery Batch / Sequence;
:Load Current Rider Position;
:Load Remaining Stops;
:Load Available Traffic Data;
:Calculate Traffic-Adjusted ETA;
:Consider Prior Stop Service Times;
if (Delay occurred?) then (Yes)
 :Recalculate ETA After Delay;
endif
if (Stop sequence changed?) then (Yes)
 :Recalculate ETA After Sequence Change;
endif
:Label ETA Source / Freshness;
:Save Updated ETA;
if (Customer update required?) then (Yes)
 :Trigger ETA Update Notification;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-67 — Calculate Last-Mile ETA — Sequence Diagram
actor "Last-Mile Planner" as LP
participant "Last-Mile ETA UI" as UI
participant "ETA Service" as ES
database "Delivery Batch Repository" as BR
participant "Position / Traffic Service" as PT
participant "Notification Service" as NS
LP -> UI: Calculate / recalculate ETA
UI -> ES: Submit batch / delivery
ES -> BR: Load sequence + remaining stops
BR --> ES: Batch data
ES -> PT: Load rider position + traffic
PT --> ES: Position / traffic data
ES -> ES: Calculate ETA and apply delay / sequence effects
ES --> UI: Updated ETA + freshness
opt Customer update required
 ES -> NS: Send ETA update event
end
@enduml
```

---

# US-68 — Handle Last-Mile Exceptions

**Primary Actor:** Last-Mile Planner  
**Goal:** Handle rider no-show, multiple attempts, address not found, access restrictions, contactless delivery, and cash disputes so last-mile disruptions can be resolved.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Last-Mile Planner" as LP
rectangle "Transport & Logistics Management System" {
 usecase "Handle Last-Mile Exception" as U0
 usecase "Handle Rider No-Show" as U1
 usecase "Manage Multiple Delivery Attempts" as U2
 usecase "Handle Address Not Found" as U3
 usecase "Handle Access Restriction" as U4
 usecase "Support Contactless Delivery" as U5
 usecase "Handle Cash Dispute" as U6
 usecase "Assign Replacement Rider" as U7
 usecase "Escalate / Re-Schedule" as U8
}
LP --> U0
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 -|> U0
U5 -|> U0
U6 -|> U0
U7 .> U0 : <<extend>>
U8 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-68 — Handle Last-Mile Exceptions
start
:Detect / Report Last-Mile Exception;
:Load Rider, Delivery and Batch Data;
:Classify Exception;
if (Rider No-Show?) then (Yes)
 :Mark Rider Unavailable;
 :Search Replacement Rider;
elseif (Multiple Attempts)
 :Load Prior Attempts;
 :Determine Next Attempt / RTO;
elseif (Address Not Found)
 :Record Address Failure;
 :Request Customer / Agent Clarification;
elseif (Access Restriction)
 :Record Access Constraint;
elseif (Contactless Delivery)
 :Apply Contactless POD Rules;
else (Cash Dispute)
 :Record Cash Dispute;
 :Place Settlement on Hold if required;
endif
if (Re-Schedule / Escalation required?) then (Yes)
 :Create Follow-Up / Escalation Action;
endif
:Record Exception Outcome;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-68 — Handle Last-Mile Exceptions — Sequence Diagram
actor "Last-Mile Planner" as LP
participant "Last-Mile Exception UI" as UI
participant "Last-Mile Exception Service" as ES
database "Delivery / Batch Repository" as DR
database "Rider Repository" as RR
participant "Re-Delivery / Escalation Service" as RS
LP -> UI: Review last-mile exception
UI -> ES: Submit exception type + details
ES -> DR: Load delivery / batch
ES -> RR: Load rider state
DR --> ES: Delivery data
RR --> ES: Rider data
ES -> ES: Apply exception-specific rules
opt Replacement / reschedule / escalation
 ES -> RS: Create follow-up action
end
ES --> UI: Updated operational outcome
@enduml
```

---

# US-69 — Receive Delivery Notifications

**Primary Actor:** Customer / Recipient  
**Goal:** Receive SMS, app, email, OTP, and delay notifications so delivery status and expected arrival remain visible.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Customer / Recipient" as CUST
rectangle "Transport & Logistics Management System" {
 usecase "Receive Delivery Notifications" as U0
 usecase "Receive SMS Notification" as U1
 usecase "Receive App Notification" as U2
 usecase "Receive Email Notification" as U3
 usecase "Receive OTP" as U4
 usecase "Receive Delay Notification" as U5
 usecase "Receive ETA Update" as U6
 usecase "Record Notification Delivery Status" as U7
}
CUST --> U0
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 .> U0 : <<extend>>
U5 .> U0 : <<extend>>
U6 .> U0 : <<extend>>
U0 .> U7 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-69 — Receive Delivery Notifications
start
:Delivery Event / ETA Update Occurs;
:Load Customer Notification Preferences;
:Select Applicable Notification Type;
if (Notification Type?) then (OTP)
 :Generate Delivery OTP;
elseif (Delay)
 :Prepare Delay Notification;
elseif (ETA Update)
 :Prepare Updated ETA Message;
else (General Delivery Update)
 :Prepare Delivery Status Message;
endif
:Select Configured Channel;
:Send SMS / App / Email Notification;
if (Delivery successful?) then (Yes)
 :Record Notification Delivered;
else (No)
 :Record Notification Failure;
 :Apply Retry / Escalation Rule if configured;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-69 — Receive Delivery Notifications — Sequence Diagram
actor "Customer / Recipient" as CUST
participant "Delivery Event Source" as EV
participant "Notification Service" as NS
database "Customer Preference Repository" as PR
participant "SMS / App / Email Channel" as CH
database "Notification Repository" as NR
EV -> NS: Delivery / OTP / delay / ETA event
NS -> PR: Load customer preferences
PR --> NS: Preferred channels
NS -> CH: Send notification
CH --> NS: Delivery status
NS -> NR: Save notification result
NS --> CUST: Delivery notification
@enduml
```

---

# US-70 — Use Customer Self-Service

**Primary Actor:** Customer / Recipient  
**Goal:** Use shipment tracking, delivery preferences, issue reporting, and feedback so customers can manage their service experience.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Customer / Recipient" as CUST
rectangle "Transport & Logistics Management System" {
 usecase "Use Customer Self-Service" as U0
 usecase "View Self-Service Tracking" as U1
 usecase "Manage Delivery Preferences" as U2
 usecase "Submit Customer Issue" as U3
 usecase "Capture Customer Feedback" as U4
 usecase "Request Re-Delivery / Preference Change" as U5
 usecase "Validate Customer Access" as U6
}
CUST --> U0
U0 .> U6 : <<include>>
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 -|> U0
U5 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-70 — Use Customer Self-Service
start
:Customer opens Self-Service Portal;
:Authenticate / Validate Customer Access;
if (Action?) then (Tracking)
 :Load Authorized Delivery / Shipment Status;
 :Display Current Status and ETA;
elseif (Preferences)
 :Load Existing Delivery Preferences;
 :Update Allowed Preferences;
 :Save Preferences;
elseif (Issue)
 :Enter Customer Issue;
 :Validate Required Details;
 :Create Customer Issue Record;
elseif (Feedback)
 :Select Completed Delivery / Service;
 :Enter Feedback / Rating;
 :Save Customer Feedback;
else (Re-Delivery Request)
 :Submit Re-Delivery / Preference Change Request;
 :Route to Delivery Scheduling Workflow;
endif
:Display Confirmation / Updated State;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-70 — Use Customer Self-Service — Sequence Diagram
actor "Customer / Recipient" as CUST
participant "Customer Portal" as UI
participant "Customer Experience Service" as CS
participant "Access Service" as AS
database "Delivery Repository" as DR
database "Customer Preference / Issue Repository" as CR
CUST -> UI: Open self-service action
UI -> AS: Validate customer access
AS --> UI: Authorized
UI -> CS: Tracking / preference / issue / feedback request
alt Tracking
 CS -> DR: Load authorized delivery state
 DR --> CS: Status / ETA
else Preference / issue / feedback
 CS -> CR: Save / load customer experience data
 CR --> CS: Updated record
end
CS --> UI: Result / confirmation
UI --> CUST: Display self-service outcome
@enduml
```

---

## Coverage Summary

| User Story | Title | Use Case | Activity | Sequence |
|---|---|---:|---:|---:|
| US-61 | Analyze Delivery Performance | Yes | Yes | Yes |
| US-62 | Handle Delivery Exceptions | Yes | Yes | Yes |
| US-63 | Manage Delivery Zones | Yes | Yes | Yes |
| US-64 | Manage Delivery Slots | Yes | Yes | Yes |
| US-65 | Manage Riders | Yes | Yes | Yes |
| US-66 | Batch Delivery Orders | Yes | Yes | Yes |
| US-67 | Calculate Last-Mile ETA | Yes | Yes | Yes |
| US-68 | Handle Last-Mile Exceptions | Yes | Yes | Yes |
| US-69 | Receive Delivery Notifications | Yes | Yes | Yes |
| US-70 | Use Customer Self-Service | Yes | Yes | Yes |

## Source Basis

The diagrams preserve the story boundaries in the supplied Transport & Logistics requirements. Delivery analytics remains read-only analysis of operational records; delivery exceptions remain distinct from failed-delivery and re-delivery workflows; last-mile zones, slots, riders, batching, and ETA are modeled as separate planning capabilities; and customer self-service consumes existing delivery capabilities rather than duplicating them in a second, cheerfully redundant logistics system.
