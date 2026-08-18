package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.domain.model.BunkerTank;
import com.transportlogistics.app.fuel.domain.model.BunkerTankStatus;
import com.transportlogistics.app.fuel.domain.model.DipReading;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.BunkerTankCreateRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.DipReadingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BunkerTankApiIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @MockBean private BunkerTankUseCase bunkerTanks;

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldListBunkerTanks() throws Exception {
        var tank = new BunkerTank(
                UUID.randomUUID(), UUID.randomUUID(), "BNK-01", "Tank 1", "DIESEL",
                new BigDecimal("10000.000"), new BigDecimal("2500.000"), new BigDecimal("1000.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(), true, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(bunkerTanks.list(any(), any(), any())).thenReturn(List.of(tank));

        mvc.perform(get("/bunker-tanks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tankCode").value("BNK-01"))
                .andExpect(jsonPath("$[0].currentStockLiters").value(2500.000));
    }

    @Test
    @WithMockUser(authorities = "BUNKER_CREATE")
    void shouldCreateBunkerTank() throws Exception {
        var tank = new BunkerTank(
                UUID.randomUUID(), UUID.randomUUID(), "BNK-02", "Tank 2", "PETROL_92",
                new BigDecimal("5000.000"), new BigDecimal("1000.000"), new BigDecimal("500.000"),
                BunkerTankStatus.ACTIVE, OffsetDateTime.now(), true, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(bunkerTanks.create(any(), any())).thenReturn(tank);

        var req = new BunkerTankCreateRequest(
                tank.fuelStationId(), "BNK-02", "Tank 2", "PETROL_92",
                new BigDecimal("5000.000"), new BigDecimal("500.000"), new BigDecimal("1000.000"), null
        );

        mvc.perform(post("/bunker-tanks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tankCode").value("BNK-02"));
    }

    @Test
    @WithMockUser(authorities = "BUNKER_DIP_RECORD")
    void shouldRecordDipReading() throws Exception {
        var tankId = UUID.randomUUID();
        var reading = new DipReading(
                UUID.randomUUID(), tankId, new BigDecimal("2400.000"), new BigDecimal("2500.000"),
                new BigDecimal("-100.000"), OffsetDateTime.now(), UUID.randomUUID(), "Weekly dip", OffsetDateTime.now()
        );
        when(bunkerTanks.recordDipReading(eq(tankId), any(), any(), any())).thenReturn(reading);

        var req = new DipReadingRequest(new BigDecimal("2400.000"), "Weekly dip");

        mvc.perform(post("/bunker-tanks/{id}/dip-readings", tankId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.varianceQuantityLiters").value(-100.000));
    }
}
