package com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UserRequest(@NotBlank String username,
                          @Email @NotBlank String email,
                          @Size(min = 12) String password,
                          @NotBlank String firstName,
                          @NotBlank String lastName,
                          String phone,
                          Boolean active,
                          Set<UUID> roleIds) {
    public UserRequest {
        if (password != null && password.isBlank()) {
            password = null;
        }
    }

    @Override
    public String toString() {
        return "UserRequest[username=" + username + ", email=" + email + ", password=***, firstName=" + firstName +
                ", lastName=" + lastName + ", phone=" + phone + ", active=" + active + ", roleIds=" + roleIds + "]";
    }
}
