package com.transportlogistics.app.notification;

import java.util.Set;
import java.util.UUID;

/** Provider-send-time extension contract; implementations must never persist returned URLs. */
public interface FinalSendCustomerLinkIssuer {
    IssuedLink issue(IssueRequest request);
    record IssueRequest(UUID deliveryOrderId, String recipientContact, Set<String> allowedActions,
                        String issuanceIdempotencyKey) {}
    record IssuedLink(String url) {}
}
