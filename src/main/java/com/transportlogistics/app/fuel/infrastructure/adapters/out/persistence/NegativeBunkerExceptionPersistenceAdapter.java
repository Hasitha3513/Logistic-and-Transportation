package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.NegativeBunkerExceptionRecorder;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
class NegativeBunkerExceptionPersistenceAdapter implements NegativeBunkerExceptionRecorder {
 private final JdbcClient jdbc;private final CurrentTenant tenants;
 NegativeBunkerExceptionPersistenceAdapter(JdbcClient jdbc,CurrentTenant tenants){this.jdbc=jdbc;this.tenants=tenants;}
 @Override @Transactional(propagation=Propagation.REQUIRES_NEW) public void record(UUID tankId,BigDecimal delta,BigDecimal balance,UUID actor){UUID t=tenants.required().tenantId(),id=UUID.randomUUID();UUID signalId=UUID.nameUUIDFromBytes((t+":"+tankId+":"+delta.stripTrailingZeros().toPlainString()+":"+balance.stripTrailingZeros().toPlainString()).getBytes(StandardCharsets.UTF_8));var now=OffsetDateTime.now();String metadata="{\"attemptedDeltaLiters\":\""+delta+"\",\"attemptedBalanceLiters\":\""+balance+"\"}";int inserted=jdbc.sql("""
 insert into fuel_exception_case(id,tenant_id,category,lifecycle,impact,source_type,source_id,source_event_id,summary,safe_metadata,tank_id,occurred_at,review_required,handoff_status,created_by,version,created_at,updated_at)
 values(:id,:t,'NEGATIVE_BUNKER_BALANCE','OPEN','HIGH','REJECTED_BUNKER_COMMAND',:tank,:signal,'Rejected Bunker stock command requires review',:metadata,:tank,:now,true,'NOT_REQUIRED',:actor,0,:now,:now)
 on conflict do nothing
 """).param("id",id).param("t",t).param("tank",tankId).param("signal",signalId).param("metadata",metadata).param("now",now).param("actor",actor).update();if(inserted==1)jdbc.sql("insert into fuel_exception_history(id,tenant_id,exception_id,action,to_lifecycle,detail,actor_id,created_at) values(:id,:t,:x,'CASE_CREATED','OPEN','Negative stock mutation rejected',:actor,:now)").param("id",UUID.randomUUID()).param("t",t).param("x",id).param("actor",actor).param("now",now).update();}
}
