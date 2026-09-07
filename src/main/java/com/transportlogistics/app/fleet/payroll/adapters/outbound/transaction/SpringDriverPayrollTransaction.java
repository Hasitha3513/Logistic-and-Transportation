package com.transportlogistics.app.fleet.payroll.adapters.outbound.transaction;
import com.transportlogistics.app.fleet.payroll.ports.outbound.DriverPayrollTransaction;import org.springframework.stereotype.Component;import org.springframework.transaction.support.TransactionTemplate;import java.util.function.Supplier;
@Component class SpringDriverPayrollTransaction implements DriverPayrollTransaction{private final TransactionTemplate tx;SpringDriverPayrollTransaction(TransactionTemplate t){tx=t;}public<T>T execute(Supplier<T>w){return tx.execute(s->w.get());}}
