package com.transportlogistics.app.notification.support;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;

import java.util.ArrayList;
import java.util.List;

public final class DeterministicEmailNotificationSender implements EmailNotificationSenderPort {
    public enum Scenario { SUCCESS, RETRYABLE_FAILURE, NON_RETRYABLE_FAILURE, FAIL_ONCE_THEN_SUCCESS,
        FAIL_TWICE_THEN_SUCCESS, ALWAYS_RETRYABLE_FAILURE }

    private final Scenario scenario;
    private final List<SendRequest> requests = new ArrayList<>();

    public DeterministicEmailNotificationSender(Scenario scenario) { this.scenario = scenario; }

    public SendResult send(SendRequest request) {
        requests.add(request);
        int call = requests.size();
        boolean fail = switch (scenario) {
            case SUCCESS -> false;
            case RETRYABLE_FAILURE, ALWAYS_RETRYABLE_FAILURE -> true;
            case NON_RETRYABLE_FAILURE -> true;
            case FAIL_ONCE_THEN_SUCCESS -> call == 1;
            case FAIL_TWICE_THEN_SUCCESS -> call <= 2;
        };
        if (!fail) return SendResult.accepted("provider-" + call);
        if (scenario == Scenario.NON_RETRYABLE_FAILURE) {
            return SendResult.rejected(EmailDeliveryErrorCategory.INVALID_RECIPIENT, "INVALID_TO", "Recipient rejected");
        }
        return SendResult.rejected(EmailDeliveryErrorCategory.TIMEOUT, "TIMEOUT", "Provider timed out");
    }

    public List<SendRequest> requests() { return List.copyOf(requests); }
}
