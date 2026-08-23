package com.transportlogistics.app.offlinesync.application.ports.out;

import java.util.function.Supplier;

public interface OfflineSyncItemTransaction {
    <T> T execute(Supplier<T> operation);
}
