package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.freight.FreightReportingQuery;
import com.transportlogistics.app.freight.FreightReportingQuery.FreightShipmentReportItem;
import com.transportlogistics.app.reporting.application.ports.in.FreightReportUseCase.Filter;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FreightReportServiceTest {
    private final FreightReportingQuery query = mock(FreightReportingQuery.class);
    private final FreightReportService service = new FreightReportService(query);

    @Test
    void rejectsInvalidDateRangeAndUnknownSort() {
        assertThatThrownBy(() -> service.summary(filter(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1))))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.shipments(filter(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)),
                0, 20, "tenantId", "ASC")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void boundsPaginationAndDelegatesOnlyWhitelistedSort() {
        when(query.shipments(any(), any())).thenReturn(new PageImpl<>(List.of()));
        service.shipments(filter(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)), -1, 500,
                "orderNumber", "ASC");
        verify(query).shipments(any(), eq(org.springframework.data.domain.PageRequest.of(0, 100,
                org.springframework.data.domain.Sort.Direction.ASC, "orderNumber")));
    }

    @Test
    void csvEscapesSpreadsheetFormulaAndReportsIncompleteDataHonestly() {
        var row = new FreightShipmentReportItem(UUID.randomUUID(), "=danger", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), OffsetDateTime.parse("2026-08-01T00:00:00Z"), null, null, null, null, false,
                0, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, "INCOMPLETE",
                List.of("VEHICLE_CAPACITY_MISSING"));
        when(query.exportShipments(any(), eq(5001))).thenReturn(List.of(row));
        String csv = new String(service.exportCsv(filter(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2))),
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("\"'=danger\"").contains("INCOMPLETE").contains("VEHICLE_CAPACITY_MISSING");
    }

    private Filter filter(LocalDate from, LocalDate to) {
        return new Filter(from, to, null, null, null, null, null, null, null, null, null);
    }
}
