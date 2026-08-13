package com.transportlogistics.app.system.infrastructure.adapters.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
class HealthController {
    @GetMapping("/health")
    Map<String, Object> health() {
        return Map.of("status", "UP", "timestamp", OffsetDateTime.now());
    }
}