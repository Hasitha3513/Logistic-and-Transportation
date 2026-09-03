package com.transportlogistics.app.delivery.ports.inbound;

import java.time.OffsetDateTime;
import java.util.List;

public interface CustomerSelfServiceUseCase {
    Projection track(String token, String sourceIp);
    Preferences preferences(String token, String sourceIp);
    Preferences replacePreferences(String token, String sourceIp, PreferenceCommand command);
    Submission submitIssue(String token, String sourceIp, String idempotencyKey, IssueCommand command);
    Submission submitFeedback(String token, String sourceIp, String idempotencyKey, FeedbackCommand command);
    Submission submitRequest(String token, String sourceIp, String idempotencyKey, RequestCommand command);

    record Projection(String deliveryNumber, String status, String explanation, OffsetDateTime scheduledStart,
                      OffsetDateTime scheduledEnd, String timeZone, OffsetDateTime estimatedArrivalAt,
                      OffsetDateTime etaCalculatedAt, String etaFreshness, List<String> availableActions,
                      String destination, String podAvailability, OffsetDateTime completedAt,
                      Preferences notificationPreferences, List<Submission> submissions, String requestState) {}
    record Preferences(boolean emailEnabled, boolean smsEnabled, String maskedEmail, String maskedPhone,
                       boolean explicitProfile, Long version) {}
    record PreferenceCommand(boolean emailEnabled, boolean smsEnabled, Long version) {}
    record IssueCommand(String category, String description) {}
    record FeedbackCommand(int rating, String comment) {}
    record RequestCommand(OffsetDateTime preferredStartAt, OffsetDateTime preferredEndAt, String notes) {}
    record Submission(String reference, String type, String status, OffsetDateTime createdAt) {}
}
