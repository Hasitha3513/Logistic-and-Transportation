package com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.offlinesync.application.ports.in.OfflineSyncUseCase;
import com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.request.OfflineSyncBatchRequest;
import com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.response.OfflineSyncBatchResponse;
import com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.mappers.OfflineSyncWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/offline-sync/operations")
public class OfflineSyncController {
    private final OfflineSyncUseCase useCase;
    private final OfflineSyncWebMapper mapper;

    public OfflineSyncController(OfflineSyncUseCase useCase, OfflineSyncWebMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<OfflineSyncBatchResponse> synchronize(
            @Valid @RequestBody OfflineSyncBatchRequest request,
            Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toUnmodifiableSet());
        OfflineSyncUseCase.BatchResult result = useCase.synchronize(new OfflineSyncUseCase.BatchCommand(
                authentication.getName(), authorities, request.operations().stream().map(mapper::toCommand).toList()));
        return ResponseEntity.ok(mapper.toResponse(result));
    }
}
