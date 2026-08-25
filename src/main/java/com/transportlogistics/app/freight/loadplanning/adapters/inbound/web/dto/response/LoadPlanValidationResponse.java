package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response;

import java.util.List;

public record LoadPlanValidationResponse(
        boolean valid,
        List<ViolationDetail> violations
) {
    public record ViolationDetail(
            String code,
            String message
    ) {}
}
