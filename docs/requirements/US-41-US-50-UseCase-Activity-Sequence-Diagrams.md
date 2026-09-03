# Transport & Logistics Management System

## UML Diagrams — US-41 to US-50

This document consolidates UML diagrams for **US-41 through US-50** based on the supplied Transport & Logistics requirements. Each story includes a **Use Case Diagram**, **Activity Diagram**, and **Sequence Diagram** in PlantUML.

> Scope: US-41–US-45 continue Driver Management, US-46–US-47 cover payroll/billing links, and US-48–US-50 begin GPS & Real-Time Tracking.

---

# US-41 — Assess Driver Performance

**Primary Actor:** Driver Manager  
**Goal:** Combine safety, fuel, on-time, and trip-rating metrics so driver performance can be evaluated consistently.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Driver Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Assess Driver Performance" as U0
 usecase "Record Trip Rating" as U1
 usecase "Calculate Safety Score" as U2
 usecase "Calculate Fuel Efficiency Score" as U3
 usecase "Calculate On-Time Score" as U4
 usecase "Calculate Driver Score" as U5
 usecase "Manage Driver Incentives" as U6
 usecase "View Performance History" as U7
}
DM --> U0
DM --> U7
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
title US-41 — Assess Driver Performance
start
:Select Driver and Assessment Period;
:Load Trip Ratings;
:Load Safety / Violation Data;
:Load Fuel Efficiency Data;
:Load On-Time Performance Data;
:Calculate Safety Score;
:Calculate Fuel Efficiency Score;
:Calculate On-Time Score;
:Combine Metrics into Driver Score;
if (Incentive rule applicable?) then (Yes)
 :Calculate / Record Incentive;
endif
:Save Performance Assessment;
:Preserve Historical Assessment;
:Display Performance Summary;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-41 — Assess Driver Performance — Sequence Diagram
actor "Driver Manager" as DM
participant "Performance UI" as UI
participant "Driver Performance Service" as PS
database "Trip / Rating Repository" as TR
database "Safety / Violation Repository" as SR
database "Fuel / On-Time Repository" as FR
database "Performance Repository" as PR
DM -> UI: Assess driver performance
UI -> PS: Submit driver + period
PS -> TR: Load trip ratings
PS -> SR: Load safety data
PS -> FR: Load fuel and on-time data
TR --> PS: Ratings
SR --> PS: Safety data
FR --> PS: Efficiency / on-time data
PS -> PS: Calculate component scores + total score
PS -> PR: Save assessment
PR --> PS: Saved
PS --> UI: Performance summary
@enduml
```

---

# US-42 — Manage Violations

**Primary Actor:** Driver Manager  
**Goal:** Record violations, fines, payments, repeat-offender flags, and disciplinary actions so driver misconduct remains traceable.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Driver Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Driver Violations" as U0
 usecase "Record Driver Violation" as U1
 usecase "Record Fine" as U2
 usecase "Record Fine Payment" as U3
 usecase "Flag Repeat Violations" as U4
 usecase "Apply Disciplinary Action" as U5
 usecase "View Violation History" as U6
}
DM --> U0
DM --> U6
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U5 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-42 — Manage Violations
start
:Select Driver;
:Record Violation Details;
if (Fine applicable?) then (Yes)
 :Record Fine Amount;
 if (Payment received?) then (Yes)
  :Record Fine Payment;
 endif
endif
:Check Prior Violation History;
if (Repeat violation threshold met?) then (Yes)
 :Flag Repeat Offender;
endif
if (Disciplinary action required?) then (Yes)
 :Validate Authorized User;
 :Record Disciplinary Action;
endif
:Save Violation Record;
:Record Audit Information;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-42 — Manage Violations — Sequence Diagram
actor "Driver Manager" as DM
participant "Violation UI" as UI
participant "Violation Service" as VS
database "Driver Repository" as DR
database "Violation Repository" as VR
participant "Disciplinary Workflow" as DW
DM -> UI: Record / update violation
UI -> VS: Submit violation, fine, payment
VS -> DR: Validate driver
DR --> VS: Driver state
VS -> VR: Load prior violations
VR --> VS: Violation history
VS -> VS: Evaluate repeat-offender rules
opt Discipline required
 VS -> DW: Start disciplinary action
 DW --> VS: Workflow state
end
VS -> VR: Save violation / fine / payment
VS --> UI: Updated violation state
@enduml
```

