package com.transportlogistics.app.fuel.application.ports.out;
import java.util.Map;
import java.util.UUID;
public interface FuelExceptionCorrectionExecutor {
    String execute(UUID tenantId, String correctionType, Map<String,String> command,
                   UUID actorId, String actor);
}
