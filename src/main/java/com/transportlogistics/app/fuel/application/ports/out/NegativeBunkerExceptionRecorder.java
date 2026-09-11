package com.transportlogistics.app.fuel.application.ports.out;
import java.math.BigDecimal;
import java.util.UUID;
public interface NegativeBunkerExceptionRecorder {
    void record(UUID tankId, BigDecimal attemptedDelta, BigDecimal attemptedBalance, UUID actorId);
}
