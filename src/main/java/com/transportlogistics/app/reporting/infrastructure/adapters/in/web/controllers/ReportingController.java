package com.transportlogistics.app.reporting.infrastructure.adapters.in.web.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class ReportingController {

    @GetMapping("/dashboard/operations")
    Map<String, Object> dashboard(@RequestParam(required = false) LocalDate date) {
        return Map.of("date", date == null ? LocalDate.now() : date, "status", "READY");
    }

    @GetMapping("/reports/trips")
    Map<String, Object> trips(@RequestParam LocalDate fromDate,
                              @RequestParam LocalDate toDate,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int limit,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) UUID customerId) {
        return Map.of("content", List.of(), "page", page, "limit", limit, "fromDate", fromDate, "toDate", toDate);
    }

    @GetMapping("/reports/driver-assignments")
    List<Map<String, Object>> driverAssignments(@RequestParam LocalDate fromDate,
                                                @RequestParam LocalDate toDate,
                                                @RequestParam(required = false) UUID driverId) {
        return List.of();
    }

    @GetMapping("/reports/vehicle-utilization")
    List<Map<String, Object>> vehicleUtilization(@RequestParam LocalDate fromDate,
                                                 @RequestParam LocalDate toDate,
                                                 @RequestParam(required = false) UUID vehicleId) {
        return List.of();
    }
}
