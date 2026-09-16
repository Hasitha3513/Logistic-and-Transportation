package com.transportlogistics.app.tracking.domain.gpsedge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GpsExceptionEpisodeTest {
    private static final Instant OPENED = Instant.parse("2026-09-16T12:00:00Z");

    @Test
    void repeatedEvidenceUpdatesOneEpisodeAndSeverityNeverDowngrades() {
        GpsExceptionEpisode episode = episode(ExceptionType.IMPOSSIBLE_MOVEMENT, Severity.WARNING);

        GpsExceptionEpisode high = episode.observe(Severity.HIGH, OPENED.plusSeconds(1));
        GpsExceptionEpisode repeatedWarning = high.observe(Severity.WARNING, OPENED.plusSeconds(2));

        assertThat(repeatedWarning.id()).isEqualTo(episode.id());
        assertThat(repeatedWarning.evidenceCount()).isEqualTo(3);
        assertThat(repeatedWarning.severity()).isEqualTo(Severity.HIGH);
        assertThat(repeatedWarning.version()).isEqualTo(2);
    }

    @Test
    void acknowledgementPreservesEvidenceAndRequiresReason() {
        GpsExceptionEpisode episode = episode(ExceptionType.SIGNAL_LOSS, Severity.WARNING);

        GpsExceptionEpisode acknowledged = episode.acknowledge("OPERATOR_REVIEWED", OPENED.plusSeconds(1));

        assertThat(acknowledged.status()).isEqualTo(EpisodeStatus.ACKNOWLEDGED);
        assertThat(acknowledged.evidenceCount()).isOne();
        assertThat(acknowledged.acknowledgementReason()).isEqualTo("OPERATOR_REVIEWED");
        assertThatThrownBy(() -> episode.acknowledge(" ", OPENED.plusSeconds(1)))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("ACKNOWLEDGEMENT_REASON_REQUIRED");
    }

    @Test
    void observableExceptionRequiresTwoConsecutiveRecoveryPoints() {
        GpsExceptionEpisode episode = episode(ExceptionType.IMPOSSIBLE_MOVEMENT, Severity.HIGH);

        GpsExceptionEpisode recovering = episode.recover(OPENED.plusSeconds(1));
        GpsExceptionEpisode resolved = recovering.recover(OPENED.plusSeconds(2));

        assertThat(recovering.status()).isEqualTo(EpisodeStatus.RECOVERING);
        assertThat(recovering.consecutiveRecoveryPoints()).isOne();
        assertThat(resolved.status()).isEqualTo(EpisodeStatus.RESOLVED);
        assertThat(resolved.resolvedAt()).isEqualTo(OPENED.plusSeconds(2));
        assertThatThrownBy(() -> resolved.observe(Severity.HIGH, OPENED.plusSeconds(3)))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("EPISODE_RESOLVED");
    }

    @Test
    void renewedEvidenceResetsRecoverySequence() {
        GpsExceptionEpisode recovering = episode(ExceptionType.SIGNAL_LOSS, Severity.WARNING)
                .recover(OPENED.plusSeconds(1));

        GpsExceptionEpisode renewed = recovering.observe(Severity.WARNING, OPENED.plusSeconds(2));

        assertThat(renewed.status()).isEqualTo(EpisodeStatus.OPEN);
        assertThat(renewed.consecutiveRecoveryPoints()).isZero();
    }

    @Test
    void bindingAndProcessingFailuresRequireConfirmedCorrection() {
        GpsExceptionEpisode binding = episode(ExceptionType.BINDING_VIOLATION, Severity.HIGH);

        assertThatThrownBy(() -> binding.recover(OPENED.plusSeconds(1)))
                .isInstanceOf(GpsEdgeCaseException.class)
                .extracting("code").isEqualTo("CORRECTION_REQUIRED");
        assertThat(binding.resolveAfterCorrection(OPENED.plusSeconds(1)).status())
                .isEqualTo(EpisodeStatus.RESOLVED);
    }

    private static GpsExceptionEpisode episode(ExceptionType type, Severity severity) {
        return GpsExceptionEpisode.open(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), type, severity, OPENED);
    }
}
