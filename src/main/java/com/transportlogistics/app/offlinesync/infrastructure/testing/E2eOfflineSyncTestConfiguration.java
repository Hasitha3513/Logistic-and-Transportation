package com.transportlogistics.app.offlinesync.infrastructure.testing;

import com.transportlogistics.app.offlinesync.application.ports.out.OfflineOperationHandler;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("e2e")
public class E2eOfflineSyncTestConfiguration {
    @Bean
    static E2eOfflineSyncControl e2eOfflineSyncControl() {
        return new E2eOfflineSyncControl();
    }

    @Bean
    static BeanPostProcessor e2eOfflineHandlerDecorator(E2eOfflineSyncControl control) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof OfflineOperationHandler delegate)) {
                    return bean;
                }
                return new OfflineOperationHandler() {
                    @Override public String operationType() { return delegate.operationType(); }
                    @Override public int operationVersion() { return delegate.operationVersion(); }
                    @Override public java.util.Set<String> requiredAuthorities() { return delegate.requiredAuthorities(); }
                    @Override public boolean isAuthorized(java.util.Set<String> currentAuthorities) {
                        return delegate.isAuthorized(currentAuthorities);
                    }
                    @Override public com.transportlogistics.app.offlinesync.domain.model.OfflineHandlerOutcome apply(
                            com.transportlogistics.app.offlinesync.domain.model.OfflineOperationContext context,
                            com.fasterxml.jackson.databind.JsonNode payload) {
                        var forced = control.beforeApply(context.operationId());
                        return forced != null ? forced : delegate.apply(context, payload);
                    }
                };
            }
        };
    }
}
