package com.transportlogistics.app.freight.order.adapters.transaction;

import com.transportlogistics.app.freight.order.ports.outbound.FreightOrderTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringFreightOrderTransaction implements FreightOrderTransaction {
    private final TransactionTemplate transactions;
    SpringFreightOrderTransaction(PlatformTransactionManager manager) { this.transactions = new TransactionTemplate(manager); }
    @Override public <T> T execute(Supplier<T> operation) {
        try {
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("FREIGHT_ORDER_CONCURRENT_UPDATE", "Freight order changed concurrently; reload and retry", exception);
        }
    }
}
