package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.CustomerSelfServiceLinkIssuer;
import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryEtaUseCase;
import com.transportlogistics.app.delivery.ports.outbound.*;
import com.transportlogistics.app.notification.CustomerOperationalPreferenceManagement;
import com.transportlogistics.app.organization.CustomerNotificationContactLookup;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.shared.domain.TooManyRequestsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomerSelfServiceService implements CustomerSelfServiceUseCase, CustomerSelfServiceLinkIssuer {
    private static final Set<SelfServiceAction> ALL_ACTIONS = Set.of(SelfServiceAction.values());
    private static final int MAX_RATE_LIMIT_KEYS = 10_000;
    private final DeliverySelfServiceAccessRepository access;
    private final DeliveryCustomerSubmissionRepository submissions;
    private final DeliveryOrderRepository orders;
    private final DeliveryAttemptRepository attempts;
    private final DeliveryBatchRepository batches;
    private final ProofOfDeliveryRepository proofs;
    private final DeliveryLocationLookupPort locations;
    private final DeliveryEtaUseCase eta;
    private final CustomerNotificationContactLookup customers;
    private final CustomerOperationalPreferenceManagement preferences;
    private final DeliveryTenantContextPort currentTenant;
    private final SelfServiceTenantExecutor tenantExecutor;
    private final DeliveryOrderTransaction transactions;
    private final Clock clock;
    private final String hmacSecret;
    private final String keyVersion;
    private final String customerOrigin;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, ArrayDeque<Instant>> readLimits = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<Instant>> writeLimits = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<Instant>> invalidLimits = new ConcurrentHashMap<>();

    public CustomerSelfServiceService(DeliverySelfServiceAccessRepository access,
            DeliveryCustomerSubmissionRepository submissions, DeliveryOrderRepository orders,
            DeliveryAttemptRepository attempts, DeliveryBatchRepository batches, ProofOfDeliveryRepository proofs,
            DeliveryLocationLookupPort locations, DeliveryEtaUseCase eta, CustomerNotificationContactLookup customers,
            CustomerOperationalPreferenceManagement preferences, DeliveryTenantContextPort currentTenant,
            SelfServiceTenantExecutor tenantExecutor, DeliveryOrderTransaction transactions, Clock clock,
            String hmacSecret, String keyVersion, String customerOrigin) {
        this.access = access; this.submissions = submissions; this.orders = orders; this.attempts = attempts;
        this.batches = batches; this.proofs = proofs; this.locations = locations; this.eta = eta;
        this.customers = customers; this.preferences = preferences; this.currentTenant = currentTenant;
        this.tenantExecutor = tenantExecutor; this.transactions = transactions; this.clock = clock;
        if (hmacSecret == null || hmacSecret.length() < 32) throw new IllegalArgumentException("Self-service HMAC secret must contain at least 32 characters");
        this.hmacSecret = hmacSecret; this.keyVersion = required(keyVersion, "Contact hash key version");
        this.customerOrigin = required(customerOrigin, "Customer origin").replaceAll("/+$", "");
    }

    @Override
    public IssuedLink issue(IssueRequest request) {
        Objects.requireNonNull(request, "Issue request is required");
        UUID tenantId = currentTenant.currentTenantId().orElseThrow(() -> invalid());
        String normalizedContact = normalizeContact(request.recipientContact());
        String contactHash = hmac(normalizedContact);
        Set<SelfServiceAction> actions = parseActions(request.allowedActions());
        String raw = token();
        String tokenHash = sha256(Base64.getUrlDecoder().decode(raw));
        OffsetDateTime now = now();
        return transactions.execute(() -> {
            DeliveryOrder order = orders.findByIdForUpdate(request.deliveryOrderId()).orElseThrow(() -> invalid());
            DeliverySelfServiceAccess existing = access.findByIssuanceKeyForUpdate(request.issuanceIdempotencyKey()).orElse(null);
            if (existing != null) {
                if (!existing.deliveryOrderId().equals(order.id().value()) || !existing.customerId().equals(order.customerId())) throw invalid();
                access.save(existing.rotate(tokenHash, contactHash, keyVersion, actions, now));
            } else {
                List<DeliverySelfServiceAccess> active = access.findActiveForUpdate(order.id().value(), order.customerId(), now);
                if (active.size() >= 5) access.revoke(active.get(0).id(), now, "ACTIVE_TOKEN_CAP");
                access.save(new DeliverySelfServiceAccess(UUID.randomUUID(), tenantId, order.id().value(),
                        order.customerId(), contactHash, keyVersion, tokenHash, actions,
                        validateIdempotency(request.issuanceIdempotencyKey()), now, now.plusDays(30), null, null, 0, 0));
            }
            return new IssuedLink(customerOrigin + "/track#access_token=" + raw);
        });
    }

    @Override public Projection track(String token, String ip) {
        return authorized(token, ip, SelfServiceAction.TRACK, false, ctx -> projection(ctx));
    }
    @Override public Preferences preferences(String token, String ip) {
        return authorized(token, ip, SelfServiceAction.PREFERENCE_READ, false, ctx -> preferenceView(ctx.customerId()));
    }
    @Override public Preferences replacePreferences(String token, String ip, PreferenceCommand command) {
        return authorized(token, ip, SelfServiceAction.PREFERENCE_WRITE, true, ctx -> {
            try {
                var value = preferences.replace(ctx.customerId(), new CustomerOperationalPreferenceManagement.ReplaceCommand(
                        command.emailEnabled(), command.smsEnabled(), command.version()));
                return map(value);
            } catch (ConflictException exception) {
                throw new ConflictException("SELF_SERVICE_PREFERENCE_VERSION_CONFLICT", exception.getMessage());
            } catch (NotFoundException exception) {
                throw invalid();
            }
        });
    }
    @Override public Submission submitIssue(String token, String ip, String key, IssueCommand command) {
        return authorized(token, ip, SelfServiceAction.ISSUE_SUBMIT, true, ctx -> {
            CustomerIssueCategory category;
            try { category = CustomerIssueCategory.valueOf(required(command.category(), "Issue category")); }
            catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unsupported issue category"); }
            String description = text(command.description(), 10, 1000, "Issue description");
            return submit(ctx, CustomerSubmissionType.ISSUE, key, category.name(), description, null, null, null);
        });
    }
    @Override public Submission submitFeedback(String token, String ip, String key, FeedbackCommand command) {
        return authorized(token, ip, SelfServiceAction.FEEDBACK_SUBMIT, true, ctx -> {
            if (ctx.order().status() != DeliveryStatus.DELIVERED) throw new ConflictException("SELF_SERVICE_FEEDBACK_NOT_AVAILABLE", "Feedback is available after delivery");
            if (command.rating() < 1 || command.rating() > 5) throw new IllegalArgumentException("Rating must be between 1 and 5");
            String comment = optionalText(command.comment(), 1000);
            String hash = requestHash("FEEDBACK|" + command.rating() + "|" + Objects.toString(comment, ""));
            DeliveryCustomerSubmission prior = idempotent(ctx, CustomerSubmissionType.FEEDBACK, key, hash);
            if (prior != null) return view(prior);
            if (submissions.feedbackExists(ctx.order().id().value(), ctx.customerId()))
                throw new ConflictException("SELF_SERVICE_FEEDBACK_ALREADY_SUBMITTED", "Feedback has already been submitted");
            return save(ctx, CustomerSubmissionType.FEEDBACK, key, hash, null, comment, command.rating(), null, null);
        });
    }
    @Override public Submission submitRequest(String token, String ip, String key, RequestCommand command) {
        return authorized(token, ip, SelfServiceAction.REDELIVERY_REQUEST_SUBMIT, true, ctx -> {
            CustomerSubmissionType type;
            if (ctx.order().status() == DeliveryStatus.FAILED_ATTEMPT && latestDisposition(ctx.order().id().value()) == DeliveryFailureDisposition.REDELIVERY_ELIGIBLE) {
                type = CustomerSubmissionType.REDELIVERY_REQUEST;
            } else if (ctx.order().status() == DeliveryStatus.READY_FOR_ASSIGNMENT) {
                type = CustomerSubmissionType.DELIVERY_PREFERENCE;
            } else throw new ConflictException("SELF_SERVICE_REQUEST_NOT_AVAILABLE", "A delivery request is not available now");
            validateWindow(command.preferredStartAt(), command.preferredEndAt());
            return submit(ctx, type, key, null, optionalText(command.notes(), 1000), null,
                    command.preferredStartAt(), command.preferredEndAt());
        });
    }

    private Projection projection(Context ctx) {
        DeliveryOrder order = ctx.order();
        String status = status(order);
        SingleOrderEtaEstimate estimate = null;
        try { estimate = eta.getOrderEta(order.id().value()); } catch (RuntimeException ignored) { /* unavailable is safe */ }
        String freshness = estimate == null ? "UNAVAILABLE" : estimate.isStale(now()) ? "STALE" : "CURRENT";
        var destination = locations.findLocation(order.destinationLocationId()).map(DeliveryLocationLookupPort.LocationReference::name).orElse("Destination");
        var pod = proofs.findByDeliveryOrderId(order.id().value());
        var submissionViews = submissions.findByDelivery(order.id().value(), order.customerId()).stream()
                .map(CustomerSelfServiceService::view).toList();
        List<String> available = new ArrayList<>();
        if (ctx.access().allowedActions().contains(SelfServiceAction.TRACK)) available.add("TRACK");
        if (ctx.access().allowedActions().contains(SelfServiceAction.PREFERENCE_READ)) available.add("PREFERENCES");
        if (ctx.access().allowedActions().contains(SelfServiceAction.ISSUE_SUBMIT)) available.add("REPORT_ISSUE");
        if (ctx.access().allowedActions().contains(SelfServiceAction.FEEDBACK_SUBMIT)
                && order.status() == DeliveryStatus.DELIVERED
                && !submissions.feedbackExists(order.id().value(), order.customerId())) available.add("FEEDBACK");
        if (ctx.access().allowedActions().contains(SelfServiceAction.REDELIVERY_REQUEST_SUBMIT)
                && (order.status() == DeliveryStatus.FAILED_ATTEMPT
                || order.status() == DeliveryStatus.READY_FOR_ASSIGNMENT)) available.add("DELIVERY_REQUEST");
        return new Projection(order.deliveryNumber().value(), status, explanation(status), order.window().start(),
                order.window().end(), currentTenant.currentTenant().map(DeliveryTenantContextPort.TenantContext::timeZone).orElse("UTC"),
                estimate == null ? null : estimate.estimatedArrivalAt(), estimate == null ? null : estimate.calculatedAt(),
                freshness, List.copyOf(available), destination, pod.isPresent() ? "AVAILABLE" : "NOT_AVAILABLE",
                pod.map(ProofOfDelivery::acceptedAt).orElse(null), preferenceView(order.customerId()), submissionViews,
                submissionViews.stream().filter(s -> s.type().contains("REQUEST") || s.type().contains("PREFERENCE")).findFirst().map(Submission::status).orElse(null));
    }

    private <T> T authorized(String raw, String ip, SelfServiceAction action, boolean write,
                             java.util.function.Function<Context, T> work) {
        String hash;
        try { hash = hashPresented(raw); } catch (RuntimeException exception) { invalidAttempt(ip); throw invalid(); }
        DeliverySelfServiceAccess bootstrap = access.findBootstrapByTokenHash(hash).orElse(null);
        if (bootstrap == null) { invalidAttempt(ip); throw invalid(); }
        limit(write ? writeLimits : readLimits, hash, write ? 10 : 120, Duration.ofMinutes(15));
        return tenantExecutor.within(bootstrap.tenantId(), () -> transactions.execute(() -> {
            DeliveryOrder order = (write ? orders.findByIdForUpdate(bootstrap.deliveryOrderId())
                    : orders.findById(bootstrap.deliveryOrderId())).orElseThrow(() -> invalid());
            DeliverySelfServiceAccess scoped = (write ? access.findByTokenHashForUpdate(hash)
                    : access.findByTokenHash(hash)).orElseThrow(() -> invalid());
            OffsetDateTime now = now();
            if (!scoped.permits(action, now) || !constantEquals(scoped.tokenHash(), hash)) throw invalid();
            if (!order.id().value().equals(scoped.deliveryOrderId()) || !order.customerId().equals(scoped.customerId())) {
                access.revoke(scoped.id(), now, "CUSTOMER_ASSOCIATION_CHANGED"); throw invalid();
            }
            var customer = customers.find(scoped.customerId()).filter(CustomerNotificationContactLookup.CustomerNotificationContact::active).orElse(null);
            if (customer == null || !contactMatches(scoped, customer)) { access.revoke(scoped.id(), now, "CUSTOMER_CONTACT_INVALID"); throw invalid(); }
            if (write && submissions.countRecent(order.id().value(), now.minusHours(1)) >= 20)
                throw new TooManyRequestsException("SELF_SERVICE_RATE_LIMITED", "Request rate limit exceeded");
            if (!access.markUsed(scoped.id(), now)) throw invalid();
            return work.apply(new Context(scoped, order, scoped.customerId()));
        }));
    }

    private Submission submit(Context ctx, CustomerSubmissionType type, String key, String category, String description,
                              Integer rating, OffsetDateTime start, OffsetDateTime end) {
        String canonical = type + "|" + Objects.toString(category, "") + "|" + Objects.toString(description, "")
                + "|" + Objects.toString(rating, "") + "|" + Objects.toString(start, "") + "|" + Objects.toString(end, "");
        String hash = requestHash(canonical);
        DeliveryCustomerSubmission prior = idempotent(ctx, type, key, hash);
        return prior == null ? save(ctx, type, key, hash, category, description, rating, start, end) : view(prior);
    }
    private DeliveryCustomerSubmission idempotent(Context ctx, CustomerSubmissionType type, String key, String hash) {
        validateIdempotency(key);
        DeliveryCustomerSubmission prior = submissions.findIdempotent(ctx.access().id(), type, key).orElse(null);
        if (prior != null && !constantEquals(prior.requestHash(), hash))
            throw new ConflictException("SELF_SERVICE_IDEMPOTENCY_CONFLICT", "Idempotency key was already used for a different request");
        return prior;
    }
    private Submission save(Context ctx, CustomerSubmissionType type, String key, String hash, String category,
                            String description, Integer rating, OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime now = now();
        DeliveryCustomerSubmission saved = submissions.save(new DeliveryCustomerSubmission(UUID.randomUUID(), ctx.access().tenantId(),
                ctx.order().id().value(), ctx.customerId(), ctx.access().id(), type, category, description, rating,
                start, end, type == CustomerSubmissionType.FEEDBACK ? "RECORDED" : "SUBMITTED",
                key, hash, now, now, 0));
        if (!constantEquals(saved.requestHash(), hash)) {
            throw new ConflictException("SELF_SERVICE_IDEMPOTENCY_CONFLICT",
                    "Idempotency key was already used for a different request");
        }
        return view(saved);
    }
    private Preferences preferenceView(UUID customerId) {
        try {
            return map(preferences.get(customerId));
        } catch (NotFoundException exception) {
            throw invalid();
        }
    }
    private static Preferences map(CustomerOperationalPreferenceManagement.PreferenceView value) {
        return new Preferences(value.emailEnabled(), value.smsEnabled(), value.maskedEmail(), value.maskedPhone(),
                value.explicitProfile(), value.version());
    }
    private static Submission view(DeliveryCustomerSubmission value) {
        return new Submission("CSR-" + value.id().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                value.type().name(), value.status(), value.createdAt());
    }
    private DeliveryFailureDisposition latestDisposition(UUID deliveryId) {
        List<DeliveryAttempt> values = attempts.findByDeliveryId(deliveryId);
        return values.isEmpty() ? null : values.get(values.size() - 1).disposition();
    }
    private void validateWindow(OffsetDateTime start, OffsetDateTime end) {
        if ((start == null) != (end == null)) throw new IllegalArgumentException("Preferred window start and end are required together");
        if (start == null) return;
        if (!end.isAfter(start) || Duration.between(start, end).compareTo(Duration.ofMinutes(30)) < 0)
            throw new IllegalArgumentException("Preferred window must be at least 30 minutes");
        if (start.isBefore(now()) || end.isAfter(now().plusDays(30))) throw new IllegalArgumentException("Preferred window must be within 30 days");
    }
    private boolean contactMatches(DeliverySelfServiceAccess value, CustomerNotificationContactLookup.CustomerNotificationContact contact) {
        if (!keyVersion.equals(value.contactHashKeyVersion())) return false;
        return (contact.email() != null && constantEquals(value.recipientContactHash(), hmac(normalizeContact(contact.email()))))
                || (contact.phone() != null && constantEquals(value.recipientContactHash(), hmac(normalizeContact(contact.phone()))));
    }
    private String hmac(String value) {
        try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException exception) { throw new IllegalStateException("HMAC-SHA-256 unavailable", exception); }
    }
    private String token() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String hashPresented(String raw) { if (raw == null || raw.length() != 43) throw invalid(); return sha256(Base64.getUrlDecoder().decode(raw)); }
    private static String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
    private static String requestHash(String value) { return sha256(value.getBytes(StandardCharsets.UTF_8)); }
    private static boolean constantEquals(String left, String right) { return MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII), right.getBytes(StandardCharsets.US_ASCII)); }
    private static String normalizeContact(String value) { String result = required(value, "Recipient contact"); return result.contains("@") ? result.toLowerCase(Locale.ROOT) : result.replaceAll("[\\s()-]", ""); }
    private static Set<SelfServiceAction> parseActions(Set<String> values) { if (values == null || values.isEmpty()) return ALL_ACTIONS; try { return values.stream().map(SelfServiceAction::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet()); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Unsupported self-service action", e); } }
    private static String validateIdempotency(String value) { String result = required(value, "Idempotency key"); if (result.length() < 16 || result.length() > 128) throw new IllegalArgumentException("Idempotency key must contain 16 to 128 characters"); return result; }
    private static String required(String value, String label) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required"); return value.trim(); }
    private static String text(String value, int min, int max, String label) { String result = required(value, label); if (result.length() < min || result.length() > max) throw new IllegalArgumentException(label + " must contain " + min + " to " + max + " characters"); return result; }
    private static String optionalText(String value, int max) { if (value == null || value.isBlank()) return null; String result = value.trim(); if (result.length() > max) throw new IllegalArgumentException("Text is too long"); return result; }
    private String status(DeliveryOrder order) {
        if (order.status() == DeliveryStatus.READY_FOR_ASSIGNMENT) {
            var membership = batches.findActiveMembershipByDeliveryOrderId(currentTenant.currentTenantId().orElseThrow(), order.id().value());
            if (membership.flatMap(value -> batches.findById(currentTenant.currentTenantId().orElseThrow(), value.batchId()))
                    .filter(batch -> batch.status() == DeliveryBatchStatus.DISPATCHED).isPresent()) return "Out for delivery";
        }
        return switch (order.status()) { case DRAFT -> "Preparing delivery"; case READY_FOR_ASSIGNMENT -> "Scheduled";
            case FAILED_ATTEMPT -> "Delivery attempt unsuccessful"; case ESCALATED -> "We are reviewing your delivery";
            case RETURN_TO_BASE -> "Returned for assistance"; case DELIVERED -> "Delivered"; };
    }
    private static String explanation(String status) { return switch (status) { case "Scheduled" -> "Your delivery is scheduled within the shown window."; case "Delivered" -> "Your delivery has been completed."; case "Delivery attempt unsuccessful" -> "A new delivery request may be available."; default -> "This is the latest customer-safe delivery update."; }; }
    private void invalidAttempt(String ip) { limit(invalidLimits, Objects.toString(ip, "unknown"), 20, Duration.ofMinutes(15), false); }
    private void limit(Map<String, ArrayDeque<Instant>> limits, String key, int maximum, Duration window) { limit(limits, key, maximum, window, true); }
    private void limit(Map<String, ArrayDeque<Instant>> limits, String key, int maximum, Duration window, boolean reveal) {
        try { synchronized (limits) {
            Instant threshold = clock.instant().minus(window);
            ArrayDeque<Instant> hits = limits.get(key);
            if (hits == null) {
                if (limits.size() >= MAX_RATE_LIMIT_KEYS) {
                    limits.values().forEach(value -> { while (!value.isEmpty() && value.peekFirst().isBefore(threshold)) value.removeFirst(); });
                    limits.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                }
                if (limits.size() >= MAX_RATE_LIMIT_KEYS) {
                    if (reveal) throw new TooManyRequestsException("SELF_SERVICE_RATE_LIMITED", "Request rate limit exceeded");
                    throw invalid();
                }
                hits = new ArrayDeque<>();
                limits.put(key, hits);
            }
            while (!hits.isEmpty() && hits.peekFirst().isBefore(threshold)) hits.removeFirst();
            if (hits.size() >= maximum) { if (reveal) throw new TooManyRequestsException("SELF_SERVICE_RATE_LIMITED", "Request rate limit exceeded"); throw invalid(); }
            hits.addLast(clock.instant());
        } }
        catch (NotFoundException | TooManyRequestsException exception) { throw exception; }
        catch (RuntimeException exception) { if (reveal) throw new TooManyRequestsException("SELF_SERVICE_RATE_LIMITED", "Request rate limit unavailable"); throw invalid(); }
    }
    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private static NotFoundException invalid() { return new NotFoundException("SELF_SERVICE_ACCESS_INVALID", "Self-service access is invalid"); }
    private record Context(DeliverySelfServiceAccess access, DeliveryOrder order, UUID customerId) {}
}
