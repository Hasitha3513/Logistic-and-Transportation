package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringReadPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.UnusedFormalParameter")
final class JdbcIdleMonitoringReadAdapter implements IdleMonitoringReadPort {
    private final JdbcTemplate jdbc;
    JdbcIdleMonitoringReadAdapter(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Override public List<IdleMonitoringQuery.State> states(UUID tenant, IdleMonitoringQuery.StateFilter filter,
            Instant after,UUID afterId,int limit){
        StringBuilder sql=new StringBuilder("SELECT vehicle_id,state,capability_state,latest_source_timestamp,candidate_started_at,last_qualifying_at,credited_seconds,evidence_count,version FROM tracking_idle_state WHERE tenant_id=?");
        List<Object> args=new ArrayList<>();args.add(tenant);
        if(filter.vehicleId()!=null){sql.append(" AND vehicle_id=?");args.add(filter.vehicleId());}
        if(filter.state()!=null){sql.append(" AND state=?");args.add(filter.state());}
        if(after!=null){sql.append(" AND (latest_source_timestamp,vehicle_id)<(?,?)");args.add(Timestamp.from(after));args.add(afterId);}
        sql.append(" ORDER BY latest_source_timestamp DESC,vehicle_id DESC LIMIT ?");args.add(limit);
        return jdbc.query(sql.toString(),this::state,args.toArray());
    }
    @Override public List<IdleMonitoringQuery.Episode> episodes(UUID tenant,IdleMonitoringQuery.EpisodeFilter filter,
            Instant after,UUID afterId,int limit){
        StringBuilder sql=new StringBuilder("SELECT id,vehicle_id,lifecycle,start_source_timestamp,confirmed_at,last_source_timestamp,end_source_timestamp,end_reason,credited_seconds,evidence_count,version FROM tracking_idle_episode WHERE tenant_id=? AND lifecycle<>'CANDIDATE' AND start_source_timestamp>=? AND start_source_timestamp<?");
        List<Object> args=new ArrayList<>();args.add(tenant);args.add(Timestamp.from(filter.from()));args.add(Timestamp.from(filter.to()));
        if(filter.vehicleId()!=null){sql.append(" AND vehicle_id=?");args.add(filter.vehicleId());}
        if(filter.endReason()!=null){sql.append(" AND end_reason=?");args.add(filter.endReason());}
        if(after!=null){sql.append(" AND (start_source_timestamp,id)<(?,?)");args.add(Timestamp.from(after));args.add(afterId);}
        sql.append(" ORDER BY start_source_timestamp DESC,id DESC LIMIT ?");args.add(limit);
        return jdbc.query(sql.toString(),this::episode,args.toArray());
    }
    @Override public Optional<IdleMonitoringQuery.Episode> episode(UUID tenant,UUID id){
        return jdbc.query("SELECT id,vehicle_id,lifecycle,start_source_timestamp,confirmed_at,last_source_timestamp,end_source_timestamp,end_reason,credited_seconds,evidence_count,version FROM tracking_idle_episode WHERE tenant_id=? AND id=? AND lifecycle<>'CANDIDATE'",this::episode,tenant,id).stream().findFirst();
    }
    @Override public List<IdleMonitoringQuery.Evidence> evidence(UUID tenant,UUID episodeId,Instant after,UUID afterId,int limit){
        StringBuilder sql=new StringBuilder("SELECT id,source_timestamp,outcome,credited_delta_seconds,created_at FROM tracking_idle_episode_evidence WHERE tenant_id=? AND episode_id=?");
        List<Object> args=new ArrayList<>();args.add(tenant);args.add(episodeId);
        if(after!=null){sql.append(" AND (source_timestamp,id)>(?,?)");args.add(Timestamp.from(after));args.add(afterId);}
        sql.append(" ORDER BY source_timestamp ASC,id ASC LIMIT ?");args.add(limit);
        return jdbc.query(sql.toString(),this::evidence,args.toArray());
    }
    private IdleMonitoringQuery.State state(ResultSet row,int n)throws SQLException{return new IdleMonitoringQuery.State(row.getObject("vehicle_id",UUID.class),row.getString("state"),row.getString("capability_state"),instant(row,"latest_source_timestamp"),instant(row,"candidate_started_at"),instant(row,"last_qualifying_at"),row.getLong("credited_seconds"),row.getInt("evidence_count"),row.getLong("version"));}
    private IdleMonitoringQuery.Episode episode(ResultSet row,int n)throws SQLException{return new IdleMonitoringQuery.Episode(row.getObject("id",UUID.class),row.getObject("vehicle_id",UUID.class),row.getString("lifecycle"),instant(row,"start_source_timestamp"),instant(row,"confirmed_at"),instant(row,"last_source_timestamp"),instant(row,"end_source_timestamp"),row.getString("end_reason"),row.getLong("credited_seconds"),row.getInt("evidence_count"),row.getLong("version"));}
    private IdleMonitoringQuery.Evidence evidence(ResultSet row,int n)throws SQLException{return new IdleMonitoringQuery.Evidence(row.getObject("id",UUID.class),instant(row,"source_timestamp"),row.getString("outcome"),row.getInt("credited_delta_seconds"),instant(row,"created_at"));}
    private static Instant instant(ResultSet row,String column)throws SQLException{Timestamp value=row.getTimestamp(column);return value==null?null:value.toInstant();}
}
