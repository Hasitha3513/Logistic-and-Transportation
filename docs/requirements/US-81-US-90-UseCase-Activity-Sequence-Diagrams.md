# Transport & Logistics Management System

## UML Diagrams — US-81 to US-90

This document is based strictly on the supplied Transport & Logistics requirements. The source defines user stories through **US-87 only**. Therefore **US-81–US-87** are fully documented below with Use Case, Activity, and Sequence diagrams, while **US-88–US-90 are explicitly marked as undefined** rather than invented.

---

# US-81 — Manage Scheduling

**Primary Actor:** Operations Manager  
**Goal:** Maintain resource, holiday, shift, and maintenance-blackout calendars so scheduling reflects true resource availability.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations Manager" as OM
rectangle "Transport & Logistics Management System" {
 usecase "Manage Scheduling" as U0
 usecase "Manage Resource Calendar" as U1
 usecase "Manage Holiday Calendar" as U2
 usecase "Manage Shifts" as U3
 usecase "Manage Maintenance Blackout Periods" as U4
 usecase "Validate Calendar Conflicts" as U5
 usecase "Check Resource Availability" as U6
}
OM --> U0
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
title US-81 — Manage Scheduling
start
:Operations Manager opens Scheduling;
:Select Calendar Type;
if (Resource Calendar?) then (Yes)
 :Create / Update Resource Calendar Entry;
elseif (Holiday Calendar)
 :Create / Update Holiday Entry;
elseif (Shift)
 :Create / Update Shift;
else (Maintenance Blackout)
 :Create / Update Maintenance Blackout Period;
endif
:Validate Date / Time Rules;
:Check Calendar Conflicts;
if (Conflict exists?) then (Yes)
 :Display Scheduling Conflict;
 :Require Correction;
else (No)
 :Save Calendar Entry;
 :Update Resource Availability;
 :Record Change History;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-81 — Manage Scheduling — Sequence Diagram
actor "Operations Manager" as OM
participant "Scheduling UI" as UI
participant "Scheduling Service" as SS
database "Calendar Repository" as CR
participant "Availability Service" as AS
OM -> UI: Create / update schedule data
UI -> SS: Submit resource / holiday / shift / blackout
SS -> CR: Load overlapping calendar entries
CR --> SS: Existing entries
SS -> SS: Validate conflicts
alt Valid
 SS -> CR: Save calendar entry
 SS -> AS: Recalculate availability
 AS --> SS: Availability state
 SS --> UI: Success
else Conflict
 SS --> UI: Conflict details
end
@enduml
```

---

# US-82 — Use Operational Analytics

**Primary Actor:** Operations Manager  
**Goal:** Use dashboards, KPIs, predictive maintenance, demand forecasting, risk scoring, and recommendations so operational decisions are data-driven.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations Manager" as OM
rectangle "Transport & Logistics Management System" {
 usecase "Use Operational Analytics" as U0
 usecase "View Dashboards" as U1
 usecase "Monitor KPIs" as U2
 usecase "Predict Maintenance" as U3
 usecase "Forecast Demand" as U4
 usecase "Calculate Risk Score" as U5
 usecase "Generate Recommendations" as U6
 usecase "Distinguish Actual vs Predictive Data" as U7
}
OM --> U0
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
title US-82 — Use Operational Analytics
start
:Operations Manager opens Analytics;
:Select Dashboard / KPI / Predictive View;
:Load Authorized Operational Data;
:Calculate KPI Metrics;
if (Predictive analysis requested?) then (Yes)
 :Run Predictive Maintenance / Demand Forecasting;
 :Calculate Risk Scores;
 :Generate Recommendations;
 :Label Predictive Results;
endif
:Display Actual and Predictive Data Separately;
:Present Dashboard / Recommendations;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-82 — Use Operational Analytics — Sequence Diagram
actor "Operations Manager" as OM
participant "Analytics UI" as UI
participant "Analytics Service" as AS
database "Operational Data Repository" as OD
participant "Prediction / Risk Engine" as PE
OM -> UI: Request analytics
UI -> AS: Submit dashboard / period / filters
AS -> OD: Load operational data
OD --> AS: Data
AS -> AS: Calculate KPI metrics
opt Predictive analytics
 AS -> PE: Run prediction / risk models
 PE --> AS: Forecasts / risk / recommendations
end
AS --> UI: Actual + predictive results with labels
@enduml
```

