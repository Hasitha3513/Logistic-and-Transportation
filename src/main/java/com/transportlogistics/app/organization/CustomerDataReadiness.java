package com.transportlogistics.app.organization;

/** Organization-owned readiness query for opt-in local data provisioning. */
public interface CustomerDataReadiness {
    boolean anyCustomerExists();
}
