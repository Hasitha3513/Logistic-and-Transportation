# Transport & Logistics Management System

## UML Diagrams — US-71 to US-80

This document consolidates UML diagrams for **US-71 through US-80** based on the supplied Transport & Logistics requirements. Each story includes a **Use Case Diagram**, **Activity Diagram**, and **Sequence Diagram** in PlantUML.

> Scope: US-71–US-78 cover cross-cutting platform concerns such as offline synchronization, compliance, integrations, security, audit/reporting, mobile operations, notifications, and exception management. US-79–US-80 begin the supporting capabilities with Master Data Management and Workflow configuration.

---

# US-71 — Support Offline Data Synchronization

**Primary Actor:** Operations User  
**Goal:** Capture and synchronize offline transactions with conflict handling and recovery so field operations continue during poor connectivity.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations User" as OU
rectangle "Transport & Logistics Management System" {
 usecase "Support Offline Data Synchronization" as U0
 usecase "Capture Data Offline" as U1
 usecase "Queue Offline Transactions" as U2
 usecase "Synchronize Pending Data" as U3
 usecase "Resolve Synchronization Conflicts" as U4
 usecase "Recover Partially Synchronized Data" as U5
 usecase "Support Low-Bandwidth Operation" as U6
 usecase "Support Store-and-Forward Processing" as U7
}
OU --> U0
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
title US-71 — Support Offline Data Synchronization
start
:Operations User performs field action;
if (Network available?) then (Yes)
 :Submit Transaction Online;
 :Validate and Persist Transaction;
else (No)
 :Capture Data Offline;
 :Queue Offline Transaction;
 :Store Source Timestamp and Local Identifier;
 :Apply Store-and-Forward Processing;
endif
if (Network restored?) then (Yes)
 :Load Pending Offline Transactions;
 :Synchronize Pending Data;
 if (Conflict detected?) then (Yes)
  :Resolve Synchronization Conflict;
  if (Conflict unresolved?) then (Yes)
   :Keep Transaction Pending / Escalate;
  endif
 endif
 if (Partial synchronization failure?) then (Yes)
  :Recover Partially Synchronized Data;
 endif
 :Mark Successfully Synced Transactions;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-71 — Support Offline Data Synchronization — Sequence Diagram
actor "Operations User" as OU
participant "Mobile / Field UI" as UI
participant "Offline Store" as OS
participant "Sync Service" as SS
database "Domain Repository" as DR
OU -> UI: Perform field transaction
alt Offline
 UI -> OS: Save local transaction
 OS --> UI: Queued
else Online
 UI -> SS: Submit transaction
 SS -> DR: Persist transaction
 DR --> SS: Saved
 SS --> UI: Success
end
opt Network restored
 OS -> SS: Send queued transactions
 SS -> DR: Validate and persist
 alt Conflict
  DR --> SS: Conflict details
  SS -> SS: Apply conflict-resolution rule
 else Success
  DR --> SS: Saved
 end
 SS -> OS: Mark synchronized / keep pending
