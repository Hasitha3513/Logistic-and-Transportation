package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.transportlogistics.app.fleet.VehiclePowertrainEligibilityQuery;
import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import com.transportlogistics.app.tracking.domain.idle.IdleCandidate;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.IdleCandidatePersistencePort;
import com.transportlogistics.app.tracking.ports.outbound.IdlePersistencePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdleEvaluationServiceTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void promotesOnlyAfterFiveContinuousMinutesAndPreservesCandidateIdentity() {
        Fixture fixture = new Fixture(VehiclePowertrainEligibilityQuery.Classification.COMBUSTION);
        fixture.evaluate(fact(0, "a", "0", "0", "2"));
        fixture.evaluate(fact(100, "b", "0.00001", "0", "2"));
        fixture.evaluate(fact(200, "c", "0.00002", "0", "2"));
        fixture.evaluate(fact(300, "d", "0.00003", "0", "2"));

        assertThat(fixture.candidates.promoted).isNotNull();
        assertThat(fixture.candidates.promoted.creditedSeconds()).isEqualTo(300);
        assertThat(fixture.candidates.promoted.evidenceCount()).isEqualTo(4);
        assertThat(fixture.candidates.promoted.candidateId()).isNotNull();
    }

    @Test
    void evidenceGapDiscardsCandidateWithoutPromotion() {
        Fixture fixture = new Fixture(VehiclePowertrainEligibilityQuery.Classification.HYBRID);
        fixture.evaluate(fact(0, "a", "0", "0", "2"));
        fixture.evaluate(fact(121, "b", "0", "0", "2"));

        assertThat(fixture.candidates.current).isNull();
        assertThat(fixture.candidates.promoted).isNull();
        assertThat(fixture.candidates.discards).isOne();
    }

    @Test
    void unknownProductionPowertrainCannotStartCandidate() {
        Fixture fixture = new Fixture(VehiclePowertrainEligibilityQuery.Classification.UNKNOWN);
        fixture.evaluate(fact(0, "a", "0", "0", "0"));
        assertThat(fixture.candidates.current).isNull();
        assertThat(fixture.candidates.promoted).isNull();
    }

    @Test
    void overAccuracyAndMovementThresholdsDoNotQualify() {
        Fixture inaccurate = new Fixture(VehiclePowertrainEligibilityQuery.Classification.COMBUSTION);
        inaccurate.evaluate(fact(0, "a", "0", "0", "101"));
        assertThat(inaccurate.candidates.current).isNull();

        Fixture moving = new Fixture(VehiclePowertrainEligibilityQuery.Classification.COMBUSTION);
        moving.evaluate(fact(0, "b", "0", "0", "1"));
        moving.evaluate(fact(60, "c", "0.001", "0", "1"));
        assertThat(moving.candidates.current).isNull();
        assertThat(moving.candidates.discards).isOne();
    }

    @Test
    void deviceReassignmentDiscardsCandidateWithoutUsingNewDeviceEvidence() {
        Fixture fixture = new Fixture(VehiclePowertrainEligibilityQuery.Classification.COMBUSTION);
        fixture.evaluate(fact(0, "a", "0", "0", "2"));
        HistoricalTelemetry reassigned = fact(60, "b", "0", "0", "2");
        reassigned = new HistoricalTelemetry(reassigned.eventId(), reassigned.eventVersion(),
                reassigned.tenantId(), reassigned.vehicleId(), UUID.randomUUID(),
                reassigned.providerAlias(), reassigned.providerMessageId(), reassigned.dedupeIdentity(),
                reassigned.latitude(), reassigned.longitude(), reassigned.speedKph(),
                reassigned.headingDegrees(), reassigned.horizontalAccuracyMeters(),
                reassigned.altitudeMeters(), reassigned.engineState(), reassigned.odometerKm(),
                reassigned.engineHours(), reassigned.recordedAt(), reassigned.receivedAt(),
                reassigned.trust(), reassigned.quality(), reassigned.ordering(),
                reassigned.tamperState(), reassigned.batteryLevelPercent(),
                reassigned.batteryVoltageVolts(), reassigned.externalPowerState(),
                reassigned.batteryChargingState(), reassigned.ignitionState(),
                reassigned.engineRunningState(), reassigned.engineRunningSource());

        fixture.evaluate(reassigned);

        assertThat(fixture.candidates.current).isNull();
        assertThat(fixture.candidates.discards).isOne();
        assertThat(fixture.candidates.promoted).isNull();
    }

    private static HistoricalTelemetry fact(long seconds, String identitySuffix,
            String latitude, String longitude, String accuracy) {
        return new HistoricalTelemetry(UUID.randomUUID(), 3, Fixture.TENANT, Fixture.VEHICLE,
                Fixture.DEVICE, "TEST", "message-" + identitySuffix,
                identitySuffix.repeat(64), new BigDecimal(latitude), new BigDecimal(longitude),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(accuracy), null, EngineState.ON,
                null, null, START.plusSeconds(seconds), START.plusSeconds(seconds), Trust.TRUSTED,
                "GOOD", Ordering.IN_ORDER, null, null, null, null, null,
                TrackingTelemetryIngestedV3.IgnitionState.ON,
                TrackingTelemetryIngestedV3.EngineRunningState.RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_CAN);
    }

    private static final class Fixture {
        static final UUID TENANT = UUID.randomUUID();
        static final UUID VEHICLE = UUID.randomUUID();
        static final UUID DEVICE = UUID.randomUUID();
        final Map<UUID, HistoricalTelemetry> history = new HashMap<>();
        final Candidates candidates = new Candidates();
        final IdleEvaluationService service;

        Fixture(VehiclePowertrainEligibilityQuery.Classification classification) {
            HistoricalTelemetryLookupPort lookup = (tenant, timestamp, id) ->
                    Optional.ofNullable(history.get(id));
            IdlePersistencePort idle = new IdlePersistencePort() {
                @Override public Optional<IdlePersistenceModels.State> findState(UUID tenant, UUID vehicle) {
                    return Optional.empty();
                }
                @Override public Optional<IdlePersistenceModels.Episode> findEpisode(UUID tenant, UUID episode) {
                    return Optional.empty();
                }
                @Override public IdlePersistenceModels.PersistResult persist(
                        IdlePersistenceModels.Mutation mutation) {
                    return IdlePersistenceModels.PersistResult.APPLIED;
                }
            };
            service = new IdleEvaluationService((tenant, vehicle, at) -> classification,
                    (tenant, device, capability, at) -> TelemetryCapabilityState.SUPPORTED,
                    lookup, candidates, idle);
        }

        void evaluate(HistoricalTelemetry fact) {
            history.put(fact.eventId(), fact);
            service.evaluate(fact, fact.receivedAt());
        }
    }

    private static final class Candidates implements IdleCandidatePersistencePort {
        IdleCandidate current;
        IdleCandidate promoted;
        int discards;
        @Override public Optional<IdleCandidate> find(UUID tenant, UUID vehicle) {
            return Optional.ofNullable(current);
        }
        @Override public Result save(IdleCandidate candidate, IdleCandidate.Evidence evidence,
                long expectedVersion) {
            current = candidate;
            return Result.APPLIED;
        }
        @Override public void discard(UUID tenant, UUID vehicle, UUID candidate, long expectedVersion) {
            current = null;
            discards++;
        }
        @Override public UUID promote(IdleCandidate candidate, IdleCandidate.Evidence evidence,
                Instant confirmedAt, long expectedVersion) {
            promoted = candidate;
            current = null;
            return UUID.randomUUID();
        }
        @Override public int purgeExpired(Instant now, int limit) { return 0; }
    }
}
