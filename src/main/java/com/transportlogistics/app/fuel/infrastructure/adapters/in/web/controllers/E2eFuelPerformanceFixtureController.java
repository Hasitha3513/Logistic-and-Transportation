package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Profile-restricted source fixture; unavailable outside the isolated E2E runtime. */
@RestController
@Profile("e2e")
@RequestMapping("/e2e/fuel-performance-fixtures")
public class E2eFuelPerformanceFixtureController {
    private final FuelIssueRepository issues;

    public E2eFuelPerformanceFixtureController(FuelIssueRepository issues) {
        this.issues = issues;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FixtureResponse create(@RequestBody FixtureRequest request) {
        if (request.vehicleIds() == null || request.driverIds() == null
                || request.vehicleIds().size() < 3 || request.driverIds().size() < 3) {
            throw new IllegalArgumentException("Three vehicles and drivers are required");
        }
        var created = new ArrayList<UUID>();
        var today = LocalDate.now(ZoneOffset.UTC);
        var fuelType = "US37_" + request.suffix().toUpperCase();
        for (int vehicleIndex = 0; vehicleIndex < 3; vehicleIndex++) {
            var vehicleId = request.vehicleIds().get(vehicleIndex);
            var driverId = request.driverIds().get(vehicleIndex);
            BigDecimal odometer = BigDecimal.valueOf(1_000L + vehicleIndex * 10_000L);
            BigDecimal hours = BigDecimal.valueOf(100L + vehicleIndex * 1_000L);
            for (int offset : List.of(13, 10, 8)) {
                created.add(save(request, vehicleId, driverId, today.minusDays(offset), 8,
                        odometer, hours, new BigDecimal("5.000"), fuelType));
                odometer = odometer.add(new BigDecimal("100.000"));
                hours = hours.add(new BigDecimal("10.000"));
            }
            for (int offset = 6; offset >= 0; offset--) {
                for (int hour : List.of(8, 16)) {
                    created.add(save(request, vehicleId, driverId, today.minusDays(offset), hour,
                            odometer, hours, new BigDecimal("5.000"), fuelType));
                    odometer = odometer.add(new BigDecimal("50.000"));
                    hours = hours.add(new BigDecimal("5.000"));
                }
            }
        }
        for (int vehicleIndex = 3; vehicleIndex < request.vehicleIds().size(); vehicleIndex++) {
            var vehicleId = request.vehicleIds().get(vehicleIndex);
            BigDecimal odometer = BigDecimal.valueOf(100_000L + vehicleIndex * 1_000L);
            for (int offset : List.of(13, 10, 8, 6, 3, 0)) {
                created.add(save(request, vehicleId, null, today.minusDays(offset), 8,
                        odometer, null, new BigDecimal("5.000"), fuelType));
                odometer = odometer.add(new BigDecimal("100.000"));
            }
        }
        created.add(save(request, request.vehicleIds().get(2), request.driverIds().get(2), today, 20,
                null, null, new BigDecimal("2.000"), "PETROL"));
        created.add(save(request, request.vehicleIds().get(2), request.driverIds().get(2), today.minusDays(1), 20,
                new BigDecimal("999999.000"), null, new BigDecimal("2.000"), "PETROL"));
        created.add(save(request, request.vehicleIds().get(2), request.driverIds().get(2), today, 21,
                new BigDecimal("999000.000"), null, new BigDecimal("2.000"), "PETROL"));
        return new FixtureResponse(List.copyOf(created), fuelType);
    }

    private UUID save(FixtureRequest request, UUID vehicleId, UUID driverId, LocalDate date, int hour,
                      BigDecimal odometer, BigDecimal engineHours, BigDecimal quantity, String fuelType) {
        var id = UUID.randomUUID();
        var occurredAt = date.atTime(hour, 0).atOffset(ZoneOffset.UTC);
        var issue = new FuelIssue(id, "US37-" + request.suffix() + '-' + id, vehicleId, null, driverId,
                fuelType, quantity, new BigDecimal("300.00"), quantity.multiply(new BigDecimal("300.00")),
                request.stationId(), odometer, engineHours, occurredAt, FuelIssueStatus.ISSUED,
                request.actorId(), request.actorId(), occurredAt, "US-37 isolated acceptance fixture",
                occurredAt, occurredAt);
        issues.save(issue);
        return id;
    }

    record FixtureRequest(String suffix, UUID stationId, UUID actorId,
                          List<UUID> vehicleIds, List<UUID> driverIds) { }
    record FixtureResponse(List<UUID> issueIds, String fuelType) { }
}
