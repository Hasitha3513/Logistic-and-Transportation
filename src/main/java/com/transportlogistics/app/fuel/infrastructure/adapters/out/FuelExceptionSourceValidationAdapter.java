package com.transportlogistics.app.fuel.infrastructure.adapters.out;
import com.transportlogistics.app.fleet.FuelExceptionReadingAccess;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionSourceValidator;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionStore;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
class FuelExceptionSourceValidationAdapter implements FuelExceptionSourceValidator {
 private final FuelExceptionStore local;private final FuelExceptionReadingAccess fleet;
 FuelExceptionSourceValidationAdapter(FuelExceptionStore local,FuelExceptionReadingAccess fleet){this.local=local;this.fleet=fleet;}
 @Override public boolean exists(UUID tenant,String type,UUID id){return "VEHICLE_READING".equals(type)?fleet.exists(id):local.sourceExists(tenant,type,id);}
}
