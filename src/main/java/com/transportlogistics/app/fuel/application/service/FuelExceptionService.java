package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelExceptionUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionHandoff;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionCorrectionExecutor;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionSourceValidator;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionStore;
import com.transportlogistics.app.fuel.application.ports.out.FuelTransaction;
import com.transportlogistics.app.fuel.domain.model.FuelExceptionCase;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FuelExceptionService implements FuelExceptionUseCase {
    private static final Set<String> SOURCES = Set.of("FUEL_ISSUE", "FUEL_PURCHASE", "FUEL_PRICE", "VEHICLE_READING", "BUNKER_TANK", "FUEL_CARD_INDICATOR", "REJECTED_BUNKER_COMMAND", "FUEL_PERFORMANCE_INDICATOR");
    private final FuelExceptionStore store; private final FuelExceptionHandoff handoff;
    private final FuelExceptionCorrectionExecutor corrections; private final FuelExceptionSourceValidator sources; private final FuelTransaction transactions; private final Clock clock;

    public FuelExceptionService(FuelExceptionStore store, FuelExceptionHandoff handoff, FuelExceptionCorrectionExecutor corrections, FuelExceptionSourceValidator sources, FuelTransaction transactions, Clock clock) {
        this.store=store; this.handoff=handoff; this.corrections=corrections; this.sources=sources; this.transactions=transactions; this.clock=clock;
    }

    @Override public FuelExceptionCase create(Context c, Create x) { return transactions.execute(() -> {
        validateCreate(c,x); var now=OffsetDateTime.now(clock); var value=new FuelExceptionCase(UUID.randomUUID(),c.tenantId(),x.category(),
            FuelExceptionCase.Lifecycle.OPEN,x.impact(),x.sourceType(),x.sourceId(),null,required(x.summary(),500),bounded(x.safeMetadata()),
            x.vehicleId(),x.driverId(),x.tripId(),x.cardId(),x.tankId(),x.occurredAt()==null?now:x.occurredAt(),true,
            FuelExceptionCase.HandoffStatus.NOT_REQUIRED,null,null,c.actorId(),null,0,now,now);
        value=store.insert(value); store.history(c.tenantId(),value.id(),"CREATED",null,"OPEN",value.summary(),c.actorId(),now); return value;
    }); }
    @Override public List<FuelExceptionCase> list(UUID t, Search s){int limit=Math.min(100,Math.max(1,s.limit())); int page=Math.max(0,s.page()); return store.search(t,new Search(page,limit,s.category(),s.lifecycle(),s.sourceType(),s.vehicleId(),s.driverId(),s.cardId(),s.tankId(),s.occurredFrom(),s.occurredTo(),s.reviewRequired(),s.handoffStatus(),s.sort(),s.direction()));}
    @Override public Detail detail(UUID t,UUID id){return store.detail(get(t,id));}
    @Override public FuelExceptionCase review(Context c,UUID id,VersionedReason x){return transition(c,id,x.version(),FuelExceptionCase.Lifecycle.OPEN,FuelExceptionCase.Lifecycle.UNDER_REVIEW,"REVIEWED",x.reason(),true,null,null);}
    @Override public Evidence addEvidence(Context c,UUID id,AddEvidence x){ get(c.tenantId(),id); required(x.evidenceType(),40); required(x.summary(),500); if(x.sourceId()!=null&&!sources.exists(c.tenantId(),x.sourceType(),x.sourceId())) sourceMissing(); return store.evidence(c.tenantId(),id,new AddEvidence(x.evidenceType(),x.sourceType(),x.sourceId(),x.summary().trim(),bounded(x.safeSnapshot())),c.actorId(),OffsetDateTime.now(clock)); }
    @Override public Note addNote(Context c,UUID id,Text x){get(c.tenantId(),id);return store.note(c.tenantId(),id,required(x.text(),1000),c.actorId(),OffsetDateTime.now(clock));}
    @Override public Correction requestCorrection(Context c,UUID id,RequestCorrection x){return transactions.execute(()->{var v=get(c.tenantId(),id);if(v.lifecycle()!=FuelExceptionCase.Lifecycle.UNDER_REVIEW&&v.lifecycle()!=FuelExceptionCase.Lifecycle.CORRECTION_PENDING)invalid(); required(x.correctionType(),50); if(x.ownerCommand()==null||x.ownerCommand().isEmpty()) correctionInvalid();var now=OffsetDateTime.now(clock);var governed=new RequestCorrection(x.correctionType(),bounded(x.ownerCommand()),true);var correction=store.correction(c.tenantId(),id,governed,c.actorId(),"AWAITING_APPROVAL",now);var next=FuelExceptionCase.Lifecycle.AWAITING_APPROVAL;store.update(c.tenantId(),id,v.version(),next,true,v.handoffStatus(),null,null,null,now);store.history(c.tenantId(),id,"CORRECTION_REQUESTED",v.lifecycle().name(),next.name(),x.correctionType(),c.actorId(),now);return correction;});}
    @Override public Correction approve(Context c,UUID id,UUID correctionId,VersionedReason x){
        var existing=store.correction(c.tenantId(),id,correctionId).orElseThrow(FuelExceptionService::notFound);
        var approved="FAILED".equals(existing.status())?retryCorrection(c,id,existing,x):reviewCorrection(c,id,correctionId,x,"APPROVED");
        try{String ref=corrections.execute(c.tenantId(),approved.correctionType(),approved.ownerCommand(),c.actorId(),c.username());var applied=store.correctionResult(c.tenantId(),id,correctionId,approved.version(),"APPLIED",ref,null,OffsetDateTime.now(clock));store.history(c.tenantId(),id,"CORRECTION_APPLIED",null,null,ref,c.actorId(),OffsetDateTime.now(clock));return applied;}catch(RuntimeException ex){store.correctionResult(c.tenantId(),id,correctionId,approved.version(),"FAILED",null,ex.getClass().getSimpleName(),OffsetDateTime.now(clock));store.history(c.tenantId(),id,"CORRECTION_FAILED",null,null,ex.getClass().getSimpleName(),c.actorId(),OffsetDateTime.now(clock));throw ex;}}
    @Override public Correction reject(Context c,UUID id,UUID correctionId,VersionedReason x){return reviewCorrection(c,id,correctionId,x,"REJECTED");}
    @Override public FuelExceptionCase resolve(Context c,UUID id,Resolve x){var v=get(c.tenantId(),id);if(v.lifecycle()==FuelExceptionCase.Lifecycle.OPEN||v.lifecycle()==FuelExceptionCase.Lifecycle.AWAITING_APPROVAL)invalid();return transition(c,id,x.version(),v.lifecycle(),FuelExceptionCase.Lifecycle.RESOLVED,"RESOLVED",required(x.reason(),500),false,x.outcome(),c.actorId());}
    @Override public FuelExceptionCase escalate(Context c,UUID id,VersionedReason x){return transactions.execute(()->{var v=get(c.tenantId(),id);if(v.handoffStatus()!=FuelExceptionCase.HandoffStatus.NOT_REQUIRED)throw new BusinessRuleException("FUEL_EXCEPTION_ALREADY_ESCALATED","FUEL_EXCEPTION_ALREADY_ESCALATED");if(v.impact()!=FuelExceptionCase.Impact.CRITICAL&&(v.impact()!=FuelExceptionCase.Impact.HIGH||x.reason()==null||x.reason().isBlank())) invalid();var now=OffsetDateTime.now(clock);UUID eventId=store.handoff(c.tenantId(),id,x.reason(),now);handoff.publish(v,eventId,x.reason(),c.correlationId());var updated=store.update(c.tenantId(),id,x.version(),v.lifecycle(),v.reviewRequired(),FuelExceptionCase.HandoffStatus.PUBLISHED,null,null,null,now);store.history(c.tenantId(),id,"ESCALATED",v.lifecycle().name(),v.lifecycle().name(),x.reason(),c.actorId(),now);return updated;});}
    private Correction reviewCorrection(Context c,UUID id,UUID correctionId,VersionedReason x,String status){return transactions.execute(()->{var v=get(c.tenantId(),id);var correction=store.correction(c.tenantId(),id,correctionId).orElseThrow(()->notFound());if(correction.requestedBy().equals(c.actorId()))throw new BusinessRuleException("FUEL_EXCEPTION_SOD_VIOLATION","FUEL_EXCEPTION_SOD_VIOLATION");if(!"AWAITING_APPROVAL".equals(correction.status())) invalid();var now=OffsetDateTime.now(clock);var result=store.reviewCorrection(c.tenantId(),id,correctionId,x.version(),status,c.actorId(),required(x.reason(),500),now);var next="APPROVED".equals(status)?FuelExceptionCase.Lifecycle.CORRECTION_PENDING:FuelExceptionCase.Lifecycle.UNDER_REVIEW;store.update(c.tenantId(),id,v.version(),next,true,v.handoffStatus(),null,null,null,now);store.history(c.tenantId(),id,"CORRECTION_"+status,v.lifecycle().name(),next.name(),x.reason(),c.actorId(),now);return result;});}
    private Correction retryCorrection(Context c,UUID id,Correction correction,VersionedReason x){get(c.tenantId(),id);if(correction.requestedBy().equals(c.actorId()))throw new BusinessRuleException("FUEL_EXCEPTION_SOD_VIOLATION","FUEL_EXCEPTION_SOD_VIOLATION");required(x.reason(),500);if(correction.version()!=x.version())throw new BusinessRuleException("FUEL_EXCEPTION_CONCURRENT_MODIFICATION","FUEL_EXCEPTION_CONCURRENT_MODIFICATION");store.history(c.tenantId(),id,"CORRECTION_RETRY",null,null,x.reason(),c.actorId(),OffsetDateTime.now(clock));return correction;}
    private FuelExceptionCase transition(Context c,UUID id,long version,FuelExceptionCase.Lifecycle expected,FuelExceptionCase.Lifecycle next,String action,String detail,boolean review,FuelExceptionCase.Outcome outcome,UUID resolvedBy){return transactions.execute(()->{var v=get(c.tenantId(),id);if(v.lifecycle()!=expected)invalid();var now=OffsetDateTime.now(clock);var updated=store.update(c.tenantId(),id,version,next,review,v.handoffStatus(),outcome,detail,resolvedBy,now);store.history(c.tenantId(),id,action,expected.name(),next.name(),detail,c.actorId(),now);return updated;});}
    private void validateCreate(Context c,Create x){if(x.category()==null||x.impact()==null||x.sourceId()==null||!SOURCES.contains(x.sourceType()))correctionInvalid();if(!sources.exists(c.tenantId(),x.sourceType(),x.sourceId()))sourceMissing();if(x.category()==FuelExceptionCase.Category.EMERGENCY_REFUEL&&(x.vehicleId()==null||(x.tripId()==null&&x.driverId()==null)||x.occurredAt()==null))correctionInvalid();}
    private FuelExceptionCase get(UUID t,UUID id){return store.find(t,id).orElseThrow(FuelExceptionService::notFound);}
    private static NotFoundException notFound(){return new NotFoundException("FUEL_EXCEPTION_NOT_FOUND","FUEL_EXCEPTION_NOT_FOUND");}
    private static void sourceMissing(){throw new NotFoundException("FUEL_EXCEPTION_SOURCE_NOT_FOUND","FUEL_EXCEPTION_SOURCE_NOT_FOUND");}
    private static void invalid(){throw new BusinessRuleException("FUEL_EXCEPTION_INVALID_STATE","FUEL_EXCEPTION_INVALID_STATE");}
    private static void correctionInvalid(){throw new BusinessRuleException("FUEL_EXCEPTION_CORRECTION_INVALID","FUEL_EXCEPTION_CORRECTION_INVALID");}
    private static String required(String v,int max){if(v==null||v.isBlank()||v.trim().length()>max)correctionInvalid();return v.trim();}
    private static Map<String,String> bounded(Map<String,String> v){if(v==null)return Map.of();if(v.size()>20||v.entrySet().stream().anyMatch(e->e.getKey()==null||e.getValue()==null||e.getKey().length()>64||e.getValue().length()>256))correctionInvalid();return Map.copyOf(v);}
}
