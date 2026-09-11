package com.transportlogistics.app.tracking.adapters.inbound.provider;

import com.transportlogistics.app.tracking.application.provider.NormalizedPositionCandidate;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome;
import com.transportlogistics.app.tracking.application.provider.ProviderIngestionOutcome.Result;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderIngestionPort;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.PositionCommand;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.ProviderContext;
import com.transportlogistics.app.tracking.ports.outbound.TrackingProviderExecutionStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
final class JdbcTrackingProviderIngestionAdapter implements TrackingProviderIngestionPort {
    private static final int MAX_CANDIDATES = 500;
    private final TrackingProviderExecutionStore executions;
    private final TrackingUseCase tracking;
    private final TransactionTemplate transactions;

    JdbcTrackingProviderIngestionAdapter(
            TrackingProviderExecutionStore executions,
            TrackingUseCase tracking,
            TransactionTemplate transactions) {
        this.executions = executions;
        this.tracking = tracking;
        this.transactions = transactions;
    }

    @Override
    public List<ProviderIngestionOutcome> ingest(
            ProviderConnectionId connectionId,
            String leaseOwner,
            List<NormalizedPositionCandidate> candidates,
            Instant receivedAt) {
        if (candidates == null || candidates.isEmpty() || candidates.size() > MAX_CANDIDATES) {
            throw new IllegalArgumentException("Internal provider batch must contain 1..500 candidates");
        }
        var connection = executions.reloadActive(connectionId, leaseOwner, receivedAt);
        if (connection.isEmpty()) {
            return candidates.stream().map(candidate -> rejected(candidate, null, -1)).toList();
        }
        List<ProviderIngestionOutcome> outcomes = new ArrayList<>(candidates.size());
        for (NormalizedPositionCandidate candidate : candidates) {
            ProviderIngestionOutcome outcome = transactions.execute(status -> {
                var binding = executions.lockActiveBindingForIngestion(
                        connection.get().tenantId(), connectionId, candidate.externalDeviceReference(),
                        leaseOwner, receivedAt);
                if (binding.isEmpty()) {
                    return rejected(candidate, null, -1);
                }
                try {
                    var result = tracking.ingest(
                            new ProviderContext(connection.get().tenantId(),
                                    connection.get().providerAlias()),
                            List.of(command(binding.get().trackingDeviceId(), candidate)),
                            receivedAt).get(0);
                    return new ProviderIngestionOutcome(
                            binding.get().id(), binding.get().version(), candidate.sourceTimestamp(),
                            candidate.providerMessageId(),
                            result.duplicate() ? Result.DUPLICATE : Result.ACCEPTED);
                } catch (RuntimeException exception) {
                    status.setRollbackOnly();
                    return rejected(candidate, binding.get().id(), binding.get().version());
                }
            });
            outcomes.add(outcome);
        }
        return List.copyOf(outcomes);
    }

    private static PositionCommand command(
            java.util.UUID deviceId, NormalizedPositionCandidate candidate) {
        return new PositionCommand(
                deviceId, candidate.providerMessageId(), candidate.providerSequence(),
                candidate.sourceTimestamp(), candidate.latitude(), candidate.longitude(),
                candidate.horizontalAccuracyMeters(), candidate.speedKph(),
                candidate.headingDegrees(), candidate.altitudeMeters(), candidate.engineState(),
                candidate.odometerKm(), candidate.engineHours(),
                Map.of("source", "provider-coordinator"));
    }

    private static ProviderIngestionOutcome rejected(
            NormalizedPositionCandidate candidate, java.util.UUID bindingId, long bindingVersion) {
        return new ProviderIngestionOutcome(
                bindingId == null ? new java.util.UUID(0, 0) : bindingId,
                bindingVersion,
                candidate.sourceTimestamp(),
                candidate.providerMessageId(),
                Result.REJECTED);
    }
}
