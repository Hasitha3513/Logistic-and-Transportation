package com.transportlogistics.app.fleet.infrastructure.transaction;

import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringVehicleReadingTransaction implements VehicleReadingTransaction {
    private final TransactionTemplate transactions;

    SpringVehicleReadingTransaction(PlatformTransactionManager transactionManager) {
        transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                return operation.get();
            }
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("VEHICLE_READING_CONCURRENT_CHANGE",
                    "Vehicle readings changed concurrently; reload and retry", exception);
        }
    }
}
