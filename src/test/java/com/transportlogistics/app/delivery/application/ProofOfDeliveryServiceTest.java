package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.inbound.ProofOfDeliveryUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

class ProofOfDeliveryServiceTest {
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-29T12:00:00Z");

    @Test void barcodeFinalizationUsesServerTimeAndCompletesDeliveryAtomically() {
        var orders = new Orders(ready()); var proofs = new Proofs(); var storage = new Storage(); var service = service(orders, proofs, storage);
        var draft = service.create(orders.value.id().value(), new ProofOfDeliveryUseCase.CreateCommand(orders.value.version(), null, null, null, null, null, null), "rider");
        var withBarcode = service.addEvidence(orders.value.id().value(), new ProofOfDeliveryUseCase.AddEvidenceCommand(draft.version(), PodEvidenceType.BARCODE, null, null, " del-2026-000001 ", "manual"), "rider");
        var result = service.finalizeProof(orders.value.id().value(), new ProofOfDeliveryUseCase.FinalizeCommand(orders.value.version(), withBarcode.version()), "rider");
        assertThat(result.proof().status()).isEqualTo(PodStatus.FINALIZED);
        assertThat(result.proof().acceptedAt()).isEqualTo(now);
        assertThat(result.delivery().status()).isEqualTo(DeliveryStatus.DELIVERED);
    }
    @Test void storageFailureLeavesProofDraftAndDeliveryReady() {
        var orders = new Orders(ready()); var proofs = new Proofs(); var storage = new Storage(); var service = service(orders, proofs, storage);
        var draft = service.create(orders.value.id().value(), new ProofOfDeliveryUseCase.CreateCommand(orders.value.version(), null, null, null, null, null, null), "rider");
        var barcode = service.addEvidence(orders.value.id().value(), new ProofOfDeliveryUseCase.AddEvidenceCommand(draft.version(), PodEvidenceType.BARCODE, null, null, "DEL-2026-000001", "MANUAL"), "rider");
        storage.failRead = true;
        // Barcode needs no binary read, so add a binary reference directly to prove dependency failure behavior.
        proofs.value = barcode.add(new PodEvidence(UUID.randomUUID(), PodEvidenceType.PHOTO, "opaque", null, "image/png", 8, "a".repeat(64), "p.png", "FILE", "rider", now), now, "rider");
        assertThatThrownBy(() -> service.finalizeProof(orders.value.id().value(), new ProofOfDeliveryUseCase.FinalizeCommand(orders.value.version(), proofs.value.version()), "rider")).isInstanceOf(DependencyUnavailableException.class);
        assertThat(orders.value.status()).isEqualTo(DeliveryStatus.READY_FOR_ASSIGNMENT);
        assertThat(proofs.value.status()).isEqualTo(PodStatus.DRAFT);
    }
    @Test void offlinePodRecordingWithValidEvidenceAndConsentCompletesDelivery() {
        var orders = new Orders(ready()); var proofs = new Proofs(); var storage = new Storage(); var service = service(orders, proofs, storage);
        var command = new com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.Command(
                orders.value.id().value(), orders.value.version(), "Bob Recipient", "Manager",
                true, "POD-CONSENT-V1", now, now, new java.math.BigDecimal("6.9271"), new java.math.BigDecimal("79.8612"),
                new java.math.BigDecimal("10.0"),
                List.of(new com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.OfflineEvidenceItem(
                        "BARCODE", null, "DEL-2026-000001", "SCANNER", null, null)),
                "offline.rider");
        var result = service.recordOfflinePod(command);
        assertThat(result.status()).isEqualTo("FINALIZED");
        assertThat(result.acceptedAt()).isEqualTo(now);
        assertThat(orders.value.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(proofs.value.status()).isEqualTo(PodStatus.FINALIZED);
        assertThat(proofs.value.evidence()).hasSize(1);
    }

    @Test void offlinePodWithoutConsentThrowsException() {
        var orders = new Orders(ready()); var proofs = new Proofs(); var storage = new Storage(); var service = service(orders, proofs, storage);
        var command = new com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.Command(
                orders.value.id().value(), orders.value.version(), "Bob Recipient", "Manager",
                false, null, null, now, null, null, null,
                List.of(new com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.OfflineEvidenceItem(
                        "SIGNATURE", new byte[]{1, 2, 3}, null, "MANUAL", "sig.png", null)),
                "offline.rider");
        assertThatThrownBy(() -> service.recordOfflinePod(command))
                .isInstanceOf(com.transportlogistics.app.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("Customer consent is required");
    }

    @Test void offlinePodWhenDeliveryAlreadyDeliveredThrowsConflict() {
        var orders = new Orders(ready().markDelivered(now, "prior.rider"));
        var proofs = new Proofs(); var storage = new Storage(); var service = service(orders, proofs, storage);
        var command = new com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.Command(
                orders.value.id().value(), orders.value.version(), "Bob Recipient", "Manager",
                true, "POD-CONSENT-V1", now, now, null, null, null,
                List.of(new com.transportlogistics.app.delivery.OfflineProofOfDeliveryRecorder.OfflineEvidenceItem(
                        "BARCODE", null, "DEL-2026-000001", "SCANNER", null, null)),
                "offline.rider");
        assertThatThrownBy(() -> service.recordOfflinePod(command))
                .isInstanceOf(com.transportlogistics.app.shared.domain.ConflictException.class);
    }

    private ProofOfDeliveryService service(Orders orders, Proofs proofs, Storage storage) {
        DeliveryTenantContextPort tenant = () -> Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT, "UTC"));
        DeliveryOrderTransaction tx = new DeliveryOrderTransaction() {
            @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
        };
        DeliveryExceptionRepository exceptions = new DeliveryExceptionRepository() {
            @Override public DeliveryExceptionCase save(DeliveryExceptionCase exceptionCase) { return exceptionCase; }
            @Override public Optional<DeliveryExceptionCase> findById(UUID id) { return Optional.empty(); }
            @Override public List<DeliveryExceptionCase> findByDeliveryOrderId(DeliveryId deliveryOrderId) { return List.of(); }
            @Override public boolean existsActiveByDeliveryOrderIdAndType(DeliveryId deliveryOrderId, DeliveryExceptionType exceptionType) { return false; }
            @Override public boolean hasActiveBlockingExceptions(DeliveryId deliveryOrderId) { return false; }
        };
        return new ProofOfDeliveryService(orders, proofs, storage, tenant, exceptions, tx, Clock.fixed(now.toInstant(), ZoneOffset.UTC));
    }
    private DeliveryOrder ready() {
        var created = DeliveryOrder.create(new DeliveryId(UUID.randomUUID()), new DeliveryNumber("DEL-2026-000001"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD, new DeliveryWindow(now.minusHours(1), now.plusHours(1)), null, now.minusHours(2), "manager");
        return created.markReadyForAssignment(now.minusHours(1), "manager");
    }
    private static final class Orders implements DeliveryOrderRepository {
        private DeliveryOrder value; Orders(DeliveryOrder value) { this.value = value; }
        public DeliveryOrder save(DeliveryOrder order) { value = order; return order; }
        public Optional<DeliveryOrder> findById(UUID id) { return value.id().value().equals(id) ? Optional.of(value) : Optional.empty(); }
        public Optional<DeliveryOrder> findByIdForUpdate(UUID id) { return findById(id); }
        public Optional<DeliveryOrder> findByDeliveryNumber(String n) { return Optional.empty(); }
        public DeliveryOrderUseCase.PageResult<DeliveryOrder> search(DeliveryOrderUseCase.SearchQuery q) { throw new UnsupportedOperationException(); }
    }
    private static final class Proofs implements ProofOfDeliveryRepository {
        private ProofOfDelivery value; public ProofOfDelivery save(ProofOfDelivery p) { value = p; return p; }
        public Optional<ProofOfDelivery> findByDeliveryOrderId(UUID id) { return Optional.ofNullable(value).filter(p -> p.deliveryOrderId().equals(id)); }
        public void delete(UUID id) {}
    }
    private static final class Storage implements DeliveryEvidenceStoragePort {
        private boolean failRead;
        public StoredEvidence store(UUID t, UUID e, byte[] c, String f) {
            String mime = "image/png";
            if (c.length >= 2 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8) mime = "image/jpeg";
            return new StoredEvidence("ref-" + e, mime, c.length, "chk-" + e);
        }
        public StoredContent read(UUID t, String r) {
            if (failRead) throw new DependencyUnavailableException("STORAGE_UNAVAILABLE", "Storage down", null);
            return new StoredContent(new byte[]{1, 2, 3}, "image/png", 3);
        }
        public void delete(UUID t, String r) {}
    }
}
