package com.transportlogistics.app.integration.ports.outbound;

import com.transportlogistics.app.integration.domain.model.IntegrationAuditEvent;

public interface IntegrationAuditRepository {
    void append(IntegrationAuditEvent event);
}
