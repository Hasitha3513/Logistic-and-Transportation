package com.transportlogistics.app.trip.infrastructure.transaction;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringTripTransaction implements TripTransaction {
    private final TransactionTemplate transactions;

    SpringTripTransaction(PlatformTransactionManager transactionManager) {
        transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        try {
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("Vehicle allocation changed concurrently; retry the assignment", exception);
        }
    }
}
