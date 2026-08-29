# Transport & Logistics Management System

## UML Use Case and Activity Diagrams --- US-01 to US-10

This document consolidates the UML diagrams prepared for User Stories
US-01 through US-10, based on the Transportation & Logistics mind map.

------------------------------------------------------------------------

# US-01 --- Manage Vehicle Master

**Primary Actor:** Fleet Manager\
**Goal:** Maintain company and rental fleet vehicle information, QR
identification, ownership/registration details, asset tagging, and
operational status.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Create Vehicle" as UC1
 usecase "View Vehicle Details" as UC2
 usecase "Update Vehicle" as UC3
 usecase "Manage Vehicle Status" as UC4
 usecase "Classify Fleet Ownership" as UC5
 usecase "Manage Vehicle QR Code" as UC6
 usecase "Maintain Registration Details" as UC7
 usecase "Maintain Asset Tagging" as UC8
 usecase "Set Status as Available" as UC9
 usecase "Set Status as Allocated" as UC10
 usecase "Set Status as In Maintenance" as UC11
 usecase "Set Status as Out of Service" as UC12
 usecase "Set Status as Retired" as UC13
 usecase "Validate Vehicle Data" as UC14
 usecase "Check Duplicate Vehicle / QR" as UC15
}
FM --> UC1
FM --> UC2
FM --> UC3
FM --> UC4
FM --> UC5
FM --> UC6
FM --> UC7
FM --> UC8
UC1 .> UC14 : <<include>>
UC3 .> UC14 : <<include>>
UC1 .> UC15 : <<include>>
UC6 .> UC15 : <<include>>
UC9 -|> UC4
UC10 -|> UC4
UC11 -|> UC4
UC12 -|> UC4
UC13 -|> UC4
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-01 — Manage Vehicle Master
start
:Fleet Manager opens Vehicle Master;
if (Choose action?) then (Create)
  :Enter vehicle details;
  :Select ownership / fleet classification;
  :Maintain registration and asset tag details;
  :Generate / assign Vehicle QR Code;
  :Validate mandatory vehicle data;
  :Check duplicate vehicle / QR identification;
  if (Valid and unique?) then (Yes)
    :Save Vehicle;
    :Set initial operational status;
    :Record audit information;
    :Display creation success;
  else (No)
    :Display validation / duplicate error;
  endif
elseif (View)
  :Search / select vehicle;
  :Display vehicle details and current status;
elseif (Update)
  :Search / select vehicle;
  :Edit allowed vehicle details;
  :Validate updated data;
  if (Update valid?) then (Yes)
    :Save changes;
    :Record change history;
    :Display update success;
  else (No)
    :Display validation errors;
  endif
elseif (Change Status)
  :Select vehicle;
  :Select Available / Allocated / In Maintenance / Out of Service / Retired;
  :Validate status transition;
  if (Transition allowed?) then (Yes)
    :Update vehicle status;
    :Record status history;
  else (No)
    :Display invalid status transition;
  endif
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-01 — Manage Vehicle Master — Sequence Diagram
actor "Fleet Manager" as FM
participant "Vehicle Master" as UI
participant "Vehicle Service" as VS
participant "Validation / Duplicate Check" as VAL
database "Vehicle Repository" as VR
participant "Audit Service" as AUD
FM -> UI: Create / update / change vehicle status
UI -> VS: Submit vehicle command
VS -> VAL: Validate data and uniqueness
VAL --> VS: Validation result
alt Valid
  VS -> VR: Save vehicle / status
  VR --> VS: Saved vehicle
  VS -> AUD: Record business change
  VS --> UI: Success + vehicle details
  UI --> FM: Display success
else Invalid / duplicate
  VS --> UI: Validation error
  UI --> FM: Display error
