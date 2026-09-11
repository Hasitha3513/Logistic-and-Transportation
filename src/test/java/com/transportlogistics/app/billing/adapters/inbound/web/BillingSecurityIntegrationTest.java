package com.transportlogistics.app.billing.adapters.inbound.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.operations.sla.enabled=false")
@AutoConfigureMockMvc
class BillingSecurityIntegrationTest {
    @Autowired MockMvc mvc;

    @Test @WithMockUser(authorities="BILLING_VIEW")
    void literalApiRouteDeniesCreateWithoutPreparePermission() throws Exception {
        mvc.perform(post("/api/v1/billing/records").contextPath("/api")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities="BILLING_PREPARE")
    void literalApiRouteDeniesReadWithoutViewPermission() throws Exception {
        mvc.perform(get("/api/v1/billing/records").contextPath("/api"))
            .andExpect(status().isForbidden());
    }
}
