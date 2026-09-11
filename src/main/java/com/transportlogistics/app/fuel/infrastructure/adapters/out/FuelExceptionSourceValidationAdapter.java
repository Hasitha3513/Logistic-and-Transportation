package com.transportlogistics.app.fuel.infrastructure.adapters.out;
import com.transportlogistics.app.fleet.FuelExceptionReadingAccess;
import com.transportlogistics.app.fleet.DriverLookup;
import com.transportlogistics.app.fleet.VehicleFuelContextLookup;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionSourceValidator;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionStore;
import com.transportlogistics.app.trip.TripFuelContextLookup;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
class FuelExceptionSourceValidationAdapter implements FuelExceptionSourceValidator {
 private final FuelExceptionStore local;private final FuelExceptionReadingAccess fleet;
 private final VehicleFuelContextLookup vehicles; private final TripFuelContextLookup trips; private final DriverLookup drivers;
 FuelExceptionSourceValidationAdapter(FuelExceptionStore local,FuelExceptionReadingAccess fleet,
                                      VehicleFuelContextLookup vehicles,TripFuelContextLookup trips,DriverLookup drivers){
     this.local=local;this.fleet=fleet;this.vehicles=vehicles;this.trips=trips;this.drivers=drivers;
 }
 @Override public boolean exists(UUID tenant,String type,UUID id){return "VEHICLE_READING".equals(type)?fleet.exists(id):local.sourceExists(tenant,type,id);}
 @Override public boolean emergencyReferencesExist(UUID tenant,UUID vehicleId,UUID tripId,UUID driverId){
     if(vehicleId==null||vehicles.find(vehicleId).isEmpty()||(tripId==null&&driverId==null))return false;
     boolean tripValid=tripId!=null&&trips.find(tripId).filter(trip->vehicleId.equals(trip.vehicleId())).isPresent();
     boolean driverValid=driverId!=null&&drivers.findDriver(driverId).isPresent();
     return tripValid||driverValid;
 }
}