end
@enduml
```

------------------------------------------------------------------------

# US-02 --- Manage Fleet Categories

**Primary Actor:** Fleet Manager\
**Goal:** Classify vehicles consistently by vehicle type, capacity
class, usage category, ownership classification, and special equipment
requirements.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Create Fleet Category" as UC1
 usecase "View Fleet Categories" as UC2
 usecase "Update Fleet Category" as UC3
 usecase "Delete / Deactivate\nFleet Category" as UC4
 usecase "Define Vehicle Type" as UC5
 usecase "Define Capacity Class" as UC6
 usecase "Define Usage Category" as UC7
 usecase "Define Ownership\nClassification" as UC8
 usecase "Define Special Equipment\nRequirements" as UC9
 usecase "Validate Category Data" as UC10
 usecase "Check Category Usage\nBefore Delete" as UC11
}
FM --> UC1
FM --> UC2
FM --> UC3
FM --> UC4
UC1 .> UC5 : <<include>>
UC1 .> UC6 : <<include>>
UC1 .> UC7 : <<include>>
UC1 .> UC8 : <<include>>
UC1 .> UC9 : <<include>>
UC1 .> UC10 : <<include>>
UC3 .> UC5 : <<include>>
UC3 .> UC6 : <<include>>
UC3 .> UC7 : <<include>>
UC3 .> UC8 : <<include>>
UC3 .> UC9 : <<include>>
UC3 .> UC10 : <<include>>
UC4 .> UC11 : <<include>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-02 — Manage Fleet Categories
start
:Fleet Manager opens Fleet Categories;
if (Choose action?) then (Create)
  :Enter category details;
  :Define Vehicle Type;
  :Define Capacity Class;
  :Define Usage Category;
  :Define Ownership Classification;
  :Define Special Equipment Requirements;
  :Validate category data;
  if (Category valid?) then (Yes)
    :Save Fleet Category;
    :Display creation success;
  else (No)
    :Display validation errors;
  endif
elseif (View)
  :Display Fleet Categories;
elseif (Update)
  :Select Fleet Category;
  :Edit classification details;
  :Validate updated category data;
  if (Update valid?) then (Yes)
    :Save changes;
    :Record change history;
  else (No)
    :Display validation errors;
  endif
elseif (Delete / Deactivate)
  :Select Fleet Category;
  :Check category usage;
  if (Category referenced?) then (Yes)
    :Prevent unsafe deletion;
    :Offer deactivation where allowed;
  else (No)
    :Delete / deactivate Fleet Category;
  endif
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-02 — Manage Fleet Categories — Sequence Diagram
actor "Fleet Manager" as FM
participant "Fleet Category UI" as UI
participant "Fleet Category Service" as FS
database "Category Repository" as CR
database "Vehicle Repository" as VR
FM -> UI: Create / update / deactivate category
UI -> FS: Submit category command
FS -> FS: Validate classification data
alt Deactivate / delete
  FS -> VR: Check category usage
  VR --> FS: Usage result
end
alt Valid and safe
  FS -> CR: Save category state
  CR --> FS: Saved category
  FS --> UI: Success
  UI --> FM: Display result
else Invalid / referenced
  FS --> UI: Validation / usage error
  UI --> FM: Display restriction
end
@enduml
```

------------------------------------------------------------------------

# US-03 --- Manage Vehicle Documents

**Primary Actor:** Fleet Manager\
**Goal:** Maintain vehicle compliance documents and track validity,
expiry, renewal, missing documents, and version history.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Upload Vehicle Document" as UC1
 usecase "View Vehicle Documents" as UC2
 usecase "Update Vehicle Document" as UC3
 usecase "Replace / Version\nVehicle Document" as UC4
 usecase "Delete / Archive\nVehicle Document" as UC5
 usecase "Validate Document Data" as UC6
 usecase "Check Document Type" as UC7
 usecase "Track Document Expiry" as UC8
 usecase "Generate Renewal Alert" as UC9
 usecase "Detect Missing Mandatory\nDocument" as UC10
 usecase "Maintain Document\nVersion History" as UC11
}
FM --> UC1
FM --> UC2
FM --> UC3
FM --> UC4
FM --> UC5
UC1 .> UC6 : <<include>>
UC1 .> UC7 : <<include>>
UC3 .> UC6 : <<include>>
UC3 .> UC7 : <<include>>
UC4 .> UC11 : <<include>>
UC8 .> UC9 : <<extend>>
UC2 .> UC8 : <<include>>
UC2 .> UC10 : <<include>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-03 — Manage Vehicle Documents
start
:Fleet Manager opens Vehicle Documents;
:Select Vehicle;
if (Vehicle exists?) then (Yes)
 :View existing vehicle documents;
 if (Choose action?) then (Upload New)
  :Select Document Type;
  :Enter document details;
  :Upload document file;
  :Validate mandatory fields;
  :Validate document type;
  :Validate issue / expiry dates;
  if (Document data valid?) then (Yes)
   :Save Vehicle Document;
   :Create initial document version;
   :Link document to vehicle;
   :Record audit information;
   :Display success message;
  else (No)
   :Display validation errors;
   :Return to document entry;
  endif
 elseif (Update / Replace)
  :Select existing document;
  :View current document details;
  :Enter updated details;
  :Upload replacement file if required;
  :Validate updated document data;
  if (Update valid?) then (Yes)
   :Preserve previous document version;
   :Create new document version;
   :Update active document record;
   :Record version history;
   :Record audit information;
   :Display update success;
  else (No)
   :Display validation errors;
   :Return to update form;
  endif
 elseif (View)
  :Display Vehicle Document Details;
  :Display Document Version History;
 elseif (Delete / Archive)
  :Select document;
  if (Deletion allowed?) then (Yes)
   :Archive / Deactivate document;
   :Preserve document history;
   :Record audit information;
   :Display archive success;
  else (No)
   :Display deletion restriction;
  endif
 endif
 :Check mandatory vehicle documents;
 if (Mandatory document missing?) then (Yes)
  :Flag Missing Document;
  :Mark compliance condition;
 endif
 :Check document expiry dates;
 if (Document expired?) then (Yes)
  :Mark document as Expired;
  :Flag vehicle compliance issue;
 elseif (Document approaching expiry?)
  :Generate Renewal Alert;
 endif
 :Refresh Vehicle Document List;
