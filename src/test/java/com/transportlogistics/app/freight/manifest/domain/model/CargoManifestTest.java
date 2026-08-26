package com.transportlogistics.app.freight.manifest.domain.model;

import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargoManifestTest {

    private static final String CLASSIFICATION_MISSING = "SPECIAL_CARGO_CLASSIFICATION_MISSING";

    private final UUID lineId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-25T00:00:00Z");

    @Test
    void acceptsExplicitTrueSpecialCargoClassifications() {
        assertClassificationComplete(item(BigDecimal.TEN, true, true));
    }

    @Test
    void acceptsExplicitFalseSpecialCargoClassifications() {
        assertClassificationComplete(item(BigDecimal.TEN, false, false));
    }

    @Test
    void rejectsUnknownFragileClassificationAtFinalization() {
        assertClassificationMissing(item(BigDecimal.TEN, null, false));
    }

    @Test
    void rejectsUnknownTemperatureClassificationAtFinalization() {
        assertClassificationMissing(item(BigDecimal.TEN, false, null));
    }

    @Test
    void rejectsBothUnknownClassificationsAtFinalization() {
        assertClassificationMissing(item(BigDecimal.TEN, null, null));
    }

    @Test
    void rejectsMultipleItemsWhenAnyClassificationIsUnknown() {
        CargoManifest manifest = manifest(List.of(
                item(new BigDecimal("5"), true, false),
                item(new BigDecimal("5"), false, null)
        ));

        List<ManifestValidationFailure> failures = manifest.validate(expected());

        assertEquals(1, failures.stream().filter(f -> CLASSIFICATION_MISSING.equals(f.code())).count());
        assertTrue(failures.stream().anyMatch(f -> f.field().contains(manifest.items().get(1).id().toString())));
        BusinessRuleException rejection = assertThrows(BusinessRuleException.class,
                () -> manifest.finalizeManifest(expected(), "finalizer", now.plusHours(1)));
        assertEquals(CLASSIFICATION_MISSING, rejection.code());
    }

    @Test
    void allExplicitClassificationsAllowFinalization() {
        CargoManifest manifest = manifest(List.of(
                item(new BigDecimal("5"), true, false),
                item(new BigDecimal("5"), false, true)
        ));

        CargoManifest finalized = manifest.finalizeManifest(expected(), "finalizer", now.plusHours(1));

        assertTrue(finalized.finalized());
        assertEquals("finalizer", finalized.finalizedBy());
        assertThrows(ConflictException.class,
                () -> finalized.addItem(item(BigDecimal.TEN, false, false), "editor", now));
    }

    @Test
    void conditionalCustomsAndHazmatAreActionable() {
        CargoManifest manifest = manifest(List.of(new CargoManifestItem(
                UUID.randomUUID(), lineId, "Cargo", BigDecimal.TEN, "Wrapped", "SUPPLIER.CODE",
                true, null, true, null, null, false, false
        )));

        List<String> codes = manifest.validate(expected()).stream().map(ManifestValidationFailure::code).toList();

        assertTrue(codes.contains("CUSTOMS_INFORMATION_REQUIRED"));
        assertTrue(codes.contains("HAZARDOUS_CLASSIFICATION_REQUIRED"));
        assertTrue(codes.contains("HAZARDOUS_DETAILS_REQUIRED"));
        assertFalse(codes.contains(CLASSIFICATION_MISSING));
        assertThrows(BusinessRuleException.class,
                () -> manifest.finalizeManifest(expected(), "actor", now));
    }

    @Test
    void flagsKnownUnmanifestedCargo() {
        CargoManifest partial = manifest(List.of(item(BigDecimal.ONE, false, false)));

        assertEquals("UNMANIFESTED_CARGO", partial.validate(expected()).getFirst().code());
    }

    @Test
    void itemInvariantsRejectInvalidData() {
        assertThrows(BusinessRuleException.class, () -> new CargoManifestItem(
                UUID.randomUUID(), lineId, "", BigDecimal.ZERO, "", "",
                false, null, false, null, null, false, false
        ));
    }

    private void assertClassificationComplete(CargoManifestItem item) {
        CargoManifest manifest = manifest(List.of(item));
        assertTrue(manifest.validate(expected()).isEmpty());
    }

    private void assertClassificationMissing(CargoManifestItem item) {
        CargoManifest manifest = manifest(List.of(item));
        ManifestValidationFailure failure = manifest.validate(expected()).stream()
                .filter(candidate -> CLASSIFICATION_MISSING.equals(candidate.code()))
                .findFirst()
                .orElseThrow();
        assertTrue(failure.field().contains(item.id().toString()));
        assertThrows(BusinessRuleException.class,
                () -> manifest.finalizeManifest(expected(), "finalizer", now.plusHours(1)));
    }

    private CargoManifest manifest(List<CargoManifestItem> items) {
        return new CargoManifest(
                UUID.randomUUID(), "CM-2026-000001", UUID.randomUUID(), "FO-2026-000001",
                items, 0, now, now, "creator", "creator", null, null
        );
    }

    private CargoManifestItem item(BigDecimal quantity, Boolean fragile, Boolean temperatureSensitive) {
        return new CargoManifestItem(
                UUID.randomUUID(), lineId, "Cargo", quantity, "Wrapped by supplier", "SUPPLIER.CODE",
                false, null, false, null, null, fragile, temperatureSensitive
        );
    }

    private List<FreightOrderLookup.ExpectedLine> expected() {
        return List.of(new FreightOrderLookup.ExpectedLine(lineId, "Cargo", BigDecimal.TEN));
    }
}
