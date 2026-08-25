package com.transportlogistics.app.freight.insurance.ports.outbound;

public interface FreightInsuranceNumberGenerator {

    String nextPolicyNumber();

    String nextClaimNumber();
}