else (No)
 :Display Vehicle Not Found Error;
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-03 — Manage Vehicle Documents — Sequence Diagram
actor "Fleet Manager" as FM
participant "Vehicle Documents UI" as UI
participant "Document Service" as DS
database "Vehicle Repository" as VR
database "Document Repository" as DR
participant "Alert Service" as AS
participant "Audit Service" as AUD
FM -> UI: Upload / replace / view document
UI -> DS: Submit document request
DS -> VR: Validate vehicle
VR --> DS: Vehicle result
DS -> DS: Validate type, dates and mandatory data
alt Valid
  DS -> DR: Save document / new version
  DR --> DS: Saved version
  DS -> AS: Evaluate expiry / renewal / missing docs
  DS -> AUD: Record change
  DS --> UI: Success + document state
else Invalid
  DS --> UI: Validation error
end
UI --> FM: Display result
@enduml
```

------------------------------------------------------------------------

# US-04 --- Allocate Vehicles

**Primary Actor:** Fleet Manager\
**Goal:** Match, reserve, and approve an eligible vehicle while
preventing conflicts and overbooking.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Match Available Vehicles" as UC1
 usecase "View Reservation Calendar" as UC2
 usecase "Reserve Vehicle" as UC3
 usecase "Validate Vehicle Status" as UC4
 usecase "Validate Allocation Conflict" as UC5
 usecase "Prevent Overbooking" as UC6
 usecase "Apply Trip Priority" as UC7
 usecase "Apply Priority Allocation Rules" as UC8
 usecase "Resolve Allocation Conflict" as UC9
 usecase "Suggest Replacement Vehicle" as UC10
 usecase "Approve Vehicle Allocation" as UC11
}
FM --> UC1
FM --> UC2
FM --> UC3
FM --> UC9
FM --> UC11
UC1 .> UC4 : <<include>>
UC1 .> UC7 : <<include>>
UC1 .> UC8 : <<include>>
UC3 .> UC5 : <<include>>
UC3 .> UC4 : <<include>>
UC5 .> UC6 : <<include>>
UC9 .> UC10 : <<extend>>
UC11 .> UC5 : <<include>>
UC11 .> UC4 : <<include>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-04 — Allocate Vehicles
start
:Fleet Manager opens Vehicle Allocation;
:Select Trip / Transport Requirement;
:Enter required allocation period;
:Check Trip Priority;
:Apply Priority Allocation Rules;
:Search Available Vehicles;
:Validate Vehicle Status;
if (Eligible vehicles available?) then (Yes)
 :Display Matching Vehicles;
 :View Reservation Calendar;
 :Fleet Manager selects Vehicle;
 :Check Existing Reservations;
 :Validate Allocation Conflict;
 if (Allocation conflict exists?) then (Yes)
  :Prevent Vehicle Overbooking;
  :Display Conflict Details;
  :Resolve Allocation Conflict;
  if (Alternative vehicle required?) then (Yes)
   :Suggest Replacement Vehicles;
   if (Replacement available?) then (Yes)
    :Fleet Manager selects Replacement Vehicle;
    :Validate Vehicle Status;
    :Check Reservation Calendar;
    :Validate Allocation Conflict;
    if (Replacement conflict free?) then (Yes)
     :Reserve Replacement Vehicle;
    else (No)
     :Display Replacement Conflict;
     stop
    endif
   else (No)
    :Display No Replacement Available;
    stop
   endif
  else (No)
   stop
  endif
 else (No)
  :Reserve Selected Vehicle;
 endif
 :Create Vehicle Allocation Record;
 :Set Allocation Status to Pending Approval;
 :Submit Allocation for Approval;
 if (Allocation approved?) then (Yes)
  :Approve Vehicle Allocation;
  :Mark Vehicle as Allocated;
  :Update Reservation Calendar;
  :Record Allocation History;
  :Display Allocation Success;
 else (No)
  :Record Rejection / Non-Approval;
  :Release Vehicle Reservation;
  :Keep Vehicle Available;
  :Display Allocation Not Approved;
 endif
else (No)
 :Display No Eligible Vehicle Available;
 :Suggest Replacement / Alternative Vehicles;
 :Record Unfulfilled Allocation Requirement;
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-04 — Allocate Vehicles — Sequence Diagram
actor "Fleet Manager" as FM
participant "Allocation UI" as UI
participant "Allocation Service" as AS
participant "Availability Service" as AV
database "Reservation Repository" as RR
database "Vehicle Repository" as VR
FM -> UI: Request vehicle allocation
UI -> AS: Submit trip, period, priority
AS -> AV: Find eligible vehicles
AV -> VR: Check status / availability
VR --> AV: Candidate vehicles
AV -> RR: Check reservations
RR --> AV: Reservation conflicts
AV --> AS: Eligible vehicles
AS --> UI: Show candidates
FM -> UI: Select vehicle
UI -> AS: Reserve selected vehicle
AS -> RR: Validate conflict
alt Conflict
  RR --> AS: Conflict detected
  AS --> UI: Conflict + replacement suggestions
else Conflict-free
  RR --> AS: Available
  AS -> RR: Create reservation
  AS -> VR: Mark vehicle allocated after approval
  AS --> UI: Allocation confirmed
end
UI --> FM: Display outcome
@enduml
```

------------------------------------------------------------------------

# US-05 --- Maintain Fuel & Lubricant Logs

