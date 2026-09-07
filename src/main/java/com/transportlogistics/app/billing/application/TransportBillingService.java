package com.transportlogistics.app.billing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.billing.BillingDeliveryPort;
import com.transportlogistics.app.billing.TransportBillingExportRequestedV1;
import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import com.transportlogistics.app.billing.domain.TransportBillingRecord.*;
import com.transportlogistics.app.billing.ports.inbound.TransportBillingUseCase;
import com.transportlogistics.app.billing.ports.outbound.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.tenancy.TenantDirectory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

public final class TransportBillingService implements TransportBillingUseCase, BillingDeliveryPort {
    private static final int MAX_PAYLOAD = 32 * 1024;
    private final BillingStore store; private final BillingSourcePort sources; private final BillingCompliancePort compliance;
    private final BillingIntegrationPort integration; private final BillingTransaction tx; private final TenantDirectory tenants;
    private final ObjectMapper json; private final Clock clock;

    public TransportBillingService(BillingStore store, BillingSourcePort sources, BillingCompliancePort compliance,
        BillingIntegrationPort integration, BillingTransaction tx, TenantDirectory tenants, ObjectMapper json, Clock clock) {
        this.store=store; this.sources=sources; this.compliance=compliance; this.integration=integration; this.tx=tx;
        this.tenants=tenants; this.json=json; this.clock=clock;
    }

    @Override public TransportBillingRecord create(Context c, Create x, String key) {
        return tx.execute(() -> {
            String k=key(key); String request=hash(x.toString());
            store.lockIdempotency(c.tenantId(),"CREATE",k);
            var replay=store.findByCreateKey(c.tenantId(),k);
            if(replay.isPresent()) {
                if(!replay.get().requestHash().equals(request)) throw conflict("BILLING_IDEMPOTENCY_CONFLICT");
                return replay.get().record();
            }
            var fact=sources.find(c.tenantId(),x.sourceType(),x.sourceId())
                .orElseThrow(()->rule("BILLING_SOURCE_NOT_ELIGIBLE"));
            if(!sources.customerActive(c.tenantId(),fact.customerId())) throw rule("BILLING_CUSTOMER_INVALID");
            if(store.sourceClaimed(c.tenantId(),x.sourceType().name(),x.sourceId(),null))
                throw rule("BILLING_SOURCE_DUPLICATE");
            String currency=tenantCurrency(c.tenantId());
            if(x.currency()==null||!currency.equalsIgnoreCase(x.currency())) throw rule("BILLING_CURRENCY_MISMATCH");
            if(x.replacementOfRecordId()!=null) {
                var prior=get(c.tenantId(),x.replacementOfRecordId());
                if(prior.lifecycle()!=Lifecycle.REVERSED||!prior.source().id().equals(x.sourceId()))
                    throw rule("BILLING_REVERSAL_INVALID");
            }
            var now=now(); var source=new Source(fact.type(),fact.id(),fact.businessNumber(),fact.lifecycle(),
                fact.completedAt(),fact.version(),hash(fact.toString()));
            var record=new TransportBillingRecord(UUID.randomUUID(),c.tenantId(),
                store.nextNumber(c.tenantId(),now.getYear()),RecordType.REGULAR,null,x.replacementOfRecordId(),source,
                fact.customerId(),currency,Lifecycle.DRAFT,List.of(),TaxFact.notSupplied(),List.of(),null,c.actorId(),
                null,null,null,null,null,null,Compliance.PENDING,0,now,now);
            var saved=store.insert(record,k,request);
            store.history(c.tenantId(),saved.id(),"CREATED",null,"DRAFT",c.actorId(),source.snapshotHash(),now);
            return saved;
        });
    }

    @Override public List<TransportBillingRecord> list(UUID t,Filters f,int p,int s){var x=f==null?new Filters(null,null,null,null,null,null,null,null,null,null,null,Sort.CREATED):f;return store.list(t,new BillingStore.Filter(x.customerId(),x.sourceType()==null?null:x.sourceType().name(),x.sourceId(),x.lifecycle()==null?null:x.lifecycle().name(),x.recordType()==null?null:x.recordType().name(),x.currency(),x.costCentreCode(),x.createdFrom(),x.createdTo(),x.finalizedFrom(),x.finalizedTo(),(x.sort()==null?Sort.CREATED:x.sort()).name()),Math.max(0,p),Math.min(100,Math.max(1,s)));}
    @Override public TransportBillingRecord get(UUID t,UUID id){return store.find(t,id).orElseThrow(()->rule("BILLING_NOT_FOUND"));}

