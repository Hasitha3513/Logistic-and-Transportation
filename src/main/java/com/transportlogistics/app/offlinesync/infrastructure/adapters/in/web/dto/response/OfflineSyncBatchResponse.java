package com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record OfflineSyncBatchResponse(OffsetDateTime serverTimestamp, List<OfflineSyncOperationResponse> results) {
}
