package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.*;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.*;
import com.transportlogistics.app.delivery.domain.model.DeliveryAttempt;
import com.transportlogistics.app.delivery.domain.model.DeliveryContactAttempt;
import com.transportlogistics.app.delivery.domain.model.DeliveryEscalation;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.ports.inbound.FailedDeliveryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries/{id}")
public class FailedDeliveryController {
    private final FailedDeliveryUseCase useCase;

    public FailedDeliveryController(FailedDeliveryUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/failed-attempt")
    @PreAuthorize("hasAuthority('DELIVERY_FAIL_RECORD')")
    public ResponseEntity<DeliveryAttemptResponse> recordFailedAttempt(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecordFailedAttemptRequest request,
            Authentication auth) {
        String actor = auth.getName();
        List<FailedDeliveryUseCase.ContactAttemptInput> contacts = request.contactAttempts() == null
                ? List.of()
                : request.contactAttempts().stream()
                .map(c -> new FailedDeliveryUseCase.ContactAttemptInput(
                        c.channel(), c.contactTimestamp(), c.outcome(), c.notes()))
                .toList();

        FailedDeliveryUseCase.RecordFailedAttemptCommand command = new FailedDeliveryUseCase.RecordFailedAttemptCommand(
                request.expectedVersion(),
                request.failureReason(),
                request.notes(),
                request.requestedDisposition(),
                request.attemptTimestamp(),
                contacts
        );

        DeliveryAttempt attempt = useCase.recordFailedAttempt(id, command, actor);
        return ResponseEntity.ok(DeliveryAttemptResponse.from(attempt));
    }

    @PostMapping("/failed-attempts/{attemptId}/contacts")
    @PreAuthorize("hasAuthority('DELIVERY_FAIL_RECORD')")
    public ResponseEntity<DeliveryContactAttemptResponse> recordContactAttempt(
            @PathVariable("id") UUID id,
            @PathVariable("attemptId") UUID attemptId,
            @Valid @RequestBody RecordContactAttemptRequest request,
            Authentication auth) {
        String actor = auth.getName();
        FailedDeliveryUseCase.RecordContactAttemptCommand command = new FailedDeliveryUseCase.RecordContactAttemptCommand(
                request.channel(), request.contactTimestamp(), request.outcome(), request.notes());

        DeliveryContactAttempt contact = useCase.recordContactAttempt(id, attemptId, command, actor);
        return ResponseEntity.ok(DeliveryContactAttemptResponse.from(contact));
    }

    @PostMapping("/escalate")
    @PreAuthorize("hasAuthority('DELIVERY_FAIL_ESCALATE')")
    public ResponseEntity<DeliveryEscalationResponse> escalateDelivery(
            @PathVariable("id") UUID id,
            @Valid @RequestBody EscalateDeliveryRequest request,
            Authentication auth) {
        String actor = auth.getName();
        FailedDeliveryUseCase.EscalateDeliveryCommand command = new FailedDeliveryUseCase.EscalateDeliveryCommand(
                request.expectedVersion(), request.deliveryAttemptId(), request.reason());

        DeliveryEscalation escalation = useCase.escalateDelivery(id, command, actor);
        return ResponseEntity.ok(DeliveryEscalationResponse.from(escalation));
    }

    @PatchMapping("/escalations/{escalationId}")
    @PreAuthorize("hasAuthority('DELIVERY_FAIL_ESCALATE')")
    public ResponseEntity<DeliveryEscalationResponse> updateEscalation(
            @PathVariable("id") UUID id,
            @PathVariable("escalationId") UUID escalationId,
            @Valid @RequestBody UpdateEscalationRequest request,
            Authentication auth) {
        String actor = auth.getName();
        FailedDeliveryUseCase.UpdateEscalationCommand command = new FailedDeliveryUseCase.UpdateEscalationCommand(
                request.status(), request.resolutionNotes(), request.nextDisposition());

        DeliveryEscalation updated = useCase.updateEscalation(id, escalationId, command, actor);
        return ResponseEntity.ok(DeliveryEscalationResponse.from(updated));
    }

    @PostMapping("/return-to-base")
    @PreAuthorize("hasAuthority('DELIVERY_RETURN_INITIATE')")
    public ResponseEntity<DeliveryOrderResponse> returnToBase(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReturnToBaseRequest request,
            Authentication auth) {
        String actor = auth.getName();
        FailedDeliveryUseCase.ReturnToBaseCommand command = new FailedDeliveryUseCase.ReturnToBaseCommand(
                request.expectedVersion(), request.reason());

        DeliveryOrder order = useCase.initiateReturnToBase(id, command, actor);
        return ResponseEntity.ok(DeliveryOrderResponse.from(order));
    }

    @GetMapping("/attempts")
    @PreAuthorize("hasAuthority('DELIVERY_FAIL_VIEW')")
    public ResponseEntity<DeliveryFailureHistoryResponse> getAttemptsHistory(@PathVariable("id") UUID id) {
        List<DeliveryAttempt> attempts = useCase.getAttemptHistory(id);
        List<DeliveryEscalation> escalations = useCase.getEscalations(id);

        List<DeliveryAttemptResponse> attemptResponses = attempts.stream()
                .map(DeliveryAttemptResponse::from)
                .toList();

        List<DeliveryEscalationResponse> escalationResponses = escalations.stream()
                .map(DeliveryEscalationResponse::from)
                .toList();

        return ResponseEntity.ok(new DeliveryFailureHistoryResponse(
                id, attempts.size(), attemptResponses, escalationResponses));
    }
}
