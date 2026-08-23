package com.transportlogistics.app.offlinesync.application.service;

import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OfflineOperationHandlerRegistry {
    private final Map<HandlerKey, OfflineOperationHandler> handlers;

    public OfflineOperationHandlerRegistry(List<OfflineOperationHandler> handlers) {
        Map<HandlerKey, OfflineOperationHandler> registrations = new HashMap<>();
        for (OfflineOperationHandler handler : handlers) {
            HandlerKey key = new HandlerKey(handler.operationType(), handler.operationVersion());
            OfflineOperationHandler previous = registrations.putIfAbsent(key, handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate offline operation handler for "
                        + key.operationType() + " version " + key.operationVersion());
            }
        }
        this.handlers = Map.copyOf(registrations);
    }

    public Optional<OfflineOperationHandler> find(String operationType, int operationVersion) {
        return Optional.ofNullable(handlers.get(new HandlerKey(operationType, operationVersion)));
    }

    private record HandlerKey(String operationType, int operationVersion) {
    }
}
