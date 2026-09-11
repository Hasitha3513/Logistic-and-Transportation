package com.transportlogistics.app.billing.adapters.configuration;
import com.transportlogistics.app.billing.ports.outbound.BillingCompliancePort;
import com.transportlogistics.app.billing.ports.outbound.BillingIntegrationPort;
import com.transportlogistics.app.billing.ports.outbound.BillingSourcePort;
import com.transportlogistics.app.billing.ports.outbound.BillingStore;
import com.transportlogistics.app.billing.ports.outbound.BillingTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.billing.application.TransportBillingService;
import com.transportlogistics.app.tenancy.TenantDirectory;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration class BillingConfiguration {@Bean TransportBillingService transportBillingUseCase(BillingStore s,BillingSourcePort source,BillingCompliancePort compliance,BillingIntegrationPort integration,BillingTransaction tx,TenantDirectory tenants,ObjectMapper json,Clock clock){return new TransportBillingService(s,source,compliance,integration,tx,tenants,json,clock);}}
