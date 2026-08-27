package com.transportlogistics.app.offlinesync.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OfflineOperationHandlerRegistryTest {
    @Test
    void duplicateTypeAndVersionFailsFast() {
        OfflineOperationHandler first = handler();
        OfflineOperationHandler second = handler();

        assertThrows(IllegalStateException.class,
                () -> new OfflineOperationHandlerRegistry(List.of(first, second)));
    }

    private OfflineOperationHandler handler() {
        return new OfflineOperationHandler() {
            @Override public String operationType() { return "VEHICLE_READING_RECORD"; }
            @Override public int operationVersion() { return 1; }
            @Override public Set<String> requiredAuthorities() { return Set.of("VEHICLE_READING_CREATE"); }
            @Override public OfflineHandlerOutcome apply(OfflineOperationContext context, JsonNode payload) {
                return OfflineHandlerOutcome.applied();
            }
        };
    }
}
