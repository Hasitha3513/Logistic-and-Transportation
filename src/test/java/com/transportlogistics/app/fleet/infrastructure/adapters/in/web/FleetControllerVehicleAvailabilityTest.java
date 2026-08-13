package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.application.ports.in.*;
import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static com.transportlogistics.app.fleet.domain.model.VehicleAvailability.Code.OVERLAPPING_ALLOCATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FleetControllerVehicleAvailabilityTest {
    @Test
    void returnsStructuredAvailabilityReasons() throws Exception {
        var availability = mock(VehicleAvailabilityUseCase.class);
        var result = new VehicleAvailability(false, List.of(
                new VehicleAvailability.Reason(OVERLAPPING_ALLOCATION, "Vehicle has an overlapping trip allocation")));
        when(availability.evaluate(any())).thenReturn(result);
        var controller = new FleetController(mock(DriverUseCase.class), mock(DriverLicenseUseCase.class),
                mock(VehicleUseCase.class), availability, mock(VehicleCategoryUseCase.class),
                mock(VehicleTypeUseCase.class), mock(VehicleDocumentUseCase.class));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/vehicles/{id}/availability", UUID.randomUUID())
                        .param("from", "2026-02-01T08:00:00Z").param("to", "2026-02-01T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.reasons[0].code").value("OVERLAPPING_ALLOCATION"))
                .andExpect(jsonPath("$.reasons[0].message").isNotEmpty());
    }
}
