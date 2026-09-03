package com.transportlogistics.app.trip.infrastructure.config;

import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripOperationalEventRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.TripOperationalNotificationPublisher;
import com.transportlogistics.app.trip.application.service.TripOperationalEventService;
import com.transportlogistics.app.trip.infrastructure.adapters.out.persistence.TripOperationalEventJpaRepository;
import com.transportlogistics.app.trip.infrastructure.adapters.out.persistence.TripOperationalEventPersistenceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
@Configuration
public class TripOperationalEventConfig {

    @Bean
    public TripOperationalEventRepository tripOperationalEventRepository(TripOperationalEventJpaRepository jpaRepo) {
        return new TripOperationalEventPersistenceAdapter(jpaRepo);
    }

    @Bean
    public TripOperationalEventService tripOperationalEventUseCase(
            TripRepository tripRepo,
            TripOperationalEventRepository eventRepo,
            TripHistoryRepository historyRepo,
            TripOperationalNotificationPublisher publisher
    ) {
        return new TripOperationalEventService(tripRepo, eventRepo, historyRepo, Clock.systemUTC(), publisher);
    }

}
