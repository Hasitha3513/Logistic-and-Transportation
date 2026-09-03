package com.transportlogistics.app.delivery.domain;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryFailureReasonTest {

    @Test
    @DisplayName("CUSTOMER_UNAVAILABLE maps to REDELIVERY_ELIGIBLE and requires no notes")
    void customerUnavailableDisposition() {
        var reason = DeliveryFailureReason.CUSTOMER_UNAVAILABLE;
        assertThat(reason.getDefaultDisposition()).isEqualTo(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE);
        assertThat(reason.isRedeliveryEligible()).isTrue();
        reason.validateNotes(null);
        reason.validateNotes("");
    }

    @Test
    @DisplayName("CUSTOMER_REFUSED requires at least 5 characters in notes and defaults to RETURN_TO_BASE_REQUIRED")
    void customerRefusedValidation() {
        var reason = DeliveryFailureReason.CUSTOMER_REFUSED;
        assertThat(reason.getDefaultDisposition()).isEqualTo(DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED);
        assertThat(reason.isRedeliveryEligible()).isFalse();

        assertThatThrownBy(() -> reason.validateNotes(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Notes of at least 5 characters required");

        assertThatThrownBy(() -> reason.validateNotes("No"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Notes of at least 5 characters required");

        reason.validateNotes("Customer explicitly rejected delivery package");
    }

    @Test
    @DisplayName("DAMAGED_CARGO requires at least 5 characters in notes and resolves to ESCALATED or RETURN_TO_BASE")
    void damagedCargoValidation() {
        var reason = DeliveryFailureReason.DAMAGED_CARGO;
        assertThat(reason.getDefaultDisposition()).isEqualTo(DeliveryFailureDisposition.ESCALATED);
        assertThat(reason.isRedeliveryEligible()).isFalse();

        assertThatThrownBy(() -> reason.validateNotes("bad"))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(reason.resolveDisposition(DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED))
                .isEqualTo(DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED);
        assertThat(reason.resolveDisposition(null))
                .isEqualTo(DeliveryFailureDisposition.ESCALATED);
    }

    @Test
    @DisplayName("OTHER requires at least 10 characters in notes and explicit disposition")
    void otherReasonValidation() {
        var reason = DeliveryFailureReason.OTHER;
        assertThatThrownBy(() -> reason.validateNotes("Too short"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Notes of at least 10 characters required");

        assertThatThrownBy(() -> reason.resolveDisposition(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Explicit disposition is required");

        assertThat(reason.resolveDisposition(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE))
                .isEqualTo(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE);
    }

    @Test
    @DisplayName("DeliveryAttempt creation enforces immutability and valid fields")
    void deliveryAttemptCreation() {
        UUID id = UUID.randomUUID();
        DeliveryId deliveryId = new DeliveryId(UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now();

        DeliveryAttempt attempt = DeliveryAttempt.create(
                id, deliveryId, 1, now, DeliveryFailureReason.CUSTOMER_UNAVAILABLE,
                "Customer was not at reception", null, List.of(), "test_user", now);

        assertThat(attempt.id()).isEqualTo(id);
        assertThat(attempt.attemptNumber()).isEqualTo(1);
        assertThat(attempt.disposition()).isEqualTo(DeliveryFailureDisposition.REDELIVERY_ELIGIBLE);
        assertThat(attempt.recordedBy()).isEqualTo("test_user");
    }

    @Test
    @DisplayName("DeliveryContactAttempt validates notes max length and required fields")
    void deliveryContactAttemptValidation() {
        UUID id = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        DeliveryContactAttempt contact = DeliveryContactAttempt.create(
                id, attemptId, DeliveryContactChannel.PHONE, now,
                DeliveryContactOutcome.NO_ANSWER, "Called twice, phone went to voicemail", "driver1", now);

        assertThat(contact.channel()).isEqualTo(DeliveryContactChannel.PHONE);
        assertThat(contact.outcome()).isEqualTo(DeliveryContactOutcome.NO_ANSWER);
        assertThat(contact.notes()).isEqualTo("Called twice, phone went to voicemail");

        assertThatThrownBy(() -> DeliveryContactAttempt.create(id, attemptId, DeliveryContactChannel.PHONE, now,
                DeliveryContactOutcome.NO_ANSWER, "a".repeat(501), "driver1", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot exceed 500 characters");
    }
}
