package com.transportlogistics.app.operations.adapters.outbound.transaction;

import com.transportlogistics.app.operations.ports.outbound.OperationsTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringOperationsTransaction implements OperationsTransaction {
    private final TransactionTemplate transactions;
    SpringOperationsTransaction(PlatformTransactionManager manager) { transactions = new TransactionTemplate(manager); }

    @Override public <T> T execute(Supplier<T> operation) {
        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) return operation.get();
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("OPERATIONAL_EXCEPTION_CONFLICT",
                "Operational exception state changed; reload and retry", exception);
        }
    }
}
