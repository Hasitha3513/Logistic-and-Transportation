package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverServiceTest {

    private final DriverRepository repository = mock(DriverRepository.class);
    private final DriverService service = new DriverService(repository);

    @Test
    void updatePreservesTheAggregateRootIdentityFromTheUseCaseBoundary() {
        var pathId = UUID.randomUUID();
        var payloadId = UUID.randomUUID();
        var current = driver(pathId, "EMP-001");
        var requested = driver(payloadId, "EMP-002");
        when(repository.findById(pathId)).thenReturn(Optional.of(current));
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update(pathId, requested);

        assertThat(updated.id()).isEqualTo(pathId);
        assertThat(updated.employeeNumber()).isEqualTo("EMP-002");
        verify(repository).save(updated);
    }

    @Test
    void updateCannotUpsertAReplacementForAMissingAggregateRoot() {
        var pathId = UUID.randomUUID();
        when(repository.findById(pathId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(pathId, driver(UUID.randomUUID(), "EMP-002")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(pathId.toString());

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Driver driver(UUID id, String employeeNumber) {
        return new Driver(id, employeeNumber, "Alex", "Driver", null, null, "AVAILABLE", true);
    }
}
