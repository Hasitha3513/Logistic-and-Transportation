package com.transportlogistics.app.identity.infrastructure.security;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BunkerSecurityIntegrationTest {

    @Autowired private MockMvc mvc;

    @MockBean private BunkerTankUseCase bunkerTanks;

    @Test
    void shouldDenyUnauthenticatedAccess() throws Exception {
        mvc.perform(get("/bunker-tanks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "RANDOM_AUTHORITY")
    void shouldDenyUnauthorizedAccess() throws Exception {
        mvc.perform(get("/bunker-tanks"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldAllowBunkerListWithViewAuthority() throws Exception {
        mvc.perform(get("/bunker-tanks"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldDenyBunkerCreateWithoutCreateAuthority() throws Exception {
        mvc.perform(post("/bunker-tanks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldDenyBunkerLedgerWithoutLedgerAuthority() throws Exception {
        mvc.perform(get("/bunker-tanks/{id}/movements", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_LEDGER_VIEW")
    void shouldAllowBunkerLedgerWithLedgerAuthority() throws Exception {
        mvc.perform(get("/bunker-tanks/{id}/movements", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
