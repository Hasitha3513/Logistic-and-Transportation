package com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.Size;

public record HoldExceptionRequest(
        @Size(max = 1000)
        String restriction,

        @Size(max = 2000)
        String reason,

        long version
) {}
