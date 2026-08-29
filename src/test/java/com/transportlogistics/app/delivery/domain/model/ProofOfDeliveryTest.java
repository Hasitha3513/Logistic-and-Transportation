package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ProofOfDeliveryTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-29T10:00:00Z");
    @Test void rejectsPartialAndOutOfRangeGeolocation() {
        assertThatThrownBy(() -> draft(BigDecimal.ONE, null, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> draft(BigDecimal.valueOf(91), BigDecimal.ZERO, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> draft(BigDecimal.ZERO, BigDecimal.valueOf(181), null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> draft(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
    }
    @Test void acceptsMissingGeolocation() { assertThat(draft(null, null, null).status()).isEqualTo(PodStatus.DRAFT); }
    @Test void rejectsFinalizationWithoutPrimaryEvidence() {
        assertThatThrownBy(() -> draft(null, null, null).finalizeAt("DEL-2026-000001", now, "rider"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("At least one");
    }
    @Test void acceptsSignaturePhotoAndBarcodeIndividually() {
        assertThat(with(binary(PodEvidenceType.SIGNATURE), "Recipient").finalizeAt("DEL-2026-000001", now, "rider").status()).isEqualTo(PodStatus.FINALIZED);
        assertThat(with(binary(PodEvidenceType.PHOTO), null).finalizeAt("DEL-2026-000001", now, "rider").status()).isEqualTo(PodStatus.FINALIZED);
        assertThat(with(barcode("DEL-2026-000001"), null).finalizeAt("DEL-2026-000001", now, "rider").status()).isEqualTo(PodStatus.FINALIZED);
    }
    @Test void signatureRequiresSignerAndBarcodeMustMatch() {
        assertThatThrownBy(() -> with(binary(PodEvidenceType.SIGNATURE), null).finalizeAt("DEL-2026-000001", now, "rider")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> with(barcode("DEL-2026-999999"), null).finalizeAt("DEL-2026-000001", now, "rider")).isInstanceOf(BusinessRuleException.class);
    }
    @Test void limitsSignatureBarcodeAndPhotos() {
        var proof = with(binary(PodEvidenceType.SIGNATURE), "Signer");
        assertThatThrownBy(() -> proof.add(binary(PodEvidenceType.SIGNATURE), now, "rider")).isInstanceOf(ConflictException.class);
        var photos = draft(null, null, null).add(binary(PodEvidenceType.PHOTO), now, "rider").add(binary(PodEvidenceType.PHOTO), now, "rider").add(binary(PodEvidenceType.PHOTO), now, "rider");
        assertThatThrownBy(() -> photos.add(binary(PodEvidenceType.PHOTO), now, "rider")).isInstanceOf(ConflictException.class);
    }
    @Test void finalizedProofIsImmutableAndUsesServerTime() {
        var finalProof = with(barcode("DEL-2026-000001"), null).finalizeAt("DEL-2026-000001", now.plusHours(2), "rider");
        assertThat(finalProof.acceptedAt()).isEqualTo(now.plusHours(2));
        assertThatThrownBy(() -> finalProof.add(binary(PodEvidenceType.PHOTO), now, "rider")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> finalProof.remove(finalProof.evidence().getFirst().id(), now, "rider")).isInstanceOf(ConflictException.class);
    }
    @Test void deliveryAllowsOnlyReadyToDelivered() {
        var ready = order().markReadyForAssignment(now, "manager");
        assertThat(ready.markDelivered(now.plusMinutes(1), "rider").status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThatThrownBy(() -> order().markDelivered(now, "rider")).isInstanceOf(BusinessRuleException.class);
    }
    private ProofOfDelivery draft(BigDecimal lat, BigDecimal lon, BigDecimal accuracy) { return ProofOfDelivery.draft(UUID.randomUUID(), UUID.randomUUID(), now.minusMinutes(2), lat, lon, accuracy, null, null, now, "rider"); }
    private ProofOfDelivery with(PodEvidence item, String signer) { var p = ProofOfDelivery.draft(UUID.randomUUID(), UUID.randomUUID(), now, null, null, null, signer, null, now, "rider"); return p.add(item, now, "rider"); }
    private PodEvidence binary(PodEvidenceType type) { return new PodEvidence(UUID.randomUUID(), type, "opaque", null, "image/png", 8, "a".repeat(64), "proof.png", "FILE", "rider", now); }
    private PodEvidence barcode(String value) { return new PodEvidence(UUID.randomUUID(), PodEvidenceType.BARCODE, null, value, null, 0, null, null, "MANUAL", "rider", now); }
    private DeliveryOrder order() { return DeliveryOrder.create(new DeliveryId(UUID.randomUUID()), new DeliveryNumber("DEL-2026-000001"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD, new DeliveryWindow(now, now.plusHours(1)), null, now, "manager"); }
}
