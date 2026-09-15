package com.transportlogistics.app.trip.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.trip.TripReplayQuery;
import com.transportlogistics.app.trip.TripReplayQuery.TripReplayScope;
import com.transportlogistics.app.trip.TripReplayQuery.VehicleTripAssignmentInterval;
import com.transportlogistics.app.trip.TripReplayQueryException;
import com.transportlogistics.app.trip.application.ports.out.TripReplayRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripReplayQueryServiceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant END = START.plusSeconds(7 * 86_400L);
    private final TripReplayRepository repository = mock(TripReplayRepository.class);
    private final TripReplayQueryService service = new TripReplayQueryService(repository);

    @Test
    void delegatesTenantQualifiedReplayScopeAndPreservesSafeAbsence() {
        UUID trip = UUID.randomUUID();
        TripReplayScope scope = new TripReplayScope(trip, VEHICLE, START, null,
                "IN_PROGRESS", UUID.randomUUID(), "REVISION:4");
        when(repository.findReplayScope(TENANT, trip)).thenReturn(Optional.of(scope));

        assertThat(service.findReplayScope(TENANT, trip)).contains(scope);
        assertThat(service.findReplayScope(UUID.randomUUID(), trip)).isEmpty();
        verify(repository).findReplayScope(TENANT, trip);
    }

    @Test
    void enforcesHalfOpenSevenDayRangeAndSingleBoundedRepositoryCall() {
        when(repository.findAssignmentsOverlapping(TENANT, VEHICLE, START, END, 2_001))
                .thenReturn(List.of());

        assertThat(service.findAssignmentsOverlapping(TENANT, VEHICLE, START, END)).isEmpty();
        verify(repository, times(1)).findAssignmentsOverlapping(TENANT, VEHICLE, START, END, 2_001);
        assertThatThrownBy(() -> service.findAssignmentsOverlapping(TENANT, VEHICLE, END, START))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findAssignmentsOverlapping(
                TENANT, VEHICLE, START, END.plusNanos(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsExactlyTwoThousandIntervalsWithoutTruncation() {
        List<VehicleTripAssignmentInterval> intervals = intervals(2_000);
        when(repository.findAssignmentsOverlapping(TENANT, VEHICLE, START, END, 2_001))
                .thenReturn(intervals);

        assertThat(service.findAssignmentsOverlapping(TENANT, VEHICLE, START, END))
                .hasSize(2_000).containsExactlyElementsOf(intervals);
    }

    @Test
    void rejectsTwoThousandAndFirstIntervalWithoutReturningPartialEvidence() {
        when(repository.findAssignmentsOverlapping(TENANT, VEHICLE, START, END, 2_001))
                .thenReturn(intervals(2_001));

        assertThatThrownBy(() -> service.findAssignmentsOverlapping(TENANT, VEHICLE, START, END))
                .isInstanceOfSatisfying(TripReplayQueryException.class, error -> {
                    assertThat(error.code()).isEqualTo(TripReplayQueryException.RESULT_LIMIT_EXCEEDED);
                    assertThat(error.getMessage()).doesNotContain(
                            TENANT.toString(), VEHICLE.toString(), START.toString(), END.toString());
                });
        verify(repository, times(1)).findAssignmentsOverlapping(TENANT, VEHICLE, START, END, 2_001);
    }

    @Test
    void publishedRecordsContainOnlyApprovedFrameworkNeutralFacts() {
        assertThat(TripReplayScope.class.getRecordComponents()).extracting("name")
                .containsExactly("tripId", "vehicleId", "actualStartTime", "actualEndTime",
                        "tripStatus", "routeId", "routeVersion");
        assertThat(VehicleTripAssignmentInterval.class.getRecordComponents()).extracting("name")
                .containsExactly("tripId", "vehicleId", "effectiveStart", "effectiveEnd",
                        "routeId", "routeVersion", "assignmentStatus");
        assertThat(TripReplayQuery.class.getAnnotations()).isEmpty();
    }

    private static List<VehicleTripAssignmentInterval> intervals(int count) {
        List<VehicleTripAssignmentInterval> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Instant start = START.plusSeconds(index);
            result.add(new VehicleTripAssignmentInterval(new UUID(0, index + 1L), VEHICLE,
                    start, start.plusSeconds(1), null, null, "COMPLETED"));
        }
        return result;
    }
}
