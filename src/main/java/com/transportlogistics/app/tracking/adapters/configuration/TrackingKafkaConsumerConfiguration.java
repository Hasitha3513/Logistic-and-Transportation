package com.transportlogistics.app.tracking.adapters.configuration;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
class TrackingKafkaConsumerConfiguration {
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV1>
            trackingLiveProjectorContainerFactory(
                    KafkaProperties properties,
                    KafkaTemplate<String, Object> kafka,
                    @Value("${app.tracking.kafka.dead-letter-topic:tracking.telemetry.ingested.v1.dlt}")
                            String deadLetterTopic) {
        Map<String, Object> consumer = properties.buildConsumerProperties(null);
        consumer.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumer.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        consumer.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        consumer.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TrackingTelemetryIngestedV1.class.getName());
        consumer.put(JsonDeserializer.TRUSTED_PACKAGES,
                TrackingTelemetryIngestedV1.class.getPackageName());
        consumer.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV1>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumer));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        var deadLetter = new DeadLetterPublishingRecoverer(
                kafka, (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));
        ConsumerRecordRecoverer recoverer = (record, exception) -> {
            if (causedBy(exception, DependencyUnavailableException.class)) {
                throw new KafkaException("Redis projection remains unavailable", exception);
            }
            deadLetter.accept(record, exception);
        };
        var errors = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
        errors.addNotRetryableExceptions(IllegalArgumentException.class);
        errors.addRetryableExceptions(DependencyUnavailableException.class);
        errors.setCommitRecovered(true);
        factory.setCommonErrorHandler(errors);
        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV1>
            trackingHistoryPersisterContainerFactory(
                    KafkaProperties properties,
                    KafkaTemplate<String, Object> kafka,
                    @Value("${app.tracking.kafka.dead-letter-topic:tracking.telemetry.ingested.v1.dlt}")
                            String deadLetterTopic,
                    @Value("${app.tracking.hybrid-storage.stream-batch-size:500}") int batchSize) {
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("Tracking history batch size must be 1..500");
        }
        Map<String, Object> consumer = consumerProperties(properties);
        consumer.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV1>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumer));
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        var deadLetter = new DeadLetterPublishingRecoverer(
                kafka, (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));
        ConsumerRecordRecoverer recoverer = (record, exception) -> {
            if (causedBy(exception, DependencyUnavailableException.class)) {
                throw new KafkaException("Historical telemetry storage remains unavailable", exception);
            }
            deadLetter.accept(record, exception);
        };
        var errors = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
        errors.addNotRetryableExceptions(IllegalArgumentException.class);
        errors.addRetryableExceptions(DependencyUnavailableException.class);
        errors.setCommitRecovered(true);
        factory.setCommonErrorHandler(errors);
        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV2>
            trackingLiveProjectorV2ContainerFactory(
                    KafkaProperties properties,
                    KafkaTemplate<String, Object> kafka,
                    @Value("${app.tracking.kafka.v2-dead-letter-topic:tracking.telemetry.ingested.v2.dlt}")
                            String deadLetterTopic) {
        return singleFactory(properties, kafka, deadLetterTopic, TrackingTelemetryIngestedV2.class,
                "Redis projection remains unavailable");
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV2>
            trackingHistoryPersisterV2ContainerFactory(
                    KafkaProperties properties,
                    KafkaTemplate<String, Object> kafka,
                    @Value("${app.tracking.kafka.v2-dead-letter-topic:tracking.telemetry.ingested.v2.dlt}")
                            String deadLetterTopic,
                    @Value("${app.tracking.hybrid-storage.stream-batch-size:500}") int batchSize) {
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("Tracking history batch size must be 1..500");
        }
        Map<String, Object> consumer = consumerProperties(properties, TrackingTelemetryIngestedV2.class);
        consumer.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV2>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumer));
        factory.setBatchListener(true);
        configure(factory, kafka, deadLetterTopic, "Historical telemetry storage remains unavailable");
        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV3>
            trackingLiveProjectorV3ContainerFactory(
                    KafkaProperties properties,
                    KafkaTemplate<String, Object> kafka,
                    @Value("${app.tracking.kafka.v3-dead-letter-topic:tracking.telemetry.ingested.v3.dlt}")
                            String deadLetterTopic) {
        return singleFactory(properties, kafka, deadLetterTopic, TrackingTelemetryIngestedV3.class,
                "Redis projection remains unavailable");
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV3>
            trackingHistoryPersisterV3ContainerFactory(
                    KafkaProperties properties,
                    KafkaTemplate<String, Object> kafka,
                    @Value("${app.tracking.kafka.v3-dead-letter-topic:tracking.telemetry.ingested.v3.dlt}")
                            String deadLetterTopic,
                    @Value("${app.tracking.hybrid-storage.stream-batch-size:500}") int batchSize) {
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("Tracking history batch size must be 1..500");
        }
        Map<String, Object> consumer = consumerProperties(properties, TrackingTelemetryIngestedV3.class);
        consumer.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TrackingTelemetryIngestedV3>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumer));
        factory.setBatchListener(true);
        configure(factory, kafka, deadLetterTopic, "Historical telemetry storage remains unavailable");
        return factory;
    }

    private static Map<String, Object> consumerProperties(KafkaProperties properties) {
        return consumerProperties(properties, TrackingTelemetryIngestedV1.class);
    }

    private static Map<String, Object> consumerProperties(KafkaProperties properties, Class<?> eventType) {
        Map<String, Object> consumer = properties.buildConsumerProperties(null);
        consumer.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumer.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        consumer.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        consumer.put(JsonDeserializer.VALUE_DEFAULT_TYPE, eventType.getName());
        consumer.put(JsonDeserializer.TRUSTED_PACKAGES,
                TrackingTelemetryIngestedV1.class.getPackageName());
        consumer.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return consumer;
    }

    private static <T> ConcurrentKafkaListenerContainerFactory<String, T> singleFactory(
            KafkaProperties properties, KafkaTemplate<String, Object> kafka, String deadLetterTopic,
            Class<T> eventType, String unavailableMessage) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, T>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerProperties(properties, eventType)));
        configure(factory, kafka, deadLetterTopic, unavailableMessage);
        return factory;
    }

    private static void configure(
            ConcurrentKafkaListenerContainerFactory<?, ?> factory,
            KafkaTemplate<String, Object> kafka, String deadLetterTopic, String unavailableMessage) {
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        var deadLetter = new DeadLetterPublishingRecoverer(
                kafka, (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));
        ConsumerRecordRecoverer recoverer = (record, exception) -> {
            if (causedBy(exception, DependencyUnavailableException.class)) {
                throw new KafkaException(unavailableMessage, exception);
            }
            deadLetter.accept(record, exception);
        };
        var errors = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
        errors.addNotRetryableExceptions(IllegalArgumentException.class);
        errors.addRetryableExceptions(DependencyUnavailableException.class);
        errors.setCommitRecovered(true);
        factory.setCommonErrorHandler(errors);
    }

    private static boolean causedBy(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
