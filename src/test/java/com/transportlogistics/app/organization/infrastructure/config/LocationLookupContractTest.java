package com.transportlogistics.app.organization.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import com.transportlogistics.app.organization.application.ports.out.LocationRepository;
import com.transportlogistics.app.organization.application.service.LocationService;
import com.transportlogistics.app.organization.domain.model.Location;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocationLookupContractTest {
    @Test
    void backgroundLookupPassesExplicitTenantToOrganizationRepository() {
        UUID tenantId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        RecordingRepository repository = new RecordingRepository(tenantId,
                new Location(locationId, "DEPOT", "Depot", "Address", 6.9, 79.8, true));

        var lookup = new LocationConfig().locationLookup(new LocationService(repository), repository);

        assertThat(lookup.find(tenantId, locationId)).get()
                .extracting(value -> value.id(), value -> value.active())
                .containsExactly(locationId, true);
        assertThat(repository.requestedTenant).isEqualTo(tenantId);
    }

    @Test
    void crossTenantLookupCannotFallBackToUnscopedIdentity() {
        UUID firstTenant = UUID.randomUUID();
        UUID secondTenant = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        RecordingRepository repository = new RecordingRepository(firstTenant,
                new Location(locationId, "DEPOT", "Depot", null, null, null, true));

        var lookup = new LocationConfig().locationLookup(new LocationService(repository), repository);

        assertThat(lookup.find(secondTenant, locationId)).isEmpty();
        assertThat(repository.requestedTenant).isEqualTo(secondTenant);
    }

    private static final class RecordingRepository implements LocationRepository {
        private final UUID allowedTenant;
        private final Location location;
        private UUID requestedTenant;

        private RecordingRepository(UUID allowedTenant, Location location) {
            this.allowedTenant = allowedTenant;
            this.location = location;
        }

        @Override
        public Location save(Location value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Location> findById(UUID id) {
            throw new AssertionError("Unscoped lookup must not be used");
        }

        @Override
        public Optional<Location> findById(UUID tenantId, UUID id) {
            requestedTenant = tenantId;
            return allowedTenant.equals(tenantId) && location.id().equals(id)
                    ? Optional.of(location) : Optional.empty();
        }

        @Override
        public List<Location> findAll() {
            throw new UnsupportedOperationException();
        }
    }
}
