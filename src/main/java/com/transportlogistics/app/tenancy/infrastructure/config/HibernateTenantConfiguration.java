package com.transportlogistics.app.tenancy.infrastructure.config;

import com.transportlogistics.app.tenancy.CanonicalTenant;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class HibernateTenantConfiguration {
    @Bean
    HibernatePropertiesCustomizer tenantIdentifierResolver(ObjectProvider<CurrentTenant> currentTenantProvider) {
        CurrentTenantIdentifierResolver<UUID> resolver = new CurrentTenantIdentifierResolver<>() {
            @Override
            public UUID resolveCurrentTenantIdentifier() {
                var currentTenant = currentTenantProvider.getIfAvailable();
                return currentTenant == null
                        ? CanonicalTenant.ID
                        : currentTenant.current().map(context -> context.tenantId()).orElse(CanonicalTenant.ID);
            }

            @Override
            public boolean validateExistingCurrentSessions() {
                return true;
            }
        };
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
