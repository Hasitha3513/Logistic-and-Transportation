package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.ports.inbound.GpsExceptionUseCase.Acknowledgement;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionManagementPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class JdbcGpsExceptionManagementAdapter implements GpsExceptionManagementPort {
    private final JdbcTemplate jdbc; private final ObjectMapper json;
    JdbcGpsExceptionManagementAdapter(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
    @Override public void serialize(UUID tenantId,String key){
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> null,
                tenantId+"|GPS_EXCEPTION_ACK|"+key);
    }
    @Override public Optional<Command> command(UUID tenantId,String key){
        return jdbc.query("SELECT request_fingerprint,actor_id,response_snapshot::text FROM tracking_gps_exception_acknowledgement_command WHERE tenant_id=? AND idempotency_key=?",
                rs->{if(!rs.next())return Optional.empty();try{return Optional.of(new Command(rs.getString(1),rs.getObject(2,UUID.class),json.readValue(rs.getString(3),Acknowledgement.class)));}catch(Exception e){throw new IllegalStateException("Stored GPS acknowledgement response is invalid",e);}},tenantId,key);
    }
    @Override public void complete(UUID tenantId,String key,String fingerprint,UUID actorId,UUID episodeId,long expectedVersion,Acknowledgement response,Instant now){try{
        jdbc.update("INSERT INTO tracking_gps_exception_acknowledgement_command(id,tenant_id,idempotency_key,request_fingerprint,actor_id,episode_id,expected_version,response_snapshot,completed_at,created_at) VALUES(?,?,?,?,?,?,?,?::jsonb,?,?)",
                UUID.randomUUID(),tenantId,key,fingerprint,actorId,episodeId,expectedVersion,json.writeValueAsString(response),Timestamp.from(now),Timestamp.from(now));
    }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("GPS acknowledgement response cannot be stored",e);}}
    @Override public void audit(UUID tenantId,UUID actorId,UUID episodeId,String correlationId,Instant now){
        String correlation=correlationId==null?"NONE":correlationId.replaceAll("[\\r\\n\\p{Cntrl}]","");
        if(correlation.length()>120)correlation=correlation.substring(0,120);
        jdbc.update("INSERT INTO tracking_audit_event(id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at) VALUES(?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),tenantId,actorId,"GPS_EXCEPTION_ACKNOWLEDGED","GPS_EXCEPTION_EPISODE",episodeId,
                "OUTCOME=SUCCESS;CORRELATION_ID="+correlation,Timestamp.from(now));
    }
}
