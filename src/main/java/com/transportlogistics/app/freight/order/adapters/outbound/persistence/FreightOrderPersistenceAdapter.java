package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.freight.order.ports.outbound.FreightOrderRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
class FreightOrderPersistenceAdapter implements FreightOrderRepository {
    private static final Set<String> SORTABLE = Set.of("orderNumber", "requestedPickupAt", "requestedDeliveryAt", "priority", "createdAt", "updatedAt");
    private final FreightOrderJpaRepository repository;
    private final FreightOrderPersistenceMapper mapper;

    FreightOrderPersistenceAdapter(FreightOrderJpaRepository repository, FreightOrderPersistenceMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }

    @Override public FreightOrder save(FreightOrder order) { return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(order))); }
    @Override public Optional<FreightOrder> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }

    @Override
    public FreightOrderUseCase.PageResult<FreightOrder> search(FreightOrderUseCase.SearchQuery request) {
        var spec = (org.springframework.data.jpa.domain.Specification<FreightOrderEntity>) (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (request.customerId() != null) predicates.add(cb.equal(root.get("customerId"), request.customerId()));
            if (request.pickupFrom() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("requestedPickupAt"), request.pickupFrom()));
            if (request.pickupTo() != null) predicates.add(cb.lessThanOrEqualTo(root.get("requestedPickupAt"), request.pickupTo()));
            if (request.search() != null && !request.search().isBlank()) {
                String value = "%" + request.search().trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("orderNumber")), value),
                        cb.like(cb.lower(root.get("serviceLevel")), value), cb.like(cb.lower(root.get("priority")), value)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        String property = SORTABLE.contains(request.sort()) ? request.sort() : "requestedPickupAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(request.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int pageNumber = Math.max(request.page(), 0); int size = Math.min(Math.max(request.limit(), 1), 100);
        var page = repository.findAll(spec, PageRequest.of(pageNumber, size, Sort.by(direction, property)));
        return new FreightOrderUseCase.PageResult<>(page.stream().map(mapper::toDomain).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
