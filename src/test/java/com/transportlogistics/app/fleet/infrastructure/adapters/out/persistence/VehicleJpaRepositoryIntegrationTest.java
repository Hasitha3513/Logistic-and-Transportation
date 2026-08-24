package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class VehicleJpaRepositoryIntegrationTest {

    @Autowired
    private VehicleRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Persist vehicle and query by unique registration, chassis, and engine number")
    void persistAndQueryUniqueIdentifiers() {
        var vehicleId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var reg = "REG-" + vehicleId.toString().substring(0, 8).toUpperCase();
        var chassis = "CHASSIS-" + vehicleId.toString().substring(0, 8).toUpperCase();
        var engine = "ENGINE-" + vehicleId.toString().substring(0, 8).toUpperCase();

        var vehicle = new Vehicle(vehicleId, reg, chassis, engine, categoryId, typeId,
                "Toyota", "Dyna", 2022, "COMPANY_OWNED", "AVAILABLE",
                1000.0, 50.0, 3000.0, true);

        vehicleHierarchy(jdbc, vehicle);
        var saved = repository.save(vehicle);

        assertThat(saved.id()).isEqualTo(vehicleId);
        assertThat(repository.findById(vehicleId)).isPresent();
        assertThat(repository.findByRegistrationNumber(reg.toLowerCase())).isPresent();
        assertThat(repository.findByChassisNumber(chassis.toLowerCase())).isPresent();
        assertThat(repository.findByEngineNumber(engine.toLowerCase())).isPresent();

        assertThat(repository.existsByRegistrationNumberAndIdNot(reg, UUID.randomUUID())).isTrue();
        assertThat(repository.existsByRegistrationNumberAndIdNot(reg, vehicleId)).isFalse();

        assertThat(repository.existsByChassisNumberAndIdNot(chassis, UUID.randomUUID())).isTrue();
        assertThat(repository.existsByChassisNumberAndIdNot(chassis, vehicleId)).isFalse();

        assertThat(repository.existsByEngineNumberAndIdNot(engine, UUID.randomUUID())).isTrue();
        assertThat(repository.existsByEngineNumberAndIdNot(engine, vehicleId)).isFalse();
    }
}
