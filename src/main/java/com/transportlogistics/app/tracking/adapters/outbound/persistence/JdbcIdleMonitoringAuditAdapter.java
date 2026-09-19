package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringAuditPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class JdbcIdleMonitoringAuditAdapter implements IdleMonitoringAuditPort {
    private final JdbcTemplate jdbc;
    JdbcIdleMonitoringAuditAdapter(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public void record(UUID tenant,UUID actor,String correlation,String action,UUID target,
            String filters,int requested,int count,Instant occurredAt){
        String detail="CORRELATION_ID="+safe(correlation)+";FILTER_SHAPE="+safe(filters)+";LIMIT="+requested+";RESULT_COUNT="+count;
        jdbc.update("INSERT INTO tracking_audit_event(id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at) VALUES(?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),tenant,actor,action,"IDLE_MONITORING",target==null?tenant:target,detail,Timestamp.from(occurredAt));
    }
    private static String safe(String value){if(value==null)return "";String result=value.replaceAll("[^A-Za-z0-9_=;|-]","");return result.substring(0,Math.min(240,result.length()));}
}
