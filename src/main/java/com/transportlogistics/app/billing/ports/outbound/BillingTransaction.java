package com.transportlogistics.app.billing.ports.outbound;

import java.util.function.Supplier;
public interface BillingTransaction { <T> T execute(Supplier<T> operation); }