**Primary Actor:** Fleet Manager\
**Goal:** Record and maintain vehicle fuel and lubricant usage so
consumption can be validated and reviewed.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Record Fuel Entry" as UC1
 usecase "Record Lubricant Entry" as UC2
 usecase "Record Service Refill" as UC3
 usecase "View Fuel & Lubricant Logs" as UC4
 usecase "Update Log Entry" as UC5
 usecase "Map Vendor" as UC6
 usecase "Validate Usage Entry" as UC7
 usecase "Validate Vehicle Reference" as UC8
 usecase "Validate Quantity and Date" as UC9
 usecase "Analyze Usage History" as UC10
}
FM --> UC1
FM --> UC2
FM --> UC3
FM --> UC4
FM --> UC5
FM --> UC10
UC1 .> UC7 : <<include>>
UC1 .> UC8 : <<include>>
UC1 .> UC9 : <<include>>
UC1 .> UC6 : <<extend>>
UC2 .> UC7 : <<include>>
UC2 .> UC8 : <<include>>
UC2 .> UC9 : <<include>>
UC2 .> UC6 : <<extend>>
UC3 .> UC7 : <<include>>
UC3 .> UC8 : <<include>>
UC3 .> UC9 : <<include>>
UC3 .> UC6 : <<extend>>
UC5 .> UC7 : <<include>>
UC5 .> UC9 : <<include>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-05 — Maintain Fuel & Lubricant Logs
start
:Fleet Manager opens Fuel & Lubricant Logs;
:Select Vehicle;
if (Vehicle exists?) then (Yes)
 :View existing Fuel & Lubricant Logs;
 if (Choose Action?) then (Record Fuel Entry)
  :Enter Fuel Quantity, Transaction Date and Usage Reference;
  if (Vendor applicable?) then (Yes)
   :Select / Map Fuel Vendor;
  endif
  :Validate Vehicle Reference;
  :Validate Quantity and Date;
  :Validate Usage Entry;
  if (Entry valid?) then (Yes)
   :Save Fuel Entry;
   :Link Entry to Vehicle;
   :Record Audit Information;
   :Display Success Message;
  else (No)
   :Display Validation Errors;
  endif
 elseif (Record Lubricant Entry)
  :Enter Lubricant Type, Quantity and Date;
  if (Vendor applicable?) then (Yes)
   :Select / Map Lubricant Vendor;
  endif
  :Validate Usage Entry;
  if (Entry valid?) then (Yes)
   :Save Lubricant Entry;
   :Link Entry to Vehicle;
   :Display Success Message;
  else (No)
   :Display Validation Errors;
  endif
 elseif (Record Service Refill)
  :Enter Refill Details;
  :Validate Usage Entry;
  if (Entry valid?) then (Yes)
   :Save Service Refill Record;
   :Link Refill to Vehicle;
  else (No)
   :Display Validation Errors;
  endif
 elseif (Update Existing Entry)
  :Select Existing Log Entry;
  :Edit Allowed Fields;
  :Validate Updated Data;
  if (Update valid?) then (Yes)
   :Save Updated Entry;
   :Record Change History;
  else (No)
   :Display Validation Errors;
  endif
 elseif (View History)
  :Display Fuel & Lubricant History;
  :Display Vehicle Usage History;
 endif
 :Refresh Fuel & Lubricant Log List;
else (No)
 :Display Vehicle Not Found Error;
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-05 — Maintain Fuel & Lubricant Logs — Sequence Diagram
actor "Fleet Manager" as FM
participant "Fuel/Lubricant UI" as UI
participant "Log Service" as LS
database "Vehicle Repository" as VR
database "Fuel/Lubricant Repository" as LR
database "Vendor Repository" as VEND
FM -> UI: Record fuel / lubricant / refill
UI -> LS: Submit log entry
LS -> VR: Validate vehicle
VR --> LS: Vehicle result
opt Vendor supplied
  LS -> VEND: Validate / map vendor
  VEND --> LS: Vendor result
end
LS -> LS: Validate quantity, date and usage
alt Valid
  LS -> LR: Save log entry
  LR --> LS: Saved entry
  LS --> UI: Success
else Invalid
  LS --> UI: Validation errors
