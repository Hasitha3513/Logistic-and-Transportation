package com.transportlogistics.app.routing.infrastructure.transaction;

import com.transportlogistics.app.routing.application.ports.out.RouteTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringRouteTransaction implements RouteTransaction {
    private final TransactionTemplate transactions;

    SpringRouteTransaction(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        try {
            return transactions.execute(status -> operation.get());
        } catch (ConcurrencyFailureException exception) {
            throw new ConflictException("ROUTE_CONCURRENT_CHANGE",
                    "Route changed concurrently; reload and retry", exception);
        }
    }
}
