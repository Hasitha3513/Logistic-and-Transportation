package com.transportlogistics.app.tracking.adapters.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GeofenceEvaluatorConfiguration {
    @Bean
    GeofenceEvaluatorSettings geofenceEvaluatorSettings(
            @Value("${app.tracking.geofence-evaluator.claim-size:16}") int claimSize,
            @Value("${app.tracking.geofence-evaluator.worker-count:4}") int workers,
            @Value("${app.tracking.geofence-evaluator.queue-capacity:32}") int queueCapacity,
            @Value("${app.tracking.geofence-evaluator.lease-duration:PT2M}") Duration leaseDuration,
            @Value("${app.tracking.geofence-evaluator.retry-backoff:PT30S}") Duration retryBackoff,
            @Value("${app.tracking.geofence-evaluator.shutdown-grace:PT10S}") Duration shutdownGrace) {
        return new GeofenceEvaluatorSettings(claimSize, workers, queueCapacity, leaseDuration,
                retryBackoff, shutdownGrace);
    }
}
