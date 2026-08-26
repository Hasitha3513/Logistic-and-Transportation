package com.transportlogistics.app.offlinesync.infrastructure.transaction;

import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncItemTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringOfflineSyncItemTransaction implements OfflineSyncItemTransaction {
    private final TransactionTemplate transactions;

    SpringOfflineSyncItemTransaction(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        return transactions.execute(status -> operation.get());
    }
}