---

# US-43 — Manage Driver Medical Fitness

**Primary Actor:** Safety Officer  
**Goal:** Track periodic medical checks, certificate expiry, restrictions, and vision results so medically unfit drivers are identified.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Safety Officer" as SO
rectangle "Transport & Logistics Management System" {
 usecase "Manage Driver Medical Fitness" as U0
 usecase "Record Periodic Medical Check" as U1
 usecase "Track Medical Certificate Expiry" as U2
 usecase "Record Medical Restrictions" as U3
 usecase "Record Vision Test Results" as U4
 usecase "Determine Medical Fitness" as U5
 usecase "Update Assignment Eligibility" as U6
}
SO --> U0
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
title US-43 — Manage Driver Medical Fitness
start
:Select Driver;
:Record Periodic Medical Check;
:Record Vision Test Results if applicable;
:Record Medical Certificate Expiry;
:Record Medical Restrictions;
:Evaluate Fitness Status;
if (Fit for duty?) then (Yes)
 :Set Medical Fitness = Fit;
 :Keep / Restore Assignment Eligibility;
else (No)
 :Set Medical Fitness = Restricted / Unfit;
 :Update Assignment Eligibility;
endif
:Check Certificate Expiry Threshold;
if (Approaching expiry?) then (Yes)
 :Generate Medical Expiry Alert;
endif
:Save Medical Record;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-43 — Manage Driver Medical Fitness — Sequence Diagram
actor "Safety Officer" as SO
participant "Medical Fitness UI" as UI
participant "Medical Fitness Service" as MS
database "Driver Repository" as DR
database "Medical Repository" as MR
participant "Eligibility Service" as ES
SO -> UI: Record medical / vision result
UI -> MS: Submit medical assessment
MS -> DR: Validate driver
DR --> MS: Driver state
MS -> MS: Evaluate certificate, restrictions, fitness
MS -> MR: Save medical record
MR --> MS: Saved
MS -> ES: Update assignment eligibility
ES --> MS: Eligibility result
MS --> UI: Fitness + eligibility state
@enduml
```

---

# US-44 — Manage Drug Tests

**Primary Actor:** Safety Officer  
**Goal:** Manage random and scheduled drug tests, failures, and return-to-duty status so testing requirements can be enforced.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Safety Officer" as SO
rectangle "Transport & Logistics Management System" {
 usecase "Manage Drug Tests" as U0
 usecase "Schedule Random Drug Test" as U1
 usecase "Schedule Planned Drug Test" as U2
 usecase "Record Drug Test Result" as U3
 usecase "Record Drug Test Failure" as U4
 usecase "Manage Return-to-Duty Status" as U5
 usecase "Update Driver Eligibility" as U6
}
SO --> U0
U1 -|> U0
U2 -|> U0
U0 .> U3 : <<include>>
U4 .> U0 : <<extend>>
U5 .> U0 : <<extend>>
U0 .> U6 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-44 — Manage Drug Tests
start
:Select Driver;
:Choose Random or Planned Test;
:Schedule Drug Test;
:Record Test Result;
if (Test failed?) then (Yes)
 :Record Drug Test Failure;
 :Restrict Driver Eligibility;
 :Set Return-to-Duty = Pending;
 if (Return-to-Duty clearance completed?) then (Yes)
  :Update Return-to-Duty Status;
  :Restore Eligibility as allowed;
 endif
else (No)
 :Record Passed Test;
 :Maintain Eligible Status;
endif
:Save Test History;
:Record Audit Information;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-44 — Manage Drug Tests — Sequence Diagram
actor "Safety Officer" as SO
participant "Drug Test UI" as UI
participant "Drug Test Service" as DS
database "Driver Repository" as DR
database "Drug Test Repository" as TR
participant "Eligibility Service" as ES
SO -> UI: Schedule / record test
UI -> DS: Submit test type + result
DS -> DR: Validate driver
DR --> DS: Driver state
DS -> TR: Save test result
TR --> DS: Saved
alt Failed
 DS -> ES: Restrict eligibility / return-to-duty pending
else Passed
 DS -> ES: Keep / restore eligibility
end
ES --> DS: Eligibility state
DS --> UI: Test + eligibility outcome
@enduml
```

---

# US-45 — Handle Driver Exceptions

