package com.transportlogistics.app.operations.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RootCauseAnalysisTest {
    @Test
    void requiresIndependentApproval() {
        UUID author = UUID.randomUUID();
        var rca = RootCauseAnalysis.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            RootCauseAnalysis.CauseCategory.PROCESS, "PROCESS_GAP", "Control step was missed", null,
            author, OffsetDateTime.now());
        assertThatThrownBy(() -> rca.approve(author, OffsetDateTime.now()))
            .isInstanceOf(BusinessRuleException.class);
        rca.approve(UUID.randomUUID(), OffsetDateTime.now());
        assertThat(rca.approved()).isTrue();
    }
}
