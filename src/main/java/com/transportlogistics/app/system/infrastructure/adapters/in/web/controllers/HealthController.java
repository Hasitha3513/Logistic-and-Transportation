package com.transportlogistics.app.system.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.system.infrastructure.adapters.in.web.dto.response.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
class HealthController {

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("UP", OffsetDateTime.now());
    }
}
