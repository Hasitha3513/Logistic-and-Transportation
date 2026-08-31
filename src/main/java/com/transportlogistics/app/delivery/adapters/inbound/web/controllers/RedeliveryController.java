package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.RedeliverySuggestionRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.RescheduleRedeliveryRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.ScheduleRedeliveryRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.RedeliveryScheduleResponse;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.RedeliverySuggestionResponse;
import com.transportlogistics.app.delivery.domain.model.DeliveryRedeliverySchedule;
import com.transportlogistics.app.delivery.ports.inbound.RedeliveryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries/{id}/redelivery")
@Validated
public class RedeliveryController {

    private final RedeliveryUseCase redeliveryUseCase;

    public RedeliveryController(RedeliveryUseCase redeliveryUseCase) {
        this.redeliveryUseCase = redeliveryUseCase;
    }

    @PostMapping("/suggestions")
    @PreAuthorize("hasAuthority('DELIVERY_REDELIVERY_VIEW')")
    public ResponseEntity<List<RedeliverySuggestionResponse>> getSuggestions(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) RedeliverySuggestionRequest request
    ) {
        var preferenceInput = request != null
                ? new RedeliveryUseCase.CustomerPreferenceInput(
                        request.preferredStartTime(),
                        request.preferredEndTime(),
                        request.customerPreferenceNotes()
                )
                : new RedeliveryUseCase.CustomerPreferenceInput(null, null, null);

        List<RedeliverySuggestionResponse> response = redeliveryUseCase.getSuggestions(id, preferenceInput)
                .stream()
                .map(RedeliverySuggestionResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAuthority('DELIVERY_REDELIVERY_SCHEDULE')")
    public ResponseEntity<RedeliveryScheduleResponse> scheduleRedelivery(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ScheduleRedeliveryRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        var command = new RedeliveryUseCase.ScheduleRedeliveryCommand(
                request.expectedVersion(),
                request.failedAttemptId(),
                request.schedulingMethod(),
                request.preferredStartTime(),
                request.preferredEndTime(),
                request.customerPreferenceNotes(),
                request.scheduledStartTime(),
                request.scheduledEndTime()
        );

        DeliveryRedeliverySchedule schedule = redeliveryUseCase.scheduleRedelivery(id, command, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(RedeliveryScheduleResponse.fromDomain(schedule));
    }

    @PostMapping("/reschedule")
    @PreAuthorize("hasAuthority('DELIVERY_REDELIVERY_SCHEDULE')")
    public ResponseEntity<RedeliveryScheduleResponse> reschedule(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RescheduleRedeliveryRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        var command = new RedeliveryUseCase.RescheduleRedeliveryCommand(
                request.expectedVersion(),
                request.supersedeReason(),
                request.scheduledStartTime(),
                request.scheduledEndTime()
        );

        DeliveryRedeliverySchedule schedule = redeliveryUseCase.reschedule(id, command, actor);
        return ResponseEntity.ok(RedeliveryScheduleResponse.fromDomain(schedule));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('DELIVERY_REDELIVERY_VIEW')")
    public ResponseEntity<List<RedeliveryScheduleResponse>> getHistory(
            @PathVariable("id") UUID id
    ) {
        List<RedeliveryScheduleResponse> history = redeliveryUseCase.getHistory(id)
                .stream()
                .map(RedeliveryScheduleResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(history);
    }
}
