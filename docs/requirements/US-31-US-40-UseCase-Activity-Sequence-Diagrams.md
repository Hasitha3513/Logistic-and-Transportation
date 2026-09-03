# Transport & Logistics Management System

## UML Diagrams — US-31 to US-40

This document consolidates the requested UML views for **US-31 through US-40**. Each story includes a **Use Case Diagram**, **Activity Diagram**, and **Sequence Diagram** in PlantUML.

> Scope: US-31–US-38 cover Fuel Management. US-39–US-40 begin Driver Management. The diagrams preserve the established user-story boundaries and source terminology.

---

# US-31 — Issue Fuel

**Primary Actor:** Fuel Manager  
**Goal:** Authorize fuel issues using limits, vouchers, station validation, and trip linkage so fuel is released only for valid operational use.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Issue Fuel" as U0
 usecase "Authorize Fuel Issue" as U1
 usecase "Apply Fuel Limits" as U2
 usecase "Issue Fuel Voucher" as U3
 usecase "Validate Fuel Station" as U4
 usecase "Link Fuel Issue to Trip" as U5
 usecase "Validate Vehicle / Trip Eligibility" as U6
 usecase "Record Fuel Issue Transaction" as U7
 usecase "Handle Limit Exception" as U8
}
FM --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U8 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-31 — Issue Fuel
start
:Fuel Manager opens Fuel Issue;
:Select Vehicle / Trip;
:Enter Requested Fuel Quantity;
:Select Fuel Station;
:Validate Vehicle / Trip Eligibility;
:Validate Fuel Station;
:Apply Configured Fuel Limits;
if (Request within limits?) then (Yes)
 :Authorize Fuel Issue;
 :Generate Fuel Voucher;
 :Link Fuel Issue to Trip;
 :Record Fuel Issue Transaction;
 :Update applicable fuel records;
 :Record Audit Information;
 :Display Fuel Issue Success;
else (No)
 :Flag Limit Exception;
 if (Authorized override allowed?) then (Yes)
  :Record Override Reason and Approver;
  :Authorize Exceptional Fuel Issue;
  :Generate Fuel Voucher;
  :Record Fuel Issue Transaction;
 else (No)
  :Reject Fuel Issue;
  :Display Limit Exceeded Error;
 endif
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-31 — Issue Fuel — Sequence Diagram
actor "Fuel Manager" as FM
participant "Fuel Issue UI" as UI
participant "Fuel Issue Service" as FS
participant "Fuel Policy Service" as PS
database "Trip / Vehicle Repository" as TV
database "Fuel Issue Repository" as FR
FM -> UI: Enter fuel issue request
UI -> FS: Submit trip, vehicle, station, quantity
FS -> TV: Validate trip / vehicle
TV --> FS: Eligibility data
FS -> PS: Validate station and fuel limits
PS --> FS: Validation result
alt Valid / authorized
 FS -> FR: Save issue + voucher + trip linkage
 FR --> FS: Saved Fuel Issue
 FS --> UI: Success + voucher
else Limit / eligibility failure
 FS --> UI: Reject or request authorized override
