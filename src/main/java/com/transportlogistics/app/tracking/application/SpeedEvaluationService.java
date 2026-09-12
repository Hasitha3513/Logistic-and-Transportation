package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold;
import com.transportlogistics.app.tracking.domain.speed.SpeedAttribution;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationResult;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.inbound.SpeedEvaluationJobUseCase;
import com.transportlogistics.app.tracking.ports.inbound.SpeedEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.SpeedAttributionLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationJobRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationTransactionPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedPositionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodePublisherPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleSpeedStateRepositoryPort;
import java.time.Instant;
import java.util.Optional;

public final class SpeedEvaluationService implements SpeedEvaluationUseCase, SpeedEvaluationJobUseCase {
    private final SpeedPositionRepositoryPort positions;
    private final SpeedRuleRepositoryPort rules;
    private final VehicleSpeedStateRepositoryPort states;
    private final SpeedingEpisodeRepositoryPort episodes;
    private final SpeedEvaluationJobRepositoryPort jobs;
    private final SpeedAttributionLookupPort attributionLookup;
    private final SpeedingEpisodePublisherPort publisher;
    private final SpeedEvaluationTransactionPort transactions;

    public SpeedEvaluationService(SpeedPositionRepositoryPort positions,
                                  SpeedRuleRepositoryPort rules,
                                  VehicleSpeedStateRepositoryPort states,
                                  SpeedingEpisodeRepositoryPort episodes,
                                  SpeedEvaluationJobRepositoryPort jobs,
                                  SpeedAttributionLookupPort attributionLookup,
                                  SpeedingEpisodePublisherPort publisher,
                                  SpeedEvaluationTransactionPort transactions) {
        this.positions = positions;
        this.rules = rules;
        this.states = states;
        this.episodes = episodes;
        this.jobs = jobs;
        this.attributionLookup = attributionLookup;
        this.publisher = publisher;
        this.transactions = transactions;
    }

    @Override
    public SpeedEvaluationResult evaluate(SpeedPosition position, Instant evaluatedAt) {
        return transactions.execute(() -> evaluateInTransaction(position, evaluatedAt));
    }

    @Override
    public Optional<SpeedEvaluationResult> process(
            SpeedEvaluationJob job, String leaseOwner, Instant evaluatedAt) {
        return transactions.execute(() -> {
            Optional<SpeedPosition> position = positions.find(job.tenantId(), job.positionId());
            Optional<SpeedEvaluationResult> result = position.map(value ->
                    evaluateInTransaction(value, evaluatedAt));
            jobs.complete(job.tenantId(), job.positionId(), leaseOwner, evaluatedAt);
            return result;
        });
    }

    private SpeedEvaluationResult evaluateInTransaction(SpeedPosition position, Instant evaluatedAt) {
        VehicleSpeedState state = states.findForUpdate(position.tenantId(), position.vehicleId())
                .orElseGet(() -> states.save(
                        VehicleSpeedState.unknown(position.tenantId(), position.vehicleId()), 0));
        SpeedAttribution attribution = attribution(position);
        Optional<ResolvedSpeedThreshold> threshold = threshold(position, attribution);
        SpeedingEpisode active = episodes.findActive(position.tenantId(), position.vehicleId()).orElse(null);
        if (active != null && state.monitoringState() == VehicleSpeedState.MonitoringState.SPEEDING) {
            threshold = Optional.of(frozenThreshold(active));
        }
        SpeedingEpisode previous = threshold.flatMap(value -> episodes.findLatestClosed(
                position.tenantId(), position.vehicleId(), value.rule().id(),
                value.rule().ruleVersion())).orElse(null);
        SpeedEvaluationResult result = state.evaluate(
                position, threshold, attribution, evaluatedAt, active, previous);
        if (!result.state().equals(state)) {
            result.episode().ifPresent(episodes::save);
            states.save(result.state(), state.version());
            if (result.outcome() == SpeedEvaluationResult.Outcome.EPISODE_CONFIRMED) {
                publisher.publish(position.tenantId(), event(result.episode().orElseThrow(), position));
            }
        }
        return result;
    }

    private SpeedAttribution attribution(SpeedPosition position) {
        try {
            return attributionLookup.findAt(position.tenantId(), position.vehicleId(),
                    position.sourceTimestamp()).orElseGet(SpeedAttribution::unknown);
        } catch (RuntimeException exception) {
            return SpeedAttribution.unknown();
        }
    }

    private Optional<ResolvedSpeedThreshold> threshold(
            SpeedPosition position, SpeedAttribution attribution) {
        if (attribution.routeId() != null && attribution.routeVersion() != null) {
            Optional<SpeedRule> route = rules.findActiveRouteRule(position.tenantId(),
                    attribution.routeId(), attribution.routeVersion());
            if (route.isPresent()) {
                return route.map(value -> new ResolvedSpeedThreshold(value,
                        ResolvedSpeedThreshold.ThresholdSource.ROUTE_CONFIG));
            }
        }
        return rules.findActiveTenantRule(position.tenantId()).map(value ->
                new ResolvedSpeedThreshold(value,
                        ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG));
    }

    private static ResolvedSpeedThreshold frozenThreshold(SpeedingEpisode episode) {
        SpeedRule.Scope scope = episode.thresholdSource()
                == ResolvedSpeedThreshold.ThresholdSource.ROUTE_CONFIG
                ? SpeedRule.Scope.ROUTE_VERSION : SpeedRule.Scope.TENANT;
        SpeedRule rule = new SpeedRule(episode.ruleId(), episode.tenantId(), "Episode snapshot",
                scope, scope == SpeedRule.Scope.ROUTE_VERSION ? episode.attribution().routeId() : null,
                scope == SpeedRule.Scope.ROUTE_VERSION ? episode.attribution().routeVersion() : null,
                episode.effectiveThresholdKph(), SpeedRule.Lifecycle.ACTIVE,
                episode.ruleVersion(), episode.confirmationSourceTimestamp());
        return new ResolvedSpeedThreshold(rule, episode.thresholdSource());
    }

    private static SpeedingEpisodePublisherPort.VehicleSpeedingDetectedV1 event(
            SpeedingEpisode episode, SpeedPosition confirming) {
        return new SpeedingEpisodePublisherPort.VehicleSpeedingDetectedV1(
                episode.id(), episode.vehicleId(), episode.attribution().driverId(),
                episode.attribution().tripId(), episode.attribution().routeId(),
                episode.attribution().routeVersion(), confirming.speedKph(),
                episode.effectiveThresholdKph(), episode.thresholdSource(), episode.ruleId(),
                episode.ruleVersion(), episode.severity(), episode.confirmationSourceTimestamp(),
                episode.repeatCount());
    }
}
