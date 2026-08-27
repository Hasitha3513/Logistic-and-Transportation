package com.transportlogistics.app.freight.exception.adapters.transaction;

import com.transportlogistics.app.freight.exception.ports.outbound.CargoExceptionTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class SpringCargoExceptionTransaction implements CargoExceptionTransaction {

    @Override
    @Transactional
    public <T> T execute(Supplier<T> work) {
        return work.get();
    }
}
