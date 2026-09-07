package com.transportlogistics.app.freight.order.adapters.outbound.persistence;

import com.transportlogistics.app.freight.FreightBillingSourceLookup;
import com.transportlogistics.app.freight.FreightBillingFactPublisher;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class FreightBillingSourceAdapter implements FreightBillingSourceLookup, FreightBillingFactPublisher {
    private final JdbcTemplate jdbc;
    FreightBillingSourceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<FreightBillingFact> findTerminal(UUID tenantId, UUID freightOrderId) {
        return jdbc.query("""
            select f.freight_order_id,o.order_number,f.lifecycle,f.completed_at,o.customer_id,f.source_version
              from freight_billing_fact f join freight_order o
                on o.id=f.freight_order_id and o.tenant_id=f.tenant_id
             where f.tenant_id=? and f.freight_order_id=? and f.lifecycle in ('COMPLETED','CLOSED')
            """, rs -> rs.next() ? Optional.of(new FreightBillingFact(
                rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getObject(4, java.time.OffsetDateTime.class), rs.getObject(5, UUID.class), rs.getLong(6)))
                : Optional.empty(), tenantId, freightOrderId);
    }

    @Override public void publishTerminal(UUID tenantId, UUID freightOrderId, String lifecycle,
                                          java.time.OffsetDateTime completedAt, long sourceVersion) {
        if (!java.util.Set.of("COMPLETED", "CLOSED").contains(lifecycle) || completedAt == null || sourceVersion < 0) {
            throw new BusinessRuleException("BILLING_SOURCE_NOT_ELIGIBLE", "Freight terminal billing fact is invalid");
        }
        int changed = jdbc.update("""
            insert into freight_billing_fact(freight_order_id,tenant_id,lifecycle,completed_at,source_version)
            select id,tenant_id,?,?,? from freight_order where id=? and tenant_id=?
            on conflict(freight_order_id) do update set lifecycle=excluded.lifecycle,
              completed_at=excluded.completed_at,source_version=excluded.source_version
            where freight_billing_fact.tenant_id=excluded.tenant_id
              and freight_billing_fact.source_version<=excluded.source_version
            """, lifecycle, completedAt, sourceVersion, freightOrderId, tenantId);
        if (changed != 1) throw new BusinessRuleException("BILLING_SOURCE_NOT_ELIGIBLE", "Freight order is unavailable");
    }
}