    @Override public TransportBillingRecord replace(Context c,UUID id,long version,Replacement x){return tx.execute(()->{
        var current=get(c.tenantId(),id); check(current,version);
        var lines=x.lines().stream().map(l->new TransportBillingRecord.Line(l.id()==null?UUID.randomUUID():l.id(),l.category(),l.reasonCode(),
            l.provenance(),l.quantity(),l.unitRate(),l.amount())).toList();
        TaxFact tax=x.tax()==null?TaxFact.notSupplied():new TaxFact(x.tax().status(),x.tax().category(),
            x.tax().jurisdictionReference(),x.tax().taxableAmount(),x.tax().rate(),x.tax().taxAmount(),
            x.tax().exemptionReference(),x.tax().provenance(),x.tax().snapshotHash());
        var centres=x.costCentres().stream().map(v->new TransportBillingRecord.CostCentre(v.id()==null?UUID.randomUUID():v.id(),v.code(),
            v.allocationPercent(),v.description(),v.source())).toList();
        var saved=store.update(current.replace(lines,tax,centres,now()),version);
        store.history(c.tenantId(),id,"DRAFT_REPLACED",current.lifecycle().name(),"DRAFT",c.actorId(),hash(x.toString()),now());
        return saved;
    });}

    @Override public TransportBillingRecord validate(Context c,UUID id,long version){return tx.execute(()->{
        var current=get(c.tenantId(),id); check(current,version); assertSourceCurrent(current);
        if(current.recordType()==RecordType.REGULAR&&store.sourceClaimed(c.tenantId(),current.source().type().name(),
            current.source().id(),current.id())) throw rule("BILLING_SOURCE_DUPLICATE");
        UUID config=integration.activeConfiguration(c.tenantId()).orElseThrow(()->rule("BILLING_EXPORT_CONFIGURATION_INVALID"));
        var decision=compliance.decision(c.tenantId(),current);
        String validation=hash(current.source()+"|"+current.lines()+"|"+current.tax()+"|"+current.costCentres()+"|"+current.totals());
        var next=current.validated(validation,config,decision,now()); requireSize(next,now());
        var saved=store.update(next,version); history(c,saved,"VALIDATED",current.lifecycle().name(),validation); return saved;
    });}

    @Override public TransportBillingRecord approve(Context c,UUID id,long version,String key){return command(c,id,version,"APPROVE",key,
        r->r.approved(c.actorId(),now()));}
    @Override public TransportBillingRecord cancel(Context c,UUID id,long version,String reason,String key){
        if(reason==null||reason.isBlank()||reason.length()>160)throw rule("BILLING_INVALID_STATE");
        return command(c,id,version,"CANCEL",key,reason,r->r.cancelled(now()));
    }
    @Override public TransportBillingRecord finalizeRecord(Context c,UUID id,long version,String key){return tx.execute(()->{
        store.lockIdempotency(c.tenantId(),"FINALIZE",key(key));
        var replay=replay(c,"FINALIZE",key,id,version,null); if(replay!=null)return replay;
        var current=get(c.tenantId(),id);check(current,version);assertSourceCurrent(current);
        if(current.recordType()==RecordType.REGULAR&&store.sourceClaimed(c.tenantId(),current.source().type().name(),current.source().id(),current.id()))throw rule("BILLING_SOURCE_DUPLICATE");
        if(current.recordType()==RecordType.REVERSAL&&store.reversalExists(c.tenantId(),current.originalBillingRecordId(),current.id()))throw rule("BILLING_REVERSAL_INVALID");
        var saved=store.update(current.finalized(now()),version);history(c,saved,"FINALIZED",current.lifecycle().name(),saved.validationHash());
        if(saved.recordType()==RecordType.REVERSAL){var original=get(c.tenantId(),saved.originalBillingRecordId());store.update(original.reversed(now()),original.version());store.history(c.tenantId(),original.id(),"REVERSED",original.lifecycle().name(),"REVERSED",c.actorId(),saved.id().toString(),now());}
        recordCommand(c,saved,"FINALIZE",key,hash(id+"|"+version));return saved;
    });}

