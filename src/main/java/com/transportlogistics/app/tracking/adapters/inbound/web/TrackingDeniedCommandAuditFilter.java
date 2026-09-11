package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Context;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/** Records safe evidence for denied authenticated Tracking management commands. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
final class TrackingDeniedCommandAuditFilter extends OncePerRequestFilter {
 private final CurrentTenant tenants;private final TrackingStore store;
 TrackingDeniedCommandAuditFilter(CurrentTenant tenants,TrackingStore store){this.tenants=tenants;this.store=store;}
 @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().contains("/v1/tracking/")||"GET".equals(request.getMethod());}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{chain.doFilter(request,response);if(response.getStatus()==HttpServletResponse.SC_FORBIDDEN||response.getStatus()==HttpServletResponse.SC_NOT_FOUND){tenants.current().ifPresent(c->{try{store.auditDenied(new Context(c.tenantId(),c.actorId(),c.correlationId()),request.getMethod()+" "+request.getRequestURI(),response.getStatus()==HttpServletResponse.SC_FORBIDDEN?"FORBIDDEN":"RESOURCE_NOT_FOUND",Instant.now());}catch(RuntimeException ignored){/* audit failure must not alter the already denied response */}});}}
}
