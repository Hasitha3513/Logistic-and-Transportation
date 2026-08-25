package com.transportlogistics.app.freight.order.adapters.inbound.web.dto.response;

import java.util.List;

public record FreightOrderPageResponse(List<FreightOrderResponse> content, int page, int limit,
                                       long totalElements, int totalPages) { }
