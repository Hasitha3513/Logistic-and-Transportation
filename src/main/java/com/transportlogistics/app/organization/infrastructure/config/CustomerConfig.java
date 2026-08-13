package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.application.ports.in.CustomerUseCase;
import com.transportlogistics.app.organization.application.ports.out.CustomerRepository;
import com.transportlogistics.app.organization.application.service.CustomerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CustomerConfig {
    @Bean
    CustomerUseCase customerUseCase(CustomerRepository repo) {
        return new CustomerService(repo);
    }
}
