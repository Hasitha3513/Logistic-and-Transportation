package com.transportlogistics.app.freight.loadplanning.adapters.outbound.manifest;

import com.transportlogistics.app.freight.loadplanning.ports.inbound.CargoManifestLookupPort;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.CargoManifestLookupPort.ManifestPlanningView;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifest;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifestItem;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManifestLoadPlanLookupAdapterTest {

    @Mock
    private CargoManifestUseCase cargoManifestUseCase;

    private ManifestLoadPlanLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ManifestLoadPlanLookupAdapter(cargoManifestUseCase);
    }

    @Test
    @DisplayName("Lookup adapter maps structured fragile and temperatureSensitive flags without loss or default conversion")
    void shouldMapStructuredFlagsWithoutDefaultConversion() {
        UUID manifestId = UUID.randomUUID();
        UUID item1Id = UUID.randomUUID();
        UUID item2Id = UUID.randomUUID();
        UUID item3Id = UUID.randomUUID();

        CargoManifestItem item1 = new CargoManifestItem(
                item1Id, UUID.randomUUID(), "Fragile Glassware", BigDecimal.ONE,
                "Boxes", "GEN", false, null,
                false, null, null,
                true, false
        );

        CargoManifestItem item2 = new CargoManifestItem(
                item2Id, UUID.randomUUID(), "Frozen Pharma", BigDecimal.TEN,
                "Coolers", "PHARMA", false, null,
                false, null, null,
                false, true
        );

        CargoManifestItem item3 = new CargoManifestItem(
                item3Id, UUID.randomUUID(), "Historical Cargo", BigDecimal.valueOf(5),
                "Crates", "GEN", false, null,
                true, "HAZ-3", null,
                null, null
        );

        CargoManifest manifest = new CargoManifest(
                manifestId, "CM-2026-000001", UUID.randomUUID(), "FO-2026-000001",
                List.of(item1, item2, item3), 1L,
                OffsetDateTime.now(), OffsetDateTime.now(), "planner", "planner",
                OffsetDateTime.now(), "planner"
        );

        when(cargoManifestUseCase.get(manifestId)).thenReturn(manifest);

        Optional<ManifestPlanningView> resultOpt = adapter.findManifest(manifestId);
        assertThat(resultOpt).isPresent();

        ManifestPlanningView view = resultOpt.get();
        assertThat(view.manifestId()).isEqualTo(manifestId);
        assertThat(view.manifestNumber()).isEqualTo("CM-2026-000001");
        assertThat(view.finalized()).isTrue();
        assertThat(view.items()).hasSize(3);

        // Item 1: fragile=true, temperatureSensitive=false
        var viewItem1 = view.items().get(0);
        assertThat(viewItem1.itemId()).isEqualTo(item1Id);
        assertThat(viewItem1.fragile()).isTrue();
        assertThat(viewItem1.temperatureSensitive()).isFalse();

        // Item 2: fragile=false, temperatureSensitive=true
        var viewItem2 = view.items().get(1);
        assertThat(viewItem2.itemId()).isEqualTo(item2Id);
        assertThat(viewItem2.fragile()).isFalse();
        assertThat(viewItem2.temperatureSensitive()).isTrue();

        // Item 3: fragile=null, temperatureSensitive=null (UNKNOWN)
        var viewItem3 = view.items().get(2);
        assertThat(viewItem3.itemId()).isEqualTo(item3Id);
        assertThat(viewItem3.fragile()).isNull();
        assertThat(viewItem3.temperatureSensitive()).isNull();
        assertThat(viewItem3.hazardous()).isTrue();
        assertThat(viewItem3.hazardousClassification()).isEqualTo("HAZ-3");
    }

    @Test
    @DisplayName("Lookup adapter returns Optional.empty() when manifest is not found")
    void shouldReturnEmptyWhenNotFound() {
        UUID manifestId = UUID.randomUUID();
        when(cargoManifestUseCase.get(manifestId)).thenThrow(new NotFoundException("NOT_FOUND", "Manifest not found"));

        Optional<ManifestPlanningView> result = adapter.findManifest(manifestId);
        assertThat(result).isEmpty();
    }
}
