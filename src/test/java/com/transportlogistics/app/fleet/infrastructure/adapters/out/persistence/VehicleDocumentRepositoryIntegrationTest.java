package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;

@SpringBootTest
@Transactional
class VehicleDocumentRepositoryIntegrationTest {
    @Autowired VehicleRepository vehicles;
    @Autowired VehicleDocumentRepository documents;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persistsQueriesAndRetainsSoftDeletedDocument() {
        var vehicleId = UUID.randomUUID();
        var vehicle = new Vehicle(vehicleId, "REG-" + vehicleId, null, null, UUID.randomUUID(), UUID.randomUUID(),
                null, null, 2025, "COMPANY_OWNED", "AVAILABLE", null, null, 1000d, true);
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        var now = OffsetDateTime.now();
        var documentId = UUID.randomUUID();
        var saved = documents.save(new VehicleDocument(documentId, vehicleId, "INSURANCE", "POL-55",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), "https://files.example/POL-55",
                true, VehicleDocumentStatus.ACTIVE, true, now, now, "tester", "tester"));

        assertEquals(documentId, saved.id());
        assertEquals(1, documents.findVisibleByVehicleId(vehicleId).size());
        assertEquals(1, documents.findActiveByVehicleId(vehicleId).size());
        assertTrue(documents.activeDuplicateExists(vehicleId, "insurance", "pol-55", null));

        documents.save(new VehicleDocument(saved.id(), saved.vehicleId(), saved.documentType(), saved.documentNumber(),
                saved.issueDate(), saved.expiryDate(), saved.fileReference(), saved.mandatoryForDispatch(),
                VehicleDocumentStatus.DELETED, false, saved.createdAt(), OffsetDateTime.now(), saved.createdBy(), "tester"));

        assertTrue(documents.findVisibleByVehicleId(vehicleId).isEmpty());
        assertTrue(documents.findActiveByVehicleId(vehicleId).isEmpty());
        assertEquals(VehicleDocumentStatus.DELETED, documents.findById(documentId).orElseThrow().status());
    }
}