---

# US-83 — Manage Documents

**Primary Actor:** Authorized User  
**Goal:** Upload, version, retain, permission, and OCR-process documents so supporting documentation is controlled and traceable.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Authorized User" as AU
rectangle "Transport & Logistics Management System" {
 usecase "Manage Documents" as U0
 usecase "Upload Document" as U1
 usecase "Version Document" as U2
 usecase "Apply Retention Policy" as U3
 usecase "Manage Document Permissions" as U4
 usecase "Extract Text Using OCR" as U5
 usecase "View Document History" as U6
}
AU --> U0
AU --> U6
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
title US-83 — Manage Documents
start
:Authorized User opens Document Management;
:Select Business Record / Document Type;
:Upload Document;
:Validate File and Metadata;
:Apply Document Permissions;
:Apply Retention Policy;
if (Existing document replaced?) then (Yes)
 :Create New Document Version;
 :Preserve Previous Version;
endif
if (OCR requested?) then (Yes)
 :Extract Text Using OCR;
 :Link OCR Output to Original Document;
endif
:Save Document Metadata and Version History;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-83 — Manage Documents — Sequence Diagram
actor "Authorized User" as AU
participant "Document UI" as UI
participant "Document Service" as DS
participant "Storage / DMS" as ST
database "Document Metadata Repository" as DR
participant "OCR Service" as OCR
AU -> UI: Upload / version document
UI -> DS: Submit file + metadata
DS -> DS: Validate permission / retention / metadata
DS -> ST: Store document binary
ST --> DS: Storage reference
opt OCR requested
 DS -> OCR: Extract text
 OCR --> DS: OCR output
end
DS -> DR: Save metadata / version / OCR reference
DR --> DS: Saved
DS --> UI: Success + version history
@enduml
```

---

# US-84 — Handle Global System Failures

**Primary Actor:** Operations Manager  
**Goal:** Handle server outage, replication lag, third-party downtime, message queue backlog, and clock drift so operational continuity is protected.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations Manager" as OM
rectangle "Transport & Logistics Management System" {
 usecase "Handle Global System Failure" as U0
 usecase "Handle Server Outage" as U1
 usecase "Handle Replication Lag" as U2
 usecase "Handle Third-Party Downtime" as U3
 usecase "Handle Message Queue Backlog" as U4
 usecase "Detect Clock Drift" as U5
 usecase "Enter Controlled Degraded Mode" as U6
 usecase "Recover and Verify Consistency" as U7
}
OM --> U0
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 -|> U0
U5 -|> U0
U6 .> U0 : <<extend>>
U7 .> U0 : <<extend>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-84 — Handle Global System Failures
start
:Monitoring detects technical failure;
:Classify Failure Type;
if (Server Outage?) then (Yes)
 :Activate Service Recovery / Failover;
elseif (Replication Lag)
 :Flag Data Freshness / Consistency Risk;
elseif (Third-Party Downtime)
 :Pause / Queue External Processing;
elseif (Message Queue Backlog)
 :Throttle / Drain Backlog Safely;
else (Clock Drift)
 :Flag Time Integrity Risk;
endif
if (Safe degraded operation possible?) then (Yes)
 :Enter Controlled Degraded Mode;
endif
:Perform Recovery;
:Verify Processing Completeness and Data Consistency;
if (Recovery verified?) then (Yes)
 :Return to Normal Operation;
else (No)
 :Keep Incident Open / Escalate;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-84 — Handle Global System Failures — Sequence Diagram
actor "Operations Manager" as OM
participant "Monitoring / Operations UI" as UI
participant "Failure Management Service" as FS
participant "Infrastructure / Integration Layer" as INF
database "Message / State Repository" as SR
INF -> FS: Failure / lag / backlog / clock alert
FS -> SR: Inspect processing state
SR --> FS: Current state
FS -> FS: Classify and select recovery strategy
FS --> UI: Incident + degraded-mode status
OM -> UI: Review / authorize operational response if needed
UI -> FS: Recovery action
FS -> INF: Execute recovery / retry / failover
INF --> FS: Recovery result
FS -> SR: Verify consistency / completeness
FS --> UI: Restored or escalated state
@enduml
```

---

# US-85 — Protect Data Integrity

