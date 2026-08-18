package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fuel.PricingSource;
import com.transportlogistics.app.fuel.TripFuelCost;
import com.transportlogistics.app.fuel.TripFuelCostCalculationStatus;
import com.transportlogistics.app.fuel.TripFuelCostLine;
import com.transportlogistics.app.fuel.application.ports.in.TripFuelCostUseCase;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers.TripFuelCostController;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.TripFuelCostWebMapper;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripFuelCostControllerTest {

    private MockMvc mockMvc;
    private TripFuelCostUseCase tripFuelCostUseCase;

    private final UUID tripId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tripFuelCostUseCase = mock(TripFuelCostUseCase.class);
        var mapper = Mappers.getMapper(TripFuelCostWebMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TripFuelCostController(tripFuelCostUseCase, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTripFuelCostReturnsOk() throws Exception {
        var cost = new TripFuelCost(
                tripId,
                vehicleId,
                new BigDecimal("30.000"),
                "LKR",
                new BigDecimal("9100.00"),
                new BigDecimal("200.000"),
                new BigDecimal("45.50"),
                new BigDecimal("15.00"),
                2,
                0,
                TripDistanceStatus.CALCULATED,
                TripFuelCostCalculationStatus.COMPLETE,
                List.of(new TripFuelCostLine(
                        UUID.randomUUID(), "VOUCHER-01", OffsetDateTime.now(), new BigDecimal("30.000"),
                        new BigDecimal("300.00"), new BigDecimal("9000.00"), PricingSource.EXPLICIT_ISSUE_PRICE,
                        "LKR", UUID.randomUUID(), "DIESEL")),
                OffsetDateTime.now()
        );
        when(tripFuelCostUseCase.getTripFuelCost(tripId)).thenReturn(cost);

        mockMvc.perform(get("/trips/{tripId}/fuel-cost", tripId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId.toString()))
                .andExpect(jsonPath("$.totalFuelCost").value(9100.00))
                .andExpect(jsonPath("$.costPerKm").value(45.50))
                .andExpect(jsonPath("$.litersPer100Km").value(15.00))
                .andExpect(jsonPath("$.calculationStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines[0].voucherNumber").value("VOUCHER-01"));
    }

    @Test
    void getTripFuelCostNotFoundReturns404() throws Exception {
        when(tripFuelCostUseCase.getTripFuelCost(tripId))
                .thenThrow(new NotFoundException("TRIP_NOT_FOUND", "Trip not found"));

        mockMvc.perform(get("/trips/{tripId}/fuel-cost", tripId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }
}