end
@enduml
```

---

# US-72 — Enforce Compliance

**Primary Actor:** Compliance Officer  
**Goal:** Validate vehicle, driver, cargo, hazmat, tax, regional, and retention rules so regulated operations remain compliant.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Compliance Officer" as CO
rectangle "Transport & Logistics Management System" {
 usecase "Enforce Compliance" as U0
 usecase "Validate Vehicle Compliance" as U1
 usecase "Validate Driver Compliance" as U2
 usecase "Validate Cargo Compliance" as U3
 usecase "Apply Hazmat Rules" as U4
 usecase "Apply Tax / Billing Rules" as U5
 usecase "Apply Regional Regulations" as U6
 usecase "Apply Data Retention Rules" as U7
 usecase "Record Compliance Decision" as U8
}
CO --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U0 .> U8 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-72 — Enforce Compliance
start
:Receive Compliance Validation Request;
:Load Relevant Vehicle / Driver / Cargo / Billing Data;
:Determine Applicable Compliance Rules;
:Validate Vehicle Compliance;
:Validate Driver Compliance;
:Validate Cargo Compliance;
:Apply Hazmat Rules if applicable;
:Apply Tax / Billing Rules if applicable;
:Apply Regional Regulations;
:Apply Data Retention Rules where relevant;
if (All mandatory rules satisfied?) then (Yes)
 :Record Compliance Decision = Compliant;
 :Allow Operation to Continue;
else (No)
 :Record Compliance Violations;
 :Block or Restrict Operation According to Policy;
endif
:Record Audit Information;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-72 — Enforce Compliance — Sequence Diagram
actor "Compliance Officer" as CO
participant "Compliance UI" as UI
participant "Compliance Service" as CS
database "Compliance Rule Repository" as RR
database "Operational Data Repositories" as OD
participant "Audit Service" as AS
CO -> UI: Validate compliance
UI -> CS: Submit entity / operation
CS -> RR: Load applicable rules
CS -> OD: Load vehicle / driver / cargo / billing data
RR --> CS: Rules
OD --> CS: Operational data
CS -> CS: Evaluate compliance
alt Compliant
 CS --> UI: Allow operation
else Non-compliant
 CS --> UI: Violations + block / restriction
end
CS -> AS: Record compliance decision
@enduml
```

---

# US-73 — Manage External Integrations

**Primary Actor:** System Administrator  
**Goal:** Configure and monitor external system integrations so business data can be exchanged reliably.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "System Administrator" as SA
rectangle "Transport & Logistics Management System" {
 usecase "Manage External Integrations" as U0
 usecase "Configure ERP Integration" as U1
 usecase "Configure Accounting Integration" as U2
 usecase "Configure CRM / HRMS Integration" as U3
 usecase "Configure Fuel / Telematics Integration" as U4
 usecase "Configure Payment / Insurance / DMS" as U5
 usecase "Expose API Gateway" as U6
 usecase "Support Webhooks" as U7
 usecase "Import / Export Files" as U8
 usecase "Monitor Integration Status" as U9
}
SA --> U0
SA --> U9
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 -|> U0
U5 -|> U0
U6 -|> U0
U7 -|> U0
U8 -|> U0
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-73 — Manage External Integrations
start
:System Administrator opens Integration Management;
:Select Integration Type;
:Enter Endpoint / Credential / Mapping Configuration;
:Validate Integration Configuration;
if (Configuration valid?) then (Yes)
 :Save Integration Endpoint;
 :Test Connectivity;
 if (Connectivity successful?) then (Yes)
  :Enable Integration;
  :Exchange Supported Data;
  :Record Integration Message Status;
 else (No)
  :Mark Integration Unavailable;
  :Record Connection Error;
 endif
else (No)
 :Display Configuration Validation Error;
endif
if (Message delivery fails?) then (Yes)
 :Record Failed Integration Message;
 :Retry According to Policy;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-73 — Manage External Integrations — Sequence Diagram
actor "System Administrator" as SA
participant "Integration UI" as UI
participant "Integration Service" as IS
database "Integration Endpoint Repository" as ER
participant "External System" as EXT
database "Integration Message Repository" as MR
SA -> UI: Configure / monitor integration
UI -> IS: Submit integration configuration
IS -> ER: Save endpoint / mappings
ER --> IS: Saved endpoint
IS -> EXT: Test / exchange data
alt Success
 EXT --> IS: Response
 IS -> MR: Save successful message
 IS --> UI: Integration healthy
else Failure
 EXT --> IS: Error / timeout
 IS -> MR: Save failed message
 IS --> UI: Failed / retry state
