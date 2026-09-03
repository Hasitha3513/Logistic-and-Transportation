package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.CreateDeliveryZoneRequest;
import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneBoundary;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.context.SpringBootTest
@AutoConfigureMockMvc
class DeliveryZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryZoneUseCase zoneUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    @WithMockUser(authorities = "DELIVERY_ZONE_CREATE")
    @DisplayName("POST /v1/delivery-zones creates zone")
    void createZoneEndpoint() throws Exception {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(79.8, 6.9),
                new DeliveryZoneCoordinate(79.9, 6.9),
                new DeliveryZoneCoordinate(79.9, 7.0),
                new DeliveryZoneCoordinate(79.8, 7.0),
                new DeliveryZoneCoordinate(79.8, 6.9)
        );
        CreateDeliveryZoneRequest request = new CreateDeliveryZoneRequest(
                "ZONE-TEST",
                "Zone Test",
                "Desc",
                DeliveryZoneType.URBAN_DENSE,
                true,
                100,
                null,
                coords,
                5
        );

        DeliveryZone zone = new DeliveryZone(
                UUID.randomUUID(),
                tenantId,
                "ZONE-TEST",
                "Zone Test",
                "Desc",
                DeliveryZoneType.URBAN_DENSE,
                DeliveryZoneStatus.ACTIVE,
                true,
                100,
                null,
                new DeliveryZoneBoundary(coords),
                5,
                0L,
                now,
                "admin",
                now,
                "admin"
        );

        when(zoneUseCase.createZone(any(), any())).thenReturn(zone);

        mockMvc.perform(post("/v1/delivery-zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zoneCode").value("ZONE-TEST"))
                .andExpect(jsonPath("$.serviceable").value(true));
    }

    @Test
    @WithMockUser(authorities = "DELIVERY_ZONE_VIEW")
    @DisplayName("GET /v1/delivery-zones lists zones")
    void listZonesEndpoint() throws Exception {
        List<DeliveryZoneCoordinate> coords = List.of(
                new DeliveryZoneCoordinate(0.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 0.0),
                new DeliveryZoneCoordinate(1.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 1.0),
                new DeliveryZoneCoordinate(0.0, 0.0)
        );
        DeliveryZone zone = new DeliveryZone(
                UUID.randomUUID(),
                tenantId,
                "ZONE-LIST",
                "Zone List",
                "Desc",
                DeliveryZoneType.SUBURBAN,
                DeliveryZoneStatus.ACTIVE,
                true,
                50,
                null,
                new DeliveryZoneBoundary(coords),
                0,
                0L,
                now,
                "admin",
                now,
                "admin"
        );

        when(zoneUseCase.listZones(null, null)).thenReturn(List.of(zone));

        mockMvc.perform(get("/v1/delivery-zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneCode").value("ZONE-LIST"));
    }
}