end
UI --> FM: Display result / history
@enduml
```

------------------------------------------------------------------------

# US-06 --- Maintain Running Logs

**Primary Actor:** Fleet Manager\
**Goal:** Record and validate vehicle running information including
kilometers, engine hours, trip-wise usage and idle time.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Record Daily Kilometer Log" as UC1
 usecase "Record Engine Hours" as UC2
 usecase "Record Trip-Wise Usage" as UC3
 usecase "Record Idle Time" as UC4
 usecase "View Running Log History" as UC5
 usecase "Update Running Log" as UC6
 usecase "Validate Running Log Entry" as UC7
 usecase "Validate Odometer Reading" as UC8
 usecase "Validate Engine Hour Reading" as UC9
 usecase "Analyze Usage Trends" as UC10
 usecase "Record Breakdown Reference" as UC11
}
FM --> UC1
FM --> UC2
FM --> UC3
FM --> UC4
FM --> UC5
FM --> UC6
FM --> UC10
UC1 .> UC7 : <<include>>
UC1 .> UC8 : <<include>>
UC2 .> UC7 : <<include>>
UC2 .> UC9 : <<include>>
UC3 .> UC7 : <<include>>
UC4 .> UC7 : <<include>>
UC6 .> UC7 : <<include>>
UC6 .> UC8 : <<include>>
UC6 .> UC9 : <<include>>
UC3 .> UC11 : <<extend>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-06 — Maintain Running Logs
start
:Fleet Manager opens Running Logs;
:Select Vehicle;
if (Vehicle exists?) then (Yes)
 :View existing Running Log history;
 if (Choose Action?) then (Create Running Log)
  :Enter Log Date;
  :Enter Daily Kilometer / Odometer Reading;
  :Enter Engine Hours;
  if (Trip-wise usage applicable?) then (Yes)
   :Select / Link Trip;
   :Enter Trip-Wise Usage;
  endif
  if (Idle time available?) then (Yes)
   :Enter Idle Time;
  endif
  if (Breakdown occurred?) then (Yes)
   :Record Breakdown Reference;
  endif
  :Validate mandatory fields;
  :Validate Odometer Reading;
  :Validate Engine Hour Reading;
  :Validate Running Log Entry;
  if (Running Log valid?) then (Yes)
   :Save Running Log;
   :Link Running Log to Vehicle;
   :Record Audit Information;
   :Display Success Message;
  else (No)
   :Display Validation Errors;
  endif
 elseif (Update Running Log)
  :Select Existing Running Log;
  :Edit Allowed Fields;
  :Validate Updated Data;
  if (Updated data valid?) then (Yes)
   :Save Updated Running Log;
   :Record Change History;
  else (No)
   :Display Validation Errors;
  endif
 elseif (View Running Log)
  :Display Running Log Details and History;
 elseif (Analyze Usage Trends)
  :Load Historical Running Logs;
  :Calculate Usage Trends;
  :Display Usage Trend Summary;
 endif
 :Refresh Running Log List;
else (No)
 :Display Vehicle Not Found Error;
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-06 — Maintain Running Logs — Sequence Diagram
actor "Fleet Manager" as FM
participant "Running Log UI" as UI
participant "Running Log Service" as RS
database "Vehicle Repository" as VR
database "Running Log Repository" as RR
database "Trip Repository" as TR
FM -> UI: Create / update running log
UI -> RS: Submit odometer, engine hours, usage, idle time
RS -> VR: Validate vehicle
VR --> RS: Vehicle result
opt Trip-wise usage
  RS -> TR: Validate trip reference
  TR --> RS: Trip result
end
RS -> RR: Load previous readings
RR --> RS: Previous odometer / engine hours
RS -> RS: Validate readings and chronology
alt Valid
  RS -> RR: Save running log
  RS --> UI: Success
else Invalid
  RS --> UI: Validation error
end
UI --> FM: Display result
@enduml
```

------------------------------------------------------------------------

# US-07 --- Link Maintenance to Availability

**Primary Actor:** Fleet Manager\
**Goal:** Ensure preventive maintenance and breakdown information affect
vehicle availability and prevent invalid allocations.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "View Maintenance-Linked\nAvailability" as UC1
 usecase "Check Maintenance\nScheduling Dependencies" as UC2
 usecase "Trigger Preventive\nMaintenance" as UC3
 usecase "Evaluate Vehicle\nAvailability" as UC4
 usecase "Validate Maintenance\nStatus" as UC5
 usecase "Mark Vehicle as\nIn Maintenance" as UC6
 usecase "Block Vehicle Allocation" as UC7
 usecase "Restore Vehicle\nAvailability" as UC8
}
FM --> UC1
FM --> UC2
FM --> UC3
FM --> UC8
UC1 .> UC4 : <<include>>
UC4 .> UC5 : <<include>>
UC2 .> UC4 : <<include>>
UC3 .> UC6 : <<include>>
UC6 .> UC7 : <<include>>
UC4 .> UC7 : <<extend>>
UC8 .> UC5 : <<include>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-07 — Link Maintenance to Availability
start
:Fleet Manager opens Vehicle Availability / Maintenance Linkage;
:Select Vehicle;
if (Vehicle exists?) then (Yes)
 :Load current Vehicle Status;
 :Load Maintenance Records;
 :Load Service Scheduling Dependencies;
 :Validate Maintenance Status;
 if (Active maintenance exists?) then (Yes)
  :Mark Vehicle as In Maintenance;
  :Set Vehicle as Unavailable;
  :Block New Vehicle Allocation;
 elseif (Preventive maintenance due?)
  :Trigger Preventive Maintenance;
  :Apply Maintenance Scheduling Restriction;
  :Set Vehicle as Unavailable;
  :Block New Allocation;
 elseif (Active breakdown exists?)
  :Mark Vehicle as Unavailable;
  :Block Vehicle Allocation;
  :Flag Breakdown Condition;
 else
  :Check Maintenance Scheduling Dependencies;
  if (Maintenance dependency exists?) then (Yes)
   :Restrict Vehicle Availability;
  else (No)
   :Vehicle remains eligible for availability evaluation;
  endif
 endif
 :Recalculate Vehicle Availability;
 if (Maintenance completed?) then (Yes)
  :Validate Maintenance Completion;
  :Check Remaining Maintenance Restrictions;
  if (No active restriction?) then (Yes)
   :Restore Vehicle Availability;
   :Mark Vehicle as Available;
  else (No)
   :Keep Vehicle Unavailable;
  endif
 endif
 :Update Vehicle Availability Status;
 :Record Status / Availability Change;
