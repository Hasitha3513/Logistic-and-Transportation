package com.transportlogistics.app.tracking.adapters.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SpeedEvaluatorConfiguration {
    @Bean
    SpeedEvaluatorSettings speedEvaluatorSettings(
            @Value("${app.tracking.speed-evaluator.claim-size:16}") int claimSize,
            @Value("${app.tracking.speed-evaluator.worker-count:4}") int workers,
            @Value("${app.tracking.speed-evaluator.queue-capacity:32}") int queueCapacity,
            @Value("${app.tracking.speed-evaluator.maximum-attempts:5}") int maximumAttempts,
            @Value("${app.tracking.speed-evaluator.lease-duration:PT2M}") Duration leaseDuration,
            @Value("${app.tracking.speed-evaluator.retry-backoff:PT30S}") Duration retryBackoff,
            @Value("${app.tracking.speed-evaluator.shutdown-grace:PT10S}") Duration shutdownGrace) {
        return new SpeedEvaluatorSettings(claimSize, workers, queueCapacity, maximumAttempts,
                leaseDuration, retryBackoff, shutdownGrace);
    }
}
