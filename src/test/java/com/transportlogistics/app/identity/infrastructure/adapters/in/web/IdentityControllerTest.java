package com.transportlogistics.app.identity.infrastructure.adapters.in.web;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.controllers.IdentityController;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.mappers.IdentityWebMapper;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentityControllerTest {
    @Test
    void userResponseNeverContainsPasswordHash() throws Exception {
        var useCase = mock(IdentityUseCase.class);
        var mapper = Mappers.getMapper(IdentityWebMapper.class);
        var currentTenant = mock(CurrentTenant.class);
        var context = new TenantExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "admin", "test");
        when(currentTenant.required()).thenReturn(context);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new IdentityController(useCase, mapper, currentTenant)).build();
        var id = UUID.randomUUID();
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var role = new Role(UUID.randomUUID(), "ADMIN", null, true, Set.of("IDENTITY_MANAGE"));
        var user = new User(id, "admin", "admin@example.com", "never-return-this", "Admin", "User", null,
                true, now, now, Set.of(role));
        when(useCase.getUser(any(), eq(id))).thenReturn(user);

        var authentication = new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("IDENTITY_MANAGE")));
        mvc.perform(get("/users/{id}", id).principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
