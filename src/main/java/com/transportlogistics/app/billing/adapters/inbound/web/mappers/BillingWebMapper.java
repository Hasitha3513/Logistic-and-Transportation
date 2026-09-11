package com.transportlogistics.app.billing.adapters.inbound.web.mappers;
import com.transportlogistics.app.billing.adapters.inbound.web.dto.response.BillingResponse;
import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import org.mapstruct.Mapper;
@Mapper(componentModel="spring") public interface BillingWebMapper {BillingResponse response(TransportBillingRecord value);}
