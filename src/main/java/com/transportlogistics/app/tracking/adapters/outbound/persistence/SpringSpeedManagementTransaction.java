package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.outbound.SpeedManagementTransactionPort;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class SpringSpeedManagementTransaction implements SpeedManagementTransactionPort {
    private final TransactionTemplate transactions;

    SpringSpeedManagementTransaction(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }
}
