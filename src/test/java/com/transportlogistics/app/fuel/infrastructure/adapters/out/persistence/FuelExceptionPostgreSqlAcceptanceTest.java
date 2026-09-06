package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FuelExceptionPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
 @Autowired JdbcClient jdbc;
 @Test void v66OwnsSixTenantIndexedTablesFivePermissionsAndFuelOperationsIntake(){assertThat(jdbc.sql("select version from flyway_schema_history where success order by installed_rank desc limit 1").query(String.class).single()).isEqualTo("66");assertThat(jdbc.sql("select count(*) from information_schema.tables where table_schema='public' and table_name like 'fuel_exception_%'").query(Integer.class).single()).isEqualTo(6);assertThat(jdbc.sql("select count(*) from app_permission where code like 'FUEL_EXCEPTION_%' and active").query(Integer.class).single()).isEqualTo(5);assertThat(jdbc.sql("select check_clause from information_schema.check_constraints where constraint_name='ck_operational_exception_source_module'").query(String.class).single()).contains("FUEL");}
 @Test void activeSourceAndTenantConsistentChildrenAreEnforced(){UUID t=UUID.randomUUID(),source=UUID.randomUUID(),first=insertCase(t,source,"OPEN");assertThatThrownBy(()->insertCase(t,source,"UNDER_REVIEW")).isInstanceOf(DataIntegrityViolationException.class);jdbc.sql("update fuel_exception_case set lifecycle='RESOLVED' where id=:id").param("id",first).update();insertCase(t,source,"OPEN");assertThatThrownBy(()->jdbc.sql("insert into fuel_exception_note(id,tenant_id,exception_id,note,added_by,created_at) values(:id,:other,:x,'review required',:actor,:now)").param("id",UUID.randomUUID()).param("other",UUID.randomUUID()).param("x",first).param("actor",UUID.randomUUID()).param("now",OffsetDateTime.now()).update()).isInstanceOf(DataIntegrityViolationException.class);}
 private UUID insertCase(UUID t,UUID source,String state){UUID id=UUID.randomUUID(),actor=UUID.randomUUID();var now=OffsetDateTime.now();jdbc.sql("insert into fuel_exception_case(id,tenant_id,category,lifecycle,impact,source_type,source_id,summary,safe_metadata,occurred_at,review_required,handoff_status,created_by,version,created_at,updated_at) values(:id,:t,'SUSPECTED_FUEL_LOSS',:state,'MEDIUM','FUEL_ISSUE',:source,'Unexplained variance requires review','{}',:now,true,'NOT_REQUIRED',:actor,0,:now,:now)").param("id",id).param("t",t).param("state",state).param("source",source).param("now",now).param("actor",actor).update();return id;}
}
