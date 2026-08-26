package com.transportlogistics.app.offlinesync.application.ports.out;

import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase.OperationCommand;

public interface OfflineRequestHasher {
    String hash(OperationCommand operation);
}
