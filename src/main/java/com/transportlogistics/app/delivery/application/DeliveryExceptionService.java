package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryExceptionUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeliveryExceptionService implements DeliveryExceptionUseCase {
    private static final long PHOTO_LIMIT = 10L * 1024 * 1024; // 10 MiB
    private final DeliveryOrderRepository orders;
    private final DeliveryAttemptRepository attempts;
    private final DeliveryExceptionRepository exceptions;
    private final DeliveryEvidenceStoragePort storage;
    private final DeliveryLocationLookupPort locations;
    private final DeliveryTenantContextPort tenantContext;
    private final DeliveryOrderTransaction transactions;
    private final Clock clock;

    public DeliveryExceptionService(
            DeliveryOrderRepository orders,
            DeliveryAttemptRepository attempts,
            DeliveryExceptionRepository exceptions,
            DeliveryEvidenceStoragePort storage,
            DeliveryLocationLookupPort locations,
            DeliveryTenantContextPort tenantContext,
            DeliveryOrderTransaction transactions,
            Clock clock
    ) {
        this.orders = orders;
        this.attempts = attempts;
        this.exceptions = exceptions;
        this.storage = storage;
        this.locations = locations;
        this.tenantContext = tenantContext;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public DeliveryExceptionCase reportException(UUID deliveryOrderId, ReportCommand command, String actor) {
        return transactions.execute(() -> {
            UUID tenantId = requiredTenant();
            DeliveryOrder delivery = delivery(deliveryOrderId);
            OffsetDateTime now = now();

            if (exceptions.existsActiveByDeliveryOrderIdAndType(delivery.id(), command.exceptionType())) {
                throw new ConflictException("DELIVERY_EXCEPTION_DUPLICATE_ACTIVE",
                        "An active exception of type " + command.exceptionType() + " already exists for this delivery");
            }

            if (command.deliveryAttemptId() != null) {
                attempts.findById(command.deliveryAttemptId())
                        .orElseThrow(() -> new NotFoundException("DELIVERY_ATTEMPT_NOT_FOUND", "Delivery attempt was not found"));
            }

            if (command.correctedLocationId() != null) {
                var loc = locations.findLocation(command.correctedLocationId());
                if (loc.isEmpty() || !loc.get().active()) {
                    throw new NotFoundException("LOCATION_NOT_FOUND", "Corrected destination location was not found or is inactive");
                }
            }

            UUID caseId = UUID.randomUUID();
            List<DeliveryExceptionEvidence> evidenceList = new ArrayList<>();

            if (command.evidenceList() != null && !command.evidenceList().isEmpty()) {
                for (var upload : command.evidenceList()) {
                    byte[] content = upload.content() == null ? new byte[0] : upload.content();
                    if (content.length == 0 || content.length > PHOTO_LIMIT) {
                        throw new BusinessRuleException("EXCEPTION_FILE_TOO_LARGE", "Evidence file is empty or exceeds 10 MiB limit");
                    }
                    UUID evidenceId = UUID.randomUUID();
                    var stored = storage.store(tenantId, evidenceId, content, upload.originalFilename());
                    if (!"image/png".equals(stored.detectedContentType()) && !"image/jpeg".equals(stored.detectedContentType())) {
                        storage.delete(tenantId, stored.storageReference());
                        throw new BusinessRuleException("INVALID_EVIDENCE_TYPE", "Only decoded PNG and JPEG evidence is accepted");
                    }
                    evidenceList.add(new DeliveryExceptionEvidence(
                            evidenceId,
                            caseId,
                            stored.storageReference(),
                            stored.detectedContentType(),
                            stored.contentLength(),
                            stored.checksum(),
                            upload.originalFilename(),
                            actor,
                            now
                    ));
                }
            }

            DeliveryExceptionCase newCase = DeliveryExceptionCase.create(
                    caseId,
                    delivery.id(),
                    command.deliveryAttemptId(),
                    command.exceptionType(),
                    command.severity(),
                    command.description(),
                    command.correctedLocationId(),
                    command.otpAttemptReference(),
                    command.deliveredItemsDescription(),
                    command.undeliveredItemsDescription(),
                    command.quantityDelivered(),
                    command.quantityUndelivered(),
                    evidenceList,
                    actor,
                    now
            );

            return exceptions.save(newCase);
        });
    }

    @Override
    public DeliveryExceptionCase investigateException(UUID deliveryOrderId, UUID exceptionId, long expectedVersion, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            delivery(deliveryOrderId);
            DeliveryExceptionCase exceptionCase = exceptionCase(exceptionId, deliveryOrderId);
            requireVersion(expectedVersion, exceptionCase.version());

            DeliveryExceptionCase updated = exceptionCase.investigate(actor);
            return exceptions.save(updated);
        });
    }

    @Override
    public DeliveryExceptionCase resolveException(UUID deliveryOrderId, UUID exceptionId, ResolveCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            DeliveryOrder delivery = delivery(deliveryOrderId);
            DeliveryExceptionCase exceptionCase = exceptionCase(exceptionId, deliveryOrderId);
            requireVersion(command.expectedVersion(), exceptionCase.version());
            OffsetDateTime now = now();

            if (command.correctedLocationId() != null) {
                var loc = locations.findLocation(command.correctedLocationId());
                if (loc.isEmpty() || !loc.get().active()) {
                    throw new NotFoundException("LOCATION_NOT_FOUND", "Corrected destination location was not found or is inactive");
                }
            }

            var resolution = new DeliveryExceptionResolution(
                    command.resolutionCode(),
                    command.resolutionNotes(),
                    command.followUpDisposition(),
                    now,
                    actor
            );

            DeliveryExceptionCase resolvedCase = exceptionCase.resolve(
                    resolution,
                    command.correctedLocationId(),
                    actor,
                    now
            );

            DeliveryExceptionCase savedCase = exceptions.save(resolvedCase);

            // Align DeliveryOrder operational state if resolution disposition demands RTO
            if (command.resolutionCode() == DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED
                    || command.followUpDisposition() == DeliveryFailureDisposition.RETURN_TO_BASE_REQUIRED) {
                if (delivery.status() != DeliveryStatus.RETURN_TO_BASE && delivery.status() != DeliveryStatus.DELIVERED) {
                    orders.save(delivery.initiateReturnToBase(now, actor));
                }
            }

            return savedCase;
        });
    }

    @Override
    public DeliveryExceptionCase cancelException(UUID deliveryOrderId, UUID exceptionId, CancelCommand command, String actor) {
        return transactions.execute(() -> {
            requiredTenant();
            delivery(deliveryOrderId);
            DeliveryExceptionCase exceptionCase = exceptionCase(exceptionId, deliveryOrderId);
            requireVersion(command.expectedVersion(), exceptionCase.version());
            OffsetDateTime now = now();

            DeliveryExceptionCase cancelledCase = exceptionCase.cancel(command.reason(), actor, now);
            return exceptions.save(cancelledCase);
        });
    }

    @Override
    public List<DeliveryExceptionCase> listExceptions(UUID deliveryOrderId) {
        requiredTenant();
        delivery(deliveryOrderId);
        return exceptions.findByDeliveryOrderId(new DeliveryId(deliveryOrderId));
    }

    @Override
    public DeliveryExceptionCase getException(UUID deliveryOrderId, UUID exceptionId) {
        requiredTenant();
        delivery(deliveryOrderId);
        return exceptionCase(exceptionId, deliveryOrderId);
    }

    private UUID requiredTenant() {
        return tenantContext.currentTenant()
                .map(DeliveryTenantContextPort.TenantContext::tenantId)
                .orElseThrow(() -> new BusinessRuleException("TENANT_REQUIRED", "Active tenant context is required"));
    }

    private DeliveryOrder delivery(UUID deliveryId) {
        return orders.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_ORDER_NOT_FOUND", "Delivery order was not found"));
    }

    private DeliveryExceptionCase exceptionCase(UUID exceptionId, UUID deliveryOrderId) {
        DeliveryExceptionCase found = exceptions.findById(exceptionId)
                .orElseThrow(() -> new NotFoundException("DELIVERY_EXCEPTION_NOT_FOUND", "Delivery exception case was not found"));
        if (!found.deliveryOrderId().value().equals(deliveryOrderId)) {
            throw new NotFoundException("DELIVERY_EXCEPTION_NOT_FOUND", "Delivery exception case does not belong to this delivery");
        }
        return found;
    }

    private void requireVersion(long expected, long actual) {
        if (expected != actual) {
            throw new ConflictException("DELIVERY_EXCEPTION_VERSION_CONFLICT",
                    "Expected version " + expected + " does not match current version " + actual);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
