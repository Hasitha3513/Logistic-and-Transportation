package com.transportlogistics.app.tracking.adapters.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

@SpringBootTest(properties = {
    "app.tracking.hybrid-storage.enabled=true",
    "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureObservability
class TrackingKafkaConsumerProxyCompatibilityIntegrationTest {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private KafkaListenerEndpointRegistry listeners;

    @Test
    void hybridConsumersAreObservableAndRegisteredExactlyOnce() {
        Object historical = context.getBean("trackingHistoricalTelemetryPersister");
        Object live = context.getBean("trackingLiveTelemetryProjector");

        assertThat(AopUtils.isAopProxy(historical)).isTrue();
        assertThat(AopUtils.isAopProxy(live)).isTrue();
        assertThat(AopUtils.getTargetClass(historical))
                .isEqualTo(TrackingHistoricalTelemetryPersister.class);
        assertThat(AopUtils.getTargetClass(live))
                .isEqualTo(TrackingLiveTelemetryProjector.class);
        assertThat(listeners.getListenerContainers()).hasSize(4);
        assertThat(listeners.getListenerContainers().stream()
                .flatMap(container -> java.util.Arrays.stream(
                        container.getContainerProperties().getTopics()))
                .toList()).containsExactlyInAnyOrder(
                        "tracking.telemetry.ingested.v1", "tracking.telemetry.ingested.v1",
                        "tracking.telemetry.ingested.v2", "tracking.telemetry.ingested.v2");
    }
}
