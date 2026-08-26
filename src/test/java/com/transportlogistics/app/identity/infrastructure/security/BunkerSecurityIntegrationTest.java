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

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldDenyDipRecordingWithoutDipAuthority() throws Exception {
        mvc.perform(post("/bunker-tanks/{id}/dip-readings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"physicalQuantityLiters\": 5000.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_DIP_RECORD")
    void shouldAllowDipRecordingWithDipAuthority() throws Exception {
        mvc.perform(post("/bunker-tanks/{id}/dip-readings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"physicalQuantityLiters\": 5000.0}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldDenyAdjustmentWithoutAdjustAuthority() throws Exception {
        mvc.perform(post("/bunker-tanks/{id}/adjustments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityDeltaLiters\": -100.0, \"reason\": \"Loss\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_ADJUST")
    void shouldAllowAdjustmentWithAdjustAuthority() throws Exception {
        mvc.perform(post("/bunker-tanks/{id}/adjustments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityDeltaLiters\": -100.0, \"reason\": \"Loss\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldAllowDipListWithViewAuthority() throws Exception {
        mvc.perform(get("/bunker-tanks/{id}/dip-readings", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "RANDOM_AUTHORITY")
    void shouldDenyDipListWithoutViewAuthority() throws Exception {
        mvc.perform(get("/bunker-tanks/{id}/dip-readings", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_VIEW")
    void shouldDenyTransferWithoutTransferAuthority() throws Exception {
        mvc.perform(post("/bunker-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceTankId\": \"" + UUID.randomUUID() + "\", \"destinationTankId\": \"" + UUID.randomUUID() + "\", \"quantityLiters\": 100.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BUNKER_TRANSFER")
    void shouldAllowTransferWithTransferAuthority() throws Exception {
        mvc.perform(post("/bunker-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceTankId\": \"" + UUID.randomUUID() + "\", \"destinationTankId\": \"" + UUID.randomUUID() + "\", \"quantityLiters\": 100.0}"))
                .andExpect(status().isOk());
    }
}