end
UI --> FM: Display outcome
@enduml
```

---

# US-32 — Manage Fuel Purchases

**Primary Actor:** Fuel Manager  
**Goal:** Maintain fuel vendors, price information, purchases, reconciliation, and tax so fuel procurement is accurately controlled.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Fuel Purchases" as U0
 usecase "Manage Fuel Vendors" as U1
 usecase "Maintain Fuel Price Catalog" as U2
 usecase "Record Fuel Purchase" as U3
 usecase "Reconcile Fuel Purchases" as U4
 usecase "Record Fuel Tax" as U5
 usecase "Validate Purchase Data" as U6
 usecase "View Purchase History" as U7
}
FM --> U0
FM --> U7
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
title US-32 — Manage Fuel Purchases
start
:Open Fuel Purchases;
:Select / Validate Vendor;
:Select Fuel Type and Price;
:Enter Quantity and Purchase Date;
:Record Tax Information if applicable;
:Validate Purchase Data;
if (Purchase valid?) then (Yes)
 :Calculate Purchase Total;
 :Save Fuel Purchase;
 :Update Purchase History;
 :Reconcile against invoice / receipt;
 if (Reconciliation difference?) then (Yes)
  :Flag Reconciliation Variance;
 else (No)
  :Mark Purchase Reconciled;
 endif
 :Record Audit Information;
else (No)
 :Display Purchase Validation Error;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-32 — Manage Fuel Purchases — Sequence Diagram
actor "Fuel Manager" as FM
participant "Fuel Purchase UI" as UI
participant "Fuel Purchase Service" as PS
database "Vendor Repository" as VR
database "Price Catalog" as PC
database "Fuel Purchase Repository" as PR
FM -> UI: Enter purchase details
UI -> PS: Submit vendor, fuel, quantity, price, tax
PS -> VR: Validate vendor
VR --> PS: Vendor state
PS -> PC: Validate / retrieve price
PC --> PS: Price data
PS -> PS: Validate and calculate total
alt Valid
 PS -> PR: Save purchase + reconciliation state
 PR --> PS: Saved purchase
 PS --> UI: Success
else Invalid
 PS --> UI: Validation / reconciliation error
end
@enduml
```

---

# US-33 — Track Mileage

**Primary Actor:** Fuel Manager  
**Goal:** Capture odometer values, calculate mileage, and detect tampering or abnormal results so consumption information remains trustworthy.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Track Mileage" as U0
 usecase "Capture Odometer" as U1
 usecase "Calculate Mileage" as U2
 usecase "Detect Odometer Tampering" as U3
 usecase "Generate Abnormal Mileage Alert" as U4
 usecase "Validate Reading Sequence" as U5
 usecase "View Mileage History" as U6
}
FM --> U0
FM --> U6
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U5 : <<include>>
U3 .> U0 : <<extend>>
U4 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-33 — Track Mileage
start
:Select Vehicle;
:Load Previous Odometer / Mileage Data;
:Capture Current Odometer;
:Validate Reading Sequence;
if (Reading valid?) then (Yes)
 :Calculate Distance Travelled;
 :Load Fuel Consumption Data;
 :Calculate Mileage / Efficiency;
 :Check for Abnormal Mileage;
 if (Abnormal result?) then (Yes)
  :Generate Abnormal Mileage Alert;
 endif
 :Check Odometer Tampering Indicators;
 if (Tampering suspected?) then (Yes)
  :Flag Odometer Tampering;
 endif
 :Save Mileage Record;
else (No)
 :Reject Invalid Reading;
 :Record Validation Exception;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-33 — Track Mileage — Sequence Diagram
actor "Fuel Manager" as FM
participant "Mileage UI" as UI
participant "Mileage Service" as MS
database "Running Log Repository" as RR
database "Fuel Transaction Repository" as FR
participant "Anomaly Service" as AS
FM -> UI: Capture odometer
UI -> MS: Submit vehicle + reading
MS -> RR: Load previous reading
RR --> MS: Previous odometer
MS -> MS: Validate sequence and calculate distance
MS -> FR: Load fuel usage
FR --> MS: Fuel data
MS -> AS: Check abnormal mileage / tampering
AS --> MS: Risk result
MS -> RR: Save mileage / reading
MS --> UI: Mileage + alerts
@enduml
```

---

# US-34 — Allocate Fuel Cost

**Primary Actor:** Fuel Manager  
**Goal:** Allocate fuel cost to trips or shared activity and calculate variance so transport costing remains accurate.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Allocate Fuel Cost" as U0
 usecase "Allocate Fuel Cost to Trip" as U1
 usecase "Allocate Shared Fuel Cost" as U2
 usecase "Calculate Fuel Cost Variance" as U3
 usecase "Validate Source Fuel Transactions" as U4
 usecase "Record Cost Allocation" as U5
 usecase "View Fuel Cost Breakdown" as U6
}
FM --> U0
FM --> U6
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U1 -|> U0
U2 -|> U0
U0 .> U3 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-34 — Allocate Fuel Cost
start
:Select Fuel Transactions / Period;
:Validate Source Fuel Transactions;
:Select Allocation Method;
if (Direct Trip Allocation?) then (Yes)
 :Select Trip;
 :Allocate Fuel Cost to Trip;
else (Shared)
 :Define Shared Allocation Basis;
 :Allocate Cost Across Applicable Trips / Activity;
endif
:Calculate Expected Cost;
:Calculate Actual Allocated Cost;
:Calculate Fuel Cost Variance;
:Validate Allocation Totals;
if (Balanced?) then (Yes)
 :Save Fuel Cost Allocation;
 :Record Audit Information;
else (No)
 :Display Allocation Imbalance;
 :Require Correction;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-34 — Allocate Fuel Cost — Sequence Diagram
actor "Fuel Manager" as FM
participant "Fuel Cost UI" as UI
participant "Fuel Cost Service" as CS
database "Fuel Transaction Repository" as FR
database "Trip Repository" as TR
database "Cost Allocation Repository" as CR
FM -> UI: Start fuel cost allocation
UI -> CS: Submit period / transactions / method
CS -> FR: Load source fuel costs
FR --> CS: Fuel transaction data
opt Trip allocation
 CS -> TR: Validate trip(s)
 TR --> CS: Trip data
end
CS -> CS: Allocate cost + calculate variance
alt Balanced / valid
 CS -> CR: Save allocations
 CR --> CS: Saved
 CS --> UI: Cost breakdown
else Invalid
 CS --> UI: Allocation / balance error
end
@enduml
```

