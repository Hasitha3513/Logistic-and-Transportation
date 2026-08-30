package com.transportlogistics.app.freight.insurance.adapters.transaction;

import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceTransaction;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringFreightInsuranceTransaction implements FreightInsuranceTransaction {

    private final TransactionTemplate transactionTemplate;

    public SpringFreightInsuranceTransaction(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        try {
            return transactionTemplate.execute(status -> operation.get());
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("INSURANCE_CONCURRENT_UPDATE", "Insurance entity was modified concurrently by another transaction; retry");
        }
    }
}
