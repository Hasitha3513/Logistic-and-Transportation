package com.transportlogistics.app.fleet.vehiclemaster.adapters.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import com.transportlogistics.app.fleet.VehiclePowertrainEligibilityQuery.Classification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehiclePowertrainEligibilityAdapterTest {
    @Test void productionClassificationFailsClosed(){
        assertThat(new VehiclePowertrainEligibilityAdapter().classify(UUID.randomUUID(),UUID.randomUUID(),Instant.now()))
                .isEqualTo(Classification.UNKNOWN);
    }
}