---

# US-35 — Manage Fuel Cards

**Primary Actor:** Fuel Manager  
**Goal:** Manage fuel-card issuance, transaction imports, restrictions, and fraud detection so card misuse can be controlled.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Fuel Cards" as U0
 usecase "Issue Fuel Card" as U1
 usecase "Import Fuel Card Transactions" as U2
 usecase "Apply Fuel Card Restrictions" as U3
 usecase "Detect Fuel Card Fraud" as U4
 usecase "Detect Card Misuse" as U5
 usecase "Block / Restrict Fuel Card" as U6
 usecase "Reconcile Card Transactions" as U7
}
FM --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U7 : <<include>>
U4 .> U0 : <<extend>>
U5 .> U0 : <<extend>>
U6 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-35 — Manage Fuel Cards
start
:Open Fuel Card Management;
if (Action?) then (Issue)
 :Enter Card and Holder / Vehicle Details;
 :Apply Usage Restrictions;
 :Validate Card Data;
 :Issue Fuel Card;
elseif (Import Transactions)
 :Import Fuel Card Transactions;
 :Validate Imported Records;
 :Reconcile with Fuel / Trip Data;
 :Run Fraud and Misuse Checks;
 if (Suspicious transaction?) then (Yes)
  :Flag Fuel Card Fraud / Misuse;
  if (Blocking required?) then (Yes)
   :Block / Restrict Fuel Card;
  endif
 endif
else (Update Restrictions)
 :Select Fuel Card;
 :Update Limits / Usage Restrictions;
 :Save Restrictions;
endif
:Record Audit Information;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-35 — Manage Fuel Cards — Sequence Diagram
actor "Fuel Manager" as FM
participant "Fuel Card UI" as UI
participant "Fuel Card Service" as FS
database "Fuel Card Repository" as CR
participant "Transaction Import Service" as IS
participant "Fraud Detection Service" as FD
FM -> UI: Issue / update / import
UI -> FS: Submit fuel-card action
alt Transaction import
 FS -> IS: Import card transactions
 IS --> FS: Imported transactions
 FS -> FD: Check fraud / misuse
 FD --> FS: Risk result
