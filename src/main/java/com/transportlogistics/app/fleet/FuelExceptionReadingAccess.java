package com.transportlogistics.app.fleet;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
/** Fleet-owned, tenant-enforced contract used by Fuel exception review. */
public interface FuelExceptionReadingAccess {
 boolean exists(UUID readingId);
 UUID correct(UUID vehicleId,UUID readingId,BigDecimal correctedValue,String reason,OffsetDateTime recordedAt,UUID actorId);
}
