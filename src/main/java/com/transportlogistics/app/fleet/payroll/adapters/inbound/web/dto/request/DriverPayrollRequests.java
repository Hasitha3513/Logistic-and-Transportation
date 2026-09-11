package com.transportlogistics.app.fleet.payroll.adapters.inbound.web.dto.request;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollInputBatch;
import com.transportlogistics.app.fleet.payroll.domain.DriverPayrollInputLine;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.math.*;import java.time.*;import java.util.*;
public final class DriverPayrollRequests{private DriverPayrollRequests(){}
 public record Create(@NotNull DriverPayrollInputBatch.Type type,UUID correctionOfBatchId,@NotNull LocalDate periodStart,@NotNull LocalDate periodEndExclusive,@NotNull OffsetDateTime cutoffAt,@NotBlank @Pattern(regexp="[A-Za-z]{3}")String currency){}
 public record Line(UUID id,@NotNull UUID driverId,@NotNull UUID tripId,@NotBlank String tripNumber,@NotNull DriverPayrollInputLine.Category category,@NotBlank @Size(max=80)String reasonCode,@NotBlank @Size(max=500)String description,@NotNull @DecimalMin("0")BigDecimal quantity,@NotNull DriverPayrollInputLine.Unit unit,@NotNull @DecimalMin("0")BigDecimal rate,@DecimalMin("0")BigDecimal amount,UUID originalLineId){}
 public record ReplaceLines(long version,@NotEmpty List<@Valid Line> lines){}
 public record Version(long version){}
 public record Correction(long version,@NotNull LocalDate periodStart,@NotNull LocalDate periodEndExclusive,@NotNull OffsetDateTime cutoffAt,@NotBlank @Pattern(regexp="[A-Za-z]{3}")String currency,@NotEmpty List<@Valid Line> lines){}
 public record Mapping(@NotBlank @Pattern(regexp="[A-Z0-9_-]{1,80}")String externalSystemAlias,@NotBlank @Size(max=160)String externalWorkerReference,boolean active,long version){}
}
