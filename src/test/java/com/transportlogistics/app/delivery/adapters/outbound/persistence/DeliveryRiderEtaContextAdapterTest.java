package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryRiderEtaContextAdapterTest {

    private final DeliveryRiderRepository repository = mock(DeliveryRiderRepository.class);
    private final DeliveryRiderEtaContextAdapter adapter = new DeliveryRiderEtaContextAdapter(repository);

    @Test
    void returnsCanonicalModeOnlyForRequestedTenant() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        DeliveryRider rider = mock(DeliveryRider.class);
        when(rider.getId()).thenReturn(riderId);
        when(rider.getTransportMode()).thenReturn(DeliveryTransportMode.BICYCLE);
        when(repository.findById(riderId, tenant)).thenReturn(Optional.of(rider));
        when(repository.findById(riderId, otherTenant)).thenReturn(Optional.empty());

        assertThat(adapter.findForRider(tenant, riderId)).get()
                .extracting("riderId", "transportMode")
                .containsExactly(riderId, DeliveryTransportMode.BICYCLE);
        assertThat(adapter.findForRider(otherTenant, riderId)).isEmpty();
    }

    @Test
    void exposesLegacyNullAsExplicitUnconfiguredContext() {
        UUID tenant = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        DeliveryRider rider = mock(DeliveryRider.class);
        when(rider.getId()).thenReturn(riderId);
        when(rider.getTransportMode()).thenReturn(null);
        when(repository.findById(riderId, tenant)).thenReturn(Optional.of(rider));

        assertThat(adapter.findForRider(tenant, riderId)).get()
                .extracting("transportMode").isNull();
    }

    @Test
    void resolvesOrderThroughAuthoritativeActiveAssignment() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        DeliveryOrderRiderAssignment assignment = mock(DeliveryOrderRiderAssignment.class);
        DeliveryRider rider = mock(DeliveryRider.class);
        when(assignment.getRiderId()).thenReturn(riderId);
        when(rider.getId()).thenReturn(riderId);
        when(rider.getTransportMode()).thenReturn(DeliveryTransportMode.VAN);
        when(repository.findActiveAssignmentForOrder(orderId, tenant)).thenReturn(Optional.of(assignment));
        when(repository.findById(riderId, tenant)).thenReturn(Optional.of(rider));

        assertThat(adapter.findForOrder(tenant, orderId)).get()
                .extracting("transportMode").isEqualTo(DeliveryTransportMode.VAN);
    }
}
