package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardImportUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.FuelCard;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

public final class FuelCardImportService implements FuelCardImportUseCase {
    private final FuelCardImportParser parser; private final FuelCardTransactionRepository transactions;
    private final FuelCardRepository cards; private final FuelCardReferencePort references;
    private final FuelTransaction transaction; private final Clock clock;
    private final FuelPerformanceTenantPort tenants;
    public FuelCardImportService(FuelCardImportParser parser,FuelCardTransactionRepository transactions,
                                 FuelCardRepository cards,FuelCardReferencePort references,
                                 FuelTransaction transaction,FuelPerformanceTenantPort tenants,Clock clock){this.parser=parser;this.transactions=transactions;
        this.cards=cards;this.references=references;this.transaction=transaction;this.tenants=tenants;this.clock=clock;}
    @Override public Batch importJson(Context c,UUID providerId,byte[] json){
        if(json==null||json.length==0||json.length>1_048_576) throw rule("FUEL_CARD_IMPORT_TOO_LARGE");
        if(!references.providerActive(providerId)) throw rule("FUEL_CARD_IMPORT_INVALID");
        var parsed=parser.parse(json);
        var existing=transactions.findBatch(c.tenantId(),providerId,parsed.providerBatchId());
        if(existing.isPresent()){
            if(!existing.get().fileHash().equals(parsed.fileHash())) throw conflict("FUEL_CARD_IMPORT_CONFLICT");
            return existing.get();
        }
        var sameFile=transactions.findBatchByHash(c.tenantId(),providerId,parsed.fileHash());
        if(sameFile.isPresent()) return sameFile.get();
        return transaction.execute(()->doImport(c,providerId,parsed));
    }
    private Batch doImport(Context c,UUID providerId,FuelCardImportParser.ParsedBatch parsed){
        var now=OffsetDateTime.now(clock); UUID batchId=UUID.randomUUID(); int review=0; var seen=new HashSet<String>();
        var prepared=new java.util.ArrayList<java.util.Map.Entry<FuelCardImportParser.ParsedTransaction,java.util.Map.Entry<FuelCard,Set<String>>>>();
        for(var fact:parsed.transactions()){
            if(!seen.add(fact.providerTransactionId())) throw rule("FUEL_CARD_IMPORT_INVALID");
            var old=transactions.findProviderTransaction(c.tenantId(),providerId,fact.providerTransactionId());
            if(old.isPresent()&&!transactions.providerTransactionHashMatches(c.tenantId(),providerId,
                    fact.providerTransactionId(),fact.canonicalHash()))
                throw conflict("FUEL_CARD_TRANSACTION_CONFLICT");
            if(old.isPresent()) continue;
            if("REVERSAL".equals(fact.transactionKind())) {
                var original=transactions.findProviderTransaction(c.tenantId(),providerId,fact.originalProviderTransactionId());
                if(original.isEmpty()||!"PURCHASE".equals(original.get().transactionKind()))
                    throw rule("FUEL_CARD_IMPORT_INVALID");
            }
            FuelCard card=cards.findByProviderReference(c.tenantId(),providerId,fact.providerCardReference())
                    .orElseThrow(()->rule("FUEL_CARD_IMPORT_INVALID"));
            Set<String> indicators=evaluate(c.tenantId(),card,fact);
            if(!indicators.isEmpty()) review++;
            prepared.add(java.util.Map.entry(fact,java.util.Map.entry(card,indicators)));
        }
        Batch batch=transactions.saveBatch(new Batch(batchId,providerId,parsed.providerBatchId(),parsed.fileHash(),
                parsed.generatedAt(),parsed.transactions().size(),prepared.size(),review,c.actorId(),now),c.tenantId());
        for(var item:prepared){
            var fact=item.getKey(); var card=item.getValue().getKey(); var indicators=item.getValue().getValue();
            var saved=transactions.saveTransaction(fact,c.tenantId(),batchId,providerId,card.id(),c.actorId(),
                    indicators.isEmpty()?"IMPORTED":"REVIEW_REQUIRED",indicators,now);
            cards.audit(c.tenantId(),card.id(),saved.id(),"TRANSACTION_IMPORTED",
                    indicators.isEmpty()?"SUCCESS":"REVIEW_REQUIRED",null,c.actorId(),now);
            if("REVERSAL".equals(fact.transactionKind()))
                transactions.markReversed(c.tenantId(),providerId,fact.originalProviderTransactionId());
        }
        return batch;
    }
    private Set<String> evaluate(UUID tenantId,FuelCard card,FuelCardImportParser.ParsedTransaction fact){
        Set<String> result=new HashSet<>();
        var tenantZone=java.time.ZoneId.of(tenants.required().timeZone());
        if(card.effectiveStatus(OffsetDateTime.now(clock),tenantZone)!=FuelCard.Status.ACTIVE) result.add("CARD_INACTIVE");
        cards.restriction(tenantId,card.id()).ifPresentOrElse(r->{
            if(!r.currency().equalsIgnoreCase(fact.currency())||fact.totalAmount().compareTo(r.maxTransactionAmount())>0) result.add("LIMIT_EXCEEDED");
            if(!r.allowedFuelTypes().stream().map(v->v.toUpperCase(Locale.ROOT)).toList().contains(fact.fuelType().toUpperCase(Locale.ROOT))) result.add("FUEL_TYPE_NOT_ALLOWED");
            if(!r.allowedStationReferences().isEmpty()&&(fact.stationReference()==null||!r.allowedStationReferences().contains(fact.stationReference()))) result.add("STATION_NOT_ALLOWED");
            var zone=java.time.ZoneId.of(tenants.required().timeZone());
            var day=fact.transactionTimestamp().atZoneSameInstant(zone).toLocalDate();
            var dayFrom=day.atStartOfDay(zone).toOffsetDateTime(); var dayTo=day.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
            var monthFrom=day.withDayOfMonth(1).atStartOfDay(zone).toOffsetDateTime();
            var monthTo=day.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toOffsetDateTime();
            var daily=transactions.totals(tenantId,card.id(),dayFrom,dayTo);
            var monthly=transactions.totals(tenantId,card.id(),monthFrom,monthTo);
            if(daily.amount().add(fact.totalAmount()).compareTo(r.maxDailyAmount())>0
                    ||daily.litres().add(fact.quantityLitres()).compareTo(r.maxDailyLitres())>0
                    ||monthly.amount().add(fact.totalAmount()).compareTo(r.maxMonthlyAmount())>0) result.add("LIMIT_EXCEEDED");
        },()->result.add("LIMIT_EXCEEDED"));
        if("REVERSAL".equals(fact.transactionKind())) result.add("REVERSAL_REVIEW_REQUIRED");
        if(fact.tripId()!=null&&!references.tripExists(fact.tripId())) result.add("BINDING_MISMATCH");
        cards.activeBinding(tenantId,card.id()).ifPresent(binding->{
            String supplied="VEHICLE".equals(binding.bindingType())?fact.providerVehicleReference():fact.providerDriverReference();
            if(supplied!=null&&!supplied.equalsIgnoreCase(binding.bindingId().toString())) result.add("BINDING_MISMATCH");
        });
        return result;
    }
    @Override public List<Batch> batches(UUID t,int p,int l){return transactions.batches(t,Math.max(0,p),Math.min(100,Math.max(1,l)));}
    @Override public Batch batch(UUID t,UUID id){return transactions.batch(t,id).orElseThrow(()->new NotFoundException("FUEL_CARD_NOT_FOUND","Import batch not found"));}
    @Override public List<Transaction> transactions(UUID t,TransactionSearch s){return transactions.transactions(t,
            new TransactionSearch(Math.max(0,s.page()),Math.min(100,Math.max(1,s.limit())),s.cardId(),s.providerId(),
                    s.from(),s.to(),s.localStatus(),s.reconciliationStatus(),s.indicator(),s.reviewRequired(),s.sort(),s.direction()));}
    @Override public Transaction transaction(UUID t,UUID id){return transactions.transaction(t,id).orElseThrow(()->new NotFoundException("FUEL_CARD_TRANSACTION_NOT_FOUND","Fuel-card transaction not found"));}
    @Override public Transaction reconcile(Context c,UUID id,Action a){
        Transaction current=transaction(c.tenantId(),id);
        if(current.importedBy().equals(c.actorId())) throw rule("FUEL_CARD_RECONCILIATION_INVALID");
        if(a.reason()==null||a.reason().isBlank()) throw rule("FUEL_CARD_RECONCILIATION_INVALID");
        if(current.version()!=a.version()) throw conflict("FUEL_CARD_TRANSACTION_CONFLICT");
        if("MATCH".equals(a.action())&&(a.purchaseId()==null||!references.purchaseExists(a.purchaseId()))) throw rule("FUEL_CARD_RECONCILIATION_INVALID");
        return transaction.execute(()->{var now=OffsetDateTime.now(clock);var result=transactions.reconcile(c.tenantId(),id,c.actorId(),a,now);
            cards.audit(c.tenantId(),current.cardId(),id,"RECONCILIATION_"+a.action().toUpperCase(Locale.ROOT),"SUCCESS",a.reason(),c.actorId(),now);
            return result;});
    }
    private static BusinessRuleException rule(String code){return new BusinessRuleException(code,code);}
    private static ConflictException conflict(String code){return new ConflictException(code,code);}
}
