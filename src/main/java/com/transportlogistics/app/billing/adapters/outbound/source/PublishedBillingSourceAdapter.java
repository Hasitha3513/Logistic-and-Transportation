package com.transportlogistics.app.billing.adapters.outbound.source;

import com.transportlogistics.app.billing.domain.TransportBillingRecord.Source.SourceType;
import com.transportlogistics.app.billing.ports.outbound.BillingSourcePort;
import com.transportlogistics.app.freight.FreightBillingSourceLookup;
import com.transportlogistics.app.organization.CustomerLookup;
import com.transportlogistics.app.trip.TripBillingSourceLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PublishedBillingSourceAdapter implements BillingSourcePort {
    private final TripBillingSourceLookup trips;
    private final FreightBillingSourceLookup freight;
    private final CustomerLookup customers;
    PublishedBillingSourceAdapter(TripBillingSourceLookup trips, FreightBillingSourceLookup freight,
                                  CustomerLookup customers) { this.trips=trips; this.freight=freight; this.customers=customers; }

    @Override public Optional<SourceFact> find(UUID tenantId, SourceType type, UUID id) {
        Optional<SourceFact> fact = switch (type) {
            case TRIP -> trips.findClosed(tenantId,id).map(x -> new SourceFact(type,x.sourceId(),x.businessNumber(),
                x.lifecycle(),x.completedAt(),x.customerId(),x.sourceVersion()));
            case FREIGHT_ORDER -> freight.findTerminal(tenantId,id).map(x -> new SourceFact(type,x.sourceId(),
                x.businessNumber(),x.lifecycle(),x.completedAt(),x.customerId(),x.sourceVersion()));
        };
        return fact;
    }

    @Override public boolean customerActive(UUID tenantId, UUID customerId) {
        return customers.find(customerId).filter(CustomerLookup.CustomerReference::active).isPresent();
    }
}
