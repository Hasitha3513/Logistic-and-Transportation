package com.transportlogistics.app.fuel.application.ports.out;

import java.time.LocalDate;

public interface FuelPurchaseNumberGenerator {
    String next(LocalDate purchaseDate);
}