**Primary Actor:** Operations Manager  
**Goal:** Detect duplicates, orphaned transactions, mismatches, and invalid master data so transactional integrity remains trustworthy.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations Manager" as OM
rectangle "Transport & Logistics Management System" {
 usecase "Protect Data Integrity" as U0
 usecase "Detect Duplicate Records" as U1
 usecase "Detect Orphaned Transactions" as U2
 usecase "Detect Odometer Mismatch" as U3
 usecase "Detect GPS vs Trip-Time Mismatch" as U4
 usecase "Detect Invalid Master Data" as U5
 usecase "Quarantine Invalid Data" as U6
 usecase "Correct / Reconcile Data" as U7
}
OM --> U0
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

```plantuml
@startuml
title US-85 — Protect Data Integrity
start
:Run / Trigger Data Integrity Validation;
:Check Duplicate Records;
:Check Orphaned Transactions;
:Check Odometer Consistency;
:Check GPS vs Trip-Time Consistency;
:Validate Referenced Master Data;
if (Integrity issue detected?) then (Yes)
 :Classify Data Integrity Issue;
 :Quarantine / Block Invalid Data where required;
 :Determine Corrective Action;
 if (Authorized correction available?) then (Yes)
  :Correct / Reconcile Data;
  :Record Audit History;
 else (No)
  :Escalate Data Integrity Issue;
 endif
else (No)
 :Mark Integrity Check Passed;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-85 — Protect Data Integrity — Sequence Diagram
actor "Operations Manager" as OM
participant "Integrity UI" as UI
participant "Data Integrity Service" as IS
database "Operational Repositories" as OR
participant "Exception / Audit Service" as EA
OM -> UI: Run / review integrity check
UI -> IS: Submit scope
IS -> OR: Query duplicates / orphans / mismatches / master data
OR --> IS: Candidate issues
IS -> IS: Validate integrity rules
alt Issue detected
 IS -> EA: Create integrity exception / audit record
 IS --> UI: Issue + corrective action
else Clean
 IS --> UI: Integrity check passed
end
@enduml
```

---

# US-86 — Handle Operational Disruptions

**Primary Actor:** Operations Manager  
**Goal:** Represent disasters, restrictions, strikes, border delays, and demand spikes as operational constraints so logistics plans can adapt.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "Operations Manager" as OM
rectangle "Transport & Logistics Management System" {
 usecase "Handle Operational Disruption" as U0
 usecase "Handle Natural Disaster" as U1
 usecase "Handle Civil Restrictions" as U2
 usecase "Handle Strike / Labor Disruption" as U3
 usecase "Handle Border Restriction" as U4
 usecase "Handle Sudden Demand Spike" as U5
 usecase "Identify Affected Routes / Resources" as U6
 usecase "Replan Operations" as U7
 usecase "Record Disruption History" as U8
}
OM --> U0
U1 -|> U0
U2 -|> U0
U3 -|> U0
U4 -|> U0
U5 -|> U0
U0 .> U6 : <<include>>
U7 .> U0 : <<extend>>
U0 .> U8 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-86 — Handle Operational Disruptions
start
:Record Operational Disruption;
:Classify Disruption Type and Scope;
:Identify Affected Routes, Resources and Schedules;
:Apply Disruption Constraints;
if (Current plan still feasible?) then (Yes)
 :Continue with Restrictions;
else (No)
 :Replan Routes / Resources / Schedules;
 :Redistribute Workload where required;
endif
:Communicate Updated Operational Plan;
:Record Disruption History and Actions;
if (Disruption ended?) then (Yes)
 :Remove Temporary Constraints;
 :Restore Normal Planning Rules;
endif
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-86 — Handle Operational Disruptions — Sequence Diagram
actor "Operations Manager" as OM
participant "Operations Control UI" as UI
participant "Disruption Service" as DS
database "Route / Schedule / Resource Repositories" as RR
participant "Planning Services" as PS
OM -> UI: Record disruption
UI -> DS: Submit type, scope, duration
DS -> RR: Identify affected operations
RR --> DS: Affected routes / schedules / resources
DS -> DS: Apply disruption constraints
alt Replanning required
 DS -> PS: Request revised operational plan
 PS --> DS: Replacement routes / resources / schedules