end
@enduml
```

---

# US-74 — Manage Security

**Primary Actor:** System Administrator  
**Goal:** Enforce RBAC, ABAC, SSO, MFA, encryption, device authentication, session controls, and segregation of duties so system access remains secure.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "System Administrator" as SA
rectangle "Transport & Logistics Management System" {
 usecase "Manage Security" as U0
 usecase "Apply Role-Based Access Control" as U1
 usecase "Apply Attribute-Based Access Control" as U2
 usecase "Support SSO" as U3
 usecase "Support MFA" as U4
 usecase "Encrypt Sensitive Data" as U5
 usecase "Authenticate Devices" as U6
 usecase "Manage User Sessions" as U7
 usecase "Enforce Segregation of Duties" as U8
 usecase "Monitor Privileged Activity" as U9
}
SA --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U0 .> U8 : <<include>>
U0 .> U9 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-74 — Manage Security
start
:User / Device Requests Access;
:Authenticate User via SSO / Credentials;
if (MFA required?) then (Yes)
 :Perform MFA Challenge;
endif
:Authenticate Device if configured;
:Load Roles and Attributes;
:Evaluate RBAC / ABAC Permissions;
:Check Segregation of Duties;
if (Access allowed?) then (Yes)
 :Create / Validate User Session;
 :Allow Authorized Operation;
 :Encrypt Sensitive Data in Transit / Storage;
 if (Privileged action?) then (Yes)
  :Record Privileged Activity;
 endif
else (No)
 :Reject Access;
 :Record Security Event;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-74 — Manage Security — Sequence Diagram
actor "System User" as U
participant "Security Gateway" as SG
participant "Identity Provider / MFA" as IDP
database "Role / Permission Repository" as RP
participant "Session Service" as SS
participant "Audit Service" as AS
U -> SG: Request protected action
SG -> IDP: Authenticate / MFA
IDP --> SG: Identity result
SG -> RP: Load roles / attributes / SoD rules
RP --> SG: Authorization data
alt Authorized
 SG -> SS: Create / validate session
 SS --> SG: Session valid
 SG -> AS: Record privileged activity if applicable
 SG --> U: Access granted
else Denied
 SG -> AS: Record denied access
 SG --> U: Access denied
end
@enduml
```

---

# US-75 — Maintain Audit and Reports

**Primary Actor:** Compliance Officer  
**Goal:** Maintain user logs, transaction trails, change history, and regulatory or operational reports so system activity remains auditable.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Compliance Officer" as CO
rectangle "Transport & Logistics Management System" {
 usecase "Maintain Audit and Reports" as U0
 usecase "Record User Activity Logs" as U1
 usecase "Maintain Transaction Audit Trail" as U2
 usecase "Maintain Change History" as U3
 usecase "Generate Regulatory Reports" as U4
 usecase "Generate Operational Reports" as U5
 usecase "Schedule Reports" as U6
 usecase "Generate Ad Hoc Reports" as U7
 usecase "Generate Exception Reports" as U8
}
CO --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U4 -|> U0
U5 -|> U0
U6 -|> U0
U7 -|> U0
U8 -|> U0
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-75 — Maintain Audit and Reports
start
:Receive Business / Security Event;
:Record Actor, Action, Timestamp and Entity;
:Maintain Transaction Audit Trail;
:Maintain Change History;
if (Report requested?) then (Yes)
 :Validate Report Permission;
 :Select Regulatory / Operational / Exception / Ad Hoc Report;
 :Load Authorized Audit and Business Data;
 :Generate Report;
 if (Scheduled report?) then (Yes)
  :Apply Report Schedule;
 endif
 :Present / Deliver Report;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-75 — Maintain Audit and Reports — Sequence Diagram
