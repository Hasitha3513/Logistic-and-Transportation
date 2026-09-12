package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.SpeedRuleManagementService;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.ports.inbound.SpeedRuleManagementUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SpeedRuleManagementPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR=UUID.fromString("50000000-0000-0000-0000-000000000010");
    private static final Instant NOW=Instant.parse("2026-09-12T06:00:00Z");
    @Autowired SpeedRuleManagementService service;
    @Autowired JdbcTemplate jdbc;

    @Test void createIsTenantScopedIdempotentAndAuditedOnce() {
        UUID tenant=UUID.randomUUID(), other=UUID.randomUUID();
        var first=service.create(context(tenant),create("Operational",80),"same-key");
        assertThat(service.create(context(tenant),create("Operational",80),"same-key").id()).isEqualTo(first.id());
        assertThat(service.create(context(other),create("Operational",80),"same-key").id()).isNotEqualTo(first.id());
        assertThat(service.rule(other,first.id())).isEmpty();
        assertThatThrownBy(()->service.create(context(tenant),create("Changed",90),"same-key"))
                .isInstanceOfSatisfying(ConflictException.class,e->assertThat(e.code()).isEqualTo("IDEMPOTENCY_KEY_CONFLICT"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_audit_event WHERE tenant_id=? AND target_id=? AND action='SPEED_RULE_CREATED'",Integer.class,tenant,first.id())).isOne();
    }

    @Test void lifecycleReactivationRetirementAndStaleVersionAreSafe() {
        UUID tenant=UUID.randomUUID();
        var draft=service.create(context(tenant),create("Lifecycle",80),"create");
        var active=service.activate(context(tenant),draft.id(),draft.ruleVersion(),"activate");
        var disabled=service.disable(context(tenant),draft.id(),active.ruleVersion(),"maintenance","disable");
        var reactivated=service.activate(context(tenant),draft.id(),disabled.ruleVersion(),"reactivate");
        var disabledAgain=service.disable(context(tenant),draft.id(),reactivated.ruleVersion(),"retire prep","disable-two");
        var retired=service.retire(context(tenant),draft.id(),disabledAgain.ruleVersion(),"obsolete","retire");
        assertThat(retired.lifecycle()).isEqualTo(SpeedRule.Lifecycle.RETIRED);
        assertThatThrownBy(()->service.activate(context(tenant),draft.id(),retired.ruleVersion(),"invalid"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->service.update(context(tenant),draft.id(),1,update("stale",81)))
                .isInstanceOfSatisfying(ConflictException.class,e->assertThat(e.code()).isEqualTo("SPEED_RULE_STALE_VERSION"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_audit_event WHERE tenant_id=? AND target_id=? AND action IN ('SPEED_RULE_ACTIVATED','SPEED_RULE_DISABLED','SPEED_RULE_RETIRED')",Integer.class,tenant,draft.id())).isEqualTo(5);
    }

    @Test void activeFallbackIsUniqueAndForeignTenantMutationIsNotFound() {
        UUID tenant=UUID.randomUUID(), other=UUID.randomUUID();
        var first=service.create(context(tenant),create("First",80),"one");
        var second=service.create(context(tenant),create("Second",90),"two");
        service.activate(context(tenant),first.id(),1,"active-one");
        assertThatThrownBy(()->service.activate(context(tenant),second.id(),1,"active-two"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(()->service.update(context(other),first.id(),2,update("Intrusion",90)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test void queriesEnforceFrozenBoundsAndRequiredRange() {
        UUID tenant=UUID.randomUUID();
        service.create(context(tenant),create("Query",80),"query");
        assertThat(service.rules(tenant,null,null,0,20).total()).isOne();
        assertThatThrownBy(()->service.rules(tenant,null,null,0,101)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->service.episodes(tenant,null,null,NOW,NOW.plusSeconds(32L*86400),null,100)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->service.episodes(tenant,null,null,NOW,NOW.plusSeconds(1),null,501)).isInstanceOf(BusinessRuleException.class);
    }

    private static SpeedRuleManagementUseCase.Context context(UUID tenant) { return new SpeedRuleManagementUseCase.Context(tenant,ACTOR,"acceptance",NOW); }
    private static SpeedRuleManagementUseCase.CreateRule create(String name,int threshold) { return new SpeedRuleManagementUseCase.CreateRule(name,SpeedRule.Scope.TENANT,null,null,SpeedKph.threshold(BigDecimal.valueOf(threshold))); }
    private static SpeedRuleManagementUseCase.UpdateRule update(String name,int threshold) { return new SpeedRuleManagementUseCase.UpdateRule(name,null,null,SpeedKph.threshold(BigDecimal.valueOf(threshold))); }
}
