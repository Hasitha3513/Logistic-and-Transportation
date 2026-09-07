package com.transportlogistics.app.fleet;
import java.time.*;import java.util.*;
/** Published composition port implemented by system for US-46 Trip facts. */
public interface DriverPayrollSourcePort {Optional<TripFact>trip(UUID driverId,UUID tripId,LocalDate start,LocalDate endExclusive);record TripFact(UUID tripId,String tripNumber,String status,UUID driverId,OffsetDateTime actualEndTime){}}
