package com.transportlogistics.app.freight.order.adapters.config;

import com.transportlogistics.app.freight.order.application.FreightOrderService;
import com.transportlogistics.app.freight.order.application.FreightOrderLookupService;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.freight.order.ports.outbound.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class FreightOrderConfig {
    @Bean
    FreightOrderLookup freightOrderLookup(FreightOrderRepository orders) {
        return new FreightOrderLookupService(orders);
    }

    @Bean
    FreightOrderUseCase freightOrderUseCase(FreightOrderRepository orders, FreightOrderNumberGenerator numbers,
                                            FreightCustomerPort customers, FreightLocationPort locations,
                                            FreightOrderTransaction transactions, FreightOrderEventPublisher events,
                                            Clock clock) {
        return new FreightOrderService(orders, numbers, customers, locations, transactions, events, clock);
    }
}
