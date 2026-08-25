package com.transportlogistics.app.freight.insurance.ports.outbound;

import java.util.function.Supplier;

public interface FreightInsuranceTransaction {

    <T> T execute(Supplier<T> operation);
}
