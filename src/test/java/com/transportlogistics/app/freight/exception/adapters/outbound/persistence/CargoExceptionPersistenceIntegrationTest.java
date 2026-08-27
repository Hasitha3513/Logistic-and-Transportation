package com.transportlogistics.app.freight.exception.adapters.outbound.persistence;

import com.transportlogistics.app.freight.exception.domain.CargoException;
import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionStatus;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import com.transportlogistics.app.freight.exception.ports.inbound.CargoExceptionUseCase;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CargoExceptionPersistenceIntegrationTest {

    @Autowired
    private CargoExceptionUseCase exceptionUseCase;

    @Autowired
    private FreightOrderUseCase freightOrderUseCase;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID customerId;
    private UUID originId;
    private UUID destinationId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        originId = UUID.randomUUID();
        destinationId = UUID.randomUUID();

        jdbc.update("INSERT INTO customer (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                customerId, "CUST-CEX-" + shortId(customerId), "Exception Test Customer");
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                originId, "LOC-O-" + shortId(originId), "Origin Hub");
        jdbc.update("INSERT INTO location (id, code, name, active) VALUES (?, ?, ?, TRUE)",
                destinationId, "LOC-D-" + shortId(destinationId), "Destination Hub");

        var order = freightOrderUseCase.create(new FreightOrderUseCase.CreateCommand(
                customerId, originId, destinationId,
                OffsetDateTime.parse("2026-09-01T00:00:00Z"),
                OffsetDateTime.parse("2026-09-02T00:00:00Z"),
                "EXPRESS", "HIGH", null,
                List.of(new FreightOrderUseCase.LineCommand(null, "Sensitive Electronics", BigDecimal.TEN))
        ), "planner");
        orderId = order.id();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM cargo_exception_history");
        jdbc.update("DELETE FROM cargo_exception");
        jdbc.update("DELETE FROM freight_order_line");
        jdbc.update("DELETE FROM freight_order");
        jdbc.update("DELETE FROM location WHERE id IN (?, ?)", originId, destinationId);
        jdbc.update("DELETE FROM customer WHERE id = ?", customerId);
    }

    @Test
    @DisplayName("Round-trip exception record, hold, release, and resolution with retained history")
    void testCargoExceptionPersistenceRoundTrip() {
        // 1. Record exception
        var recordCmd = new CargoExceptionUseCase.RecordExceptionCommand(
                ExceptionType.DAMAGE,
                ExceptionSeverity.HIGH,
                orderId,
                null,
                null,
                "Package torn in transit",
                "Moisture risk",
                null,
                "Isolate in clean room"
        );
        CargoException exc = exceptionUseCase.record(recordCmd, "officer_1");
        assertThat(exc.getExceptionNumber()).startsWith("CEX-");
        assertThat(exc.getStatus()).isEqualTo(ExceptionStatus.OPEN);
        assertThat(exc.getSeverity()).isEqualTo(ExceptionSeverity.HIGH);

        // 2. Hold
        var holdCmd = new CargoExceptionUseCase.HoldExceptionCommand(
                "Hold at Bay 4", "Requires visual inspection", exc.getVersion()
        );
        CargoException held = exceptionUseCase.hold(exc.getId(), holdCmd, "supervisor");
        assertThat(held.getStatus()).isEqualTo(ExceptionStatus.HELD);
        assertThat(held.getRestriction()).isEqualTo("Hold at Bay 4");

        // 3. Release
        var releaseCmd = new CargoExceptionUseCase.ReleaseExceptionCommand(
                "Inspection passed, safe to handle", held.getVersion()
        );
        CargoException released = exceptionUseCase.release(exc.getId(), releaseCmd, "safety_officer");
        assertThat(released.getStatus()).isEqualTo(ExceptionStatus.OPEN);
        assertThat(released.getRestriction()).isNull();

        // 4. Resolve
        var resolveCmd = new CargoExceptionUseCase.ResolveExceptionCommand(
                "Repackaged with waterproof film and certified",
                "Re-sealed container",
                "QA certificate issued",
                released.getVersion()
        );
        CargoException resolved = exceptionUseCase.resolve(exc.getId(), resolveCmd, "manager");
        assertThat(resolved.getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
        assertThat(resolved.getResolution()).contains("Repackaged with waterproof film");

        // 5. Reload and verify retained history in database
        CargoException reloaded = exceptionUseCase.get(exc.getId());
        assertThat(reloaded.getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
        assertThat(reloaded.getHistory()).hasSize(3);
        assertThat(reloaded.getHistory().get(0).getAction()).isEqualTo("HOLD_APPLIED");
        assertThat(reloaded.getHistory().get(1).getAction()).isEqualTo("RELEASED");
        assertThat(reloaded.getHistory().get(2).getAction()).isEqualTo("RESOLVED");
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
