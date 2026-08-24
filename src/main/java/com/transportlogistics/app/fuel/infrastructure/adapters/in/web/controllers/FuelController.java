package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.AuthorizationRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.CancellationRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.FuelIssueRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request.FuelStationRequest;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelIssueHistoryResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelIssueResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.FuelStationResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.PageResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.FuelWebMapper;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class FuelController {

    private final FuelIssueUseCase issues;
    private final FuelStationUseCase stations;
    private final FuelWebMapper mapper;

    public FuelController(FuelIssueUseCase issues, FuelStationUseCase stations, FuelWebMapper mapper) {
        this.issues = issues;
        this.stations = stations;
        this.mapper = mapper;
    }

    @GetMapping("/fuel-issues")
    public PageResponse<FuelIssueResponse> search(@RequestParam(defaultValue = "0") int page,
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
    public FuelIssueResponse create(@Valid @RequestBody FuelIssueRequest request, Principal principal) {
        return response(issues.create(request.createCommand(), actor(principal)));
    }

    @GetMapping("/fuel-issues/{id}")
    public FuelIssueResponse get(@PathVariable UUID id) {
        return response(issues.get(id));
    }

    @PutMapping("/fuel-issues/{id}")
    public FuelIssueResponse update(@PathVariable UUID id, @Valid @RequestBody FuelIssueRequest request,
                                    Principal principal) {
        return response(issues.update(id, request.updateCommand(), actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/submit")
    public FuelIssueResponse submit(@PathVariable UUID id, Principal principal) {
        return response(issues.submit(id, actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/authorize")
    public FuelIssueResponse authorize(@PathVariable UUID id,
                                       @RequestBody(required = false) AuthorizationRequest request,
                                       Principal principal) {
        return response(issues.authorize(id, request == null ? null : request.comment(), actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/issue")
    public FuelIssueResponse issue(@PathVariable UUID id, Principal principal) {
        return response(issues.issue(id, actor(principal)));
    }

    @PostMapping("/fuel-issues/{id}/cancel")
    public FuelIssueResponse cancel(@PathVariable UUID id,
                                    @Valid @RequestBody CancellationRequest request,
                                    Principal principal) {
        return response(issues.cancel(id, request.reason(), actor(principal)));
    }

    @GetMapping("/fuel-issues/{id}/history")
    public List<FuelIssueHistoryResponse> history(@PathVariable UUID id) {
        return mapper.toFuelIssueHistoryResponseList(issues.history(id));
    }

    @GetMapping("/fuel-stations")
    public List<FuelStationResponse> stations(@RequestParam(required = false) Boolean active) {
        return mapper.toFuelStationResponseList(stations.list(active));
    }

    @PostMapping("/fuel-stations")
    @ResponseStatus(HttpStatus.CREATED)
    public FuelStationResponse createStation(@Valid @RequestBody FuelStationRequest request) {
        return mapper.toResponse(stations.create(request.command()));
    }

    @GetMapping("/fuel-stations/{id}")
    public FuelStationResponse station(@PathVariable UUID id) {
        return mapper.toResponse(stations.get(id));
    }

    @PutMapping("/fuel-stations/{id}")
    public FuelStationResponse updateStation(@PathVariable UUID id,
                                             @Valid @RequestBody FuelStationRequest request) {
        return mapper.toResponse(stations.update(id, request.command()));
    }

    private FuelIssueResponse response(FuelIssue issue) {
        FuelStation station = stations.get(issue.stationId());
        return mapper.toResponse(issue, station);
    }

    private String actor(Principal principal) {
        return PrincipalUtils.resolveActorName(principal, null);
    }
}
