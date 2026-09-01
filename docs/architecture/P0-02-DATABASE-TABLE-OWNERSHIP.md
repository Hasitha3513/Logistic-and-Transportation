# P0-02 Database Table Ownership

Status: Enforced baseline  
Scope: current Flyway schema through V56  
Rule: one table has one owning top-level Spring Modulith module; only that owner may persist or modify it.

`Migration` in the repository column means the table is intentionally managed without a standalone JPA repository (for example a join, collection, counter, or catalogue table). Flyway is the schema deployment mechanism, not a business-data owner.

| Table | Owner | Entity | Repository | Writers | Readers | Cross-module access | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| `tenant` | tenancy | `TenantEntity` | `TenantJpaRepository` | tenancy | tenancy, identity contract | UUID reference only | VALID |
| `tenant_membership` | identity | `TenantMembershipEntity` | `TenantMembershipJpaRepository` | identity | identity | references tenancy UUID | VALID |
| `tenant_membership_role` | identity | — | `IdentityPersistenceAdapter` | identity | identity | none | VALID |
| `app_user` | identity | `UserEntity` | `UserJpaRepository` | identity | identity | UUID references only | VALID |
| `app_role` | identity | `RoleEntity` | `RoleJpaRepository` | identity | identity | none | VALID |
| `app_permission` | identity | — | `IdentityPersistenceAdapter` | identity migrations | identity | none | VALID |
| `app_user_role` | identity | — | migration (legacy) | identity migrations | identity compatibility | none | VALID |
| `app_role_permission` | identity | — | `IdentityPersistenceAdapter` | identity | identity | none | VALID |
| `refresh_token` | identity | `RefreshTokenEntity` | `RefreshTokenJpaRepository` | identity | identity | none | VALID |
| `customer` | organization | `CustomerEntity` | `CustomerJpaRepository` | organization | organization; system bootstrap (legacy) | system direct SQL | VIOLATION |
| `department` | organization | `DepartmentEntity` | `DepartmentJpaRepository` | organization | organization | UUID references only | VALID |
| `location` | organization | `LocationEntity` | `LocationJpaRepository` | organization | organization | UUID references only | VALID |
| `project` | organization | `ProjectEntity` | `ProjectJpaRepository` | organization | organization | UUID references only | VALID |
| `vendor` | organization | `VendorEntity` | `VendorJpaRepository` | organization | organization | UUID references only | VALID |
| `vehicle_category` | fleet | `VehicleCategoryEntity` | `VehicleCategoryJpaRepository` | fleet | fleet | none | VALID |
| `vehicle_type` | fleet | `VehicleTypeEntity` | `VehicleTypeJpaRepository` | fleet | fleet | UUID references only | VALID |
| `vehicle` | fleet | `VehicleEntity` | `VehicleJpaRepository` | fleet | fleet; freight reporting (legacy) | freight direct SQL join | VIOLATION |
| `vehicle_document` | fleet | `VehicleDocumentEntity` | `VehicleDocumentJpaRepository` | fleet | fleet | none | VALID |
| `vehicle_reading` | fleet | `VehicleReadingEntity` | `VehicleReadingJpaRepository` | fleet | fleet | none | VALID |
| `vehicle_meter_reset` | fleet | `VehicleMeterResetEntity` | `VehicleMeterResetJpaRepository` | fleet | fleet | identity UUID reference | VALID |
| `maintenance_schedule` | fleet | `MaintenanceScheduleEntity` | `MaintenanceScheduleJpaRepository` | fleet | fleet | vehicle internal FK | VALID |
| `driver` | fleet | `DriverEntity` | `DriverJpaRepository` | fleet | fleet | UUID references only | VALID |
| `driver_license` | fleet | `DriverLicenseEntity` | `DriverLicenseJpaRepository` | fleet | fleet | none | VALID |
| `driver_exception` | fleet | `DriverExceptionEntity` | `DriverExceptionJpaRepository` | fleet | fleet | none | VALID |
| `driver_violation` | fleet | `DriverViolationEntity` | `DriverViolationJpaRepository` | fleet | fleet | none | VALID |
| `driver_medical_record` | fleet | `DriverMedicalRecordEntity` | `DriverMedicalRecordJpaRepository` | fleet | fleet | none | VALID |
| `driver_drug_test` | fleet | `DriverDrugTestEntity` | `DriverDrugTestJpaRepository` | fleet | fleet | none | VALID |
| `lubricant_log` | fleet | `LubricantLogEntity` | `LubricantLogJpaRepository` | fleet | fleet | none | VALID |
| `route` | routing | `RouteEntity` | `RouteJpaRepository` | routing | routing | organization UUID references | VALID |
| `route_stop` | routing | `RouteEntity` collection | `RouteJpaRepository` | routing | routing | organization UUID reference | VALID |
| `route_revision` | routing | `RouteRevisionEntity` | `RouteRevisionJpaRepository` | routing | routing | none | VALID |
| `route_revision_stop` | routing | `RouteRevisionEntity` collection | `RouteRevisionJpaRepository` | routing | routing | organization UUID reference | VALID |
| `route_disruption` | routing | `RouteDisruptionEntity` | `RouteDisruptionJpaRepository` | routing | routing | none | VALID |
| `trip` | trip | `TripEntity` | `TripJpaRepository` | trip | trip public reporting contract | logical IDs; legacy physical FKs | VALID |
| `trip_status_history` | trip | `TripHistoryEntity` | `TripHistoryJpaRepository` | trip | trip public reporting contract | logical vehicle/driver IDs | VALID |
| `trip_dispatch` | trip | `TripDispatchEntity` | `TripDispatchJpaRepository` | trip | trip | none | VALID |
| `trip_operational_event` | trip | `TripOperationalEventEntity` | `TripOperationalEventJpaRepository` | trip | trip | none | VALID |
| `fuel_station` | fuel | `FuelStationEntity` | `FuelStationJpaRepository` | fuel | fuel | organization UUID references | VALID |
| `fuel_limit_policy` | fuel | `FuelLimitPolicyEntity` | `FuelLimitPolicyJpaRepository` | fuel | fuel | fleet UUID reference | VALID |
| `fuel_issue` | fuel | `FuelIssueEntity` | `FuelIssueJpaRepository` | fuel | fuel | logical operational IDs; legacy FKs | VALID |
| `fuel_issue_history` | fuel | `FuelIssueHistoryEntity` | `FuelIssueHistoryJpaRepository` | fuel | fuel | identity UUID reference | VALID |
| `fuel_price` | fuel | `FuelPriceEntity` | `FuelPriceJpaRepository` | fuel | fuel | organization UUID reference | VALID |
| `fuel_purchase` | fuel | `FuelPurchaseEntity` | `FuelPurchaseJpaRepository` | fuel | fuel | logical reference IDs; legacy FKs | VALID |
| `fuel_purchase_history` | fuel | `FuelPurchaseHistoryEntity` | `FuelPurchaseHistoryJpaRepository` | fuel | fuel | identity UUID reference | VALID |
| `bunker_tank` | fuel | `BunkerTankEntity` | `BunkerTankJpaRepository` | fuel | fuel | none | VALID |
| `bunker_dip_reading` | fuel | `DipReadingEntity` | `DipReadingJpaRepository` | fuel | fuel | identity UUID reference | VALID |
| `bunker_stock_adjustment` | fuel | `StockAdjustmentEntity` | `StockAdjustmentJpaRepository` | fuel | fuel | identity UUID reference | VALID |
| `bunker_stock_movement` | fuel | `BunkerStockMovementEntity` | `BunkerStockMovementJpaRepository` | fuel | fuel | none | VALID |
| `freight_order` | freight | `FreightOrderEntity` | `FreightOrderJpaRepository` | freight | freight public reporting contract | organization UUID references | VALID |
| `freight_order_line` | freight | `FreightOrderLineEntity` | aggregate repository | freight | freight | none | VALID |
| `cargo_manifest` | freight | `CargoManifestEntity` | `CargoManifestJpaRepository` | freight | freight | none | VALID |
| `cargo_manifest_item` | freight | `CargoManifestItemEntity` | aggregate repository | freight | freight | none | VALID |
| `load_plan` | freight | `LoadPlanEntity` | `LoadPlanJpaRepository` | freight | freight | fleet vehicle UUID reference | VALID |
| `load_plan_item_placement` | freight | `LoadPlanItemPlacementEntity` | aggregate repository | freight | freight | none | VALID |
| `freight_insurance_policy` | freight | `FreightInsurancePolicyEntity` | `FreightInsurancePolicyJpaRepository` | freight | freight | none | VALID |
| `freight_insurance_claim` | freight | `FreightInsuranceClaimEntity` | `FreightInsuranceClaimJpaRepository` | freight | freight | none | VALID |
| `freight_insurance_settlement` | freight | `FreightInsuranceSettlementEntity` | aggregate repository | freight | freight | none | VALID |
| `cargo_exception` | freight | `CargoExceptionEntity` | `CargoExceptionJpaRepository` | freight | freight | none | VALID |
| `cargo_exception_history` | freight | `CargoExceptionHistoryEntity` | aggregate repository | freight | freight | none | VALID |
| `delivery_order` | delivery | `DeliveryOrderEntity` | `DeliveryOrderJpaRepository` | delivery | delivery public reporting contract | logical customer/location IDs | VALID |
| `delivery_number_counter` | delivery | — | number generator | delivery | delivery | none | VALID |
| `proof_of_delivery` | delivery | `ProofOfDeliveryEntity` | `ProofOfDeliveryJpaRepository` | delivery | delivery | none | VALID |
| `pod_evidence` | delivery | `PodEvidenceEntity` | `PodEvidenceJpaRepository` | delivery | delivery | none | VALID |
| `delivery_attempt` | delivery | `DeliveryAttemptEntity` | `DeliveryAttemptJpaRepository` | delivery | delivery | none | VALID |
| `delivery_contact_attempt` | delivery | `DeliveryContactAttemptEntity` | `DeliveryContactAttemptJpaRepository` | delivery | delivery | none | VALID |
| `delivery_escalation` | delivery | `DeliveryEscalationEntity` | `DeliveryEscalationJpaRepository` | delivery | delivery | none | VALID |
| `delivery_redelivery_schedule` | delivery | `DeliveryRedeliveryScheduleEntity` | `DeliveryRedeliveryScheduleJpaRepository` | delivery | delivery | none | VALID |
| `delivery_exception_case` | delivery | `DeliveryExceptionCaseEntity` | `DeliveryExceptionJpaRepository` | delivery | delivery | none | VALID |
| `delivery_exception_evidence` | delivery | `DeliveryExceptionEvidenceEntity` | aggregate repository | delivery | delivery | none | VALID |
| `delivery_zone` | delivery | `DeliveryZoneEntity` | `DeliveryZoneJpaRepository` | delivery | delivery | none | VALID |
| `delivery_slot` | delivery | `DeliverySlotEntity` | `DeliverySlotJpaRepository` | delivery | delivery | none | VALID |
| `delivery_slot_reservation` | delivery | `DeliverySlotReservationEntity` | `DeliverySlotReservationJpaRepository` | delivery | delivery | none | VALID |
| `delivery_rider` | delivery | `DeliveryRiderEntity` | `DeliveryRiderJpaRepository` | delivery | delivery | fleet driver UUID reference | VALID |
| `delivery_rider_zone` | delivery | `DeliveryRiderEntity` collection | `DeliveryRiderJpaRepository` | delivery | delivery | none | VALID |
| `delivery_rider_shift` | delivery | `DeliveryRiderShiftEntity` | `DeliveryRiderShiftJpaRepository` | delivery | delivery | none | VALID |
| `delivery_order_rider_assignment` | delivery | `DeliveryOrderRiderAssignmentEntity` | `DeliveryOrderRiderAssignmentJpaRepository` | delivery | delivery | none | VALID |
| `delivery_batch` | delivery | `DeliveryBatchEntity` | `DeliveryBatchJpaRepository` | delivery | delivery | none | VALID |
| `delivery_batch_order` | delivery | `DeliveryBatchOrderEntity` | `DeliveryBatchOrderJpaRepository` | delivery | delivery | none | VALID |
| `delivery_batch_counter` | delivery | — | batch-code generator | delivery | delivery | none | VALID |
| `notification` | notification | `NotificationEntity` | `NotificationJpaRepository` | notification | notification | logical source/recipient IDs | VALID |
| `notification_template` | notification | `NotificationTemplateEntity` | `NotificationTemplateJpaRepository` | notification | notification | none | VALID |
| `notification_rule` | notification | `NotificationRuleEntity` | `NotificationRuleJpaRepository` | notification | notification | none | VALID |
| `notification_rule_policy` | notification | `NotificationRulePolicyEntity` | `NotificationRulePolicyJpaRepository` | notification | notification | none | VALID |
| `notification_rule_quiet_day` | notification | policy collection | policy repository | notification | notification | none | VALID |
| `notification_rule_execution` | notification | `NotificationRuleExecutionEntity` | `NotificationRuleExecutionJpaRepository` | notification | notification | none | VALID |
| `notification_delivery_attempt` | notification | `NotificationDeliveryAttemptEntity` | `NotificationDeliveryAttemptJpaRepository` | notification | notification | none | VALID |
| `offline_sync_operation` | offlinesync | `OfflineSyncOperationEntity` | `OfflineSyncOperationJpaRepository` | offlinesync | offlinesync | logical actor/aggregate IDs | VALID |

## Legacy violations reserved for P0-03

1. `FreightReportingJdbcAdapter` joins Fleet-owned `vehicle` for capacity facts. Replace this with a Fleet-owned reporting contract or a Freight-owned event-fed snapshot/read model.
2. `LocalSampleDataBootstrap` probes Organization-owned `customer` and executes a multi-owner SQL fixture. Replace it with owner-specific bootstrap ports/runners or a test-only provisioning mechanism.
3. Integration tests use cross-owner SQL for fixture setup and cleanup. Replace shared database cleanup with owner-scoped fixtures as a separate test-infrastructure remediation.

No REST API, event contract, runtime behavior, database schema, or historical migration changes are made by P0-02.
