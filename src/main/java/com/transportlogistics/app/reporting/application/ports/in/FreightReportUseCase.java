package com.transportlogistics.app.reporting.application.ports.in;

import com.transportlogistics.app.freight.FreightReportingQuery.FreightShipmentReportItem;
import com.transportlogistics.app.freight.FreightReportingQuery.FreightSummary;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface FreightReportUseCase {
    FreightSummary summary(Filter filter);
    Page<FreightShipmentReportItem> shipments(Filter filter, int page, int size, String sort, String direction);
    byte[] exportCsv(Filter filter);

    record Filter(LocalDate fromDate, LocalDate toDate, UUID customerId, UUID freightOrderId,
                  UUID originLocationId, UUID destinationLocationId, String loadPlanStatus,
                  String exceptionStatus, String exceptionType, String policyStatus, String claimStatus) {}
}