actor "Compliance Officer" as CO
participant "Audit / Reporting UI" as UI
participant "Audit Service" as AS
database "Audit Event Repository" as AR
participant "Reporting Service" as RS
database "Business Data Repositories" as BR
CO -> UI: Request report / review audit
UI -> RS: Submit filters / report type
RS -> AR: Load audit events
RS -> BR: Load authorized business data
AR --> RS: Audit data
BR --> RS: Business data
RS -> RS: Generate report
RS --> UI: Report output
@enduml
```

---

# US-76 — Support Mobile Operations

**Primary Actor:** Operations User  
**Goal:** Provide driver, dispatcher, and delivery mobile capabilities with offline-first workflows, camera, signature, push, background sync, and device health.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations User" as OU
rectangle "Transport & Logistics Management System" {
 usecase "Support Mobile Operations" as U0
 usecase "Provide Driver Mobile App" as U1
 usecase "Provide Dispatcher Mobile App" as U2
 usecase "Provide Delivery Mobile App" as U3
 usecase "Support Offline-First Use" as U4
 usecase "Support Camera" as U5
 usecase "Support Digital Signature" as U6
 usecase "Support Push Notifications" as U7
 usecase "Support Background Synchronization" as U8
 usecase "Monitor Device Health" as U9
}
OU --> U0
U1 -|> U0
U2 -|> U0
U3 -|> U0
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U0 .> U8 : <<include>>
U0 .> U9 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-76 — Support Mobile Operations
start
:Operations User opens Mobile App;
:Authenticate User and Device;
:Load Role-Specific Mobile Functions;
if (Network available?) then (Yes)
 :Load / Submit Live Operational Data;
else (No)
 :Enable Offline-First Workflow;
 :Queue Offline Transactions;
endif
if (Camera / Signature required?) then (Yes)
 :Capture Camera / Signature Evidence;
endif
:Receive Push Notifications if enabled;
:Run Background Synchronization when possible;
:Monitor Device Health;
if (Device / Sync problem detected?) then (Yes)
 :Display Mobile Operational Warning;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-76 — Support Mobile Operations — Sequence Diagram
actor "Operations User" as OU
participant "Mobile App" as APP
participant "Mobile Backend Service" as MB
participant "Security Service" as SS
participant "Offline Sync Service" as SYNC
participant "Device Health Service" as DH
OU -> APP: Open mobile workflow
APP -> SS: Authenticate user / device
SS --> APP: Authorized
APP -> MB: Load role-specific operations
MB --> APP: Mobile data
opt Offline
 APP -> SYNC: Queue transaction
end
opt Background sync available
 SYNC -> MB: Synchronize queued data
 MB --> SYNC: Sync result
end
APP -> DH: Report device health
DH --> APP: Health state
@enduml
```

---

# US-77 — Manage Notification Rules

**Primary Actor:** System Administrator  
**Goal:** Configure channels, templates, escalations, and quiet hours so notifications are controlled consistently across the platform.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "System Administrator" as SA
rectangle "Transport & Logistics Management System" {
 usecase "Manage Notification Rules" as U0
 usecase "Configure SMS Notifications" as U1
 usecase "Configure Email Notifications" as U2
 usecase "Configure Push / In-App Notifications" as U3
 usecase "Configure Webhook Notifications" as U4
 usecase "Manage Notification Templates" as U5
 usecase "Manage Escalations" as U6
 usecase "Apply Quiet Hours" as U7
 usecase "Track Notification Delivery Status" as U8
}
SA --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U0 .> U8 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-77 — Manage Notification Rules
start
:System Administrator opens Notification Rules;
:Select Business Event / Notification Type;
:Select Channel(s);
:Select / Create Notification Template;
:Configure Recipient Rules;
:Configure Escalation Rules;
:Configure Quiet Hours / Suppression;
:Validate Notification Rule;
if (Rule valid?) then (Yes)
 :Save Notification Rule;
 :Activate Rule;
else (No)
 :Display Rule Validation Error;
endif
:When event occurs, evaluate active rule;
if (Suppressed by quiet hours?) then (Yes)
 :Queue / Suppress according to policy;
else (No)
 :Send Notification;
 :Record Delivery Status;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-77 — Manage Notification Rules — Sequence Diagram
