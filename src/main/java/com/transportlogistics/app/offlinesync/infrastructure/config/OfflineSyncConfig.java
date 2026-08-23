package com.transportlogistics.app.offlinesync.infrastructure.config;

import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineRequestHasher;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncActorDirectory;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncItemTransaction;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncOperationRepository;
import com.transportlogistics.app.offlinesync.application.service.OfflineOperationHandlerRegistry;
import com.transportlogistics.app.offlinesync.application.service.OfflineSyncBatchService;
import com.transportlogistics.app.offlinesync.application.service.OfflineSyncItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class OfflineSyncConfig {
    @Bean
    OfflineOperationHandlerRegistry offlineOperationHandlerRegistry(List<OfflineOperationHandler> handlers) {
        return new OfflineOperationHandlerRegistry(handlers);
    }

    @Bean
    OfflineSyncItemProcessor offlineSyncItemProcessor(OfflineSyncOperationRepository operations, Clock clock) {
        return new OfflineSyncItemProcessor(operations, clock);
    }

    @Bean
    OfflineSyncUseCase offlineSyncUseCase(OfflineSyncActorDirectory actors,
                                          OfflineOperationHandlerRegistry handlers,
                                          OfflineRequestHasher hasher,
                                          OfflineSyncItemTransaction transactions,
                                          OfflineSyncItemProcessor processor,
                                          Clock clock) {
        return new OfflineSyncBatchService(actors, handlers, hasher, transactions, processor, clock);
    }
}
