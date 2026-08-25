package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.CustomerLookup;
import com.transportlogistics.app.organization.application.ports.in.CustomerUseCase;
import com.transportlogistics.app.organization.application.ports.out.CustomerRepository;
import com.transportlogistics.app.organization.application.service.CustomerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
class CustomerConfig {
    @Bean
    CustomerUseCase customerUseCase(CustomerRepository repo) {
        return new CustomerService(repo);
    }

    @Bean
    CustomerLookup customerLookup(CustomerUseCase customers) {
        return id -> {
            try {
                var customer = customers.get(id);
                return Optional.of(new CustomerLookup.CustomerReference(
                        customer.id(), customer.code(), customer.name(), customer.active()
                ));
            } catch (com.transportlogistics.app.shared.domain.NotFoundException ignored) {
                return Optional.empty();
            }
        };
    }
}