end
FS -> CR: Save card / restrictions / transaction state
CR --> FS: Saved state
FS --> UI: Result + alerts if any
@enduml
```

---

# US-36 — Manage Fuel Bunkers

**Primary Actor:** Fuel Manager  
**Goal:** Manage tanks, stock movements, dip readings, and reorder levels so physical fuel inventory remains reliable.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Fuel Bunkers" as U0
 usecase "Manage Fuel Tanks" as U1
 usecase "Record Stock In" as U2
 usecase "Record Stock Out" as U3
 usecase "Record Dip Readings" as U4
 usecase "Generate Reorder Requirement" as U5
 usecase "Analyze Stock Variance" as U6
 usecase "Prevent Negative Fuel Balance" as U7
}
FM --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U5 .> U0 : <<extend>>
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-36 — Manage Fuel Bunkers
start
:Select Fuel Tank / Bunker;
:Load Current Calculated Stock;
if (Transaction?) then (Stock In)
 :Enter Received Quantity;
 :Validate Source / Purchase;
 :Increase Calculated Stock;
elseif (Stock Out)
 :Enter Issued Quantity;
 if (Sufficient stock?) then (Yes)
  :Decrease Calculated Stock;
 else (No)
  :Prevent Negative Fuel Balance;
  :Display Insufficient Stock Error;
 endif
else (Dip Reading)
 :Record Physical Dip Reading;
 :Compare Physical vs Calculated Stock;
 if (Variance outside tolerance?) then (Yes)
  :Flag Stock Variance;
 endif
endif
:Check Reorder Level;
if (Below reorder level?) then (Yes)
 :Generate Reorder Requirement;
endif
:Save Stock Movement / Reading;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-36 — Manage Fuel Bunkers — Sequence Diagram
actor "Fuel Manager" as FM
participant "Bunker UI" as UI
participant "Bunker Service" as BS
database "Fuel Bunker Repository" as BR
database "Stock Movement Repository" as SR
FM -> UI: Stock in / stock out / dip reading
UI -> BS: Submit tank transaction
BS -> BR: Load tank balance + reorder level
BR --> BS: Tank state
BS -> BS: Validate quantity and calculate balance / variance
alt Valid
 BS -> SR: Save stock movement / dip reading
 BS -> BR: Update bunker balance
 BS --> UI: Updated stock + reorder / variance alert
else Invalid / negative balance
 BS --> UI: Reject transaction
end
@enduml
```

---

# US-37 — Analyze Fuel Performance

**Primary Actor:** Fuel Manager  
**Goal:** Analyze vehicle and driver efficiency, anomalies, and leakage indicators so fuel waste can be identified.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Analyze Fuel Performance" as U0
 usecase "Analyze Fuel Efficiency" as U1
 usecase "Detect Fuel Anomalies" as U2
 usecase "Compare Vehicle Fuel Performance" as U3
 usecase "Compare Driver Fuel Performance" as U4
 usecase "Detect Fuel Leakage Pattern" as U5
 usecase "View Fuel Trends" as U6
}
FM --> U0
FM --> U6
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
title US-37 — Analyze Fuel Performance
start
:Select Analysis Period / Fleet Scope;
:Load Fuel Transactions;
:Load Mileage / Running Data;
:Load Driver and Vehicle References;
:Calculate Fuel Efficiency Trends;
:Compare Vehicle Performance;
:Compare Driver Performance;
:Run Fuel Anomaly Detection;
if (Leakage pattern indicator?) then (Yes)
 :Flag Possible Fuel Leakage;
