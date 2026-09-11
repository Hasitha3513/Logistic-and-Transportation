package com.transportlogistics.app.fleet.payroll.ports.outbound;
import java.util.function.Supplier;
public interface DriverPayrollTransaction { <T> T execute(Supplier<T> work); }
