package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import java.util.List;

public record DeliveryOrderPageResponse(List<DeliveryOrderResponse> content, int page, int size,
                                        long totalElements, int totalPages) {}