    @Override public TransportBillingRecord reverse(Context c,UUID id,long version,String reason,String key){return tx.execute(()->{
        if(reason==null||reason.isBlank()||reason.length()>160)throw rule("BILLING_REVERSAL_INVALID");
        store.lockIdempotency(c.tenantId(),"REVERSE",key(key));
        var replay=replay(c,"REVERSE",key,id,version,reason);if(replay!=null)return replay;
        var original=get(c.tenantId(),id);check(original,version);
        if(original.recordType()!=RecordType.REGULAR||!Set.of(Lifecycle.FINALIZED,Lifecycle.EXPORT_REQUESTED,Lifecycle.EXPORTED).contains(original.lifecycle())||store.reversalExists(c.tenantId(),id,null))throw rule("BILLING_REVERSAL_INVALID");
        var at=now(); var reversal=new TransportBillingRecord(UUID.randomUUID(),c.tenantId(),store.nextNumber(c.tenantId(),at.getYear()),
            RecordType.REVERSAL,id,null,original.source(),original.customerId(),original.currency(),Lifecycle.DRAFT,
            original.lines().stream().map(line->new TransportBillingRecord.Line(UUID.randomUUID(),line.category(),line.reasonCode(),
                line.provenance(),line.quantity(),line.unitRate(),line.amount())).toList(),original.tax(),
            original.costCentres().stream().map(centre->new TransportBillingRecord.CostCentre(UUID.randomUUID(),centre.code(),
                centre.allocationPercent(),centre.description(),centre.source())).toList(),null,c.actorId(),null,null,null,null,null,null,
            Compliance.PENDING,0,at,at);
        var saved=store.insert(reversal,"reversal:"+key(key),hash(id+"|"+version+"|"+reason));
        history(c,saved,"REVERSAL_CREATED",null,reason);recordCommand(c,saved,"REVERSE",key,hash(id+"|"+version+"|"+reason));return saved;
    });}

    @Override public TransportBillingRecord export(Context c,UUID id,long version,String key){return tx.execute(()->{
        store.lockIdempotency(c.tenantId(),"EXPORT",key(key));
        var replay=replay(c,"EXPORT",key,id,version,null);if(replay!=null)return replay;
        var current=get(c.tenantId(),id); if(Set.of(Lifecycle.EXPORT_REQUESTED,Lifecycle.EXPORTED).contains(current.lifecycle()))return current;
        check(current,version);UUID event=current.exportEventId()==null?UUID.randomUUID():current.exportEventId();var at=now();
        Map<String,Object> payload=payload(current,at);requireSize(payload);integration.publish(new TransportBillingExportRequestedV1(event,c.tenantId(),id,at,payload));
        var saved=store.update(current.exportRequested(event,at),version);history(c,saved,"EXPORT_REQUESTED",current.lifecycle().name(),event.toString());recordCommand(c,saved,"EXPORT",key,hash(id+"|"+version));return saved;
    });}
    @Override public List<BillingStore.History> history(UUID t,UUID id){get(t,id);return store.history(t,id);}

    @Override public void fileDelivered(UUID tenantId,UUID event,String payloadHash,String filename,OffsetDateTime at){tx.execute(()->{
        var current=store.findByExportEvent(tenantId,event).orElse(null);if(current==null||current.lifecycle()==Lifecycle.EXPORTED)return null;
        var saved=store.update(current.exported(at),current.version());store.history(tenantId,saved.id(),"FILE_DELIVERED",
            current.lifecycle().name(),"EXPORTED",new UUID(0,0),payloadHash+":"+filename,at);return null;
    });}

