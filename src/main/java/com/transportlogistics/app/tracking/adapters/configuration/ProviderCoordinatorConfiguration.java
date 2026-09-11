package com.transportlogistics.app.tracking.adapters.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProviderCoordinatorConfiguration {
    @Bean
    ProviderCoordinatorSettings providerCoordinatorSettings(
            @Value("${app.tracking.provider-coordinator.max-connections-per-tick:8}") int claims,
            @Value("${app.tracking.provider-coordinator.max-workers:4}") int workers,
            @Value("${app.tracking.provider-coordinator.queue-capacity:8}") int queue,
            @Value("${app.tracking.provider-coordinator.max-devices-per-fetch:100}") int devices,
            @Value("${app.tracking.provider-coordinator.max-batches-per-job:10}") int batches,
            @Value("${app.tracking.provider-coordinator.provider-concurrency:2}") int providerConcurrency,
            @Value("${app.tracking.provider-coordinator.response-byte-limit:1048576}") int responseLimit,
            @Value("${app.tracking.provider-coordinator.lease-duration:PT2M}") Duration lease,
            @Value("${app.tracking.provider-coordinator.fetch-deadline:PT45S}") Duration deadline,
            @Value("${app.tracking.provider-coordinator.failure-backoff:PT30S}") Duration backoff,
            @Value("${app.tracking.provider-coordinator.shutdown-grace:PT10S}") Duration shutdown) {
        return new ProviderCoordinatorSettings(
                claims, workers, queue, devices, batches, providerConcurrency, responseLimit,
                lease, deadline, backoff, shutdown);
    }
}
