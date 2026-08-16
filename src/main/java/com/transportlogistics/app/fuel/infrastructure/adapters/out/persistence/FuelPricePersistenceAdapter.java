package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class FuelPricePersistenceAdapter implements FuelPriceRepository {
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);
    private final FuelPriceJpaRepository repository;
    FuelPricePersistenceAdapter(FuelPriceJpaRepository repository) { this.repository = repository; }

    @Override public FuelPrice save(FuelPrice price) { return map(repository.save(entity(price))); }
    @Override public Optional<FuelPrice> findById(UUID id) { return repository.findById(id).map(this::map); }

    @Override
    public List<FuelPrice> find(UUID vendorId, String fuelType, Boolean active, LocalDate effectiveOn) {
        var specification = (org.springframework.data.jpa.domain.Specification<FuelPriceEntity>) (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (vendorId != null) predicates.add(cb.equal(root.get("vendorId"), vendorId));
            if (fuelType != null) predicates.add(cb.equal(root.get("fuelType"), fuelType));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            if (effectiveOn != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effectiveFrom"), effectiveOn));
                predicates.add(cb.or(cb.isNull(root.get("effectiveTo")), cb.greaterThanOrEqualTo(root.get("effectiveTo"), effectiveOn)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(specification, Sort.by(Sort.Direction.DESC, "effectiveFrom")).stream().map(this::map).toList();
    }

    @Override
    public boolean hasOverlappingActivePrice(UUID vendorId, String fuelType, LocalDate from, LocalDate to, UUID excludingId) {
        return repository.hasOverlap(vendorId, fuelType, from, to == null ? MAX_DATE : to,
                excludingId == null ? new UUID(0, 0) : excludingId);
    }

    @Override
    public Optional<FuelPrice> findEffective(UUID vendorId, String fuelType, LocalDate date) {
        if (vendorId == null || fuelType == null || date == null) return Optional.empty();
        var specification = (org.springframework.data.jpa.domain.Specification<FuelPriceEntity>) (root, query, cb) -> cb.and(
                cb.equal(root.get("vendorId"), vendorId), cb.equal(root.get("fuelType"), fuelType), cb.isTrue(root.get("active")),
                cb.lessThanOrEqualTo(root.get("effectiveFrom"), date),
                cb.or(cb.isNull(root.get("effectiveTo")), cb.greaterThanOrEqualTo(root.get("effectiveTo"), date)));
        return repository.findAll(specification, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "effectiveFrom")))
                .stream().findFirst().map(this::map);
    }

    private FuelPriceEntity entity(FuelPrice price) {
        var e = new FuelPriceEntity(); e.setId(price.id()); e.setVendorId(price.vendorId()); e.setFuelType(price.fuelType());
        e.setEffectiveFrom(price.effectiveFrom()); e.setEffectiveTo(price.effectiveTo()); e.setUnitPrice(price.unitPrice());
        e.setCurrencyCode(price.currencyCode()); e.setActive(price.active()); e.setCreatedAt(price.createdAt()); e.setUpdatedAt(price.updatedAt()); return e;
    }
    private FuelPrice map(FuelPriceEntity e) { return new FuelPrice(e.getId(), e.getVendorId(), e.getFuelType(), e.getEffectiveFrom(), e.getEffectiveTo(), e.getUnitPrice(), e.getCurrencyCode(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt()); }
}
