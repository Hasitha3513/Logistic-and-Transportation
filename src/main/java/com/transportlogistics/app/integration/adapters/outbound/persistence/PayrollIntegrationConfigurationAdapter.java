package com.transportlogistics.app.integration.adapters.outbound.persistence;
import com.transportlogistics.app.integration.PayrollIntegrationConfigurationLookup;import org.springframework.stereotype.Component;import java.util.*;
@Component class PayrollIntegrationConfigurationAdapter implements PayrollIntegrationConfigurationLookup{
 private final IntegrationConfigurationJpaRepository configurations;private final IntegrationMappingJpaRepository mappings;
 PayrollIntegrationConfigurationAdapter(IntegrationConfigurationJpaRepository c,IntegrationMappingJpaRepository m){configurations=c;mappings=m;}
 public Optional<UUID>activePayrollConfiguration(UUID tenantId){return configurations.findByTenantIdAndLifecycle(tenantId,com.transportlogistics.app.integration.domain.model.IntegrationConfiguration.Lifecycle.ACTIVE).stream().filter(c->c.getDataClassification()==com.transportlogistics.app.integration.domain.model.IntegrationConfiguration.DataClassification.FINANCIAL_CONFIDENTIAL&&c.getCurrentMappingId()!=null&&mappings.findByTenantIdAndId(tenantId,c.getCurrentMappingId()).filter(x->"DRIVER_PAYROLL_INPUT_V1".equals(x.getSourceContract())).isPresent()).map(IntegrationConfigurationEntity::getId).findFirst();}
}
