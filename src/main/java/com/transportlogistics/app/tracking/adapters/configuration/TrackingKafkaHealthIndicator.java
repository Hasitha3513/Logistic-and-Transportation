package com.transportlogistics.app.tracking.adapters.configuration;

import java.time.Duration;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Component;

@Component("trackingKafka")
final class TrackingKafkaHealthIndicator implements HealthIndicator {
    private final KafkaProperties properties;

    TrackingKafkaHealthIndicator(KafkaProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        try (var admin = AdminClient.create(properties.buildAdminProperties(null))) {
            var nodes = admin.describeCluster().nodes().get(Duration.ofSeconds(2).toMillis(),
                    java.util.concurrent.TimeUnit.MILLISECONDS);
            return Health.up().withDetail("brokers", nodes.size()).build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Health.down().withDetail("category", "INTERRUPTED").build();
        } catch (Exception exception) {
            return Health.down().withDetail("category", "UNAVAILABLE").build();
        }
    }
}