else (No)
 :Display Vehicle Not Found Error;
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-07 — Link Maintenance to Availability — Sequence Diagram
actor "Fleet Manager" as FM
participant "Availability UI" as UI
participant "Availability Service" as AV
database "Maintenance Repository" as MR
database "Vehicle Repository" as VR
participant "Allocation Service" as AL
FM -> UI: Review maintenance-linked availability
UI -> AV: Evaluate vehicle
AV -> MR: Load maintenance / breakdown / schedule dependencies
MR --> AV: Maintenance state
alt Active maintenance / due PM / breakdown
  AV -> VR: Mark unavailable / in maintenance
  AV -> AL: Block new allocation
  AV --> UI: Unavailable + reason
else No blocking condition
  AV -> VR: Keep / restore availability
  AV --> UI: Available
end
UI --> FM: Display availability
@enduml
```

------------------------------------------------------------------------

# US-08 --- Handle Fleet Exceptions

**Primary Actor:** Fleet Manager\
**Goal:** Detect and control abnormal fleet conditions so invalid or
unsafe allocations are prevented.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Fleet Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Handle Fleet Exception" as UC0
 usecase "Handle Vehicle Breakdown\nDuring Allocation" as UC1
 usecase "Detect Missing\nVehicle Documents" as UC2
 usecase "Detect Expired\nRegistration" as UC3
 usecase "Detect Double\nAllocation Request" as UC4
 usecase "Detect Maintenance\nStatus Conflict" as UC5
 usecase "Detect Expired\nRental Contract" as UC6
 usecase "Block Invalid Allocation" as UC7
 usecase "Flag Fleet Exception" as UC8
 usecase "Suggest Replacement Vehicle" as UC9
 usecase "Resolve Fleet Exception" as UC10
}
FM --> UC0
FM --> UC10
UC1 -|> UC0
UC2 -|> UC0
UC3 -|> UC0
UC4 -|> UC0
UC5 -|> UC0
UC6 -|> UC0
UC0 .> UC8 : <<include>>
UC0 .> UC7 : <<extend>>
UC0 .> UC9 : <<extend>>
UC10 .> UC8 : <<include>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-08 — Handle Fleet Exceptions
start
:Fleet Manager initiates / reviews vehicle allocation;
:Load Vehicle Details;
:Load Allocation Details;
:Load Vehicle Documents;
:Load Maintenance Status;
:Load Rental Contract Details;
:Evaluate Fleet Exception Conditions;
if (Vehicle breakdown during allocation?) then (Yes)
 :Flag Vehicle Breakdown Exception;
 :Mark Vehicle Unavailable;
 :Block Current Allocation;
 :Suggest Replacement Vehicle;
elseif (Mandatory document missing?)
 :Flag Missing Document Exception;
 :Block Vehicle Allocation;
elseif (Registration expired?)
 :Flag Expired Registration Exception;
 :Mark Vehicle Non-Compliant;
 :Block Vehicle Allocation;
elseif (Double allocation detected?)
 :Flag Double Allocation Exception;
 :Display Conflicting Reservation Details;
 :Prevent Overbooking;
elseif (Vehicle under maintenance marked Available?)
 :Flag Maintenance Status Conflict;
 :Correct Availability Status;
 :Mark Vehicle In Maintenance;
 :Block Vehicle Allocation;
elseif (Rental contract expired?)
 :Flag Expired Rental Contract;
 :Mark Rental Vehicle Unavailable;
 :Block Vehicle Allocation;
else (No Exception)
 :Continue Normal Vehicle Allocation;
endif
if (Fleet exception detected?) then (Yes)
 :Create / Update Fleet Exception Record;
 :Record Exception Type and Details;
 :Record Audit Information;
 :Fleet Manager reviews exception;
 if (Exception can be resolved?) then (Yes)
  :Perform Corrective Action;
  :Revalidate Vehicle Status, Documents, Contract and Allocation;
  if (Vehicle now valid?) then (Yes)
   :Mark Exception Resolved;
   :Allow Allocation Process to Continue;
  else (No)
   :Keep Exception Open;
   :Keep Vehicle Allocation Blocked;
  endif
 else (No)
  :Keep Exception Open;
  :Maintain Allocation Block;
 endif
endif
:Refresh Fleet / Allocation Status;
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-08 — Handle Fleet Exceptions — Sequence Diagram
actor "Fleet Manager" as FM
participant "Allocation / Fleet UI" as UI
participant "Fleet Exception Service" as ES
database "Vehicle Repository" as VR
database "Document Repository" as DR
database "Allocation Repository" as AR
database "Maintenance Repository" as MR
FM -> UI: Review allocation / fleet condition
UI -> ES: Evaluate fleet exception
ES -> VR: Load vehicle / rental state
ES -> DR: Check mandatory / expired documents
ES -> AR: Check duplicate allocation
ES -> MR: Check maintenance conflict
alt Exception detected
  ES -> VR: Apply unavailable / non-compliant state if required
  ES -> AR: Block invalid allocation if required
  ES --> UI: Exception + corrective / replacement options
  FM -> UI: Submit corrective action
  UI -> ES: Revalidate
  ES --> UI: Resolved or remains blocked
else No exception
  ES --> UI: Continue normal allocation
end
UI --> FM: Display result
@enduml
```

