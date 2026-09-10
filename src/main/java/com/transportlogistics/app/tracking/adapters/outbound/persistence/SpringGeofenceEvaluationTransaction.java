package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationTransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class SpringGeofenceEvaluationTransaction implements GeofenceEvaluationTransactionPort {
    private final TransactionTemplate transactions;

    SpringGeofenceEvaluationTransaction(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        return Objects.requireNonNull(transactions.execute(status -> operation.get()));
    }
}
