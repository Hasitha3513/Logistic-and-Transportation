package com.transportlogistics.app.fuel.infrastructure.transaction;

import com.transportlogistics.app.fuel.application.ports.out.FuelTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringFuelTransaction implements FuelTransaction {
    private final TransactionTemplate transactions;

    SpringFuelTransaction(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        try {
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("FUEL_ISSUE_CONCURRENT_CHANGE",
                    "Fuel issue changed concurrently; reload and retry", exception);
        }
    }
}