**Primary Actor:** Driver Manager  
**Goal:** Handle suspension, medical emergency, unavailability, behavioral issues, expired endorsements, and identity mismatch so invalid drivers are not assigned.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Driver Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Handle Driver Exception" as U0
 usecase "Handle Driver Suspension" as U1
 usecase "Handle Driver Medical Emergency" as U2
 usecase "Mark Driver Unavailable" as U3
 usecase "Handle Behavioral Incident" as U4
 usecase "Detect Expired Endorsement" as U5
 usecase "Detect Driver Identity Mismatch" as U6
 usecase "Prevent Driver Assignment" as U7
 usecase "Assign Replacement Driver" as U8
 usecase "Escalate Driver Exception" as U9
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
U9 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-45 — Handle Driver Exceptions
start
:Detect / Report Driver Exception;
:Load Driver Profile, License and Fitness Data;
:Classify Exception;
if (Suspension?) then (Yes)
 :Set Driver Suspended;
elseif (Medical Emergency)
 :Set Driver Unavailable / Restricted;
elseif (Unavailability)
 :Mark Driver Unavailable;
elseif (Behavioral Incident)
 :Record Behavioral Incident;
elseif (Expired Endorsement)
 :Mark License Eligibility Invalid;
else (Identity Mismatch)
 :Flag Identity Validation Failure;
endif
:Prevent New Assignment if ineligible;
if (Current trip affected?) then (Yes)
 :Search Replacement Driver;
endif
if (Exception resolvable?) then (Yes)
 :Apply Corrective Action;
 :Revalidate Driver Eligibility;
else (No)
 :Escalate Driver Exception;
endif
:Record Exception History;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-45 — Handle Driver Exceptions — Sequence Diagram
actor "Driver Manager" as DM
participant "Driver Exception UI" as UI
participant "Driver Exception Service" as ES
database "Driver Repository" as DR
database "License / Medical Repository" as LR
participant "Assignment Service" as AS
DM -> UI: Review driver exception
UI -> ES: Submit / evaluate exception
ES -> DR: Load driver state
ES -> LR: Load license / medical state
DR --> ES: Driver state
LR --> ES: Compliance data
ES -> ES: Classify exception
ES -> AS: Prevent assignment / request replacement if needed
AS --> ES: Assignment impact
ES --> UI: Resolution / escalation state
@enduml
```

---

# US-46 — Process Driver Payroll Link

**Primary Actor:** Finance Officer  
**Goal:** Link trip earnings, allowances, overtime, and deductions to drivers so payroll-related settlement is accurate.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Finance Officer" as FO
rectangle "Transport & Logistics Management System" {
 usecase "Process Driver Payroll Link" as U0
 usecase "Calculate Trip Earnings" as U1
 usecase "Calculate Allowances" as U2
 usecase "Calculate Overtime" as U3
 usecase "Apply Payroll Deductions" as U4
 usecase "Validate Driver / Trip Inputs" as U5
 usecase "Create Payroll Export / Entry" as U6
}
FO --> U0
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
title US-46 — Process Driver Payroll Link
start
:Select Driver / Payroll Period;
:Load Completed Trip Records;
:Validate Driver / Trip Inputs;
:Calculate Trip Earnings;
:Calculate Allowances;
:Calculate Overtime;
:Apply Payroll Deductions;
:Calculate Net Payroll Input;
if (Calculation valid?) then (Yes)
 :Create Payroll Entry / Export;
 :Record Source Trip References;
 :Record Audit Information;
else (No)
 :Display Payroll Link Validation Error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-46 — Process Driver Payroll Link — Sequence Diagram
actor "Finance Officer" as FO
participant "Payroll Link UI" as UI
participant "Payroll Link Service" as PS
database "Trip Repository" as TR
database "Driver Repository" as DR
database "Payroll Entry Repository" as PR
FO -> UI: Process payroll inputs
UI -> PS: Submit driver + period
PS -> DR: Validate driver
PS -> TR: Load completed trips
DR --> PS: Driver data
TR --> PS: Trip earnings inputs
PS -> PS: Calculate earnings, allowances, overtime, deductions
alt Valid
 PS -> PR: Save payroll entry / export state
 PS --> UI: Payroll input summary
else Invalid
 PS --> UI: Validation error
end
@enduml
```

---

# US-47 — Manage Transport Billing

