package com.transportlogistics.app.identity.infrastructure.security;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.TenantAccessResolver;
import com.transportlogistics.app.identity.application.ports.out.AccessTokenService;
import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import com.transportlogistics.app.tenancy.TenantExecutionContext;

@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AccessTokenService tokens;
    private final SecurityErrorWriter errors;
    private final IdentityUseCase identity;
    private final TenantAccessResolver tenantAccess;
    private final RequestTenantContext tenantContext;

    JwtAuthenticationFilter(AccessTokenService tokens, SecurityErrorWriter errors, IdentityUseCase identity,
                            TenantAccessResolver tenantAccess, RequestTenantContext tenantContext) {
        this.tokens = tokens;
        this.errors = errors;
        this.identity = identity;
        this.tenantAccess = tenantAccess;
        this.tenantContext = tenantContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            var claims = tokens.verify(authorization.substring(7));
            var user = identity.currentUser(claims.username());
            var resolved = tenantAccess.resolve(user.id());
            var authorities = Stream.concat(
                            user.roleNames().stream().map(role -> "ROLE_" + role), user.permissions().stream())
                    .map(SimpleGrantedAuthority::new).toList();
            var authentication = new UsernamePasswordAuthenticationToken(claims.username(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            tenantContext.establish(new TenantExecutionContext(resolved.tenantId(), user.id(), user.username(),
                    Optional.ofNullable(request.getHeader("X-Correlation-ID")).filter(value -> !value.isBlank())
                            .orElseGet(() -> UUID.randomUUID().toString())));
            try {
                chain.doFilter(request, response);
            } finally {
                tenantContext.clear();
            }
        } catch (AuthenticationFailedException ex) {
            tenantContext.clear();
            SecurityContextHolder.clearContext();
            errors.write(request, response, org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", ex.getMessage());
        }
    }
}