endif
:Display Analytics and Supporting Evidence;
:Keep Raw Fuel Transactions Unchanged;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-37 — Analyze Fuel Performance — Sequence Diagram
actor "Fuel Manager" as FM
participant "Fuel Analytics UI" as UI
participant "Fuel Analytics Service" as AS
database "Fuel Transaction Repository" as FR
database "Running / Mileage Repository" as RR
database "Driver / Vehicle Repository" as DVR
FM -> UI: Request fuel analysis
UI -> AS: Submit period and scope
AS -> FR: Load fuel transactions
AS -> RR: Load distance / mileage
AS -> DVR: Load driver / vehicle dimensions
FR --> AS: Fuel data
RR --> AS: Running data
DVR --> AS: Reference data
AS -> AS: Calculate trends, comparisons, anomalies
AS --> UI: Analytics + anomaly / leakage indicators
@enduml
```

---

# US-38 — Handle Fuel Exceptions

**Primary Actor:** Fuel Manager  
**Goal:** Control theft, incorrect readings, price changes, emergency refueling, card misuse, and negative balances so exceptional events do not corrupt inventory or cost records.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Fuel Manager" as FM
rectangle "Transport & Logistics Management System" {
 usecase "Handle Fuel Exception" as U0
 usecase "Handle Fuel Theft" as U1
 usecase "Handle Incorrect Meter Reading" as U2
 usecase "Handle Sudden Fuel Price Change" as U3
 usecase "Process Emergency Refuel" as U4
 usecase "Handle Fuel Card Misuse" as U5
 usecase "Prevent Negative Fuel Balance" as U6
 usecase "Investigate Fuel Exception" as U7
 usecase "Correct / Reconcile Fuel Record" as U8
 usecase "Escalate Fuel Exception" as U9
}
FM --> U0
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
title US-38 — Handle Fuel Exceptions
start
:Detect / Report Fuel Exception;
:Load Related Fuel, Vehicle, Card and Stock Data;
:Classify Exception;
if (Exception Type?) then (Theft)
 :Record Suspected Theft Evidence;
elseif (Incorrect Reading)
 :Validate Source Reading;
 :Prepare Authorized Correction;
elseif (Price Change)
 :Record New Price Effective Period;
 :Preserve Historical Prices;
elseif (Emergency Refuel)
 :Record Emergency Refuel and Reason;
elseif (Card Misuse)
 :Flag Fuel Card Misuse;
 :Restrict / Block Card if required;
else (Negative Balance)
 :Block Invalid Stock Transaction;
endif
:Investigate Fuel Exception;
if (Corrective action valid?) then (Yes)
 :Correct / Reconcile Applicable Records;
 :Record Audit Information;
 :Mark Exception Resolved;
else (No)
 :Escalate Fuel Exception;
 :Keep Exception Open;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-38 — Handle Fuel Exceptions — Sequence Diagram
actor "Fuel Manager" as FM
participant "Fuel Exception UI" as UI
participant "Fuel Exception Service" as ES
database "Fuel Repository" as FR
database "Fuel Card / Bunker Repository" as BR
participant "Exception / Audit Service" as EA
FM -> UI: Report / review fuel exception
UI -> ES: Submit exception
ES -> FR: Load fuel transaction / price data
ES -> BR: Load card / stock data
FR --> ES: Fuel data
BR --> ES: Supporting data
ES -> ES: Classify and validate corrective action
alt Resolvable
 ES -> FR: Apply authorized correction / reconciliation
 ES -> EA: Record resolution + audit
 ES --> UI: Resolved
else Requires escalation
 ES -> EA: Create / escalate exception
 ES --> UI: Exception remains open
end
@enduml
```

---

# US-39 — Manage Driver Profiles

**Primary Actor:** Driver Manager  
**Goal:** Maintain driver contact, address, identification, and employment information so complete driver profiles are available.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Driver Manager" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Driver Profiles" as U0
 usecase "Create Driver Profile" as U1
 usecase "View Driver Profile" as U2
 usecase "Update Contact Details" as U3
 usecase "Update Address" as U4
 usecase "Manage Driver IDs" as U5
 usecase "Manage Employment Details" as U6
 usecase "Validate Driver Identity" as U7
 usecase "Detect Duplicate Active Driver" as U8
}
DM --> U0
DM --> U2
U0 .> U1 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U8 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-39 — Manage Driver Profiles
start
:Driver Manager opens Driver Profiles;
if (Action?) then (Create)
 :Enter Contact Details;
 :Enter Address;
 :Enter Driver Identification Details;
 :Enter Employment Details;
 :Validate Mandatory Fields;
 :Validate Driver Identity;
 :Check Duplicate Active Driver;
 if (Valid and unique?) then (Yes)
  :Save Driver Profile;
  :Record Audit Information;
 else (No)
  :Display Validation / Duplicate Error;
 endif
elseif (Update)
 :Select Driver;
 :Edit Allowed Profile Details;
 :Validate Changes;
 :Save Updated Profile and Change History;
else (View)
 :Search and Display Driver Profile;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-39 — Manage Driver Profiles — Sequence Diagram
