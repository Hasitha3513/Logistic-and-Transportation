package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import jakarta.validation.constraints.Size;

public record ReconciliationRequest(@Size(max = 1000) String reconciliationNotes,
                                    @Size(max = 100) String referenceNumber) {

    public FuelPurchaseUseCase.ReconciliationCommand command() {
        return new FuelPurchaseUseCase.ReconciliationCommand(reconciliationNotes, referenceNumber);
    }
}
