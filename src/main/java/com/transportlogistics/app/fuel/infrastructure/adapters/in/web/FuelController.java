package com.transportlogistics.app.fuel.infrastructure.adapters.in.web;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
class FuelController {
    private final FuelIssueUseCase issues;
    private final FuelStationUseCase stations;

    FuelController(FuelIssueUseCase issues, FuelStationUseCase stations) {
        this.issues = issues;
        this.stations = stations;
    }

    @GetMapping("/fuel-issues")
    PageResponse<FuelIssueResponse> search(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int limit,
                                           @RequestParam(required = false) UUID vehicleId,
                                           @RequestParam(required = false) UUID tripId,
                                           @RequestParam(required = false) FuelIssueStatus status,
                                           @RequestParam(required = false) LocalDate fromDate,
                                           @RequestParam(required = false) LocalDate toDate,
                                           @RequestParam(required = false) String voucherNumber) {
        var result = issues.search(new FuelIssueUseCase.SearchQuery(page, limit, vehicleId, tripId, status,
                fromDate, toDate, voucherNumber));
        return new PageResponse<>(result.content().stream().map(this::response).toList(), result.page(),
                result.limit(), result.totalElements(), result.totalPages());
    }

    @PostMapping("/fuel-issues")
    @ResponseStatus(HttpStatus.CREATED)
    FuelIssueResponse create(@Valid @RequestBody FuelIssueRequest request, Principal principal) {
        return response(issues.create(request.createCommand(), actor(principal)));
    }

    @GetMapping("/fuel-issues/{id}")
    FuelIssueResponse get(@PathVariable UUID id) {
        return response(issues.get(id));
    }

    @PutMapping("/fuel-issues/{id}")
    FuelIssueResponse update(@PathVariable UUID id, @Valid @RequestBody FuelIssueRequest request,
                             Principal principal) {
        return response(issues.update(id, request.updateCommand(), actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/submit")
    FuelIssueResponse submit(@PathVariable UUID id, Principal principal) {
        return response(issues.submit(id, actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/authorize")
    FuelIssueResponse authorize(@PathVariable UUID id, @RequestBody(required = false) AuthorizationRequest request,
                                Principal principal) {
        return response(issues.authorize(id, request == null ? null : request.comment(), actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/issue")
    FuelIssueResponse issue(@PathVariable UUID id, Principal principal) {
        return response(issues.issue(id, actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/cancel")
    FuelIssueResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancellationRequest request,
                             Principal principal) {
        return response(issues.cancel(id, request.reason(), actor(principal)));
    }

    @GetMapping("/fuel-issues/{id}/history")
    List<FuelIssueHistory> history(@PathVariable UUID id) {
        return issues.history(id);
    }

    @GetMapping("/fuel-stations")
    List<FuelStation> stations(@RequestParam(required = false) Boolean active) {
        return stations.list(active);
    }

    @PostMapping("/fuel-stations")
    @ResponseStatus(HttpStatus.CREATED)
    FuelStation createStation(@Valid @RequestBody FuelStationRequest request) {
        return stations.create(request.command());
    }

    @GetMapping("/fuel-stations/{id}")
    FuelStation station(@PathVariable UUID id) {
        return stations.get(id);
    }

    @PutMapping("/fuel-stations/{id}")
    FuelStation updateStation(@PathVariable UUID id, @Valid @RequestBody FuelStationRequest request) {
        return stations.update(id, request.command());
    }

    private FuelIssueResponse response(FuelIssue issue) {
        FuelStation station = stations.get(issue.stationId());
        return new FuelIssueResponse(issue.id(), issue.voucherNumber(), new Reference(issue.vehicleId()),
                nullableReference(issue.tripId()), nullableReference(issue.driverId()), issue.fuelType(),
                issue.quantity(), issue.unitPrice(), issue.totalAmount(), station, issue.odometer(),
                issue.engineHours(), issue.issueDateTime(), issue.status(), issue.requestedBy(), issue.authorizedBy(),
                issue.authorizationDateTime(), issue.notes(), issue.createdAt(), issue.updatedAt());
    }

    private Reference nullableReference(UUID id) {
        return id == null ? null : new Reference(id);
    }

    private String actor(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    record FuelIssueRequest(@NotNull UUID vehicleId, UUID tripId, UUID driverId,
                            @NotBlank @Size(max = 40) String fuelType,
                            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
                            @DecimalMin(value = "0.0") BigDecimal unitPrice,
                            @NotNull UUID stationId, @DecimalMin(value = "0.0") BigDecimal odometer,
                            @DecimalMin(value = "0.0") BigDecimal engineHours,
                            @NotNull OffsetDateTime issueDateTime, @Size(max = 1000) String notes) {
        FuelIssueUseCase.CreateCommand createCommand() {
            return new FuelIssueUseCase.CreateCommand(vehicleId, tripId, driverId, fuelType, quantity, unitPrice,
                    stationId, odometer, engineHours, issueDateTime, notes);
        }

        FuelIssueUseCase.UpdateCommand updateCommand() {
            return new FuelIssueUseCase.UpdateCommand(vehicleId, tripId, driverId, fuelType, quantity, unitPrice,
                    stationId, odometer, engineHours, issueDateTime, notes);
        }
    }

    record AuthorizationRequest(@Size(max = 1000) String comment) {
    }

    record CancellationRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    record FuelStationRequest(@NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 160) String name,
                              @NotNull FuelStationType stationType, Boolean active, UUID vendorId, UUID locationId) {
        FuelStationUseCase.Command command() {
            return new FuelStationUseCase.Command(code, name, stationType, active, vendorId, locationId);
        }
    }

    record Reference(UUID id) {
    }

    record FuelIssueResponse(UUID id, String voucherNumber, Reference vehicle, Reference trip, Reference driver,
                             String fuelType, BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalAmount,
                             FuelStation station, BigDecimal odometer, BigDecimal engineHours,
                             OffsetDateTime issueDateTime, FuelIssueStatus status, UUID requestedBy,
                             UUID authorizedBy, OffsetDateTime authorizationDateTime, String notes,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    record PageResponse<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {
    }
}