actor "System Administrator" as SA
participant "Notification Rule UI" as UI
participant "Notification Rule Service" as RS
database "Template / Rule Repository" as RR
participant "Notification Engine" as NE
database "Notification Repository" as NR
SA -> UI: Configure notification rule
UI -> RS: Submit channels, template, escalation, quiet hours
RS -> RS: Validate rule
RS -> RR: Save active rule
RR --> RS: Saved
NE -> RR: Load rule for business event
RR --> NE: Rule / template
NE -> NE: Apply quiet hours / escalation
NE -> NR: Record delivery / suppression status
NE --> UI: Notification status
@enduml
```

---

# US-78 — Manage Operational Exceptions

**Primary Actor:** Operations Manager  
**Goal:** Classify, assign, prioritize, escalate, and close operational exceptions with SLA, corrective action, and root cause tracking.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations Manager" as OM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Operational Exceptions" as U0
 usecase "Classify Exception" as U1
 usecase "Auto-Assign Exception" as U2
 usecase "Set Severity" as U3
 usecase "Apply Escalation Matrix" as U4
 usecase "Track SLA" as U5
 usecase "Record Corrective Action" as U6
 usecase "Record Root Cause Analysis" as U7
 usecase "Close Exception" as U8
}
OM --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U0 .> U7 : <<include>>
U0 .> U8 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-78 — Manage Operational Exceptions
start
:Operational Exception Created;
:Classify Exception;
:Set Severity;
:Auto-Assign Responsible Owner;
:Start SLA Timer;
:Apply Escalation Matrix;
:Investigate Exception;
:Record Corrective Action;
if (Root Cause Analysis required?) then (Yes)
 :Record Root Cause Analysis;
endif
if (SLA at risk / breached?) then (Yes)
 :Escalate Exception;
endif
:Validate Resolution;
if (Resolution complete?) then (Yes)
 :Close Exception;
else (No)
 :Keep Exception Open;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-78 — Manage Operational Exceptions — Sequence Diagram
actor "Operations Manager" as OM
participant "Exception Management UI" as UI
participant "Exception Service" as ES
database "Exception Repository" as ER
participant "Workflow / SLA Service" as WS
OM -> UI: Review operational exception
UI -> ES: Submit classification / action
ES -> ER: Load exception
ER --> ES: Exception state
ES -> WS: Apply assignment / SLA / escalation rules
WS --> ES: Workflow state
ES -> ER: Save severity, corrective action, RCA, status
alt Resolution complete
 ES -> ER: Close exception
else SLA risk / incomplete
 ES -> WS: Escalate / continue tracking
end
ES --> UI: Updated exception state
@enduml
```

---

# US-79 — Manage Master Data

**Primary Actor:** System Administrator  
**Goal:** Maintain company, branch, vendor, customer, depot, product, and commodity reference data so operational transactions use controlled master records.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "System Administrator" as SA
rectangle "Transport & Logistics Management System" {
 usecase "Manage Master Data" as U0
 usecase "Manage Company" as U1
 usecase "Manage Branch" as U2
 usecase "Manage Vendor" as U3
 usecase "Manage Customer" as U4
 usecase "Manage Depot" as U5
 usecase "Manage Product" as U6
 usecase "Manage Commodity" as U7
 usecase "Validate Master Data" as U8
 usecase "Prevent Unsafe Deletion" as U9
}
SA --> U0
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 -|> U0
U5 -|> U0
U6 -|> U0
U7 -|> U0
U0 .> U8 : <<include>>
U9 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-79 — Manage Master Data
start
:System Administrator opens Master Data Management;
:Select Master Data Type;
if (Action?) then (Create / Update)
 :Enter Master Data Details;
 :Validate Mandatory Fields;
 :Check Duplicate / Invalid Master Data;
 if (Valid and unique?) then (Yes)
  :Save Master Record;
  :Record Change History;
 else (No)
  :Display Validation / Duplicate Error;
 endif
