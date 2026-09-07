package com.transportlogistics.app.billing.adapters.outbound.transaction;
import com.transportlogistics.app.billing.ports.outbound.BillingTransaction;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
@Component class SpringBillingTransaction implements BillingTransaction {private final TransactionTemplate tx;SpringBillingTransaction(TransactionTemplate tx){this.tx=tx;}@Override public <T>T execute(Supplier<T> operation){return tx.execute(status->operation.get());}}
