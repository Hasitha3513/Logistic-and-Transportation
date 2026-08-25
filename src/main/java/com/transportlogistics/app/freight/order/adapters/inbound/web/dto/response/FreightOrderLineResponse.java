package com.transportlogistics.app.freight.order.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FreightOrderLineResponse(UUID id, String description, BigDecimal quantity) { }
