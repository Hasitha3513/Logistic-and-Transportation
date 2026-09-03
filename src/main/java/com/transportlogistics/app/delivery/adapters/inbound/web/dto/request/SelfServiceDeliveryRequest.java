package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = false)
public record SelfServiceDeliveryRequest(OffsetDateTime preferredStartAt, OffsetDateTime preferredEndAt, String notes) {}
