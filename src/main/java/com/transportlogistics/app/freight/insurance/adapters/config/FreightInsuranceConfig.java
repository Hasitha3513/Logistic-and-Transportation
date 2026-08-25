package com.transportlogistics.app.freight.insurance.adapters.config;

import com.transportlogistics.app.freight.insurance.application.FreightInsuranceService;
import com.transportlogistics.app.freight.insurance.ports.inbound.FreightInsuranceUseCase;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceClaimRepository;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceNumberGenerator;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsurancePolicyRepository;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceTransaction;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class FreightInsuranceConfig {

    @Bean
    public FreightInsuranceUseCase freightInsuranceUseCase(
            FreightInsurancePolicyRepository policyRepo,
            FreightInsuranceClaimRepository claimRepo,
            FreightInsuranceNumberGenerator numberGenerator,
            FreightOrderLookup freightOrderLookup,
            FreightInsuranceTransaction transactions,
            Clock clock) {
        return new FreightInsuranceService(
                policyRepo,
                claimRepo,
                numberGenerator,
                freightOrderLookup,
                transactions,
                clock
        );
    }
}
