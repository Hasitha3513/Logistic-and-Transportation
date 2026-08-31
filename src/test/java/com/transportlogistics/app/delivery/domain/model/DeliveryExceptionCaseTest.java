package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DeliveryExceptionCaseTest {

    private final OffsetDateTime now = OffsetDateTime.now();
    private final DeliveryId deliveryId = new DeliveryId(UUID.randomUUID());
    private final String actor = "manager.operator";

    @Test
    void createDamagedDeliveryRequiresAtLeastOnePhotoEvidence() {
        UUID caseId = UUID.randomUUID();
        assertThatThrownBy(() -> DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH, "Broken carton", null, null, null, null,
                null, null, List.of(), actor, now
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("At least one photo evidence is required");
    }

    @Test
    void createDamagedDeliveryEnforcesMaxThreePhotos() {
        UUID caseId = UUID.randomUUID();
        var ev1 = new DeliveryExceptionEvidence(UUID.randomUUID(), caseId, "ref1", "image/png", 100, "chk1", "photo1.png", actor, now);
        var ev2 = new DeliveryExceptionEvidence(UUID.randomUUID(), caseId, "ref2", "image/png", 100, "chk2", "photo2.png", actor, now);
        var ev3 = new DeliveryExceptionEvidence(UUID.randomUUID(), caseId, "ref3", "image/png", 100, "chk3", "photo3.png", actor, now);
        var ev4 = new DeliveryExceptionEvidence(UUID.randomUUID(), caseId, "ref4", "image/png", 100, "chk4", "photo4.png", actor, now);

        assertThatThrownBy(() -> DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH, "Broken carton", null, null, null, null,
                null, null, List.of(ev1, ev2, ev3, ev4), actor, now
        )).isInstanceOf(ConflictException.class)
          .hasMessageContaining("A maximum of 3 photo evidences are allowed");
    }

    @Test
    void createWrongAddressCaseSuccessfully() {
        UUID caseId = UUID.randomUUID();
        DeliveryExceptionCase exc = DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.MEDIUM, "Building does not exist on map", null, null, null, null,
                null, null, List.of(), actor, now
        );

        assertThat(exc.status()).isEqualTo(DeliveryExceptionStatus.OPEN);
        assertThat(exc.exceptionType()).isEqualTo(DeliveryExceptionType.WRONG_ADDRESS);
        assertThat(exc.resolution()).isNull();
    }

    @Test
    void investigateTransitionsStatus() {
        UUID caseId = UUID.randomUUID();
        DeliveryExceptionCase exc = DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.MEDIUM, "Missing street number", null, null, null, null,
                null, null, List.of(), actor, now
        );

        DeliveryExceptionCase investigated = exc.investigate("investigator.user");
        assertThat(investigated.status()).isEqualTo(DeliveryExceptionStatus.UNDER_INVESTIGATION);
        assertThat(investigated.version()).isEqualTo(1L);
    }

    @Test
    void resolveAddressCorrectionRequiresCorrectedLocation() {
        UUID caseId = UUID.randomUUID();
        DeliveryExceptionCase exc = DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.MEDIUM, "Incorrect address provided", null, null, null, null,
                null, null, List.of(), actor, now
        );

        var resolution = new DeliveryExceptionResolution(
                DeliveryExceptionResolutionCode.ADDRESS_CORRECTED,
                "New destination verified with recipient",
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE,
                now,
                actor
        );

        assertThatThrownBy(() -> exc.resolve(resolution, null, actor, now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Corrected destination location is required");
    }

    @Test
    void resolveAddressCorrectionSucceedsWithLocation() {
        UUID caseId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        DeliveryExceptionCase exc = DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.WRONG_ADDRESS,
                DeliveryExceptionSeverity.MEDIUM, "Incorrect address provided", null, null, null, null,
                null, null, List.of(), actor, now
        );

        var resolution = new DeliveryExceptionResolution(
                DeliveryExceptionResolutionCode.ADDRESS_CORRECTED,
                "New destination verified with recipient",
                DeliveryFailureDisposition.REDELIVERY_ELIGIBLE,
                now,
                actor
        );

        DeliveryExceptionCase resolved = exc.resolve(resolution, locId, actor, now);
        assertThat(resolved.status()).isEqualTo(DeliveryExceptionStatus.RESOLVED);
        assertThat(resolved.correctedLocationId()).isEqualTo(locId);
        assertThat(resolved.resolution().resolutionCode()).isEqualTo(DeliveryExceptionResolutionCode.ADDRESS_CORRECTED);
    }

    @Test
    void cancelCaseSetsCancelledStatus() {
        UUID caseId = UUID.randomUUID();
        DeliveryExceptionCase exc = DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.RECIPIENT_REFUSAL,
                DeliveryExceptionSeverity.LOW, "Customer changed mind", null, null, null, null,
                null, null, List.of(), actor, now
        );

        DeliveryExceptionCase cancelled = exc.cancel("Duplicate case opened in error", actor, now);
        assertThat(cancelled.status()).isEqualTo(DeliveryExceptionStatus.CANCELLED);
    }

    @Test
    void terminalCaseCannotBeMutated() {
        UUID caseId = UUID.randomUUID();
        DeliveryExceptionCase exc = DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.RECIPIENT_REFUSAL,
                DeliveryExceptionSeverity.LOW, "Refusal at gate", null, null, null, null,
                null, null, List.of(), actor, now
        );

        var resolution = new DeliveryExceptionResolution(
                DeliveryExceptionResolutionCode.REFUSAL_CONFIRMED_RTO,
                "Return authorized",
                DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED,
                now,
                actor
        );
        DeliveryExceptionCase resolved = exc.resolve(resolution, null, actor, now);

        assertThatThrownBy(() -> resolved.investigate(actor))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> resolved.cancel("reason", actor, now))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> resolved.resolve(resolution, null, actor, now))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void blockingPodFinalizationCheck() {
        UUID caseId = UUID.randomUUID();
        var ev = new DeliveryExceptionEvidence(UUID.randomUUID(), caseId, "ref1", "image/png", 100, "chk1", "photo1.png", actor, now);
        DeliveryExceptionCase damage = DeliveryExceptionCase.create(
                caseId, deliveryId, null, DeliveryExceptionType.DAMAGED_DELIVERY,
                DeliveryExceptionSeverity.HIGH, "Punctured container", null, null, null, null,
                null, null, List.of(ev), actor, now
        );
        assertThat(damage.isBlockingPodFinalization()).isTrue();

        DeliveryExceptionCase otp = DeliveryExceptionCase.create(
                UUID.randomUUID(), deliveryId, null, DeliveryExceptionType.OTP_MISMATCH,
                DeliveryExceptionSeverity.HIGH, "Customer OTP mismatch", null, "REF-123", null, null,
                null, null, List.of(), actor, now
        );
        assertThat(otp.isBlockingPodFinalization()).isTrue();

        DeliveryExceptionCase partial = DeliveryExceptionCase.create(
                UUID.randomUUID(), deliveryId, null, DeliveryExceptionType.PARTIAL_DELIVERY,
                DeliveryExceptionSeverity.LOW, "1 box missing", null, null, "1 box", "1 box",
                BigDecimal.ONE, BigDecimal.ONE, List.of(), actor, now
        );
        assertThat(partial.isBlockingPodFinalization()).isFalse();
    }
}