else (Delete / Deactivate)
 :Select Master Record;
 :Check Referential Usage;
 if (Record referenced?) then (Yes)
  :Prevent Unsafe Deletion;
  :Offer Deactivation where allowed;
 else (No)
  :Delete / Deactivate Master Record;
 endif
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-79 — Manage Master Data — Sequence Diagram
actor "System Administrator" as SA
participant "Master Data UI" as UI
participant "Master Data Service" as MS
database "Master Data Repository" as MR
database "Operational References" as OR
SA -> UI: Create / update / delete master record
UI -> MS: Submit master-data command
MS -> MS: Validate mandatory / duplicate data
opt Delete / deactivate
 MS -> OR: Check referential usage
 OR --> MS: Reference state
end
alt Valid / safe
 MS -> MR: Save master record state
 MR --> MS: Saved
 MS --> UI: Success
else Invalid / referenced
 MS --> UI: Validation / deletion restriction
end
@enduml
```

---

# US-80 — Configure Workflows

**Primary Actor:** System Administrator  
**Goal:** Configure approval states, conditions, and escalation rules so business processes follow defined workflow logic.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "System Administrator" as SA
rectangle "Transport & Logistics Management System" {
 usecase "Configure Workflows" as U0
 usecase "Configure Approval Workflow" as U1
 usecase "Configure Escalation" as U2
 usecase "Configure Workflow States" as U3
 usecase "Configure Workflow Conditions" as U4
 usecase "Validate State Transitions" as U5
 usecase "Activate Workflow Definition" as U6
 usecase "Version Workflow Definition" as U7
}
SA --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U0 .> U6 : <<include>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-80 — Configure Workflows
start
:System Administrator opens Workflow Configuration;
:Create / Select Workflow Definition;
:Define Workflow States;
:Define Allowed State Transitions;
:Define Approval Rules;
:Define Workflow Conditions;
:Define Escalation Rules;
:Validate Workflow Definition;
if (Definition valid?) then (Yes)
 :Save Workflow Version;
 :Activate Workflow Definition;
else (No)
 :Display Workflow Validation Error;
endif
if (Workflow updated later?) then (Yes)
 :Create New Workflow Version;
 :Preserve Existing Instance History;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-80 — Configure Workflows — Sequence Diagram
actor "System Administrator" as SA
participant "Workflow Configuration UI" as UI
participant "Workflow Definition Service" as WS
database "Workflow Definition Repository" as WR
participant "Workflow Engine" as WE
SA -> UI: Create / update workflow
UI -> WS: Submit states, conditions, approvals, escalation
WS -> WS: Validate transitions / logic
alt Valid
 WS -> WR: Save workflow version
 WR --> WS: Saved
 WS -> WE: Activate definition
 WE --> WS: Activated
 WS --> UI: Workflow active
else Invalid
 WS --> UI: Validation errors
end
@enduml
```

---

## Coverage Summary

| User Story | Title | Use Case | Activity | Sequence |
|---|---|---:|---:|---:|
| US-71 | Support Offline Data Synchronization | Yes | Yes | Yes |
| US-72 | Enforce Compliance | Yes | Yes | Yes |
| US-73 | Manage External Integrations | Yes | Yes | Yes |
| US-74 | Manage Security | Yes | Yes | Yes |
| US-75 | Maintain Audit and Reports | Yes | Yes | Yes |
| US-76 | Support Mobile Operations | Yes | Yes | Yes |
| US-77 | Manage Notification Rules | Yes | Yes | Yes |
| US-78 | Manage Operational Exceptions | Yes | Yes | Yes |
| US-79 | Manage Master Data | Yes | Yes | Yes |
| US-80 | Configure Workflows | Yes | Yes | Yes |

## Source Basis

The diagrams preserve the supplied requirements model and terminology. Offline synchronization is treated as a reusable platform capability; compliance evaluates domain data without owning it; integrations handle connectivity and message exchange rather than business decisions; security separates authentication, authorization, sessions, device trust, SoD, and privileged monitoring; audit/reporting remains observational; and workflow configuration is modeled as a reusable engine rather than a dozen mutually incompatible approval buttons breeding across modules.
