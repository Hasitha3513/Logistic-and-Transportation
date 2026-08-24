package com.transportlogistics.app.reporting.application.ports.in;

import com.transportlogistics.app.reporting.domain.model.OperationsDashboard;

import java.time.LocalDate;

public interface OperationsDashboardUseCase {

    OperationsDashboard getOperationsDashboard(LocalDate date);
}
