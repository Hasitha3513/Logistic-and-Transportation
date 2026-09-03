package com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.notification.application.ports.in.CustomerNotificationPreferenceUseCase;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.ReplaceCustomerNotificationPreferenceRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.CustomerNotificationPreferenceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/notification-customer-preferences", "/v1/notification-customer-preferences"})
public class CustomerNotificationPreferenceController {
    private final CustomerNotificationPreferenceUseCase useCase;

    public CustomerNotificationPreferenceController(CustomerNotificationPreferenceUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{customerId}")
    CustomerNotificationPreferenceResponse get(@PathVariable UUID customerId) {
        return response(useCase.get(customerId));
    }

    @PutMapping("/{customerId}")
    CustomerNotificationPreferenceResponse replace(
            @PathVariable UUID customerId,
            @Valid @RequestBody ReplaceCustomerNotificationPreferenceRequest request) {
        return response(useCase.replace(customerId, new CustomerNotificationPreferenceUseCase.ReplaceCommand(
                request.emailEnabled(), request.smsEnabled(), request.version())));
    }

    private static CustomerNotificationPreferenceResponse response(
            CustomerNotificationPreferenceUseCase.PreferenceView value) {
        return new CustomerNotificationPreferenceResponse(value.customerId(), value.explicitProfile(),
                value.emailEnabled(), value.smsEnabled(), value.maskedEmail(), value.maskedPhone(), value.version());
    }
}
