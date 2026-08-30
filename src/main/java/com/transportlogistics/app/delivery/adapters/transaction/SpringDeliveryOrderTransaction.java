package com.transportlogistics.app.delivery.adapters.transaction;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringDeliveryOrderTransaction implements DeliveryOrderTransaction {
    private final TransactionTemplate transactions;
    SpringDeliveryOrderTransaction(PlatformTransactionManager manager) { this.transactions = new TransactionTemplate(manager); }
    @Override public <T> T execute(Supplier<T> operation) {
        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                return operation.get();
            }
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("DELIVERY_VERSION_CONFLICT", "Delivery Order changed concurrently; reload and retry", exception);
        }
    }
}
