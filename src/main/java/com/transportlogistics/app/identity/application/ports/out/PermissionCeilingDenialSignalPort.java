package com.transportlogistics.app.identity.application.ports.out;

import com.transportlogistics.app.identity.domain.risk.PermissionCeilingDenialSignal;

/** Internal source boundary only; CS01 provides no adapter or runtime producer. */
public interface PermissionCeilingDenialSignalPort {
    void record(PermissionCeilingDenialSignal signal);
}