**Primary Actor:** Billing Officer  
**Goal:** Calculate trip and freight billing with surcharges, penalties, and cost-center allocation so transport activities are financially accounted for.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Billing Officer" as BO
rectangle "Transport & Logistics Management System" {
 usecase "Manage Transport Billing" as U0
 usecase "Calculate Trip Cost" as U1
 usecase "Generate Freight Billing" as U2
 usecase "Apply Surcharges" as U3
 usecase "Apply Penalties" as U4
 usecase "Allocate Cost Center" as U5
 usecase "Validate Billing Data" as U6
 usecase "Finalize Billing Record" as U7
}
BO --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-47 — Manage Transport Billing
start
:Select Completed Trip / Freight Activity;
:Load Cost and Commercial Data;
:Calculate Base Trip / Freight Cost;
:Apply Applicable Surcharges;
:Apply Applicable Penalties;
:Allocate Cost Center;
:Validate Billing Data;
if (Billing valid?) then (Yes)
 :Calculate Final Billing Amount;
 :Finalize Billing Record;
 :Record Audit Information;
else (No)
 :Display Billing Validation Error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-47 — Manage Transport Billing — Sequence Diagram
actor "Billing Officer" as BO
participant "Billing UI" as UI
participant "Billing Service" as BS
database "Trip / Freight Repository" as TR
database "Cost / Charge Repository" as CR
database "Billing Repository" as BR
BO -> UI: Generate / finalize billing
UI -> BS: Submit trip / freight reference
BS -> TR: Load operational data
BS -> CR: Load cost, surcharge, penalty rules
TR --> BS: Operational data
CR --> BS: Charge data
BS -> BS: Calculate billing + cost center
alt Valid
 BS -> BR: Save finalized billing
 BR --> BS: Saved
 BS --> UI: Billing summary
else Invalid
 BS --> UI: Validation error
end
@enduml
```

---

# US-48 — Track Vehicles Live

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** View live location, status, last known location, connectivity, and GPS accuracy so fleet movement can be monitored.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "Track Vehicles Live" as U0
 usecase "View Vehicle on Map" as U1
 usecase "Track Vehicle Status" as U2
 usecase "Detect Connectivity Loss" as U3
 usecase "Display Last Known Location" as U4
 usecase "Display GPS Accuracy" as U5
 usecase "Show Data Freshness / Timestamp" as U6
}
TO --> U0
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
title US-48 — Track Vehicles Live
start
:Open Live Tracking;
:Select Fleet / Vehicle;
:Load Latest Tracking Data;
if (Fresh GPS data available?) then (Yes)
 :Display Current Location on Map;
 :Display Vehicle Status;
 :Display GPS Accuracy;
 :Display Source Timestamp;
else (No)
 :Detect Connectivity Loss / Stale Data;
 :Display Last Known Location;
 :Display Last Known Timestamp;
endif
:Refresh Tracking View;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-48 — Track Vehicles Live — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "Live Tracking UI" as UI
participant "Tracking Service" as TS
database "Tracking Device Repository" as DR
database "Position Event Repository" as PR
TO -> UI: View live vehicle
UI -> TS: Request current tracking state
TS -> DR: Load device status / last seen
TS -> PR: Load latest position
DR --> TS: Device state
PR --> TS: Latest position
alt Fresh telemetry
 TS --> UI: Current location + status + accuracy + timestamp
else Stale / disconnected
 TS --> UI: Connectivity loss + last known location + timestamp
end
@enduml
```

---

# US-49 — Manage Geofences

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** Create depot, customer, and unauthorized-zone geofences with entry and exit detection so controlled boundary movement is visible.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "Manage Geofences" as U0
 usecase "Create Geofence" as U1
 usecase "Define Depot Geofence" as U2
 usecase "Define Customer Geofence" as U3
 usecase "Detect Geofence Entry" as U4
 usecase "Detect Geofence Exit" as U5
 usecase "Detect Unauthorized Zone Entry" as U6
 usecase "Generate Entry / Exit Alert" as U7
}
TO --> U0
U0 .> U1 : <<include>>
U2 -|> U1
U3 -|> U1
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-49 — Manage Geofences
start
:Open Geofence Management;
if (Action?) then (Create / Update)
 :Enter Geofence Name and Type;
 :Define Geofence Geometry;
 :Validate Geofence Definition;
 if (Valid?) then (Yes)
  :Save Geofence;
 else (No)
  :Display Geometry Validation Error;
 endif