    private TransportBillingRecord command(Context c,UUID id,long version,String scope,String key,
        java.util.function.Function<TransportBillingRecord,TransportBillingRecord> mutation){return tx.execute(()->{
        return commandBody(c,id,version,scope,key,null,mutation);
    });}
    private TransportBillingRecord command(Context c,UUID id,long version,String scope,String key,String material,
        java.util.function.Function<TransportBillingRecord,TransportBillingRecord> mutation){return tx.execute(()->{
        return commandBody(c,id,version,scope,key,material,mutation);
    });}
    private TransportBillingRecord commandBody(Context c,UUID id,long version,String scope,String key,String material,
        java.util.function.Function<TransportBillingRecord,TransportBillingRecord> mutation){
        store.lockIdempotency(c.tenantId(),scope,key(key));
        var replay=replay(c,scope,key,id,version,material);if(replay!=null)return replay;var current=get(c.tenantId(),id);check(current,version);
        var saved=store.update(mutation.apply(current),version);history(c,saved,scope+"D",current.lifecycle().name(),null);
        recordCommand(c,saved,scope,key,requestHash(id,version,material));return saved;
    }
    private TransportBillingRecord replay(Context c,String scope,String key,UUID id,long version,String material){String k=key(key),h=requestHash(id,version,material);var r=store.command(c.tenantId(),scope,k);if(r.isEmpty())return null;if(!r.get().requestHash().equals(h))throw conflict("BILLING_IDEMPOTENCY_CONFLICT");return get(c.tenantId(),r.get().recordId());}
    private static String requestHash(UUID id,long version,String material){return hash(id+"|"+version+(material==null?"":"|"+material));}
    private void recordCommand(Context c,TransportBillingRecord r,String scope,String key,String hash){store.command(c.tenantId(),r.id(),scope,key(key),hash,c.actorId(),r.version(),now());}
    private void assertSourceCurrent(TransportBillingRecord r){if(r.recordType()==RecordType.REVERSAL)return;var fact=sources.find(r.tenantId(),r.source().type(),r.source().id()).orElseThrow(()->rule("BILLING_SOURCE_NOT_ELIGIBLE"));if(!hash(fact.toString()).equals(r.source().snapshotHash()))throw rule("BILLING_SOURCE_NOT_ELIGIBLE");}
    private String tenantCurrency(UUID tenant){return tenants.findTenant(tenant).filter(TenantDirectory.TenantView::active).map(TenantDirectory.TenantView::defaultCurrency).orElseThrow(()->rule("BILLING_CURRENCY_MISMATCH"));}
    private Map<String,Object> payload(TransportBillingRecord b,OffsetDateTime at){var p=new LinkedHashMap<String,Object>();p.put("schemaVersion",1);p.put("billingRecordId",b.id().toString());p.put("billingNumber",b.billingNumber());p.put("recordType",b.recordType().name());p.put("customerId",b.customerId().toString());p.put("currency",b.currency());p.put("source",Map.of("type",b.source().type().name(),"id",b.source().id().toString(),"businessNumber",b.source().businessNumber(),"snapshotHash",b.source().snapshotHash()));var t=b.totals();var amounts=new LinkedHashMap<String,Object>();amounts.put("baseCharge",plain(t.baseCharge()));amounts.put("surcharges",plain(t.surcharges()));amounts.put("penalties",plain(t.penalties()));amounts.put("creditAdjustments",plain(t.creditAdjustments()));amounts.put("subtotal",plain(t.subtotal()));amounts.put("taxAmount",plain(t.taxAmount()));amounts.put("totalAmount",plain(t.totalAmount()));p.put("amounts",amounts);var tax=new LinkedHashMap<String,Object>();tax.put("status",b.tax().status().name());tax.put("category",b.tax().category());tax.put("jurisdictionReference",b.tax().jurisdictionReference());tax.put("taxableAmount",plain(b.tax().taxableAmount()));tax.put("rate",plain(b.tax().rate()));tax.put("exemptionReference",b.tax().exemptionReference());p.put("tax",tax);p.put("costCentres",b.costCentres().stream().sorted(Comparator.comparing(TransportBillingRecord.CostCentre::code)).map(x->Map.of("code",x.code(),"allocationPercent",x.allocationPercent().toPlainString())).toList());p.put("originalBillingRecordId",b.originalBillingRecordId()==null?null:b.originalBillingRecordId().toString());p.put("finalizedAt",b.finalizedAt()==null?at.toString():b.finalizedAt().toString());return p;}
    private static String plain(java.math.BigDecimal v){return v==null?null:v.toPlainString();}
    private void requireSize(TransportBillingRecord b,OffsetDateTime at){requireSize(payload(b,at));}
    private void requireSize(Map<String,Object> p){try{if(json.writeValueAsBytes(p).length>MAX_PAYLOAD)throw rule("BILLING_EXPORT_CONFIGURATION_INVALID");}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw rule("BILLING_EXPORT_CONFIGURATION_INVALID");}}
    private void history(Context c,TransportBillingRecord r,String action,String from,String detail){store.history(c.tenantId(),r.id(),action,from,r.lifecycle().name(),c.actorId(),detail,now());}
    private static void check(TransportBillingRecord r,long v){if(r.version()!=v)throw conflict("BILLING_STALE_VERSION");}
    private OffsetDateTime now(){return OffsetDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);}
    private static String key(String k){if(k==null||k.isBlank()||k.length()>160)throw rule("BILLING_INVALID_STATE");return k.trim();}
    private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static BusinessRuleException rule(String c){return new BusinessRuleException(c,c);}
    private static ConflictException conflict(String c){return new ConflictException(c,c);}
}