------------------------------------------------------------------------

# US-09 --- Create Trip Orders

**Primary Actor:** Dispatcher / Transport Coordinator\
**Goal:** Create Trip Orders manually, in bulk, from templates, or as
recurring trips while preserving priority and customer instructions.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Dispatcher / Transport Coordinator" as DISP
rectangle "Transport & Logistics Management System" {
 usecase "Create Trip Order" as UC0
 usecase "Create Trip Manually" as UC1
 usecase "Create Trips in Bulk" as UC2
 usecase "Create Trip from Template" as UC3
 usecase "Create Recurring Trip" as UC4
 usecase "Set Trip Priority" as UC5
 usecase "Record Customer Instructions" as UC6
 usecase "Validate Trip Order Data" as UC7
 usecase "Detect Duplicate Trip Order" as UC8
 usecase "Save Trip Order" as UC9
}
DISP --> UC0
UC1 -|> UC0
UC2 -|> UC0
UC3 -|> UC0
UC4 -|> UC0
UC0 .> UC7 : <<include>>
UC0 .> UC9 : <<include>>
UC0 .> UC5 : <<include>>
UC0 .> UC6 : <<extend>>
UC0 .> UC8 : <<extend>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-09 — Create Trip Orders
start
:Dispatcher opens Trip Order Creation;
:Select Creation Method;
if (Creation Method?) then (Manual)
 :Enter Trip Order Details;
elseif (Bulk)
 :Upload / Enter Bulk Trip Data;
 :Parse Bulk Records;
 if (Invalid bulk rows found?) then (Yes)
  :Display Invalid Rows;
  :Allow Correction / Exclusion;
 endif
elseif (Template)
 :Select Trip Template;
 :Load Template Data;
 :Modify Required Trip Details;
elseif (Recurring)
 :Enter Recurring Trip Details;
 :Define Recurrence Pattern;
 :Define Start / End Dates;
endif
:Set Trip Priority;
if (Customer instructions available?) then (Yes)
 :Record Customer Instructions;
endif
:Validate Mandatory Trip Data;
if (Trip Order data valid?) then (Yes)
 :Check Duplicate Trip Order;
 if (Duplicate detected?) then (Yes)
  :Display Duplicate Warning;
 endif
 :Create Trip Order;
 :Generate Trip Number / Reference;
 :Set Initial Trip Status;
 :Save Trip Order;
 :Record Audit Information;
 if (Recurring Trip?) then (Yes)
  :Generate / Schedule Recurring Trip Instances;
 endif
 if (Bulk Creation?) then (Yes)
  :Save Valid Bulk Trip Orders;
  :Report Failed / Rejected Rows;
 endif
 :Display Trip Order Creation Success;
else (No)
 :Display Validation Errors;
 :Return to Trip Order Entry;
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-09 — Create Trip Orders — Sequence Diagram
actor "Dispatcher / Transport Coordinator" as DISP
participant "Trip Order UI" as UI
participant "Trip Order Service" as TS
participant "Trip Validation Service" as VAL
database "Trip Repository" as TR
participant "Scheduler" as SCH
DISP -> UI: Select manual / bulk / template / recurring
UI -> TS: Submit trip order data
TS -> VAL: Validate mandatory data / duplicate risk
VAL --> TS: Validation result
alt Valid
  TS -> TR: Save Trip Order(s)
  TR --> TS: Trip reference(s)
  opt Recurring
    TS -> SCH: Schedule recurring trip instances
  end
  TS --> UI: Creation success
else Invalid / duplicate
  TS --> UI: Validation / duplicate details
