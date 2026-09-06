package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import java.util.UUID;

public interface FuelExceptionHandoff {
    void publish(FuelExceptionCase value, UUID handoffId, String reason, String correlationId);
}
