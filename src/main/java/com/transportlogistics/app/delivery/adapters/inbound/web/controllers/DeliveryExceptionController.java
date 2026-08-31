package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CancelDeliveryExceptionRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.InvestigateDeliveryExceptionRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.ReportDeliveryExceptionRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.ResolveDeliveryExceptionRequest;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.response.DeliveryExceptionResponse;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionCase;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionEvidence;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionResolution;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryExceptionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries/{id}/exceptions")
public class DeliveryExceptionController {

    private final DeliveryExceptionUseCase exceptionUseCase;

    public DeliveryExceptionController(DeliveryExceptionUseCase exceptionUseCase) {
        this.exceptionUseCase = exceptionUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DELIVERY_EXCEPTION_CREATE')")
    public ResponseEntity<DeliveryExceptionResponse> reportException(
            @PathVariable("id") UUID deliveryId,
            @Valid @RequestBody ReportDeliveryExceptionRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        List<DeliveryExceptionUseCase.EvidenceUpload> uploads = new ArrayList<>();
        if (request.evidenceList() != null) {
            for (var item : request.evidenceList()) {
                byte[] content = new byte[0];
                if (item.base64Content() != null && !item.base64Content().isBlank()) {
                    try {
                        content = Base64.getDecoder().decode(item.base64Content());
                    } catch (IllegalArgumentException ignored) {
                        content = new byte[0];
                    }
                }
                uploads.add(new DeliveryExceptionUseCase.EvidenceUpload(content, item.originalFilename()));
            }
        }

        var command = new DeliveryExceptionUseCase.ReportCommand(
                request.deliveryAttemptId(),
                request.exceptionType(),
                request.severity(),
                request.description(),
                request.correctedLocationId(),
                request.otpAttemptReference(),
                request.deliveredItemsDescription(),
                request.undeliveredItemsDescription(),
                request.quantityDelivered(),
                request.quantityUndelivered(),
                uploads
        );

        DeliveryExceptionCase created = exceptionUseCase.reportException(deliveryId, command, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DELIVERY_EXCEPTION_VIEW')")
    public ResponseEntity<List<DeliveryExceptionResponse>> listExceptions(@PathVariable("id") UUID deliveryId) {
        List<DeliveryExceptionCase> list = exceptionUseCase.listExceptions(deliveryId);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{exceptionId}")
    @PreAuthorize("hasAuthority('DELIVERY_EXCEPTION_VIEW')")
    public ResponseEntity<DeliveryExceptionResponse> getException(
            @PathVariable("id") UUID deliveryId,
            @PathVariable("exceptionId") UUID exceptionId
    ) {
        DeliveryExceptionCase item = exceptionUseCase.getException(deliveryId, exceptionId);
        return ResponseEntity.ok(toResponse(item));
    }

    @PostMapping("/{exceptionId}/investigate")
    @PreAuthorize("hasAuthority('DELIVERY_EXCEPTION_MANAGE')")
    public ResponseEntity<DeliveryExceptionResponse> investigateException(
            @PathVariable("id") UUID deliveryId,
            @PathVariable("exceptionId") UUID exceptionId,
            @Valid @RequestBody InvestigateDeliveryExceptionRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        DeliveryExceptionCase updated = exceptionUseCase.investigateException(
                deliveryId, exceptionId, request.expectedVersion(), actor);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/{exceptionId}/resolve")
    @PreAuthorize("hasAuthority('DELIVERY_EXCEPTION_RESOLVE')")
    public ResponseEntity<DeliveryExceptionResponse> resolveException(
            @PathVariable("id") UUID deliveryId,
            @PathVariable("exceptionId") UUID exceptionId,
            @Valid @RequestBody ResolveDeliveryExceptionRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        var command = new DeliveryExceptionUseCase.ResolveCommand(
                request.expectedVersion(),
                request.resolutionCode(),
                request.resolutionNotes(),
                request.correctedLocationId(),
                request.followUpDisposition()
        );
        DeliveryExceptionCase resolved = exceptionUseCase.resolveException(deliveryId, exceptionId, command, actor);
        return ResponseEntity.ok(toResponse(resolved));
    }

    @PostMapping("/{exceptionId}/cancel")
    @PreAuthorize("hasAuthority('DELIVERY_EXCEPTION_RESOLVE')")
    public ResponseEntity<DeliveryExceptionResponse> cancelException(
            @PathVariable("id") UUID deliveryId,
            @PathVariable("exceptionId") UUID exceptionId,
            @Valid @RequestBody CancelDeliveryExceptionRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        var command = new DeliveryExceptionUseCase.CancelCommand(request.expectedVersion(), request.reason());
        DeliveryExceptionCase cancelled = exceptionUseCase.cancelException(deliveryId, exceptionId, command, actor);
        return ResponseEntity.ok(toResponse(cancelled));
    }

    private DeliveryExceptionResponse toResponse(DeliveryExceptionCase item) {
        if (item == null) return null;
        DeliveryExceptionResponse.ResolutionInfo resInfo = null;
        if (item.resolution() != null) {
            DeliveryExceptionResolution r = item.resolution();
            resInfo = new DeliveryExceptionResponse.ResolutionInfo(
                    r.resolutionCode(),
                    r.resolutionNotes(),
                    r.followUpDisposition(),
                    r.resolvedAt(),
                    r.resolvedBy()
            );
        }

        List<DeliveryExceptionResponse.EvidenceInfo> evInfo = new ArrayList<>();
        if (item.evidence() != null) {
            for (DeliveryExceptionEvidence ev : item.evidence()) {
                evInfo.add(new DeliveryExceptionResponse.EvidenceInfo(
                        ev.id(),
                        ev.storageReference(),
                        ev.detectedContentType(),
                        ev.contentLength(),
                        ev.sha256Checksum(),
                        ev.originalFilename(),
                        ev.createdBy(),
                        ev.createdAt()
                ));
            }
        }

        return new DeliveryExceptionResponse(
                item.id(),
                item.deliveryOrderId().value(),
                item.deliveryAttemptId(),
                item.exceptionType(),
                item.severity(),
                item.status(),
                item.description(),
                item.correctedLocationId(),
                item.otpAttemptReference(),
                item.deliveredItemsDescription(),
                item.undeliveredItemsDescription(),
                item.quantityDelivered(),
                item.quantityUndelivered(),
                resInfo,
                item.version(),
                item.reportedAt(),
                item.reportedBy(),
                item.resolvedAt(),
                item.resolvedBy(),
                evInfo
        );
    }
}
