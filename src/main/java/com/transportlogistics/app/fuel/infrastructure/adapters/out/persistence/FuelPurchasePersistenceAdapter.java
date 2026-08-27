package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseRepository;
import com.transportlogistics.app.fuel.domain.model.FuelPurchase;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Component
class FuelPurchasePersistenceAdapter implements FuelPurchaseRepository {
    private final FuelPurchaseJpaRepository repository;
    FuelPurchasePersistenceAdapter(FuelPurchaseJpaRepository repository) { this.repository = repository; }
    @Override public FuelPurchase save(FuelPurchase purchase) { return map(repository.save(entity(purchase))); }
    @Override public Optional<FuelPurchase> findById(UUID id) { return repository.findById(id).map(this::map); }
    @Override public Optional<FuelPurchase> findByIdForUpdate(UUID id) { return repository.findByIdForUpdate(id).map(this::map); }

    @Override
    public FuelPurchaseUseCase.PageResult<FuelPurchase> search(FuelPurchaseUseCase.SearchQuery request) {
        var spec = (org.springframework.data.jpa.domain.Specification<FuelPurchaseEntity>) (root, query, cb) -> {
            var p = new ArrayList<Predicate>();
            if (request.vendorId() != null) p.add(cb.equal(root.get("vendorId"), request.vendorId()));
            if (request.fuelType() != null && !request.fuelType().isBlank()) p.add(cb.equal(root.get("fuelType"), request.fuelType().trim().toUpperCase()));
            if (request.status() != null) p.add(cb.equal(root.get("status"), request.status()));
            if (request.reconciliationStatus() != null) p.add(cb.equal(root.get("reconciliationStatus"), request.reconciliationStatus()));
            if (request.fromDate() != null) p.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), request.fromDate()));
            if (request.toDate() != null) p.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), request.toDate()));
            if (hasText(request.purchaseNumber())) p.add(like(cb, root.get("purchaseNumber"), request.purchaseNumber()));
            if (hasText(request.invoiceNumber())) p.add(like(cb, root.get("invoiceNumber"), request.invoiceNumber()));
            if (hasText(request.search())) p.add(cb.or(like(cb, root.get("purchaseNumber"), request.search()), like(cb, root.get("invoiceNumber"), request.search())));
            return cb.and(p.toArray(Predicate[]::new));
        };
        var page = repository.findAll(spec, PageRequest.of(request.page(), request.limit(), Sort.by(Sort.Direction.DESC, "purchaseDate").and(Sort.by(Sort.Direction.DESC, "createdAt"))));
        return new FuelPurchaseUseCase.PageResult<>(page.stream().map(this::map).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override public boolean existsByPurchaseNumber(String number) { return repository.existsByPurchaseNumber(number); }
    @Override public boolean existsByVendorAndInvoice(UUID vendorId, String invoice, UUID excludingId) {
        return repository.existsByVendorIdAndInvoiceNumberIgnoreCaseAndIdNot(vendorId, invoice, excludingId == null ? new UUID(0, 0) : excludingId);
    }

    private Predicate like(jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Path<String> path, String value) {
        return cb.like(cb.lower(path), "%" + value.trim().toLowerCase() + "%");
    }
    private boolean hasText(String v) { return v != null && !v.isBlank(); }

    private FuelPurchaseEntity entity(FuelPurchase p) {
        var e = new FuelPurchaseEntity(); e.setId(p.id()); e.setPurchaseNumber(p.purchaseNumber()); e.setVendorId(p.vendorId());
        e.setFuelStationId(p.fuelStationId()); e.setFuelType(p.fuelType()); e.setPurchaseDate(p.purchaseDate()); e.setInvoiceNumber(p.invoiceNumber());
        e.setInvoiceDate(p.invoiceDate()); e.setQuantity(p.quantity()); e.setUnitPrice(p.unitPrice()); e.setSubtotal(p.subtotal()); e.setTaxRate(p.taxRate());
        e.setTaxAmount(p.taxAmount()); e.setOtherCharges(p.otherCharges()); e.setTotalAmount(p.totalAmount()); e.setCurrencyCode(p.currencyCode());
        e.setStatus(p.status()); e.setReconciliationStatus(p.reconciliationStatus()); e.setReceivedQuantity(p.receivedQuantity());
        e.setQuantityVariance(p.quantityVariance()); e.setExpectedUnitPrice(p.expectedUnitPrice()); e.setPriceVariance(p.priceVariance());
        e.setDestinationFuelStationId(p.destinationFuelStationId()); e.setDeliveryNoteNumber(p.deliveryNoteNumber()); e.setReceivedAt(p.receivedAt());
        e.setApprovedBy(p.approvedBy()); e.setApprovedAt(p.approvedAt()); e.setReconciledBy(p.reconciledBy()); e.setReconciledAt(p.reconciledAt());
        e.setReconciliationNotes(p.reconciliationNotes()); e.setReconciliationReference(p.reconciliationReference()); e.setNotes(p.notes()); e.setCreatedBy(p.createdBy());
        e.setCreatedAt(p.createdAt()); e.setUpdatedAt(p.updatedAt()); return e;
    }

    private FuelPurchase map(FuelPurchaseEntity e) {
        return new FuelPurchase(e.getId(), e.getPurchaseNumber(), e.getVendorId(), e.getFuelStationId(), e.getFuelType(), e.getPurchaseDate(),
                e.getInvoiceNumber(), e.getInvoiceDate(), e.getQuantity(), e.getUnitPrice(), e.getSubtotal(), e.getTaxRate(), e.getTaxAmount(),
                e.getOtherCharges(), e.getTotalAmount(), e.getCurrencyCode(), e.getStatus(), e.getReconciliationStatus(), e.getReceivedQuantity(),
                e.getQuantityVariance(), e.getExpectedUnitPrice(), e.getPriceVariance(), e.getDestinationFuelStationId(), e.getDeliveryNoteNumber(),
                e.getReceivedAt(), e.getApprovedBy(), e.getApprovedAt(), e.getReconciledBy(), e.getReconciledAt(), e.getReconciliationNotes(),
                e.getReconciliationReference(), e.getNotes(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
