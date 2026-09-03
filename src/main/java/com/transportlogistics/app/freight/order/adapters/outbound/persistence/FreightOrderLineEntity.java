package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "freight_order_line")
@Getter @Setter @NoArgsConstructor
class FreightOrderLineEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "freight_order_id", nullable = false)
    FreightOrderEntity order;
    @Column(nullable = false, length = 500) String description;
    @Column(nullable = false, precision = 19, scale = 4) BigDecimal quantity;
    @Column(name = "line_order", nullable = false) int lineOrder;
}
