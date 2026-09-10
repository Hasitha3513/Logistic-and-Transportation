package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.outbound.GeofenceManagementTransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class SpringGeofenceManagementTransaction implements GeofenceManagementTransactionPort {
    private final TransactionTemplate transactions;

    SpringGeofenceManagementTransaction(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return Objects.requireNonNull(transactions.execute(status -> work.get()));
    }
}
