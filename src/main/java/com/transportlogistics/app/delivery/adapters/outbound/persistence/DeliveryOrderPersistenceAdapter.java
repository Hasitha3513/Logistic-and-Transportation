package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryOrderUseCase;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Component
class DeliveryOrderPersistenceAdapter implements DeliveryOrderRepository {
    private final DeliveryOrderJpaRepository repository;
    private final DeliveryOrderPersistenceMapper mapper;
    DeliveryOrderPersistenceAdapter(DeliveryOrderJpaRepository repository, DeliveryOrderPersistenceMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }
    @Override public DeliveryOrder save(DeliveryOrder order) { return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(order))); }
    @Override public Optional<DeliveryOrder> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    @Override public Optional<DeliveryOrder> findByDeliveryNumber(String value) { return repository.findByDeliveryNumber(value).map(mapper::toDomain); }
    @Override public DeliveryOrderUseCase.PageResult<DeliveryOrder> search(DeliveryOrderUseCase.SearchQuery request) {
        var spec = (org.springframework.data.jpa.domain.Specification<DeliveryOrderEntity>) (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (request.status() != null) predicates.add(cb.equal(root.get("status"), request.status().name()));
            if (request.customerId() != null) predicates.add(cb.equal(root.get("customerId"), request.customerId()));
            if (request.windowFrom() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("windowStart"), request.windowFrom()));
            if (request.windowTo() != null) predicates.add(cb.lessThanOrEqualTo(root.get("windowEnd"), request.windowTo()));
            if (request.search() != null && !request.search().isBlank()) {
                String value = "%" + request.search().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("deliveryNumber")), value));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        int page = Math.max(request.page(), 0), size = Math.min(Math.max(request.size(), 1), 100);
        var result = repository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new DeliveryOrderUseCase.PageResult<>(result.stream().map(mapper::toDomain).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
