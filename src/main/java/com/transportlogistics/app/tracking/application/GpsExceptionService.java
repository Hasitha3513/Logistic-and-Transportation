package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus;
import com.transportlogistics.app.tracking.ports.inbound.GpsExceptionUseCase;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEvidenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionManagementPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionTransactionPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GpsExceptionService implements GpsExceptionUseCase {
    private static final Duration MAX_RANGE=Duration.ofDays(7);
    private final GpsExceptionRepositoryPort episodes; private final GpsExceptionEvidenceRepositoryPort evidence;
    private final GpsExceptionManagementPort management; private final GpsExceptionCursorPort cursors;
    private final GpsExceptionTransactionPort transactions;
    public GpsExceptionService(GpsExceptionRepositoryPort episodes,GpsExceptionEvidenceRepositoryPort evidence,
            GpsExceptionManagementPort management,GpsExceptionCursorPort cursors,GpsExceptionTransactionPort transactions){
        this.episodes=episodes;this.evidence=evidence;this.management=management;this.cursors=cursors;this.transactions=transactions;
    }
    @Override public Page<GpsExceptionEpisode> episodes(Context c,Filter f,String cursor,int limit){
        required(c); validate(f,limit); String binding=binding(f,"LIST"); var after=cursor==null?null:cursors.decode(c.tenantId(),binding,cursor);
        List<GpsExceptionEpisode> rows=episodes.search(c.tenantId(),f.from(),f.to(),f.status(),f.type(),f.severity(),f.vehicleId(),f.deviceId(),after==null?null:after.timestamp(),after==null?null:after.id(),limit+1);
        boolean more=rows.size()>limit; List<GpsExceptionEpisode> items=List.copyOf(rows.subList(0,Math.min(limit,rows.size())));
        String next=more?cursors.encode(c.tenantId(),binding,items.getLast().lastObservedAt(),items.getLast().id()):null; return new Page<>(items,next);
    }
    @Override public java.util.Optional<GpsExceptionEpisode> episode(Context c,UUID id){required(c);return episodes.findById(c.tenantId(),required(id,"Episode ID"));}
    @Override public Page<GpsExceptionEvidence> evidence(Context c,UUID episodeId,String cursor,int limit){required(c);bounds(limit);UUID id=required(episodeId,"Episode ID");
        if(episodes.findById(c.tenantId(),id).isEmpty())throw notFound(); String binding="EVIDENCE|"+id; var after=cursor==null?null:cursors.decode(c.tenantId(),binding,cursor);
        List<GpsExceptionEvidence> rows=evidence.findByEpisode(c.tenantId(),id,after==null?null:after.timestamp(),after==null?null:after.id(),limit+1);
        boolean more=rows.size()>limit; List<GpsExceptionEvidence> items=List.copyOf(rows.subList(0,Math.min(limit,rows.size())));
        String next=more?cursors.encode(c.tenantId(),binding,items.getLast().assessedAt(),items.getLast().id()):null;return new Page<>(items,next);
    }
    @Override public Acknowledgement acknowledge(Context c,UUID episodeId,long expectedVersion,String reason,String key){required(c);UUID id=required(episodeId,"Episode ID");String normalized=reason(reason);String commandKey=key(key);
        String fingerprint=hash("GPS_EXCEPTION_ACK_V1|"+c.tenantId()+"|"+c.actorId()+"|"+id+"|"+expectedVersion+"|"+normalized);
        return transactions.execute(()->{management.serialize(c.tenantId(),commandKey);var existing=management.command(c.tenantId(),commandKey);
            if(existing.isPresent()){if(!existing.get().fingerprint().equals(fingerprint)||!existing.get().actorId().equals(c.actorId()))throw conflict("GPS_EXCEPTION_IDEMPOTENCY_CONFLICT","Idempotency-Key was used for another request");return existing.get().response();}
            GpsExceptionEpisode before=episodes.findByIdForUpdate(c.tenantId(),id).orElseThrow(GpsExceptionService::notFound);
            if(before.status()==EpisodeStatus.ACKNOWLEDGED)throw conflict("GPS_EXCEPTION_ALREADY_ACKNOWLEDGED","GPS exception is already acknowledged");
            if(before.status()==EpisodeStatus.RESOLVED)throw conflict("GPS_EXCEPTION_RESOLVED","Resolved GPS exception cannot be acknowledged");
            if(before.version()!=expectedVersion)throw conflict("GPS_EXCEPTION_STALE_VERSION","GPS exception changed concurrently");
            GpsExceptionEpisode saved=episodes.save(before.acknowledge(normalized,c.now()));
            Acknowledgement response=new Acknowledgement(saved.id(),saved.status(),saved.severity(),saved.version(),c.now());
            management.complete(c.tenantId(),commandKey,fingerprint,c.actorId(),id,expectedVersion,response,c.now());
            management.audit(c.tenantId(),c.actorId(),id,c.correlationId(),c.now()); return response;});
    }
    private static void validate(Filter f,int limit){if(f==null||f.from()==null||f.to()==null||!f.from().isBefore(f.to())||Duration.between(f.from(),f.to()).compareTo(MAX_RANGE)>0)throw rule("GPS_EXCEPTION_RANGE_INVALID","Range must be positive and at most seven days");bounds(limit);}
    private static void bounds(int limit){if(limit<1||limit>500)throw rule("GPS_EXCEPTION_LIMIT_INVALID","Limit must be 1..500");}
    private static String binding(Filter f,String op){return op+"|"+f.from()+"|"+f.to()+"|"+f.status()+"|"+f.type()+"|"+f.severity()+"|"+f.vehicleId()+"|"+f.deviceId();}
    private static String reason(String value){if(value==null)throw rule("ACKNOWLEDGEMENT_REASON_REQUIRED","Acknowledgement reason is required");String r=value.trim();if(r.isEmpty()||r.length()>500||r.chars().anyMatch(Character::isISOControl))throw rule("ACKNOWLEDGEMENT_REASON_INVALID","Acknowledgement reason must contain 1..500 safe characters");return r;}
    private static String key(String value){if(value==null||value.length()<16||value.length()>160||value.chars().anyMatch(Character::isWhitespace))throw rule("IDEMPOTENCY_KEY_INVALID","Idempotency-Key must contain 16..160 non-whitespace characters");return value;}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static Context required(Context c){Objects.requireNonNull(c);Objects.requireNonNull(c.tenantId());Objects.requireNonNull(c.actorId());Objects.requireNonNull(c.now());return c;}
    private static <T>T required(T v,String name){if(v==null)throw rule("GPS_EXCEPTION_REQUEST_INVALID",name+" is required");return v;}
    private static BusinessRuleException rule(String c,String m){return new BusinessRuleException(c,m);} private static ConflictException conflict(String c,String m){return new ConflictException(c,m);} private static NotFoundException notFound(){return new NotFoundException("GPS_EXCEPTION_NOT_FOUND","GPS exception not found");}
}