actor "Driver Manager" as DM
participant "Driver Profile UI" as UI
participant "Driver Profile Service" as DS
participant "Identity Validation Service" as IV
database "Driver Repository" as DR
DM -> UI: Create / update driver profile
UI -> DS: Submit contact, address, IDs, employment
DS -> IV: Validate identity / duplicate risk
IV --> DS: Validation result
alt Valid
 DS -> DR: Save driver profile
 DR --> DS: Saved driver
 DS --> UI: Success
else Invalid / duplicate
 DS --> UI: Validation error
end
UI --> DM: Display outcome
@enduml
```

---

# US-40 — Manage Driver Licensing

**Primary Actor:** Driver Manager / Safety Officer  
**Goal:** Maintain license class, expiry, suspensions, and regional endorsements so only legally qualified drivers are eligible for assignment.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Driver Manager / Safety Officer" as DM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Driver Licensing" as U0
 usecase "Maintain License Details" as U1
 usecase "Validate License Class" as U2
 usecase "Generate License Expiry Alert" as U3
 usecase "Record License Suspension" as U4
 usecase "Manage Regional Endorsements" as U5
 usecase "Validate Driver Assignment Eligibility" as U6
 usecase "Renew Driver License" as U7
}
DM --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U3 .> U0 : <<extend>>
U4 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-40 — Manage Driver Licensing
start
:Select Driver;
:Load Current License Details;
if (Action?) then (Create / Update)
 :Enter License Number, Class and Expiry;
 :Enter Regional Endorsements;
 :Validate License Class;
 :Validate Expiry and Endorsements;
 if (License valid?) then (Yes)
  :Save License Details;
 else (No)
  :Display License Validation Error;
 endif
elseif (Suspend)
 :Record Suspension Details;
 :Set License Status = Suspended;
 :Update Driver Assignment Eligibility;
elseif (Renew)
 :Record Renewal Details;
 :Validate New Expiry and Class;
 :Save Renewed License;
endif
:Check License Expiry Threshold;
if (Approaching expiry?) then (Yes)
 :Generate License Expiry Alert;
endif
:Recalculate Driver License Eligibility;
:Record Audit Information;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-40 — Manage Driver Licensing — Sequence Diagram
actor "Driver Manager / Safety Officer" as DM
participant "Driver Licensing UI" as UI
participant "Driver License Service" as LS
database "Driver Repository" as DR
database "License Repository" as LR
participant "Alert / Eligibility Service" as AE
DM -> UI: Create / update / suspend / renew license
UI -> LS: Submit license action
LS -> DR: Validate driver
DR --> LS: Driver state
LS -> LS: Validate class, expiry, endorsements
alt Valid
 LS -> LR: Save license state
 LR --> LS: Saved license
 LS -> AE: Recalculate eligibility / expiry alert
 AE --> LS: Eligibility result
 LS --> UI: Success + eligibility
else Invalid
 LS --> UI: Validation error
end
@enduml
```

---

## Coverage Summary

| User Story | Title | Use Case | Activity | Sequence |
|---|---|---:|---:|---:|
| US-31 | Issue Fuel | Yes | Yes | Yes |
| US-32 | Manage Fuel Purchases | Yes | Yes | Yes |
| US-33 | Track Mileage | Yes | Yes | Yes |
| US-34 | Allocate Fuel Cost | Yes | Yes | Yes |
| US-35 | Manage Fuel Cards | Yes | Yes | Yes |
| US-36 | Manage Fuel Bunkers | Yes | Yes | Yes |
| US-37 | Analyze Fuel Performance | Yes | Yes | Yes |
| US-38 | Handle Fuel Exceptions | Yes | Yes | Yes |
| US-39 | Manage Driver Profiles | Yes | Yes | Yes |
| US-40 | Manage Driver Licensing | Yes | Yes | Yes |

## Source Basis

The document is grounded in the supplied Transport & Logistics requirements. Fuel Issue, Fuel Purchases, Mileage, Fuel Costing, Fuel Cards, Fuel Bunkers, Fuel Analytics, and Fuel Edge Cases remain separate business stories. Driver Profile and Driver Licensing are also kept distinct so licensing eligibility can be consumed by later assignment workflows without turning the driver master record into a small administrative hydra.
