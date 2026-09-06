package com.transportlogistics.app.fuel.application.ports.out;
import java.util.UUID;
public interface FuelExceptionSourceValidator { boolean exists(UUID tenantId,String sourceType,UUID sourceId); }
