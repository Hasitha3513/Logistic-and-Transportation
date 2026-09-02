package com.transportlogistics.app.identity.infrastructure.adapters.in.web.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Profile-restricted fixture endpoint; it is unavailable outside the isolated E2E runtime. */
@RestController
@Profile("e2e")
@RequestMapping("/e2e/tenant-fixtures")
public class E2eTenantFixtureController {
    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;

    public E2eTenantFixtureController(JdbcClient jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    ResponseEntity<TenantFixtureResponse> create(@RequestBody TenantFixtureRequest request) {
        var tenantId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var membershipId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var username = "e2e-tenant-" + request.suffix();
        var password = "E2e!Tenant-" + request.suffix();
        var roleId = jdbc.sql("select id from app_role where name = 'LOCAL_MVP_ADMIN'").query(UUID.class).single();
        jdbc.sql("""
                insert into tenant (tenant_id, tenant_code, tenant_name, default_currency, default_time_zone, status, created_at, created_by, updated_at, updated_by, version)
                values (:tenantId, :code, :name, 'LKR', 'Asia/Colombo', 'ACTIVE', :now, 'e2e', :now, 'e2e', 0)
                """).param("tenantId", tenantId).param("code", "E2E-" + request.suffix()).param("name", "E2E tenant " + request.suffix()).param("now", now).update();
        jdbc.sql("""
                insert into app_user (id, username, email, password_hash, first_name, last_name, phone, active, created_at, updated_at)
                values (:userId, :username, :email, :password, 'E2E', 'Tenant', null, true, :now, :now)
                """).param("userId", userId).param("username", username).param("email", username + "@example.test").param("password", passwordEncoder.encode(password)).param("now", now).update();
        jdbc.sql("""
                insert into tenant_membership (membership_id, tenant_id, user_id, status, created_at, created_by, updated_at, updated_by, version)
                values (:membershipId, :tenantId, :userId, 'ACTIVE', :now, 'e2e', :now, 'e2e', 0)
                """).param("membershipId", membershipId).param("tenantId", tenantId).param("userId", userId).param("now", now).update();
        jdbc.sql("insert into tenant_membership_role (membership_id, role_id) values (:membershipId, :roleId)")
                .param("membershipId", membershipId).param("roleId", roleId).update();
        return ResponseEntity.status(201).body(new TenantFixtureResponse(tenantId, username, password));
    }

    record TenantFixtureRequest(String suffix) { }
    record TenantFixtureResponse(UUID tenantId, String username, String password) { }
}
