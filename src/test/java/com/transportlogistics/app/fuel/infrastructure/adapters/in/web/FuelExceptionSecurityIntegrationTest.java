package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="app.operations.sla.enabled=false") @AutoConfigureMockMvc
class FuelExceptionSecurityIntegrationTest {
 @Autowired MockMvc mvc;
 @Test @WithMockUser(authorities="FUEL_EXCEPTION_VIEW") void literalApiUrlDeniesEveryRestrictedCommandWithoutItsPermission()throws Exception{
  deny("/api/v1/fuel/exceptions");deny("/api/v1/fuel/exceptions/00000000-0000-0000-0000-000000000038/review");deny("/api/v1/fuel/exceptions/00000000-0000-0000-0000-000000000038/corrections");deny("/api/v1/fuel/exceptions/00000000-0000-0000-0000-000000000038/corrections/00000000-0000-0000-0000-000000000039/approve");deny("/api/v1/fuel/exceptions/00000000-0000-0000-0000-000000000038/escalate");
 }
 private void deny(String path)throws Exception{mvc.perform(post(path).contextPath("/api").contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isForbidden());}
}
