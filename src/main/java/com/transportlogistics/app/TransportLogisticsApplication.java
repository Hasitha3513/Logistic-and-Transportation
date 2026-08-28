package com.transportlogistics.app;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantAwareJpaRepository;
import com.transportlogistics.app.tenancy.infrastructure.config.HibernateTenantConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = TenantAwareJpaRepository.class)
@Import(HibernateTenantConfiguration.class)
public class TransportLogisticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransportLogisticsApplication.class, args);
    }
}
