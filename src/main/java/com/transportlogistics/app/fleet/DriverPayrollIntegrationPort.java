package com.transportlogistics.app.fleet;
import com.transportlogistics.app.shared.DurableEventEnvelope;import java.util.*;
/** Published composition port for the governed US-46 outbound exchange. */
public interface DriverPayrollIntegrationPort {Optional<UUID>activePayrollConfiguration(UUID tenantId);void publish(DurableEventEnvelope event);}
