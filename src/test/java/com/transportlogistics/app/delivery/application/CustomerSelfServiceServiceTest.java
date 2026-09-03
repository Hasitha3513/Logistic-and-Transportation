package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.CustomerSelfServiceLinkIssuer;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryEtaUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.notification.CustomerOperationalPreferenceManagement;
import com.transportlogistics.app.organization.CustomerNotificationContactLookup;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.domain.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerSelfServiceServiceTest {
    @Mock DeliveryCustomerSubmissionRepository submissions;
    @Mock DeliveryOrderRepository orders;
    @Mock DeliveryAttemptRepository attempts;
    @Mock DeliveryBatchRepository batches;
    @Mock ProofOfDeliveryRepository proofs;
    @Mock DeliveryLocationLookupPort locations;
    @Mock DeliveryEtaUseCase eta;
    @Mock CustomerNotificationContactLookup customers;
    @Mock CustomerOperationalPreferenceManagement preferences;
    @Mock DeliveryTenantContextPort tenant;
    private final AccessMemory access = new AccessMemory();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC);
    private CustomerSelfServiceService service;
    private DeliveryOrder order;

    @BeforeEach void setUp() {
        var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        order = DeliveryOrder.create(new DeliveryId(orderId), new DeliveryNumber("DEL-2026-000070"), customerId,
                UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
                new DeliveryWindow(now.plusHours(1), now.plusHours(3)), null, now, "operator");
        service = new CustomerSelfServiceService(access, submissions, orders, attempts, batches, proofs, locations,
                eta, customers, preferences, tenant, new SelfServiceTenantExecutor() {
                    @Override public <T> T within(UUID ignored, java.util.function.Supplier<T> work) { return work.get(); }
                }, new DeliveryOrderTransaction() {
                    @Override public <T> T execute(java.util.function.Supplier<T> work) { return work.get(); }
                }, clock,
                "unit_test_hmac_secret_with_more_than_32_characters", "v1", "https://track.example.test");
    }

    @Test void issuanceStoresOnlyHashAndRotatesSameAttempt() {
        stubIssuance();
        var request = new CustomerSelfServiceLinkIssuer.IssueRequest(orderId, "Customer@Example.com", Set.of(),
                "notification-attempt-00000001");
        String first = service.issue(request).url();
        DeliverySelfServiceAccess firstRow = access.values().get(0);
        String rawFirst = first.substring(first.indexOf('=') + 1);

        assertThat(rawFirst).hasSize(43).doesNotContain("=");
        assertThat(firstRow.tokenHash()).matches("[0-9a-f]{64}").doesNotContain(rawFirst);
        assertThat(firstRow.recipientContactHash()).matches("[0-9a-f]{64}").doesNotContain("customer@example.com");
        assertThat(firstRow.expiresAt()).isEqualTo(firstRow.issuedAt().plusDays(30));

        String second = service.issue(request).url();
        DeliverySelfServiceAccess secondRow = access.values().get(0);
        assertThat(secondRow.id()).isEqualTo(firstRow.id());
        assertThat(secondRow.tokenHash()).isNotEqualTo(firstRow.tokenHash());
        assertThat(second).isNotEqualTo(first);
    }

    @Test void validTokenReturnsSafeProjectionAndChangedContactFailsAsSafe404() {
        stubIssuance();
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        when(tenant.currentTenant()).thenReturn(Optional.of(new DeliveryTenantContextPort.TenantContext(tenantId, "Asia/Colombo")));
        String url = service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId, "customer@example.com",
                Set.of(), "notification-attempt-00000002")).url();
        String raw = url.substring(url.indexOf('=') + 1);
        when(customers.find(customerId)).thenReturn(Optional.of(new CustomerNotificationContactLookup.CustomerNotificationContact(
                customerId, true, "Customer", "+94770000000", "customer@example.com")));
        when(preferences.get(customerId)).thenReturn(new CustomerOperationalPreferenceManagement.PreferenceView(
                customerId, false, true, false, "c***@example.com", "+94******000", null));
        when(locations.findLocation(order.destinationLocationId())).thenReturn(Optional.of(
                new DeliveryLocationLookupPort.LocationReference(order.destinationLocationId(), "DEST", "Colombo", true)));
        when(proofs.findByDeliveryOrderId(orderId)).thenReturn(Optional.empty());
        when(submissions.findByDelivery(orderId, customerId)).thenReturn(List.of());

        var projection = service.track(raw, "127.0.0.1");
        assertThat(projection.deliveryNumber()).isEqualTo("DEL-2026-000070");
        assertThat(projection.destination()).isEqualTo("Colombo");
        assertThat(projection.etaFreshness()).isEqualTo("UNAVAILABLE");

        when(customers.find(customerId)).thenReturn(Optional.of(new CustomerNotificationContactLookup.CustomerNotificationContact(
                customerId, true, "Customer", null, "changed@example.com")));
        assertThatThrownBy(() -> service.track(raw, "127.0.0.1"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SELF_SERVICE_ACCESS_INVALID"));
    }

    @Test void expiredAndActionScopedAccessAreDenied() {
        var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        var expired = new DeliverySelfServiceAccess(UUID.randomUUID(), tenantId, orderId, customerId,
                "a".repeat(64), "v1", "b".repeat(64), Set.of(SelfServiceAction.TRACK), "attempt-000000000003",
                now.minusDays(31), now.minusDays(1), null, null, 0, 0);
        assertThat(expired.permits(SelfServiceAction.TRACK, now)).isFalse();
        assertThat(expired.permits(SelfServiceAction.ISSUE_SUBMIT, now.minusDays(2))).isFalse();
    }

    @Test void sixthActiveTokenRevokesTheOldestAndPreservesTheCapOfFive() {
        stubIssuance();
        for (int index = 0; index < 6; index++) {
            service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId, "customer@example.com", Set.of(),
                    "notification-attempt-cap-" + index));
        }
        var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        assertThat(access.values().stream().filter(value -> value.permits(SelfServiceAction.TRACK, now))).hasSize(5);
        assertThat(access.values().stream().filter(value -> value.revokedAt() != null)).hasSize(1);
    }

    @Test void revokedTokenAndMissingActionReturnTheSameSafeNotFound() {
        stubIssuance();
        String revoked = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of(), "notification-attempt-revoked")));
        access.revoke(access.values().getFirst().id(), OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), "TEST");
        assertThatThrownBy(() -> service.track(revoked, "127.0.0.1"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SELF_SERVICE_ACCESS_INVALID"));

        String scoped = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of("TRACK"), "notification-attempt-scoped")));
        assertThatThrownBy(() -> service.submitIssue(scoped, "127.0.0.1", "issue-key-0000000001",
                new com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase.IssueCommand(
                        "OTHER", "A sufficiently detailed issue.")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SELF_SERVICE_ACCESS_INVALID"));
    }

    @Test void customerRequestAndFeedbackPersistOnlyCustomerSubmissions() {
        stubIssuance();
        String raw = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of(), "notification-attempt-submissions")));
        var ready = order.markReadyForAssignment(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), "operator");
        stubAuthorization(ready);
        when(submissions.findIdempotent(any(), any(), anyString())).thenReturn(Optional.empty());
        when(submissions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = service.submitRequest(raw, "127.0.0.1", "request-key-000000001",
                new com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase.RequestCommand(
                        OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(1),
                        OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(1).plusHours(1), "Call first"));
        assertThat(request.type()).isEqualTo("DELIVERY_PREFERENCE");
        assertThat(ready.status()).isEqualTo(DeliveryStatus.READY_FOR_ASSIGNMENT);
        verifyNoInteractions(batches);

        order = ready.markDelivered(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).plusMinutes(1), "rider");
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(submissions.feedbackExists(orderId, customerId)).thenReturn(false);
        var feedback = service.submitFeedback(raw, "127.0.0.1", "feedback-key-00000001",
                new com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase.FeedbackCommand(5, "Excellent"));
        assertThat(feedback.type()).isEqualTo("FEEDBACK");
        assertThat(feedback.status()).isEqualTo("RECORDED");
    }

    @Test void idempotentIssueReplayReturnsOriginalAndChangedPayloadConflicts() {
        stubIssuance();
        String raw = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of(), "notification-attempt-idempotency")));
        stubAuthorization(order);
        AtomicReference<DeliveryCustomerSubmission> stored = new AtomicReference<>();
        when(submissions.findIdempotent(any(), eq(CustomerSubmissionType.ISSUE), eq("issue-key-0000000001")))
                .thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        when(submissions.save(any())).thenAnswer(invocation -> {
            DeliveryCustomerSubmission value = invocation.getArgument(0);
            stored.compareAndSet(null, value);
            return stored.get();
        });

        var first = service.submitIssue(raw, "127.0.0.1", "issue-key-0000000001",
                new com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase.IssueCommand(
                        "OTHER", "A sufficiently detailed issue."));
        var replay = service.submitIssue(raw, "127.0.0.1", "issue-key-0000000001",
                new com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase.IssueCommand(
                        "OTHER", "A sufficiently detailed issue."));
        assertThat(replay.reference()).isEqualTo(first.reference());
        verify(submissions, times(1)).save(any());

        assertThatThrownBy(() -> service.submitIssue(raw, "127.0.0.1", "issue-key-0000000001",
                new com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase.IssueCommand(
                        "OTHER", "A different detailed issue payload.")))
                .isInstanceOfSatisfying(com.transportlogistics.app.shared.domain.ConflictException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SELF_SERVICE_IDEMPOTENCY_CONFLICT"));
    }

    @Test void projectionAdvertisesOnlyActionsGrantedToTheToken() {
        stubIssuance();
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        when(tenant.currentTenant()).thenReturn(Optional.of(
                new DeliveryTenantContextPort.TenantContext(tenantId, "Asia/Colombo")));
        String raw = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of("TRACK"), "notification-attempt-track-only")));
        when(customers.find(customerId)).thenReturn(Optional.of(customer()));
        when(preferences.get(customerId)).thenReturn(new CustomerOperationalPreferenceManagement.PreferenceView(
                customerId, false, true, false, "c***@example.com", "+94******000", null));
        when(locations.findLocation(order.destinationLocationId())).thenReturn(Optional.empty());
        when(proofs.findByDeliveryOrderId(orderId)).thenReturn(Optional.empty());
        when(submissions.findByDelivery(orderId, customerId)).thenReturn(List.of());

        assertThat(service.track(raw, "127.0.0.1").availableActions()).containsExactly("TRACK");
    }

    @Test void inactiveCustomerAndConcurrentRevocationUseTheSameSafeDenial() {
        stubIssuance();
        String raw = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of(), "notification-attempt-inactive")));
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        when(customers.find(customerId)).thenReturn(Optional.of(new CustomerNotificationContactLookup.CustomerNotificationContact(
                customerId, false, "Customer", "+94770000000", "customer@example.com")));
        assertThatThrownBy(() -> service.track(raw, "127.0.0.1"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SELF_SERVICE_ACCESS_INVALID"));

        String concurrent = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of(), "notification-attempt-concurrent-revoke")));
        access.allowUse = false;
        when(customers.find(customerId)).thenReturn(Optional.of(customer()));
        assertThatThrownBy(() -> service.track(concurrent, "127.0.0.2"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SELF_SERVICE_ACCESS_INVALID"));
    }

    @Test void writeRateLimitRejectsTheEleventhRequestWithoutRevealingDeliveryFacts() {
        stubIssuance();
        String raw = raw(service.issue(new CustomerSelfServiceLinkIssuer.IssueRequest(orderId,
                "customer@example.com", Set.of(), "notification-attempt-rate-limit")));
        stubAuthorization(order);
        when(preferences.replace(eq(customerId), any())).thenReturn(new CustomerOperationalPreferenceManagement.PreferenceView(
                customerId, true, true, false, "c***@example.com", "+94******000", 1L));
        var command = new com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase.PreferenceCommand(
                true, false, 0L);
        for (int index = 0; index < 10; index++) service.replacePreferences(raw, "127.0.0.1", command);
        assertThatThrownBy(() -> service.replacePreferences(raw, "127.0.0.1", command))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SELF_SERVICE_RATE_LIMITED"));
    }

    private void stubIssuance() {
        when(tenant.currentTenantId()).thenReturn(Optional.of(tenantId));
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
    }

    private void stubAuthorization(DeliveryOrder value) {
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(value));
        when(customers.find(customerId)).thenReturn(Optional.of(customer()));
    }

    private CustomerNotificationContactLookup.CustomerNotificationContact customer() {
        return new CustomerNotificationContactLookup.CustomerNotificationContact(
                customerId, true, "Customer", "+94770000000", "customer@example.com");
    }

    private static String raw(CustomerSelfServiceLinkIssuer.IssuedLink link) {
        return link.url().substring(link.url().indexOf('=') + 1);
    }

    private static final class AccessMemory implements DeliverySelfServiceAccessRepository {
        private final List<DeliverySelfServiceAccess> rows = new ArrayList<>();
        private boolean allowUse = true;
        List<DeliverySelfServiceAccess> values() { return List.copyOf(rows); }
        @Override public Optional<DeliverySelfServiceAccess> findBootstrapByTokenHash(String hash) { return find(hash); }
        @Override public Optional<DeliverySelfServiceAccess> findByTokenHash(String hash) { return find(hash); }
        @Override public Optional<DeliverySelfServiceAccess> findByTokenHashForUpdate(String hash) { return find(hash); }
        private Optional<DeliverySelfServiceAccess> find(String hash) { return rows.stream().filter(v -> v.tokenHash().equals(hash)).findFirst(); }
        @Override public Optional<DeliverySelfServiceAccess> findByIssuanceKeyForUpdate(String key) { return rows.stream().filter(v -> v.issuanceIdempotencyKey().equals(key)).findFirst(); }
        @Override public List<DeliverySelfServiceAccess> findActiveForUpdate(UUID deliveryId, UUID customerId, OffsetDateTime now) { return rows.stream().filter(v -> v.deliveryOrderId().equals(deliveryId) && v.customerId().equals(customerId) && v.permits(SelfServiceAction.TRACK, now)).toList(); }
        @Override public DeliverySelfServiceAccess save(DeliverySelfServiceAccess value) { rows.removeIf(v -> v.id().equals(value.id())); rows.add(value); return value; }
        @Override public void revoke(UUID id, OffsetDateTime at, String reason) { rows.replaceAll(v -> v.id().equals(id) ? new DeliverySelfServiceAccess(v.id(), v.tenantId(), v.deliveryOrderId(), v.customerId(), v.recipientContactHash(), v.contactHashKeyVersion(), v.tokenHash(), v.allowedActions(), v.issuanceIdempotencyKey(), v.issuedAt(), v.expiresAt(), at, v.lastUsedAt(), v.useCount(), v.version()) : v); }
        @Override public boolean markUsed(UUID id, OffsetDateTime at) { return allowUse; }
    }
}
