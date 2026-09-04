package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.operations.sla.enabled=false")
@AutoConfigureMockMvc
class FuelCardSecurityIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(authorities = "FUEL_CARD_VIEW")
    void literalApiRoutesDenyManagementImportAndReconciliationWithoutDedicatedPermissions() throws Exception {
        mvc.perform(post("/api/v1/fuel/cards").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/fuel/card-imports").contextPath("/api")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/fuel/card-transactions/00000000-0000-0000-0000-000000000035/match")
                        .contextPath("/api").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
