package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.application.ports.in.*;
import com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase;
import com.transportlogistics.app.fleet.domain.model.DriverAvailability;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers.FleetController;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.FleetWebMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static com.transportlogistics.app.fleet.domain.model.DriverAvailability.Code.OVERLAPPING_ASSIGNMENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FleetControllerDriverAvailabilityTest {
    @Test
    void returnsStructuredDriverAvailabilityReasons() throws Exception {
        var availability = mock(DriverAvailabilityUseCase.class);
        when(availability.evaluate(any())).thenReturn(new DriverAvailability(false, List.of(
                new DriverAvailability.Reason(OVERLAPPING_ASSIGNMENT,
                        "Driver has an overlapping trip assignment"))));
        var mapper = Mappers.getMapper(FleetWebMapper.class);
        var controller = new FleetController(mock(DriverUseCase.class), availability,
                mock(DriverLicenseUseCase.class), mock(VehicleUseCase.class), mock(VehicleAvailabilityUseCase.class),
                mock(VehicleCategoryUseCase.class), mock(VehicleTypeUseCase.class),
                mock(VehicleDocumentUseCase.class), mapper);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/drivers/{id}/availability", UUID.randomUUID())
                        .param("from", "2026-02-01T08:00:00Z").param("to", "2026-02-01T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.reasons[0].code").value("OVERLAPPING_ASSIGNMENT"))
                .andExpect(jsonPath("$.reasons[0].message").isNotEmpty());
    }
}
