package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.transportlogistics.app.support.AcceptanceDatabaseGuard;
import com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest;
import com.transportlogistics.app.tracking.domain.idle.IdleCandidate;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class Us51V103CandidatePersistencePostgreSqlAcceptanceTest {
 private static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>(DockerImageName.parse("timescale/timescaledb:latest-pg16").asCompatibleSubstituteFor("postgres")).withDatabaseName(AcceptanceDatabaseGuard.REQUIRED_DATABASE).withUsername("transport_test").withPassword("transport_test");
 private static final Instant NOW=Instant.parse("2026-09-18T12:00:00Z"); private static JdbcTemplate jdbc; private static Flyway flyway; private static JdbcIdleCandidatePersistenceAdapter adapter;
 @BeforeAll static void start(){DB.start();var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());AcceptanceDatabaseGuard.verify(ds);jdbc=new JdbcTemplate(ds);flyway=Flyway.configure().dataSource(ds).cleanDisabled(false).placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04()).load();adapter=new JdbcIdleCandidatePersistenceAdapter(jdbc,new TransactionTemplate(new DataSourceTransactionManager(ds)));}
 @AfterAll static void stop(){DB.stop();}
 @BeforeEach void reset(){flyway.clean();flyway.migrate();AcceptanceDatabaseGuard.verify(jdbc.getDataSource());}
 @Test void freshHeadIsV104AndCandidateEvidenceIsAppendOnlyWithRetention(){assertThat(jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",Integer.class)).isEqualTo(104);assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_name='tracking_idle_candidate_evidence' and column_name='retain_until'",Integer.class)).isOne();}
 @Test void populatedV102UpgradePreservesLegacyCandidateWithoutFabrication(){flyway.clean();Flyway.configure().dataSource(jdbc.getDataSource()).target("102").placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04()).load().migrate();UUID tenant=UUID.randomUUID(),vehicle=UUID.randomUUID(),device=device(tenant),episode=UUID.randomUUID();jdbc.update("INSERT INTO tracking_idle_episode(id,tenant_id,vehicle_id,device_id,lifecycle,start_source_timestamp,last_source_timestamp) VALUES(?,?,?,?,'CANDIDATE',?,?)",episode,tenant,vehicle,device,ts(NOW),ts(NOW));jdbc.update("INSERT INTO tracking_idle_state(tenant_id,vehicle_id,device_id,state,capability_state,latest_source_timestamp,candidate_started_at,last_qualifying_at,credited_seconds,evidence_count,open_episode_id,last_dedupe_identity) VALUES(?,?,?,'CANDIDATE','SUPPORTED',?,?,?,0,1,?,?)",tenant,vehicle,device,ts(NOW),ts(NOW),ts(NOW),episode,"a".repeat(64));flyway.migrate();assertThat(jdbc.queryForObject("select candidate_id from tracking_idle_state where tenant_id=? and vehicle_id=?",UUID.class,tenant,vehicle)).isEqualTo(episode);assertThat(jdbc.queryForObject("select count(*) from tracking_idle_episode where id=? and lifecycle='CANDIDATE'",Integer.class,episode)).isOne();}
 @Test void candidateRoundTripsDiscardsWithoutEpisodeAndPromotesAtomically(){UUID tenant=UUID.randomUUID(),vehicle=UUID.randomUUID(),device=device(tenant),id=UUID.randomUUID();IdleCandidate first=candidate(tenant,vehicle,device,id,0);var evidence=evidence(first,"b".repeat(64),NOW);assertThat(adapter.save(first,evidence,-1)).isEqualTo(com.transportlogistics.app.tracking.ports.outbound.IdleCandidatePersistencePort.Result.APPLIED);assertThat(adapter.save(first,evidence,0)).isEqualTo(com.transportlogistics.app.tracking.ports.outbound.IdleCandidatePersistencePort.Result.DUPLICATE);assertThat(adapter.find(tenant,vehicle)).contains(first);adapter.discard(tenant,vehicle,id,0);assertThat(adapter.find(tenant,vehicle)).isEmpty();assertThat(jdbc.queryForObject("select count(*) from tracking_idle_episode where tenant_id=? and vehicle_id=?",Integer.class,tenant,vehicle)).isZero();assertThat(jdbc.queryForObject("select count(*) from tracking_idle_candidate_evidence where candidate_id=?",Integer.class,id)).isOne();IdleCandidate second=candidate(tenant,vehicle,device,UUID.randomUUID(),0);adapter.save(second,evidence(second,"c".repeat(64),NOW.plusSeconds(60)),-1);UUID episode=adapter.promote(second,evidence(second,"d".repeat(64),NOW.plusSeconds(300)),NOW.plusSeconds(300),0);assertThat(jdbc.queryForObject("select candidate_id from tracking_idle_episode where id=?",UUID.class,episode)).isEqualTo(second.candidateId());assertThat(adapter.find(tenant,vehicle)).isEmpty();}
 @Test void updateIsRejectedAndExpiredEvidenceCanBePurged(){UUID tenant=UUID.randomUUID(),vehicle=UUID.randomUUID(),device=device(tenant);IdleCandidate c=candidate(tenant,vehicle,device,UUID.randomUUID(),0);var e=evidence(c,"e".repeat(64),NOW.minusSeconds(181L*86400));adapter.save(c,e,-1);assertThatThrownBy(()->jdbc.update("update tracking_idle_candidate_evidence set outcome='UNKNOWN' where id=?",e.id())).isInstanceOf(RuntimeException.class);assertThat(adapter.purgeExpired(NOW,100)).isOne();}
 private static IdleCandidate candidate(UUID t,UUID v,UUID d,UUID c,long version){return new IdleCandidate(t,v,d,c,UUID.randomUUID(),NOW,NOW,NOW,null,0,1,"f".repeat(64),version);}
 private static IdleCandidate.Evidence evidence(IdleCandidate c,String dedupe,Instant at){return new IdleCandidate.Evidence(UUID.randomUUID(),c.tenantId(),c.candidateId(),c.vehicleId(),c.deviceId(),UUID.randomUUID(),at,dedupe,"QUALIFYING","RUNNING","DEVICE_NATIVE_CAN",BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.ZERO,0);}
 private static UUID device(UUID tenant){UUID id=UUID.randomUUID();jdbc.update("INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,lifecycle,registered_at,registered_by,version,created_at,updated_at) VALUES(?,?,?,'FIXTURE','ACTIVE',?,?,0,?,?)",id,tenant,id.toString(),ts(NOW),UUID.randomUUID(),ts(NOW),ts(NOW));return id;}
 private static Timestamp ts(Instant i){return Timestamp.from(i);}
}
