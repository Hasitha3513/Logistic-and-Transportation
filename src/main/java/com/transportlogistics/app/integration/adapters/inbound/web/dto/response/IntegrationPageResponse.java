package com.transportlogistics.app.integration.adapters.inbound.web.dto.response;

import java.util.List;

public record IntegrationPageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
