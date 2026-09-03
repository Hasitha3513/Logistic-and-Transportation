package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryExceptionUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryExceptionServiceTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-31T10:00:00Z");
    private final Clock clock = Clock.fixed(now.toInstant(), ZoneOffset.UTC);

    private InMemoryOrderRepo orderRepo;
    private InMemoryAttemptRepo attemptRepo;
    private InMemoryExceptionRepo exceptionRepo;
    private DummyStorage storage;
    private DummyLocationLookup locationLookup;
    private DeliveryOperationalExceptionPublisher operationalExceptions;
    private DeliveryExceptionService service;

    @BeforeEach
    void setUp() {
        orderRepo = new InMemoryOrderRepo();
        attemptRepo = new InMemoryAttemptRepo();
        exceptionRepo = new InMemoryExceptionRepo();
        storage = new DummyStorage();
        locationLookup = new DummyLocationLookup();
        operationalExceptions = mock(DeliveryOperationalExceptionPublisher.class);
        DeliveryTenantContextPort tenantContext = () -> Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT_A, "UTC"));
        DeliveryOrderTransaction tx = new DeliveryOrderTransaction() {
            @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
        };

        service = new DeliveryExceptionService(
                orderRepo, attemptRepo, exceptionRepo, storage, locationLookup, tenantContext, tx,
                operationalExceptions, clock
        );
    }

    @Test
    void reportDamagedDeliveryRequiresPhotoUpload() {
        var order = createReadyOrder();
        orderRepo.save(order);

        var command = new DeliveryExceptionUseCase.ReportCommand(
                null,
                DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH,
                "Package torn",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> service.reportException(order.id().value(), command, "dispatcher"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("At least one photo evidence is required");
    }

    @Test
    void reportDamagedDeliverySucceedsWithPhoto() {
        var order = createReadyOrder();
        orderRepo.save(order);

        var command = new DeliveryExceptionUseCase.ReportCommand(
                null,
                DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH,
                "Package torn and leaking",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(new DeliveryExceptionUseCase.EvidenceUpload("fake-bytes".getBytes(), "damage.png"))
        );

        DeliveryExceptionCase result = service.reportException(order.id().value(), command, "dispatcher");
        assertThat(result.status()).isEqualTo(DeliveryExceptionStatus.OPEN);
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().get(0).detectedContentType()).isEqualTo("image/png");
        verify(operationalExceptions).publish(result);
    }

    @Test
    void duplicateActiveExceptionOfSameTypeRejected() {
        var order = createReadyOrder();
        orderRepo.save(order);

        var command = new DeliveryExceptionUseCase.ReportCommand(
                null,
                DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.MEDIUM,
                "Wrong door number",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        service.reportException(order.id().value(), command, "dispatcher");

        assertThatThrownBy(() -> service.reportException(order.id().value(), command, "dispatcher"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists for this delivery");
    }

    @Test
    void resolveWrongAddressUpdatesLocationAndClosesCase() {
        var order = createReadyOrder();
        orderRepo.save(order);
        UUID locId = UUID.randomUUID();
        locationLookup.addActive(locId);

        var reportCmd = new DeliveryExceptionUseCase.ReportCommand(
                null,
                DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.MEDIUM,
                "Wrong street",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        DeliveryExceptionCase created = service.reportException(order.id().value(), reportCmd, "dispatcher");

        var resolveCmd = new DeliveryExceptionUseCase.ResolveCommand(
                created.version(),
                DeliveryExceptionResolutionCode.ADDRESS_CORRECTED,
                "Customer gave corrected flat address",
                locId,
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE
        );

        DeliveryExceptionCase resolved = service.resolveException(order.id().value(), created.id(), resolveCmd, "manager");
        assertThat(resolved.status()).isEqualTo(DeliveryExceptionStatus.RESOLVED);
        assertThat(resolved.correctedLocationId()).isEqualTo(locId);
    }

    @Test
    void resolveRtoDispositionInitiatesReturnToBaseOnOrder() {
        var order = createReadyOrder();
        orderRepo.save(order);

        var reportCmd = new DeliveryExceptionUseCase.ReportCommand(
                null,
                DeliveryExceptionType.RECIPIENT_REFUSAL,
                DeliveryExceptionSeverity.HIGH,
                "Customer refused payment",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        DeliveryExceptionCase created = service.reportException(order.id().value(), reportCmd, "dispatcher");

        var resolveCmd = new DeliveryExceptionUseCase.ResolveCommand(
                created.version(),
                DeliveryExceptionResolutionCode.REFUSAL_CONFIRMED_RTO,
                "Authorize parcel return to depot",
                null,
                DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED
        );

        service.resolveException(order.id().value(), created.id(), resolveCmd, "manager");
        DeliveryOrder updatedOrder = orderRepo.findById(order.id().value()).orElseThrow();
        assertThat(updatedOrder.status()).isEqualTo(DeliveryStatus.RETURN_TO_BASE);
    }

    @Test
    void staleVersionThrowsConflict() {
        var order = createReadyOrder();
        orderRepo.save(order);

        var reportCmd = new DeliveryExceptionUseCase.ReportCommand(
                null,
                DeliveryExceptionType.OTP_MISMATCH,
                DeliveryExceptionSeverity.HIGH,
                "Failed 3 times",
                null,
                "REF-FAIL-1",
                null,
                null,
                null,
                null,
                List.of()
        );
        DeliveryExceptionCase created = service.reportException(order.id().value(), reportCmd, "dispatcher");

        var resolveCmd = new DeliveryExceptionUseCase.ResolveCommand(
                999L, // wrong version
                DeliveryExceptionResolutionCode.OTP_OVERRIDDEN_BY_MANAGER,
                "Customer ID verified",
                null,
                null
        );

        assertThatThrownBy(() -> service.resolveException(order.id().value(), created.id(), resolveCmd, "manager"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("version");
    }

    private DeliveryOrder createReadyOrder() {
        var order = DeliveryOrder.create(
                new DeliveryId(UUID.randomUUID()),
                new DeliveryNumber("DEL-2026-000001"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DeliveryPriority.NORMAL,
                DeliveryServiceType.STANDARD,
                new DeliveryWindow(now.minusHours(1), now.plusHours(1)),
                null,
                now.minusHours(2),
                "dispatcher"
        );
        return order.markReadyForAssignment(now.minusHours(1), "dispatcher");
    }

    // --- In-Memory Test Doubles ---
    private static class InMemoryOrderRepo implements DeliveryOrderRepository {
        private final Map<UUID, DeliveryOrder> store = new ConcurrentHashMap<>();
        @Override public DeliveryOrder save(DeliveryOrder order) { store.put(order.id().value(), order); return order; }
        @Override public Optional<DeliveryOrder> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<DeliveryOrder> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public Optional<DeliveryOrder> findByDeliveryNumber(String deliveryNumber) { return Optional.empty(); }
        @Override public DeliveryOrderUseCase.PageResult<DeliveryOrder> search(DeliveryOrderUseCase.SearchQuery query) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryAttemptRepo implements DeliveryAttemptRepository {
        @Override public DeliveryAttempt save(DeliveryAttempt attempt) { return attempt; }
        @Override public Optional<DeliveryAttempt> findById(UUID id) { return Optional.empty(); }
        @Override public List<DeliveryAttempt> findByDeliveryId(UUID deliveryId) { return List.of(); }
        @Override public int countByDeliveryId(UUID deliveryId) { return 1; }
        @Override public Optional<DeliveryAttempt> findLatestByDeliveryId(UUID deliveryId) { return Optional.empty(); }
    }

    private static class InMemoryExceptionRepo implements DeliveryExceptionRepository {
        private final Map<UUID, DeliveryExceptionCase> store = new ConcurrentHashMap<>();
        @Override public DeliveryExceptionCase save(DeliveryExceptionCase exceptionCase) { store.put(exceptionCase.id(), exceptionCase); return exceptionCase; }
        @Override public Optional<DeliveryExceptionCase> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<DeliveryExceptionCase> findByDeliveryOrderId(DeliveryId deliveryOrderId) {
            return store.values().stream().filter(e -> e.deliveryOrderId().equals(deliveryOrderId)).toList();
        }
        @Override public boolean existsActiveByDeliveryOrderIdAndType(DeliveryId deliveryOrderId, DeliveryExceptionType exceptionType) {
            return store.values().stream().anyMatch(e -> e.deliveryOrderId().equals(deliveryOrderId)
                    && e.exceptionType() == exceptionType
                    && (e.status() == DeliveryExceptionStatus.OPEN || e.status() == DeliveryExceptionStatus.UNDER_INVESTIGATION));
        }
        @Override public boolean hasActiveBlockingExceptions(DeliveryId deliveryOrderId) {
            return store.values().stream().anyMatch(e -> e.deliveryOrderId().equals(deliveryOrderId) && e.isBlockingPodFinalization());
        }
    }

    private static class DummyStorage implements DeliveryEvidenceStoragePort {
        @Override public StoredEvidence store(UUID tenantId, UUID evidenceId, byte[] content, String originalFilename) {
            return new StoredEvidence("ref-" + evidenceId, "image/png", content.length, "dummy-sha256");
        }
        @Override public StoredContent read(UUID tenantId, String storageReference) {
            return new StoredContent(new byte[10], "image/png", 10);
        }
        @Override public void delete(UUID tenantId, String storageReference) {}
    }

    private static class DummyLocationLookup implements DeliveryLocationLookupPort {
        private final Set<UUID> activeLocations = new HashSet<>();
        void addActive(UUID id) { activeLocations.add(id); }
        @Override public Optional<LocationReference> findLocation(UUID locationId) {
            if (activeLocations.contains(locationId)) {
                return Optional.of(new LocationReference(locationId, "LOC-01", "Depot", true));
            }
            return Optional.empty();
        }
    }
}
