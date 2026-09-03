package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.CustomerSelfServiceLinkIssuer;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/** Profile-restricted fixture controls for real-browser self-service acceptance. */
@RestController
@Profile("e2e")
@RequestMapping("/e2e/delivery-self-service")
public class E2eCustomerSelfServiceTestController {
    private final CustomerSelfServiceLinkIssuer links;
    private final JdbcClient jdbc;
    private final CurrentTenant tenants;
    private final Clock clock;

    public E2eCustomerSelfServiceTestController(CustomerSelfServiceLinkIssuer links, JdbcClient jdbc,
                                                CurrentTenant tenants, Clock clock) {
        this.links = links;
        this.jdbc = jdbc;
        this.tenants = tenants;
        this.clock = clock;
    }

    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    LinkResponse issue(@RequestBody LinkRequest request) {
        return new LinkResponse(links.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(
                request.deliveryOrderId(), request.recipientContact(), Set.of(), request.idempotencyKey())).url());
    }

    @PostMapping("/expire")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void expire(@RequestBody TokenRequest request) {
        update("issued_at = :at - interval '31 days', expires_at = :at", request.token());
    }

    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@RequestBody TokenRequest request) {
        update("revoked_at = :at, revocation_reason = 'E2E_ACCEPTANCE'", request.token());
    }

    @PostMapping("/mismatch-customer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void mismatchCustomer(@RequestBody CustomerMismatchRequest request) {
        int changed = jdbc.sql("""
                update delivery_self_service_access set customer_id = :customerId, updated_at = :at,
                  updated_by = 'e2e', version = version + 1
                where tenant_id = :tenantId and token_hash = :hash
                """).param("customerId", request.customerId()).param("at", now())
                .param("tenantId", tenants.required().tenantId()).param("hash", hash(request.token())).update();
        if (changed != 1) throw new IllegalArgumentException("E2E token fixture was not found");
    }

    private void update(String assignment, String token) {
        OffsetDateTime at = now().minusSeconds(1);
        int changed = jdbc.sql("update delivery_self_service_access set " + assignment
                        + ", updated_at = :at, updated_by = 'e2e', version = version + 1"
                        + " where tenant_id = :tenantId and token_hash = :hash")
                .param("at", at).param("tenantId", tenants.required().tenantId()).param("hash", hash(token)).update();
        if (changed != 1) throw new IllegalArgumentException("E2E token fixture was not found");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static String hash(String token) {
        try {
            byte[] raw = java.util.Base64.getUrlDecoder().decode(token);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        } catch (IllegalArgumentException | NoSuchAlgorithmException exception) {
            throw new IllegalArgumentException("Invalid E2E token fixture", exception);
        }
    }

    record LinkRequest(UUID deliveryOrderId, String recipientContact, String idempotencyKey) { }
    record LinkResponse(String url) { }
    record TokenRequest(String token) { }
    record CustomerMismatchRequest(String token, UUID customerId) { }
}