else (Monitor)
 :Receive Vehicle Position Event;
 :Check Position Against Active Geofences;
 if (Entry detected?) then (Yes)
  :Record Geofence Entry;
  :Generate Entry Alert if configured;
 elseif (Exit detected?)
  :Record Geofence Exit;
  :Generate Exit Alert if configured;
 endif
 if (Unauthorized zone entered?) then (Yes)
  :Generate Unauthorized Zone Alert;
 endif
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-49 — Manage Geofences — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "Geofence UI" as UI
participant "Geofence Service" as GS
database "Geofence Repository" as GR
participant "Position Stream" as PS
participant "Alert Service" as AS
TO -> UI: Create / monitor geofence
UI -> GS: Submit geometry / configuration
GS -> GS: Validate geometry
GS -> GR: Save geofence
GR --> GS: Saved
PS -> GS: New vehicle position
GS -> GR: Load relevant geofences
GR --> GS: Geofence definitions
GS -> GS: Evaluate entry / exit / unauthorized zone
opt Event detected
 GS -> AS: Create configured alert
end
GS --> UI: Updated geofence event state
@enduml
```

---

# US-50 — Monitor Speed

**Primary Actor:** Tracking / Control Room Operator  
**Goal:** Monitor speed thresholds and road-specific rules with repeat-violation tracking so unsafe driving can be detected.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Tracking / Control Room Operator" as TO
rectangle "Transport & Logistics Management System" {
 usecase "Monitor Speed" as U0
 usecase "Configure Speed Threshold" as U1
 usecase "Apply Road-Specific Speed Rules" as U2
 usecase "Evaluate Vehicle Speed" as U3
 usecase "Generate Over-Speed Notification" as U4
 usecase "Track Repeat Speed Violations" as U5
 usecase "Record Speed Violation Event" as U6
}
TO --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U6 : <<include>>
U4 .> U0 : <<extend>>
U5 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-50 — Monitor Speed
start
:Load Configured Speed Thresholds;
:Receive Vehicle Speed / Position Event;
:Determine Applicable Road-Specific Rule;
:Compare Actual Speed with Applicable Limit;
if (Over-speed detected?) then (Yes)
 :Record Speed Violation Event;
 :Generate Over-Speed Notification;
 :Check Prior Speed Violations;
 if (Repeat violation threshold met?) then (Yes)
  :Flag Repeat Speed Violation;
 endif
else (No)
 :Record / Maintain Normal Speed State;
endif
:Update Tracking Alert View;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-50 — Monitor Speed — Sequence Diagram
actor "Tracking / Control Room Operator" as TO
participant "Speed Monitoring UI" as UI
participant "Speed Monitoring Service" as SS
participant "Position Stream" as PS
database "Speed Rule Repository" as RR
database "Tracking Alert Repository" as AR
PS -> SS: Vehicle speed + position
SS -> RR: Load configured / road-specific limit
RR --> SS: Applicable speed rule
SS -> SS: Compare actual speed vs limit
alt Over-speed
 SS -> AR: Save violation event / alert
 SS --> UI: Over-speed notification
else Within limit
 SS --> UI: Normal speed state
end
TO -> UI: Review speed alerts / repeat violations
@enduml
```

---

## Coverage Summary

| User Story | Title | Use Case | Activity | Sequence |
|---|---|---:|---:|---:|
| US-41 | Assess Driver Performance | Yes | Yes | Yes |
| US-42 | Manage Violations | Yes | Yes | Yes |
| US-43 | Manage Driver Medical Fitness | Yes | Yes | Yes |
| US-44 | Manage Drug Tests | Yes | Yes | Yes |
| US-45 | Handle Driver Exceptions | Yes | Yes | Yes |
| US-46 | Process Driver Payroll Link | Yes | Yes | Yes |
| US-47 | Manage Transport Billing | Yes | Yes | Yes |
| US-48 | Track Vehicles Live | Yes | Yes | Yes |
| US-49 | Manage Geofences | Yes | Yes | Yes |
| US-50 | Monitor Speed | Yes | Yes | Yes |

## Source Basis

The story boundaries follow the supplied requirements model: Driver Performance, Violations, Medical Fitness, Drug Tests, and Driver Edge Cases are kept separate; Payroll Link and Transport Billing remain distinct financial processes; and GPS live tracking, geofencing, and speed monitoring are modeled as separate tracking capabilities rather than one enormous map-shaped use case.
