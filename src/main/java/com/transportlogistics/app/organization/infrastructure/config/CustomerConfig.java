package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.CustomerDataReadiness;
import com.transportlogistics.app.organization.CustomerLookup;
import com.transportlogistics.app.organization.CustomerNotificationContactLookup;
import com.transportlogistics.app.organization.application.ports.in.CustomerUseCase;
import com.transportlogistics.app.organization.application.ports.out.CustomerRepository;
import com.transportlogistics.app.organization.application.service.CustomerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Bean
    CustomerNotificationContactLookup customerNotificationContactLookup(CustomerUseCase customers) {
        return id -> {
            try {
                var customer = customers.get(id);
                return Optional.of(new CustomerNotificationContactLookup.CustomerNotificationContact(
                        customer.id(), customer.active(), customer.name(), customer.phone(), customer.email()
                ));
            } catch (com.transportlogistics.app.shared.domain.NotFoundException ignored) {
                return Optional.empty();
            }
        };
    }

    @Bean
    CustomerDataReadiness customerDataReadiness(JdbcTemplate jdbc) {
        return () -> Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM customer)", Boolean.class));
    }
}
