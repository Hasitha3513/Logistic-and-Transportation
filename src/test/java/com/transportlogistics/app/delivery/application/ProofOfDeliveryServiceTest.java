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
    private ProofOfDeliveryService service(Orders orders, Proofs proofs, Storage storage) {
        DeliveryTenantContextPort tenant = () -> Optional.of(new DeliveryTenantContextPort.TenantContext(TENANT, "UTC"));
        DeliveryOrderTransaction tx = new DeliveryOrderTransaction() {
            @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
        };
        return new ProofOfDeliveryService(orders, proofs, storage, tenant, tx, Clock.fixed(now.toInstant(), ZoneOffset.UTC));
    }
    private DeliveryOrder ready() {
        var created = DeliveryOrder.create(new DeliveryId(UUID.randomUUID()), new DeliveryNumber("DEL-2026-000001"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD, new DeliveryWindow(now.minusHours(1), now.plusHours(1)), null, now.minusHours(2), "manager");
        return created.markReadyForAssignment(now.minusHours(1), "manager");
    }
    private static final class Orders implements DeliveryOrderRepository {
        private DeliveryOrder value; Orders(DeliveryOrder value) { this.value = value; }
        public DeliveryOrder save(DeliveryOrder order) { value = order; return order; }
        public Optional<DeliveryOrder> findById(UUID id) { return value.id().value().equals(id) ? Optional.of(value) : Optional.empty(); }
        public Optional<DeliveryOrder> findByDeliveryNumber(String n) { return Optional.empty(); }
        public DeliveryOrderUseCase.PageResult<DeliveryOrder> search(DeliveryOrderUseCase.SearchQuery q) { throw new UnsupportedOperationException(); }
    }
    private static final class Proofs implements ProofOfDeliveryRepository {
        private ProofOfDelivery value; public ProofOfDelivery save(ProofOfDelivery p) { value = p; return p; }
        public Optional<ProofOfDelivery> findByDeliveryOrderId(UUID id) { return Optional.ofNullable(value).filter(p -> p.deliveryOrderId().equals(id)); }
    }
    private static final class Storage implements DeliveryEvidenceStoragePort {
        private boolean failRead;
        public StoredEvidence store(UUID t, UUID id, byte[] c, String f) { return new StoredEvidence("opaque", "image/png", c.length, "a".repeat(64)); }
        public StoredContent read(UUID t, String r) { if (failRead) throw new DependencyUnavailableException("POD_STORAGE_UNAVAILABLE", "storage unavailable", null); return new StoredContent(new byte[8], "image/png", 8); }
        public void delete(UUID t, String r) {}
    }
}