end
UI --> DISP: Display result
@enduml
```

------------------------------------------------------------------------

# US-10 --- Assign Driver and Vehicle

**Primary Actor:** Dispatcher / Transport Coordinator\
**Goal:** Assign an available, eligible driver and suitable vehicle to a
Trip Order while enforcing availability, skills, license, capacity, and
fatigue rules.

## Use Case Diagram --- PlantUML

``` plantuml
@startuml
left to right direction
actor "Dispatcher / Transport Coordinator" as DISP
rectangle "Transport & Logistics Management System" {
 usecase "Assign Driver and Vehicle" as UC0
 usecase "Check Driver Availability" as UC1
 usecase "Check Vehicle Availability" as UC2
 usecase "Match Driver Skills" as UC3
 usecase "Validate Driver License" as UC4
 usecase "Validate Vehicle Capacity" as UC5
 usecase "Validate Driver Fatigue Rules" as UC6
 usecase "Assign Substitute Driver" as UC7
 usecase "Assign Substitute Vehicle" as UC8
 usecase "Escalate Assignment Approval" as UC9
 usecase "Confirm Trip Assignment" as UC10
}
DISP --> UC0
UC0 .> UC1 : <<include>>
UC0 .> UC2 : <<include>>
UC0 .> UC3 : <<include>>
UC0 .> UC4 : <<include>>
UC0 .> UC5 : <<include>>
UC0 .> UC6 : <<include>>
UC7 .> UC0 : <<extend>>
UC8 .> UC0 : <<extend>>
UC9 .> UC0 : <<extend>>
UC0 .> UC10 : <<include>>
@enduml
```

## Activity Diagram --- PlantUML

``` plantuml
@startuml
title US-10 — Assign Driver and Vehicle
start
:Dispatcher opens Driver & Vehicle Assignment;
:Select Trip Order;
if (Trip Order exists?) then (Yes)
 :Load trip requirements;
 :Search available drivers;
 :Search available vehicles;
 if (Available drivers found?) then (Yes)
  :Evaluate Driver Availability;
  :Match Driver Skills;
  :Validate Driver License;
  :Validate Driver Fatigue Rules;
  if (Driver eligible?) then (Yes)
   :Select Driver;
  else (No)
   :Display Driver Ineligibility Reason;
   if (Substitute driver available?) then (Yes)
    :Suggest Substitute Driver;
    :Select Substitute Driver;
    :Revalidate Driver Eligibility;
   else (No)
    :Escalate Assignment Approval;
    stop
   endif
  endif
 else (No)
  :Display No Available Driver;
  :Escalate Assignment Approval;
  stop
 endif
 if (Available vehicles found?) then (Yes)
  :Evaluate Vehicle Availability;
  :Validate Vehicle Capacity;
  if (Vehicle suitable?) then (Yes)
   :Select Vehicle;
  else (No)
   :Display Vehicle Ineligibility Reason;
   if (Substitute vehicle available?) then (Yes)
    :Suggest Substitute Vehicle;
    :Select Substitute Vehicle;
    :Revalidate Vehicle Availability and Capacity;
   else (No)
    :Escalate Assignment Approval;
    stop
   endif
  endif
 else (No)
  :Display No Available Vehicle;
  :Escalate Assignment Approval;
  stop
 endif
 :Validate Final Driver + Vehicle Combination;
 if (Assignment valid?) then (Yes)
  :Create Trip Assignment;
  :Link Driver to Trip;
  :Link Vehicle to Trip;
  :Confirm Trip Assignment;
  :Update Driver Availability;
  :Update Vehicle Availability;
  :Record Assignment History / Audit;
  :Display Assignment Success;
 else (No)
  :Display Assignment Validation Error;
  :Escalate or Re-select Resources;
 endif
else (No)
 :Display Trip Order Not Found;
endif
stop
@enduml
```

## Sequence Diagram --- PlantUML

``` plantuml
@startuml
title US-10 — Assign Driver and Vehicle — Sequence Diagram
actor "Dispatcher / Transport Coordinator" as DISP
participant "Assignment UI" as UI
participant "Assignment Service" as AS
participant "Driver Eligibility Service" as DS
participant "Vehicle Availability Service" as VS
database "Trip Repository" as TR
database "Assignment Repository" as AR
DISP -> UI: Select Trip Order
UI -> AS: Request assignment
AS -> TR: Load trip requirements
TR --> AS: Trip requirements
AS -> DS: Check availability, skills, license, fatigue
DS --> AS: Eligible drivers / reasons
AS -> VS: Check availability and capacity
VS --> AS: Eligible vehicles / reasons
alt Eligible combination exists
  AS --> UI: Show eligible resources
  DISP -> UI: Select driver + vehicle
  UI -> AS: Confirm selection
  AS -> DS: Revalidate driver
  AS -> VS: Revalidate vehicle
  AS -> AR: Save confirmed assignment
  AS --> UI: Assignment success
else No eligible combination
  AS --> UI: Substitute options / escalation required
end
UI --> DISP: Display outcome
@enduml
```

------------------------------------------------------------------------

## Coverage Summary

  User Story   Title                                Use Case   Activity   Sequence
  ------------ ---------------------------------- ---------- ---------- ----------
  US-01        Manage Vehicle Master                     Yes        Yes        Yes
  US-02        Manage Fleet Categories                   Yes        Yes        Yes
  US-03        Manage Vehicle Documents                  Yes        Yes        Yes
  US-04        Allocate Vehicles                         Yes        Yes        Yes
  US-05        Maintain Fuel & Lubricant Logs            Yes        Yes        Yes
  US-06        Maintain Running Logs                     Yes        Yes        Yes
  US-07        Link Maintenance to Availability          Yes        Yes        Yes
  US-08        Handle Fleet Exceptions                   Yes        Yes        Yes
  US-09        Create Trip Orders                        Yes        Yes        Yes
  US-10        Assign Driver and Vehicle                 Yes        Yes        Yes

------------------------------------------------------------------------

## Source Basis

This document is based on the supplied Transportation & Logistics mind
map and the previously consolidated US-01 to US-10 UML material.
Sequence diagrams were added for all ten stories, and activity diagrams
were completed for US-01 and US-02 so that every story now has all three
requested UML views.
