package com.transportlogistics.app.fuel.infrastructure.adapters.out.identity;

import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityFuelActorAdapterTest {

    @Test
    void resolvesFuelActorThroughPublicIdentityContract() {
        var userId = UUID.randomUUID();
        var users = mock(AuthenticatedUserLookup.class);
        when(users.findByUsername("fuel.operator"))
                .thenReturn(Optional.of(new AuthenticatedUserLookup.AuthenticatedUser(userId, "fuel.operator")));

        var adapter = new IdentityFuelActorAdapter(users);

        assertThat(adapter.find("fuel.operator"))
                .hasValueSatisfying(actor -> {
                    assertThat(actor.id()).isEqualTo(userId);
                    assertThat(actor.username()).isEqualTo("fuel.operator");
                });
    }

    @Test
    void preservesMissingActorResult() {
        var users = mock(AuthenticatedUserLookup.class);
        when(users.findByUsername("missing")).thenReturn(Optional.empty());

        assertThat(new IdentityFuelActorAdapter(users).find("missing")).isEmpty();
    }
}
