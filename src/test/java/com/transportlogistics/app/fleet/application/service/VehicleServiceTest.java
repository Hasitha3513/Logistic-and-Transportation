package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.application.ports.out.VehicleCategoryRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleTypeRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleCategory;
import com.transportlogistics.app.fleet.domain.model.VehicleType;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VehicleServiceTest {

    private VehicleRepository repo;
    private VehicleCategoryRepository categories;
    private VehicleTypeRepository types;
    private VehicleAllocationAvailability allocations;
    private VehicleService service;

    private UUID categoryId;
    private UUID typeId;
    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        repo = mock(VehicleRepository.class);
        categories = mock(VehicleCategoryRepository.class);
        types = mock(VehicleTypeRepository.class);
        allocations = mock(VehicleAllocationAvailability.class);
        service = new VehicleService(repo, categories, types, allocations);

        categoryId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        sampleVehicle = new Vehicle(UUID.randomUUID(), "WP-CAB-1201", "CH-111", "ENG-222",
                categoryId, typeId, "Isuzu", "NPR", 2021, "COMPANY_OWNED",
                "AVAILABLE", 10000.0, 200.0, 5000.0, true);

        when(categories.findById(categoryId)).thenReturn(Optional.of(new VehicleCategory(categoryId, "TRK", "Trucks", "Heavy trucks", true)));
        when(types.findById(typeId)).thenReturn(Optional.of(new VehicleType(typeId, categoryId, "BOX", "Box Truck", "Enclosed cargo", true)));
        when(repo.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Successfully create vehicle with valid master data and unique identifiers")
    void createVehicleSuccess() {
        when(repo.findByRegistrationNumber("WP-CAB-1201")).thenReturn(Optional.empty());
        when(repo.findByChassisNumber("CH-111")).thenReturn(Optional.empty());
        when(repo.findByEngineNumber("ENG-222")).thenReturn(Optional.empty());

        var created = service.create(sampleVehicle);

        assertThat(created).isNotNull();
        assertThat(created.registrationNumber()).isEqualTo("WP-CAB-1201");
        verify(repo).save(sampleVehicle);
    }

    @Test
    @DisplayName("Reject create when registration number is duplicate")
    void createDuplicateRegistration() {
        when(repo.findByRegistrationNumber("WP-CAB-1201")).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> service.create(sampleVehicle))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).code()).isEqualTo("VEHICLE_REGISTRATION_DUPLICATE"));

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Reject create when chassis number is duplicate")
    void createDuplicateChassis() {
        when(repo.findByRegistrationNumber("WP-CAB-1201")).thenReturn(Optional.empty());
        when(repo.findByChassisNumber("CH-111")).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> service.create(sampleVehicle))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).code()).isEqualTo("VEHICLE_CHASSIS_DUPLICATE"));

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Reject create when engine number is duplicate")
    void createDuplicateEngine() {
        when(repo.findByRegistrationNumber("WP-CAB-1201")).thenReturn(Optional.empty());
        when(repo.findByChassisNumber("CH-111")).thenReturn(Optional.empty());
        when(repo.findByEngineNumber("ENG-222")).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> service.create(sampleVehicle))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).code()).isEqualTo("VEHICLE_ENGINE_DUPLICATE"));

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Reject create when category does not exist or is inactive")
    void createInvalidCategory() {
        when(categories.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(sampleVehicle))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> assertThat(((BusinessRuleException) e).code()).isEqualTo("VEHICLE_MASTER_REFERENCE_INVALID"));

        when(categories.findById(categoryId)).thenReturn(Optional.of(new VehicleCategory(categoryId, "TRK", "Trucks", "Heavy", false)));
        assertThatThrownBy(() -> service.create(sampleVehicle))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> assertThat(((BusinessRuleException) e).code()).isEqualTo("VEHICLE_MASTER_REFERENCE_INVALID"));
    }

    @Test
    @DisplayName("Reject create when vehicle type does not belong to selected category")
    void createMismatchedCategoryAndType() {
        var otherCategoryId = UUID.randomUUID();
        when(types.findById(typeId)).thenReturn(Optional.of(new VehicleType(typeId, otherCategoryId, "BOX", "Box Truck", "Enclosed cargo", true)));

        assertThatThrownBy(() -> service.create(sampleVehicle))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> assertThat(((BusinessRuleException) e).code()).isEqualTo("VEHICLE_MASTER_REFERENCE_INVALID"));
    }

    @Test
    @DisplayName("Get vehicle returns entity or throws NotFoundException")
    void getVehicle() {
        var id = sampleVehicle.id();
        when(repo.findById(id)).thenReturn(Optional.of(sampleVehicle));

        var found = service.get(id);
        assertThat(found).isEqualTo(sampleVehicle);

        var unknownId = UUID.randomUUID();
        when(repo.findById(unknownId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(unknownId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("List returns all vehicles")
    void listVehicles() {
        when(repo.findAll()).thenReturn(List.of(sampleVehicle));
        var list = service.list();
        assertThat(list).hasSize(1);
    }

    @Test
    @DisplayName("Successfully update vehicle specifications")
    void updateVehicleSuccess() {
        var id = sampleVehicle.id();
        when(repo.findById(id)).thenReturn(Optional.of(sampleVehicle));
        when(repo.existsByRegistrationNumberAndIdNot("WP-CAB-1201", id)).thenReturn(false);
        when(repo.existsByChassisNumberAndIdNot("CH-111", id)).thenReturn(false);
        when(repo.existsByEngineNumberAndIdNot("ENG-222", id)).thenReturn(false);

        var updatedInput = new Vehicle(id, "WP-CAB-1201", "CH-111", "ENG-222", categoryId, typeId,
                "Isuzu", "NPR Updated", 2021, "COMPANY_OWNED", "AVAILABLE", 12000.0, 250.0, 5000.0, true);

        var result = service.update(id, updatedInput);
        assertThat(result.model()).isEqualTo("NPR Updated");
        assertThat(result.currentOdometerKm()).isEqualTo(12000.0);
    }

    @Test
    @DisplayName("Reject update with retrograde odometer reading")
    void updateRetrogradeOdometer() {
        var id = sampleVehicle.id();
        when(repo.findById(id)).thenReturn(Optional.of(sampleVehicle));

        var retrograde = new Vehicle(id, "WP-CAB-1201", "CH-111", "ENG-222", categoryId, typeId,
                "Isuzu", "NPR", 2021, "COMPANY_OWNED", "AVAILABLE", 5000.0, 200.0, 5000.0, true);

        assertThatThrownBy(() -> service.update(id, retrograde))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> assertThat(((BusinessRuleException) e).code()).isEqualTo("VEHICLE_DATA_INVALID"));
    }

    @Test
    @DisplayName("Reject update with invalid operational status transition")
    void updateInvalidStatusTransition() {
        var id = sampleVehicle.id();
        var maintenanceVehicle = new Vehicle(id, "WP-CAB-1201", "CH-111", "ENG-222", categoryId, typeId,
                "Isuzu", "NPR", 2021, "COMPANY_OWNED", "MAINTENANCE", 10000.0, 200.0, 5000.0, true);
        when(repo.findById(id)).thenReturn(Optional.of(maintenanceVehicle));

        var invalidAllocated = new Vehicle(id, "WP-CAB-1201", "CH-111", "ENG-222", categoryId, typeId,
                "Isuzu", "NPR", 2021, "COMPANY_OWNED", "ALLOCATED", 10000.0, 200.0, 5000.0, true);

        assertThatThrownBy(() -> service.update(id, invalidAllocated))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).code()).isEqualTo("VEHICLE_STATUS_TRANSITION_INVALID"));
    }

    @Test
    @DisplayName("Deactivate sets active flag to false")
    void deactivateSuccess() {
        var id = sampleVehicle.id();
        when(repo.findById(id)).thenReturn(Optional.of(sampleVehicle));
        when(allocations.hasOverlap(eq(id), any(), any(), isNull())).thenReturn(false);

        service.deactivate(id);

        verify(repo).save(argThat(v -> !v.active()));
    }

    @Test
    @DisplayName("Reject deactivation when vehicle has active trip allocations")
    void deactivateBlockedByActiveAllocation() {
        var id = sampleVehicle.id();
        when(repo.findById(id)).thenReturn(Optional.of(sampleVehicle));
        when(allocations.hasOverlap(eq(id), any(), any(), isNull())).thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).code()).isEqualTo("VEHICLE_RETIREMENT_BLOCKED"));
    }
}
