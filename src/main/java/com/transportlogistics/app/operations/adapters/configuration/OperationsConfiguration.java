package com.transportlogistics.app.operations.adapters.configuration;

import com.transportlogistics.app.operations.application.OperationalExceptionService;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import com.transportlogistics.app.operations.ports.outbound.CorrectiveActionRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationalAssigneeDirectory;
import com.transportlogistics.app.operations.ports.outbound.OperationalExceptionCaseRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationalExceptionHistoryRepository;
import com.transportlogistics.app.operations.ports.outbound.OperationsEventPublisher;
import com.transportlogistics.app.operations.ports.outbound.OperationsTransaction;
import com.transportlogistics.app.operations.ports.outbound.RootCauseAnalysisRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class OperationsConfiguration {
    @Bean
    OperationalExceptionUseCase operationalExceptionUseCase(OperationalExceptionCaseRepository cases,
            CorrectiveActionRepository actions, RootCauseAnalysisRepository rcas,
            OperationalExceptionHistoryRepository history, OperationalAssigneeDirectory assignees,
            OperationsEventPublisher events, OperationsTransaction transactions, Clock clock) {
        return new OperationalExceptionService(cases, actions, rcas, history, assignees, events, transactions, clock);
    }
}
