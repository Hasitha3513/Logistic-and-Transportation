package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationTransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class SpringSpeedEvaluationTransaction implements SpeedEvaluationTransactionPort {
    private final TransactionTemplate transactions;

    SpringSpeedEvaluationTransaction(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        return Objects.requireNonNull(transactions.execute(status -> operation.get()));
    }
}
