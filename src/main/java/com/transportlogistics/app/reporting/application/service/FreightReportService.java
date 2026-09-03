package com.transportlogistics.app.reporting.application.service;

import com.transportlogistics.app.freight.FreightReportingQuery;
import com.transportlogistics.app.freight.FreightReportingQuery.FreightReportCriteria;
import com.transportlogistics.app.freight.FreightReportingQuery.FreightShipmentReportItem;
import com.transportlogistics.app.freight.FreightReportingQuery.FreightSummary;
import com.transportlogistics.app.reporting.application.ports.in.FreightReportUseCase;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class FreightReportService implements FreightReportUseCase {
    static final int EXPORT_LIMIT = 5_000;
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "orderNumber", "requestedPickupAt", "manifestNumber", "loadPlanStatus");
    private final FreightReportingQuery query;

    public FreightReportService(FreightReportingQuery query) { this.query = query; }

    @Override public FreightSummary summary(Filter filter) { return query.summary(criteria(filter)); }

    @Override
    public Page<FreightShipmentReportItem> shipments(Filter filter, int page, int size, String sort, String direction) {
        String safeSort = sort == null || sort.isBlank() ? "createdAt" : sort;
        if (!SORT_FIELDS.contains(safeSort)) throw new BusinessRuleException("INVALID_REPORT_SORT", "Unsupported freight report sort field");
        Sort.Direction safeDirection;
        try { safeDirection = Sort.Direction.fromOptionalString(direction == null ? "DESC" : direction).orElse(Sort.Direction.DESC); }
        catch (IllegalArgumentException ex) { throw new BusinessRuleException("INVALID_REPORT_SORT", "Sort direction must be ASC or DESC"); }
        return query.shipments(criteria(filter), PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), safeDirection, safeSort));
    }

    @Override
    public byte[] exportCsv(Filter filter) {
        var rows = query.exportShipments(criteria(filter), EXPORT_LIMIT + 1);
        if (rows.size() > EXPORT_LIMIT) throw new BusinessRuleException("REPORT_EXPORT_LIMIT_EXCEEDED", "Freight export exceeds 5000 rows; narrow the filters");
        StringBuilder csv = new StringBuilder("orderNumber,customerId,originLocationId,destinationLocationId,createdAt,requestedPickupAt,requestedDeliveryAt,manifestNumber,manifestFinalized,manifestItemCount,loadPlanNumber,loadPlanStatus,cargoWeightKg,cargoVolumeM3,payloadUtilizationPercent,volumeUtilizationPercent,complianceOutcome,incompleteDiagnostics\r\n");
        rows.forEach(row -> csv.append(row(row)).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private FreightReportCriteria criteria(Filter filter) {
        if (filter == null || filter.fromDate() == null || filter.toDate() == null) throw new BusinessRuleException("INVALID_DATE_RANGE", "Both fromDate and toDate are required");
        if (filter.fromDate().isAfter(filter.toDate())) throw new BusinessRuleException("INVALID_DATE_RANGE", "fromDate cannot be after toDate");
        return new FreightReportCriteria(filter.fromDate().atStartOfDay().atOffset(ZoneOffset.UTC),
                filter.toDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC), filter.customerId(),
                filter.freightOrderId(), filter.originLocationId(), filter.destinationLocationId(), filter.loadPlanStatus(),
                filter.exceptionStatus(), filter.exceptionType(), filter.policyStatus(), filter.claimStatus());
    }

    private String row(FreightShipmentReportItem r) {
        return String.join(",", csv(r.orderNumber()), csv(r.customerId()), csv(r.originLocationId()), csv(r.destinationLocationId()),
                csv(r.createdAt()), csv(r.requestedPickupAt()), csv(r.requestedDeliveryAt()), csv(r.manifestNumber()),
                csv(r.manifestFinalized()), csv(r.manifestItemCount()), csv(r.loadPlanNumber()), csv(r.loadPlanStatus()),
                csv(r.cargoWeightKg()), csv(r.cargoVolumeM3()), csv(r.payloadUtilizationPercent()),
                csv(r.volumeUtilizationPercent()), csv(r.complianceOutcome()), csv(String.join("|", r.incompleteDiagnostics())));
    }

    private String csv(Object raw) {
        String value = raw == null ? "" : raw.toString();
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
