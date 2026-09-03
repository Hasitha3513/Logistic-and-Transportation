# Transport & Logistics Management System

## UML Diagrams — US-21 to US-30

This document consolidates UML views for **US-21 through US-30** based
on the supplied Transport & Logistics requirements. Each story contains
a **Use Case Diagram**, **Activity Diagram**, and **Sequence Diagram**
in PlantUML.

The scope continues directly from US-11–US-20: US-21–US-23 complete
Route Management, while US-24–US-30 cover Freight & Cargo Management.

<hr>

# US-21 — Maintain Route History

**Primary Actor:** Route Planner

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Route Planner" as A
rectangle "Transport & Logistics Management System" {
 usecase "Maintain Route History" as U0
 usecase "View Executed Routes" as U1
 usecase "Retain Route Deviations" as U2
 usecase "Identify Frequent Corridors" as U3
 usecase "Search Route History" as U4
 usecase "Preserve Route Execution Records" as U5
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-21 — Maintain Route History
start
:Open Route History;
:Select date / vehicle / route filters;
:Load executed route records;
:Load recorded deviations;
:Identify frequent corridors;
:Display historical route evidence;
:Preserve records according to retention rules;
if (Validation successful?) then (Yes)
:Persist / present successful result;
else (No)
:Display validation or processing error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-21 — Maintain Route History — Sequence Diagram
actor "Route Planner" as A
participant "Route History UI" as UI
participant "Route History Service" as S
database "Route Execution Repository" as R
A -> UI: Start Maintain Route History
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-22 — Analyze Route Performance

**Primary Actor:** Route Planner

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Route Planner" as A
rectangle "Transport & Logistics Management System" {
 usecase "Analyze Route Performance" as U0
 usecase "Analyze On-Time Performance" as U1
 usecase "Analyze Route Delays" as U2
 usecase "Analyze Route Cost" as U3
 usecase "Analyze Route Utilization" as U4
 usecase "Compare Route Performance" as U5
 usecase "Generate Route Analytics" as U6
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-22 — Analyze Route Performance
start
:Select analytics period / route set;
:Load route execution history;
:Calculate on-time performance;
:Calculate delay metrics;
:Calculate route cost;
:Calculate utilization;
:Compare route performance;
:Display analytics results;
if (Validation successful?) then (Yes)
:Persist / present successful result;
else (No)
:Display validation or processing error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-22 — Analyze Route Performance — Sequence Diagram
actor "Route Planner" as A
participant "Route Analytics UI" as UI
participant "Route Analytics Service" as S
database "Route Execution Repository" as R
A -> UI: Start Analyze Route Performance
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-23 — Handle Route Disruptions

**Primary Actor:** Route Planner

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Route Planner" as A
rectangle "Transport & Logistics Management System" {
 usecase "Handle Route Disruptions" as U0
 usecase "Handle Road Closure" as U1
 usecase "Handle Weather Disruption" as U2
 usecase "Handle Restricted Zone" as U3
 usecase "Handle Toll Change" as U4
 usecase "Handle Bridge Weight Limit" as U5
 usecase "Handle Temporary Detour" as U6
 usecase "Validate Alternate Route" as U7
 usecase "Record Route Exception" as U8
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
U8 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-23 — Handle Route Disruptions
start
:Detect / report route disruption;
:Classify disruption type;
:Load affected active routes;
:Apply new route constraint;
:Find alternate route;
:Validate vehicle, time and restriction constraints;
:Assign feasible detour or escalate;
:Record disruption and route change;
if (Resolved / feasible?) then (Yes)
:Save resolution and audit trail;
else (No)
:Escalate and keep exception open;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-23 — Handle Route Disruptions — Sequence Diagram
actor "Route Planner" as A
participant "Route Operations UI" as UI
participant "Route Disruption Service" as S
database "Route Repository" as R
A -> UI: Start Handle Route Disruptions
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-24 — Manage Freight Orders

**Primary Actor:** Freight Manager

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Freight Manager" as A
rectangle "Transport & Logistics Management System" {
 usecase "Manage Freight Orders" as U0
 usecase "Create Freight Order" as U1
 usecase "Set Service Level / SLA" as U2
 usecase "Set Priority" as U3
 usecase "Record Special Handling Instructions" as U4
 usecase "Maintain Shipment Line Items" as U5
 usecase "Validate Freight Order" as U6
 usecase "Update Freight Order" as U7
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-24 — Manage Freight Orders
start
:Open Freight Orders;
:Create or select freight order;
:Enter customer and shipment requirements;
:Set SLA and priority;
:Record special handling instructions;
:Add shipment line items;
:Validate mandatory data;
:Save freight order and audit change;
if (Validation successful?) then (Yes)
:Persist / present successful result;
else (No)
:Display validation or processing error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-24 — Manage Freight Orders — Sequence Diagram
actor "Freight Manager" as A
participant "Freight Order UI" as UI
participant "Freight Order Service" as S
database "Freight Order Repository" as R
A -> UI: Start Manage Freight Orders
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-25 — Manage Cargo Manifest

**Primary Actor:** Freight Manager

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Freight Manager" as A
rectangle "Transport & Logistics Management System" {
 usecase "Manage Cargo Manifest" as U0
 usecase "Create Cargo Manifest" as U1
 usecase "Maintain Cargo Items" as U2
 usecase "Classify Commodity" as U3
 usecase "Maintain Customs Information" as U4
 usecase "Apply Hazmat Labeling" as U5
 usecase "Validate Manifest" as U6
 usecase "Finalize Manifest" as U7
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-25 — Manage Cargo Manifest
start
:Select freight order;
:Create cargo manifest;
:Add cargo line items;
:Classify commodities;
:Enter customs information when required;
:Apply hazardous-material labels when required;
:Validate manifest completeness;
:Finalize and save manifest;
if (Validation successful?) then (Yes)
:Persist / present successful result;
else (No)
:Display validation or processing error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-25 — Manage Cargo Manifest — Sequence Diagram
actor "Freight Manager" as A
participant "Cargo Manifest UI" as UI
participant "Manifest Service" as S
database "Manifest Repository" as R
A -> UI: Start Manage Cargo Manifest
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-26 — Plan Cargo Loads

**Primary Actor:** Freight Manager

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Freight Manager" as A
rectangle "Transport & Logistics Management System" {
 usecase "Plan Cargo Loads" as U0
 usecase "Create Load Plan" as U1
 usecase "Plan Weight Distribution" as U2
 usecase "Apply Stacking Rules" as U3
 usecase "Plan Pallet Placement" as U4
 usecase "Optimize Container Utilization" as U5
 usecase "Separate Fragile Goods" as U6
 usecase "Place Temperature-Controlled Cargo" as U7
 usecase "Validate Load Plan" as U8
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
U8 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-26 — Plan Cargo Loads
start
:Select manifest / cargo items;
:Select vehicle or container;
:Load cargo dimensions and constraints;
:Plan weight distribution;
:Apply stacking and separation rules;
:Place pallets / cargo;
:Check temperature-controlled placement;
:Validate capacity and load plan;
:Save approved load plan;
if (Validation successful?) then (Yes)
:Persist / present successful result;
else (No)
:Display validation or processing error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-26 — Plan Cargo Loads — Sequence Diagram
actor "Freight Manager" as A
participant "Load Planning UI" as UI
participant "Load Planning Service" as S
database "Load Plan Repository" as R
A -> UI: Start Plan Cargo Loads
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-27 — Calculate Weight and Volume

**Primary Actor:** Freight Manager

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Freight Manager" as A
rectangle "Transport & Logistics Management System" {
 usecase "Calculate Weight and Volume" as U0
 usecase "Calculate Gross Weight" as U1
 usecase "Calculate Net Weight" as U2
 usecase "Calculate Cubic Volume" as U3
 usecase "Validate Axle Load" as U4
 usecase "Prevent Overload" as U5
 usecase "Record Weight Discrepancy" as U6
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-27 — Calculate Weight and Volume
start
:Select cargo / load plan;
:Load item quantities, dimensions and weights;
:Calculate net weight;
:Calculate gross weight;
:Calculate cubic volume;
:Validate vehicle / container capacity;
:Validate axle load;
:Block overload or flag discrepancy;
:Save verified calculations;
if (Within limits?) then (Yes)
:Confirm calculated values;
else (No)
:Block overload / require correction;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-27 — Calculate Weight and Volume — Sequence Diagram
actor "Freight Manager" as A
participant "Weight & Volume UI" as UI
participant "Cargo Calculation Service" as S
database "Load Plan Repository" as R
A -> UI: Start Calculate Weight and Volume
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-28 — Manage Freight Insurance

**Primary Actor:** Freight Manager

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Freight Manager" as A
rectangle "Transport & Logistics Management System" {
 usecase "Manage Freight Insurance" as U0
 usecase "Verify Insurance Coverage" as U1
 usecase "Track Insurance Premium" as U2
 usecase "Create Insurance Claim" as U3
 usecase "Assess Cargo Damage" as U4
 usecase "Process Claim Workflow" as U5
 usecase "Track Claim Settlement" as U6
 usecase "Link Claim to Freight Order" as U7
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-28 — Manage Freight Insurance
start
:Select freight order / shipment;
:Verify insurance coverage;
:Record policy and premium details;
:If damage occurs, create claim;
:Record damage assessment and evidence;
:Submit claim workflow;
:Track insurer decision / settlement;
:Close or escalate claim;
if (Claim settled?) then (Yes)
:Record settlement and close claim;
else (No)
:Keep claim pending / escalate;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-28 — Manage Freight Insurance — Sequence Diagram
actor "Freight Manager" as A
participant "Insurance & Claims UI" as UI
participant "Freight Insurance Service" as S
database "Insurance Repository" as R
A -> UI: Start Manage Freight Insurance
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-29 — Generate Freight Reports

**Primary Actor:** Freight Manager

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Freight Manager" as A
rectangle "Transport & Logistics Management System" {
 usecase "Generate Freight Reports" as U0
 usecase "Generate Shipment Status Report" as U1
 usecase "Generate Cargo Utilization Report" as U2
 usecase "Generate Claims Report" as U3
 usecase "Generate Compliance Report" as U4
 usecase "Filter Report Data" as U5
 usecase "Export Freight Report" as U6
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-29 — Generate Freight Reports
start
:Open Freight Reports;
:Select report type;
:Enter filters and reporting period;
:Load operational freight data;
:Aggregate requested metrics;
:Generate read-only report;
:Display report;
:Export if requested;
if (Validation successful?) then (Yes)
:Persist / present successful result;
else (No)
:Display validation or processing error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-29 — Generate Freight Reports — Sequence Diagram
actor "Freight Manager" as A
participant "Freight Reporting UI" as UI
participant "Freight Reporting Service" as S
database "Freight Data Repositories" as R
A -> UI: Start Generate Freight Reports
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

# US-30 — Handle Cargo Exceptions

**Primary Actor:** Freight Manager

## Use Case Diagram — PlantUML

``` plantuml
@startuml
left to right direction
actor "Freight Manager" as A
rectangle "Transport & Logistics Management System" {
 usecase "Handle Cargo Exceptions" as U0
 usecase "Handle Damaged Cargo" as U1
 usecase "Handle Partial Delivery" as U2
 usecase "Handle Weight Discrepancy" as U3
 usecase "Handle Hazardous Material Exception" as U4
 usecase "Handle Unmanifested Cargo" as U5
 usecase "Handle Seal Tampering" as U6
 usecase "Create Insurance Claim" as U7
 usecase "Correct Manifest" as U8
 usecase "Place Cargo on Hold" as U9
 usecase "Escalate Cargo Exception" as U10
 usecase "Release Cargo" as U11
}
A --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
U8 .> U0 : <<extend>>
U9 .> U0 : <<extend>>
U10 .> U0 : <<extend>>
U11 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

``` plantuml
@startuml
title US-30 — Handle Cargo Exceptions
start
:Detect / report cargo exception;
:Load freight order, manifest and load data;
:Classify exception;
:Record evidence and severity;
:Apply hold when required;
:Perform corrective action;
:Create claim / correct manifest / escalate when applicable;
:Revalidate cargo compliance;
:Release cargo if safe and valid;
:Close or keep exception open;
if (Resolved / feasible?) then (Yes)
:Save resolution and audit trail;
else (No)
:Escalate and keep exception open;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

``` plantuml
@startuml
title US-30 — Handle Cargo Exceptions — Sequence Diagram
actor "Freight Manager" as A
participant "Cargo Exception UI" as UI
participant "Cargo Exception Service" as S
database "Freight / Manifest Repository" as R
A -> UI: Start Handle Cargo Exceptions
UI -> S: Submit request / criteria
S -> R: Load required domain data
R --> S: Domain data
S -> S: Apply business rules and validation
alt Successful / valid
 S -> R: Save result / retrieve analysis
 R --> S: Confirmed data
 S --> UI: Success / result
 UI --> A: Display outcome
else Invalid / exception
 S --> UI: Validation, conflict or escalation details
 UI --> A: Display corrective action
end
@enduml
```

<hr>

## Coverage Summary

| User Story | Title                       | Use Case | Activity | Sequence |
|------------|-----------------------------|---------:|---------:|---------:|
| US-21      | Maintain Route History      |      Yes |      Yes |      Yes |
| US-22      | Analyze Route Performance   |      Yes |      Yes |      Yes |
| US-23      | Handle Route Disruptions    |      Yes |      Yes |      Yes |
| US-24      | Manage Freight Orders       |      Yes |      Yes |      Yes |
| US-25      | Manage Cargo Manifest       |      Yes |      Yes |      Yes |
| US-26      | Plan Cargo Loads            |      Yes |      Yes |      Yes |
| US-27      | Calculate Weight and Volume |      Yes |      Yes |      Yes |
| US-28      | Manage Freight Insurance    |      Yes |      Yes |      Yes |
| US-29      | Generate Freight Reports    |      Yes |      Yes |      Yes |
| US-30      | Handle Cargo Exceptions     |      Yes |      Yes |      Yes |

## Source Basis

The story boundaries and terminology follow the supplied Transportation
& Logistics requirements and mind map. Route History remains distinct
from Route Analytics; route disruptions remain distinct from route
definition/optimization; Freight Orders remain distinct from manifests
and physical load planning; and Cargo Exceptions use a shared exception
lifecycle rather than duplicating the same workflow for every exception
type.
