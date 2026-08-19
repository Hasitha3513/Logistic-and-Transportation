package com.transportlogistics.app.identity.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.dev.identity-bootstrap.enabled=true",
        "app.dev.identity-bootstrap.username=bootstrap.integration",
        "app.dev.identity-bootstrap.password=BootstrapIntegration!2026",
        "app.dev.identity-bootstrap.email=bootstrap@example.test"
})
@AutoConfigureMockMvc
class LocalIdentityBootstrapIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void createsAnOptInLocalUserWithMvpPermissionsAfterFlywayMigration() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bootstrap.integration\",\"password\":\"BootstrapIntegration!2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        var accessToken = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(login.getResponse().getContentAsString()).get("accessToken").asText();

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Local"))
                .andExpect(jsonPath("$.lastName").value("Administrator"))
                .andExpect(jsonPath("$.permissions.length()").value(76))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("BUNKER_VIEW")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("FUEL_COST_VIEW")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("VEHICLE_MAINTENANCE_MANAGE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("DRIVER_EXCEPTION_MANAGE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("DRIVER_VIOLATION_MANAGE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("CUSTOMER_UPDATE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("DEPARTMENT_UPDATE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("LOCATION_UPDATE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("PROJECT_UPDATE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("FUEL_ISSUE_AUTHORIZE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("FUEL_PURCHASE_APPROVE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("FUEL_PURCHASE_RECONCILE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("FUEL_PRICE_MANAGE")))
                .andExpect(jsonPath("$.permissions").isArray());
    }
}
