package com.transportlogistics.app.notification.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class NotificationEventCatalogue {
    public static final String MILESTONE_METADATA_KEY = "catalogueMilestone";
    private static final Set<NotificationChannel> MVP_CHANNELS = Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    private static final Set<NotificationChannel> CUSTOMER_CHANNELS =
        Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
    private static final Set<String> COMMON_REQUIRED = Set.of("eventTime", "severity");
    private static final Map<String, NotificationEventDefinition> EVENTS = definitions();

    private NotificationEventCatalogue() {
    }

    public static List<NotificationEventDefinition> all() {
        return List.copyOf(EVENTS.values());
    }

    public static Optional<NotificationEventDefinition> find(String eventType) {
        if (eventType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(EVENTS.get(eventType.trim().toUpperCase()));
    }

    public static NotificationEventDefinition require(String eventType) {
        return find(eventType).orElseThrow(() -> new IllegalArgumentException("Unsupported notification event: " + eventType));
    }

    private static Map<String, NotificationEventDefinition> definitions() {
        Map<String, NotificationEventDefinition> definitions = new LinkedHashMap<>();
        add(definitions, "TRIP_DELAY_RECORDED", "trip", NotificationSeverity.WARNING, "TRIP_DELAY",
            required("tripId", "tripNumber", "delayMinutes", "reason"), Set.of("locationDescription"), 15, null);
        add(definitions, "TRIP_INCIDENT_RECORDED", "trip", NotificationSeverity.WARNING, "TRIP_INCIDENT",
            required("tripId", "tripNumber", "incidentSeverity", "description"), Set.of("locationDescription"), 0, null);
        add(definitions, "VEHICLE_MAINTENANCE_DUE", "fleet", NotificationSeverity.WARNING, "VEHICLE_MAINTENANCE_DUE",
            required("vehicleId", "vehicleRegistration", "maintenanceType", "scheduledStart", "scheduledEnd"), Set.of(),
            1440, MILESTONE_METADATA_KEY);
        add(definitions, "VEHICLE_DOCUMENT_EXPIRING", "fleet", NotificationSeverity.WARNING, "VEHICLE_DOCUMENT_EXPIRING",
            required("vehicleId", "vehicleRegistration", "documentId", "documentType", "documentNumber", "expiryDate"), Set.of(),
            1440, MILESTONE_METADATA_KEY);
        add(definitions, "DRIVER_EXCEPTION_RECORDED", "fleet", NotificationSeverity.WARNING, "DRIVER_EXCEPTION",
            required("driverId", "driverName", "exceptionId", "exceptionType", "startTime", "endTime"), Set.of("reason"), 0, null);
        add(definitions, "DRIVER_MEDICAL_EXPIRING", "fleet", NotificationSeverity.WARNING, "DRIVER_MEDICAL_EXPIRING",
            required("driverId", "driverName", "medicalRecordId", "validUntil", "fitnessStatus"), Set.of(),
            1440, MILESTONE_METADATA_KEY);
        add(definitions, "DRIVER_DRUG_TEST_FAILED", "fleet", NotificationSeverity.CRITICAL, "DRIVER_DRUG_TEST_FAILED",
            required("driverId", "driverName", "drugTestId", "resultDate", "testType"), Set.of(), 0, null);
        add(definitions, "DRIVER_LICENSE_EXPIRING", "fleet", NotificationSeverity.WARNING, "DRIVER_LICENSE_EXPIRING",
            required("driverId", "driverName", "licenseId", "licenseNumber", "licenseClass", "expiryDate"), Set.of(),
            1440, MILESTONE_METADATA_KEY);
        addCustomer(definitions, "DELIVERY_OUT_FOR_DELIVERY", NotificationSeverity.INFO,
            required("deliveryNumber", "status", "customerDisplayName"), Set.of(), 0, null);
        addCustomer(definitions, "DELIVERY_ETA_RISK_CHANGED", NotificationSeverity.WARNING,
            required("deliveryNumber", "slaStatus", "estimatedArrivalAt", "customerDisplayName"),
            Set.of("previousSlaStatus"), 1440, "slaStatus");
        addCustomer(definitions, "DELIVERY_COMPLETED", NotificationSeverity.INFO,
            required("deliveryNumber", "status", "completedAt", "customerDisplayName"), Set.of(), 0, null);
        addCustomer(definitions, "DELIVERY_FAILED_ATTEMPT_RECORDED", NotificationSeverity.WARNING,
            required("deliveryNumber", "status", "failureDisposition", "customerDisplayName"), Set.of(), 0, null);
        addCustomer(definitions, "DELIVERY_REDELIVERY_SCHEDULED", NotificationSeverity.INFO,
            required("deliveryNumber", "status", "scheduledWindowStart", "scheduledWindowEnd",
                "customerDisplayName"), Set.of(), 1440, "scheduleId");
        return Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    }

    private static Set<String> required(String... variables) {
        var required = new java.util.HashSet<>(COMMON_REQUIRED);
        required.addAll(List.of(variables));
        return Set.copyOf(required);
    }

    private static void add(Map<String, NotificationEventDefinition> definitions, String eventType,
                            String owningModule, NotificationSeverity severity, String templateCode,
                            Set<String> required, Set<String> optional, int suppressionMinutes,
                            String milestoneMetadataKey) {
        definitions.put(eventType, new NotificationEventDefinition(eventType, owningModule, severity,
            MVP_CHANNELS, templateCode, required, optional, suppressionMinutes, milestoneMetadataKey));
    }

    private static void addCustomer(Map<String, NotificationEventDefinition> definitions, String eventType,
                                    NotificationSeverity severity, Set<String> required, Set<String> optional,
                                    int suppressionMinutes, String milestoneMetadataKey) {
        definitions.put(eventType, new NotificationEventDefinition(eventType, "delivery", severity,
            CUSTOMER_CHANNELS, eventType, required, optional, suppressionMinutes, milestoneMetadataKey));
    }
}
