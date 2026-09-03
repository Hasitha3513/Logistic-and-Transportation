package com.transportlogistics.app.integration.adapters.configuration;

import com.transportlogistics.app.integration.application.service.IntegrationService;
import com.transportlogistics.app.integration.ports.outbound.IntegrationAttemptRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationAuditRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationConfigurationRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationEndpointPort;
import com.transportlogistics.app.integration.ports.outbound.IntegrationEventPublisher;
import com.transportlogistics.app.integration.ports.outbound.IntegrationExchangeRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationMappingRepository;
import com.transportlogistics.app.integration.ports.outbound.IntegrationPayloadPort;
import com.transportlogistics.app.integration.ports.outbound.IntegrationRateLimiter;
import com.transportlogistics.app.integration.ports.outbound.IntegrationTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class IntegrationConfigurationConfig {
    @Bean
    IntegrationService integrationService(IntegrationConfigurationRepository configurations,
                                          IntegrationMappingRepository mappings,
                                          IntegrationExchangeRepository exchanges,
                                          IntegrationAttemptRepository attempts,
                                          IntegrationAuditRepository audits,
                                          IntegrationEndpointPort endpoint,
                                          IntegrationPayloadPort payloads,
                                          IntegrationEventPublisher events,
                                          IntegrationRateLimiter rateLimiter,
                                          IntegrationTransaction transactions,
                                          Clock clock) {
        return new IntegrationService(configurations, mappings, exchanges, attempts, audits, endpoint, payloads,
            events, rateLimiter, transactions, clock);
    }

}
