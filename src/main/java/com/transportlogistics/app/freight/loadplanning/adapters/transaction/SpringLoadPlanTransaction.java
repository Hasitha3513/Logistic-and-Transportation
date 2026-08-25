package com.transportlogistics.app.freight.loadplanning.adapters.transaction;

import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringLoadPlanTransaction implements LoadPlanTransaction {

    private final TransactionTemplate tx;

    public SpringLoadPlanTransaction(PlatformTransactionManager manager) {
        this.tx = new TransactionTemplate(manager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        try {
            return tx.execute(status -> operation.get());
        } catch (ConcurrencyFailureException ex) {
            throw new ConflictException("LOAD_PLAN_CONCURRENT_UPDATE", "Load plan changed concurrently; reload and retry", ex);
        }
    }
}
