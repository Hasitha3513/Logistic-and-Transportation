package com.transportlogistics.app.tenancy.infrastructure.config;

import com.transportlogistics.app.tenancy.TenantDirectory;
import com.transportlogistics.app.tenancy.TenantContextExecutor;
import com.transportlogistics.app.tenancy.TenantJobExecutor;
import com.transportlogistics.app.tenancy.application.ports.out.TenantRepository;
import com.transportlogistics.app.tenancy.application.service.TenantDirectoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TenancyConfig {
    @Bean
    TenantDirectory tenantDirectory(TenantRepository repository) {
        return new TenantDirectoryService(repository);
    }

    @Bean
    TenantJobExecutor tenantJobExecutor(TenantDirectory tenants, TenantContextExecutor contexts) {
        return new TenantJobExecutor(tenants, contexts);
    }
}
