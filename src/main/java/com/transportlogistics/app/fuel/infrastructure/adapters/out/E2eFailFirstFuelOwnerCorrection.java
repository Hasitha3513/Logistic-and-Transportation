package com.transportlogistics.app.fuel.infrastructure.adapters.out;

import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionCorrectionExecutor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** E2E-only adapter-boundary failure injection for owner-command retry proof. */
@Component
@Primary
@Profile("e2e")
class E2eFailFirstFuelOwnerCorrection implements FuelExceptionCorrectionExecutor {
    static final String FAILURE_MARKER = "[E2E_FAIL_FIRST]";
    private final FuelOwnerCorrectionAdapter delegate;
    private final Set<String> failedCommands = ConcurrentHashMap.newKeySet();

    E2eFailFirstFuelOwnerCorrection(FuelOwnerCorrectionAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public String execute(UUID tenantId, String correctionType, Map<String, String> command,
                          UUID actorId, String actor) {
        String reason = command.get("reason");
        String key = tenantId + ":" + correctionType + ":" + reason;
        if (reason != null && reason.startsWith(FAILURE_MARKER) && failedCommands.add(key)) {
            throw new IllegalStateException("Controlled first-attempt Fuel owner-command failure");
        }
        return delegate.execute(tenantId, correctionType, command, actorId, actor);
    }
}
