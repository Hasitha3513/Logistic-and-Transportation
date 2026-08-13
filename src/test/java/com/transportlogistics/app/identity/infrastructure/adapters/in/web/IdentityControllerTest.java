package com.transportlogistics.app.identity.infrastructure.adapters.in.web;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentityControllerTest {
    @Test
    void userResponseNeverContainsPasswordHash() throws Exception {
        var useCase = mock(IdentityUseCase.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new IdentityController(useCase)).build();
        var id = UUID.randomUUID();
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var role = new Role(UUID.randomUUID(), "ADMIN", null, true, Set.of("IDENTITY_MANAGE"));
        var user = new User(id, "admin", "admin@example.com", "never-return-this", "Admin", "User", null,
                true, now, now, Set.of(role));
        when(useCase.getUser(id)).thenReturn(user);

        mvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
