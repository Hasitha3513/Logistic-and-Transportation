package com.transportlogistics.app.integration.adapters.outbound.transaction;

import com.transportlogistics.app.integration.ports.outbound.IntegrationTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringIntegrationTransaction implements IntegrationTransaction {
    private final TransactionTemplate transactions;

    SpringIntegrationTransaction(PlatformTransactionManager manager) {
        this.transactions = new TransactionTemplate(manager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) return operation.get();
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("INTEGRATION_CONFLICT",
                "Integration state changed concurrently; reload and retry", exception);
        }
    }
}
