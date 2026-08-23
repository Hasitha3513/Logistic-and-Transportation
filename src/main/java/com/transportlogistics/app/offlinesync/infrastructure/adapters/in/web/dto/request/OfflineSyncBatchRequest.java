package com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OfflineSyncBatchRequest(
        @NotNull(message = "operations is required")
        @Size(min = 1, message = "operations must contain at least one item")
        List<@Valid OfflineSyncOperationRequest> operations
) {
}
