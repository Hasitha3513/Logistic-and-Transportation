package com.transportlogistics.app.integration.adapters.inbound.web.dto.response;

import java.time.OffsetDateTime;

public record IntegrationTestResponse(IntegrationResponse integration, boolean success, String code,
                                      OffsetDateTime testedAt) {}
