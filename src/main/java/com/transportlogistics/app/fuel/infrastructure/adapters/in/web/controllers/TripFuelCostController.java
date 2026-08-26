package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fuel.application.ports.in.TripFuelCostUseCase;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response.TripFuelCostResponse;
import com.transportlogistics.app.fuel.infrastructure.adapters.in.web.mappers.TripFuelCostWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}/fuel-cost")
public class TripFuelCostController {

    private final TripFuelCostUseCase tripFuelCostUseCase;
    private final TripFuelCostWebMapper mapper;

    public TripFuelCostController(TripFuelCostUseCase tripFuelCostUseCase, TripFuelCostWebMapper mapper) {
        this.tripFuelCostUseCase = tripFuelCostUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<TripFuelCostResponse> getTripFuelCost(@PathVariable UUID tripId) {
        var cost = tripFuelCostUseCase.getTripFuelCost(tripId);
        return ResponseEntity.ok(mapper.toResponse(cost));
    }
}
