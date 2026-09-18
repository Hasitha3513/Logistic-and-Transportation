package com.transportlogistics.app.tracking.adapters.configuration;

import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration(proxyBeanMethods = false)
class TrackingKafkaConfiguration {
    @Bean
    KafkaTemplate<String, Object> trackingTelemetryKafkaTemplate(
            KafkaProperties properties) {
        Map<String, Object> producer = properties.buildProducerProperties(null);
        producer.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producer.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producer));
    }

    @Bean
    @ConditionalOnProperty(name = "app.tracking.kafka.manage-topic", havingValue = "true")
    NewTopic trackingTelemetryTopic(
            @Value("${app.tracking.kafka.topic:tracking.telemetry.ingested.v1}") String topic,
            @Value("${app.tracking.kafka.partitions:6}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.tracking.kafka.manage-topic", havingValue = "true")
    NewTopic trackingTelemetryV2Topic(
            @Value("${app.tracking.kafka.v2-topic:tracking.telemetry.ingested.v2}") String topic,
            @Value("${app.tracking.kafka.partitions:6}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }

    @Bean
    @ConditionalOnProperty(name = {"app.tracking.kafka.manage-topic",
            "app.tracking.kafka.v3-enabled"}, havingValue = "true")
    NewTopic trackingTelemetryV3Topic(
            @Value("${app.tracking.kafka.v3-topic:tracking.telemetry.ingested.v3}") String topic,
            @Value("${app.tracking.kafka.partitions:6}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.tracking.kafka.manage-topic", havingValue = "true")
    NewTopic trackingTelemetryDeadLetterTopic(
            @Value("${app.tracking.kafka.dead-letter-topic:tracking.telemetry.ingested.v1.dlt}")
                    String topic,
            @Value("${app.tracking.kafka.partitions:6}") int partitions) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(1)
                .config("retention.ms", Long.toString(java.time.Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.tracking.kafka.manage-topic", havingValue = "true")
    NewTopic trackingTelemetryV2DeadLetterTopic(
            @Value("${app.tracking.kafka.v2-dead-letter-topic:tracking.telemetry.ingested.v2.dlt}")
                    String topic,
            @Value("${app.tracking.kafka.partitions:6}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1)
                .config("retention.ms", Long.toString(java.time.Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = {"app.tracking.kafka.manage-topic",
            "app.tracking.kafka.v3-enabled"}, havingValue = "true")
    NewTopic trackingTelemetryV3DeadLetterTopic(
            @Value("${app.tracking.kafka.v3-dead-letter-topic:tracking.telemetry.ingested.v3.dlt}")
                    String topic,
            @Value("${app.tracking.kafka.partitions:6}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1)
                .config("retention.ms", Long.toString(java.time.Duration.ofDays(7).toMillis()))
                .build();
    }
}
