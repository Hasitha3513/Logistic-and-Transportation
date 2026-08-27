package com.transportlogistics.app.fuel.infrastructure.adapters.out.organization;

import com.transportlogistics.app.organization.VendorLookup;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationFuelVendorAdapterTest {

    @Test
    void resolvesFuelVendorThroughPublicOrganizationContract() {
        var vendorId = UUID.randomUUID();
        var vendors = mock(VendorLookup.class);
        when(vendors.find(vendorId)).thenReturn(Optional.of(
                new VendorLookup.VendorReference(vendorId, "V-001", "Acme Fuel", true)));

        var result = new OrganizationFuelVendorAdapter(vendors).find(vendorId);

        assertThat(result).hasValueSatisfying(vendor -> {
            assertThat(vendor.id()).isEqualTo(vendorId);
            assertThat(vendor.code()).isEqualTo("V-001");
            assertThat(vendor.name()).isEqualTo("Acme Fuel");
            assertThat(vendor.active()).isTrue();
        });
    }

    @Test
    void preservesMissingVendorResult() {
        var vendorId = UUID.randomUUID();
        var vendors = mock(VendorLookup.class);
        when(vendors.find(vendorId)).thenReturn(Optional.empty());

        assertThat(new OrganizationFuelVendorAdapter(vendors).find(vendorId)).isEmpty();
    }
}