end
DS -> RR: Save updated plan / disruption history
DS --> UI: Operational response
@enduml
```

---

# US-87 — Detect User Risk

**Primary Actor:** System Administrator  
**Goal:** Detect unauthorized overrides, missing mandatory fields, fraudulent activity, shared logins, and delayed reporting so operational misuse can be controlled.

## Use Case Diagram — PlantUML

```plantuml
@startuml
left to right direction
actor "System Administrator" as SA
rectangle "Transport & Logistics Management System" {
 usecase "Detect User Risk" as U0
 usecase "Detect Unauthorized Override" as U1
 usecase "Prevent Submission with Missing Mandatory Fields" as U2
 usecase "Detect Fraudulent Activity" as U3
 usecase "Detect Shared Login Usage" as U4
 usecase "Flag Delayed Operational Reporting" as U5
 usecase "Apply Risk-Based Security Action" as U6
 usecase "Record User Risk Event" as U7
}
SA --> U0
U0 .> U1 : <<include>>
U0 .> U2 : <<include>>
U0 .> U3 : <<include>>
U0 .> U4 : <<include>>
U0 .> U5 : <<include>>
U6 .> U0 : <<extend>>
U0 .> U7 : <<include>>
@enduml
```

## Activity Diagram — PlantUML

```plantuml
@startuml
title US-87 — Detect User Risk
start
:Receive User Action / Security Signal;
:Evaluate Mandatory Field Completion;
:Evaluate Override Authorization;
:Evaluate Fraud Indicators;
:Evaluate Shared Login Indicators;
:Evaluate Reporting Delay Threshold;
if (Risk signal detected?) then (Yes)
 :Calculate / Assign Risk Severity;
 :Record User Risk Event;
 if (Policy requires action?) then (Yes)
  :Apply Risk-Based Security Action;
  :Require Reauthentication / MFA / Restriction as configured;
 endif
else (No)
 :Allow Normal Processing;
endif
:Preserve Audit Trail;
stop
@enduml
```

## Sequence Diagram — PlantUML

```plantuml
@startuml
title US-87 — Detect User Risk — Sequence Diagram
actor "System Administrator" as SA
participant "Security / Risk UI" as UI
participant "User Risk Service" as RS
participant "User Activity Stream" as US
database "Audit / Session Repository" as AR
participant "Security Enforcement Service" as SE
US -> RS: User action / behavioral signal
RS -> AR: Load session / audit context
AR --> RS: Historical context
RS -> RS: Evaluate override / fields / fraud / shared login / delay
alt Risk detected
 RS -> AR: Save risk event
 RS -> SE: Apply configured risk action
 SE --> RS: Enforcement state
 RS --> UI: Risk alert / restriction
else No risk
 RS --> UI: Normal state
end
SA -> UI: Review user-risk events
@enduml
```

---

# US-88 — Not Defined in Supplied Requirements

**Status:** Not available in the supplied source material.

No Use Case, Activity, or Sequence diagram has been generated for this ID because doing so would require inventing a user story that is not present in the provided requirements.

---

# US-89 — Not Defined in Supplied Requirements

**Status:** Not available in the supplied source material.

No Use Case, Activity, or Sequence diagram has been generated for this ID because doing so would require inventing a user story that is not present in the provided requirements.

---

# US-90 — Not Defined in Supplied Requirements

**Status:** Not available in the supplied source material.

No Use Case, Activity, or Sequence diagram has been generated for this ID because doing so would require inventing a user story that is not present in the provided requirements.

---

## Coverage Summary

| User Story | Title / Status | Use Case | Activity | Sequence |
|---|---|---:|---:|---:|
| US-81 | Manage Scheduling | Yes | Yes | Yes |
| US-82 | Use Operational Analytics | Yes | Yes | Yes |
| US-83 | Manage Documents | Yes | Yes | Yes |
| US-84 | Handle Global System Failures | Yes | Yes | Yes |
| US-85 | Protect Data Integrity | Yes | Yes | Yes |
| US-86 | Handle Operational Disruptions | Yes | Yes | Yes |
| US-87 | Detect User Risk | Yes | Yes | Yes |
| US-88 | Not defined in supplied requirements | No | No | No |
| US-89 | Not defined in supplied requirements | No | No | No |
| US-90 | Not defined in supplied requirements | No | No | No |

## Source Boundary Note

The supplied requirements model ends at **US-87 — Detect User Risk**. US-88, US-89, and US-90 are not defined in the uploaded material, so they are intentionally left without invented diagrams. This preserves traceability and prevents three imaginary requirements from sneaking into the backlog wearing very convincing UML costumes.
