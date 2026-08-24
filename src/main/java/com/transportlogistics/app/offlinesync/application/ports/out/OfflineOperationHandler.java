package com.transportlogistics.app.offlinesync.application.ports.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;

import java.util.Set;

public interface OfflineOperationHandler {
    String operationType();

    int operationVersion();

    Set<String> requiredAuthorities();

    default boolean isAuthorized(Set<String> currentAuthorities) {
        return currentAuthorities.containsAll(requiredAuthorities());
    }

    OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload);
}
