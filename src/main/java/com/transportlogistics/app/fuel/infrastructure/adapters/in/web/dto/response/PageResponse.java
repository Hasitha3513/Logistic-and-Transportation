package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {
}
