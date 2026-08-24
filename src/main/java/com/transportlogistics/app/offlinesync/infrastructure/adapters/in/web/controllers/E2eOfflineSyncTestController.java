package com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.offlinesync.infrastructure.testing.E2eOfflineSyncControl;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@Profile("e2e")
@RequestMapping("/e2e/offline-sync")
public class E2eOfflineSyncTestController {
    private final E2eOfflineSyncControl control;
    private final JdbcClient jdbc;

    public E2eOfflineSyncTestController(E2eOfflineSyncControl control, JdbcClient jdbc) {
        this.control = control;
        this.jdbc = jdbc;
    }

    @PostMapping("/outcomes")
    ResponseEntity<Void> configure(@RequestBody OutcomeRequest request) {
        control.configure(request.operationId(), request.mode(), request.remainingAttempts() == null ? 1 : request.remainingAttempts());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/release")
    ResponseEntity<Void> release(@RequestParam UUID operationId) {
        control.release(operationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/outcomes")
    ResponseEntity<Void> reset() {
        control.reset();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inbox")
    Map<String, Object> inbox(@RequestParam UUID operationId) {
        return jdbc.sql("""
                select operation_id, operation_type, actor_id, aggregate_id, result_status, result_code, processed_at
                from offline_sync_operation where operation_id = :operationId
                """).param("operationId", operationId).query().singleRow();
    }

    record OutcomeRequest(UUID operationId, E2eOfflineSyncControl.Mode mode, Integer remainingAttempts) {}
}
