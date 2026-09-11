package com.transportlogistics.app.fleet.infrastructure.adapters.integration;
import com.transportlogistics.app.fleet.FuelExceptionReadingAccess;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
@Component
class FuelExceptionReadingAccessAdapter implements FuelExceptionReadingAccess {
 private final VehicleReadingUseCase readings;
 FuelExceptionReadingAccessAdapter(VehicleReadingUseCase readings){this.readings=readings;}
 @Override public boolean exists(UUID id){try{readings.get(id);return true;}catch(NotFoundException ex){return false;}}
 @Override public UUID correct(UUID vehicleId,UUID readingId,BigDecimal value,String reason,OffsetDateTime at,UUID actor){return readings.correct(new VehicleReadingUseCase.CorrectCommand(vehicleId,readingId,value,reason,at,actor)).id();}
}
