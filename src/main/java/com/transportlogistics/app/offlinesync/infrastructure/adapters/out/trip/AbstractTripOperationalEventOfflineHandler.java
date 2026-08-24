package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.trip;

import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome;
import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncConflictException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.TripOperationalEventRecorder;

import java.util.Set;
import java.util.function.Supplier;

abstract class AbstractTripOperationalEventOfflineHandler implements OfflineOperationHandler {
    private static final Set<String> AUTHORITIES = Set.of("TRIP_DISPATCH", "TRIP_LOG_MANAGE", "TRIP_UPDATE");
    protected final TripOperationalEventRecorder events;

    AbstractTripOperationalEventOfflineHandler(TripOperationalEventRecorder events) {
        this.events = events;
    }

    @Override
    public final int operationVersion() {
        return 1;
    }

    @Override
    public final Set<String> requiredAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public final boolean isAuthorized(Set<String> currentAuthorities) {
        return currentAuthorities.stream().anyMatch(AUTHORITIES::contains);
    }

    protected OfflineHandlerOutcome invoke(Supplier<TripOperationalEventRecorder.Result> operation) {
        try {
            operation.get();
            return OfflineHandlerOutcome.applied();
        } catch (NotFoundException exception) {
            return OfflineHandlerOutcome.rejected(exception.code(), exception.getMessage());
        } catch (ConflictException | BusinessRuleException exception) {
            throw new OfflineSyncConflictException(exception.getMessage());
        }
    }
}
