package com.transportlogistics.app.integration.adapters.outbound.persistence;
import com.transportlogistics.app.integration.BillingIntegrationConfigurationLookup;
import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
@Component class BillingIntegrationConfigurationAdapter implements BillingIntegrationConfigurationLookup {
 private final IntegrationConfigurationJpaRepository configurations;private final IntegrationMappingJpaRepository mappings;
 BillingIntegrationConfigurationAdapter(IntegrationConfigurationJpaRepository c,IntegrationMappingJpaRepository m){configurations=c;mappings=m;}
 @Override public Optional<UUID> activeBillingConfiguration(UUID tenantId){return configurations.findByTenantIdAndLifecycle(tenantId,IntegrationConfiguration.Lifecycle.ACTIVE).stream().filter(c->c.getDataClassification()==IntegrationConfiguration.DataClassification.FINANCIAL_CONFIDENTIAL&&c.getCurrentMappingId()!=null&&mappings.findByTenantIdAndId(tenantId,c.getCurrentMappingId()).filter(x->"TRANSPORT_BILLING_V1".equals(x.getSourceContract())).isPresent()).map(IntegrationConfigurationEntity::getId).findFirst();}
}
