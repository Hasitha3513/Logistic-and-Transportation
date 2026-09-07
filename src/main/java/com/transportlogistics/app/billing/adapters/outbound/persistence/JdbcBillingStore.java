package com.transportlogistics.app.billing.adapters.outbound.persistence;

import com.transportlogistics.app.billing.domain.TransportBillingRecord;
import com.transportlogistics.app.billing.domain.TransportBillingRecord.*;
import com.transportlogistics.app.billing.ports.outbound.BillingStore;
import com.transportlogistics.app.shared.domain.ConflictException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcBillingStore implements BillingStore {
    private final JdbcTemplate jdbc;
    JdbcBillingStore(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Override public Optional<TransportBillingRecord> find(UUID t,UUID id){return jdbc.query(
        "select * from transport_billing_record where tenant_id=? and id=?",rs->rs.next()?Optional.of(record(rs)):Optional.empty(),t,id);}
    @Override public Optional<CreateReplay> findByCreateKey(UUID t,String k){return jdbc.query(
        "select * from transport_billing_record where tenant_id=? and create_idempotency_key=?",rs->rs.next()?Optional.of(new CreateReplay(rs.getString("create_request_hash"),record(rs))):Optional.empty(),t,k);}
    @Override public void lockIdempotency(UUID t,String scope,String key){jdbc.query(
        "select pg_advisory_xact_lock(hashtext(?))",rs->null,t+":billing:"+scope+":"+key);}
    @Override public Optional<TransportBillingRecord> findByExportEvent(UUID t,UUID e){return jdbc.query(
        "select * from transport_billing_record where tenant_id=? and export_event_id=?",rs->rs.next()?Optional.of(record(rs)):Optional.empty(),t,e);}
    @Override public List<TransportBillingRecord> list(UUID t,Filter f,int p,int s){var sql=new StringBuilder("select distinct b.* from transport_billing_record b");var args=new ArrayList<Object>();if(f.costCentreCode()!=null)sql.append(" join transport_billing_cost_centre c on c.tenant_id=b.tenant_id and c.billing_record_id=b.id");sql.append(" where b.tenant_id=?");args.add(t);append(sql,args,"b.customer_id",f.customerId());append(sql,args,"b.source_type",f.sourceType());append(sql,args,"b.source_id",f.sourceId());append(sql,args,"b.lifecycle",f.lifecycle());append(sql,args,"b.record_type",f.recordType());append(sql,args,"b.currency",f.currency()==null?null:f.currency().toUpperCase(Locale.ROOT));append(sql,args,"c.code",f.costCentreCode());range(sql,args,"b.created_at",f.createdFrom(),f.createdTo());range(sql,args,"b.finalized_at",f.finalizedFrom(),f.finalizedTo());String order=switch(f.sort()){case "UPDATED"->"b.updated_at";case "FINALIZED"->"b.finalized_at";case "CUSTOMER"->"b.customer_id";case "TOTAL"->"b.total_amount";case "LIFECYCLE"->"b.lifecycle";default->"b.created_at";};sql.append(" order by ").append(order).append(" desc nulls last,b.id limit ? offset ?");args.add(s);args.add(p*s);return jdbc.query(sql.toString(),(rs,n)->record(rs),args.toArray());}
    @Override public String nextNumber(UUID t,int year){
        jdbc.query("select pg_advisory_xact_lock(hashtext(?))", rs -> null, t+":billing:"+year);
        String prefix="TB-"+year+"-"; Integer max=jdbc.queryForObject("select coalesce(max(cast(right(billing_number,6) as integer)),0) from transport_billing_record where tenant_id=? and billing_number like ?",Integer.class,t,prefix+"%");
        if(max==null||max>=999999)throw new IllegalStateException("Billing number exhausted");return prefix+"%06d".formatted(max+1);
    }
    @Override public TransportBillingRecord insert(TransportBillingRecord b,String key,String requestHash){try{
        var t=b.totals();jdbc.update("""
            insert into transport_billing_record(id,tenant_id,billing_number,record_type,original_billing_record_id,replacement_of_record_id,
            source_type,source_id,source_business_number,source_terminal_lifecycle,source_completion_time,source_version,source_snapshot_hash,
            customer_id,currency,lifecycle,base_charge,surcharges,penalties,credit_adjustments,subtotal,tax_amount,total_amount,prepared_by,
            approved_by,approved_at,finalized_at,export_configuration_id,export_event_id,validation_hash,compliance,create_idempotency_key,
            create_request_hash,version,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,b.id(),b.tenantId(),b.billingNumber(),b.recordType().name(),b.originalBillingRecordId(),b.replacementOfRecordId(),
            b.source().type().name(),b.source().id(),b.source().businessNumber(),b.source().terminalLifecycle(),b.source().completionTime(),
            b.source().sourceVersion(),b.source().snapshotHash(),b.customerId(),b.currency(),b.lifecycle().name(),t.baseCharge(),t.surcharges(),
            t.penalties(),t.creditAdjustments(),t.subtotal(),t.taxAmount(),t.totalAmount(),b.preparedBy(),b.approvedBy(),b.approvedAt(),
            b.finalizedAt(),b.exportConfigurationId(),b.exportEventId(),b.validationHash(),b.compliance().name(),key,requestHash,b.version(),b.createdAt(),b.updatedAt());
        children(b);if(b.recordType()==RecordType.REGULAR)jdbc.update("insert into transport_billing_source_claim(id,tenant_id,billing_record_id,source_type,source_id,active,created_at) values(?,?,?,?,?,true,?)",UUID.randomUUID(),b.tenantId(),b.id(),b.source().type().name(),b.source().id(),b.createdAt());return find(b.tenantId(),b.id()).orElseThrow();
    }catch(DuplicateKeyException e){throw new ConflictException("BILLING_SOURCE_DUPLICATE","BILLING_SOURCE_DUPLICATE");}}
    @Override public TransportBillingRecord update(TransportBillingRecord b,long expected){var t=b.totals();int n=jdbc.update("""
        update transport_billing_record set lifecycle=?,base_charge=?,surcharges=?,penalties=?,credit_adjustments=?,subtotal=?,tax_amount=?,
        total_amount=?,approved_by=?,approved_at=?,finalized_at=?,export_configuration_id=?,export_event_id=?,validation_hash=?,compliance=?,
        version=version+1,updated_at=? where tenant_id=? and id=? and version=?
        """,b.lifecycle().name(),t.baseCharge(),t.surcharges(),t.penalties(),t.creditAdjustments(),t.subtotal(),t.taxAmount(),t.totalAmount(),
        b.approvedBy(),b.approvedAt(),b.finalizedAt(),b.exportConfigurationId(),b.exportEventId(),b.validationHash(),b.compliance().name(),
        b.updatedAt(),b.tenantId(),b.id(),expected);if(n!=1)throw new ConflictException("BILLING_STALE_VERSION","BILLING_STALE_VERSION");
        if(b.lifecycle()==Lifecycle.DRAFT||b.lifecycle()==Lifecycle.VALIDATED){jdbc.update("delete from transport_billing_line where tenant_id=? and billing_record_id=?",b.tenantId(),b.id());jdbc.update("delete from transport_billing_cost_centre where tenant_id=? and billing_record_id=?",b.tenantId(),b.id());jdbc.update("delete from transport_billing_tax_fact where tenant_id=? and billing_record_id=?",b.tenantId(),b.id());children(b);}
        if(b.lifecycle()==Lifecycle.REVERSED)jdbc.update("update transport_billing_source_claim set active=false,released_at=? where tenant_id=? and billing_record_id=? and active",b.updatedAt(),b.tenantId(),b.id());
        return find(b.tenantId(),b.id()).orElseThrow();}
    @Override public boolean sourceClaimed(UUID t,String type,UUID source,UUID excluding){return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from transport_billing_source_claim where tenant_id=? and source_type=? and source_id=? and active and (?::uuid is null or billing_record_id<>?::uuid))",Boolean.class,t,type,source,excluding,excluding));}
    @Override public boolean reversalExists(UUID t,UUID original,UUID excluding){return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from transport_billing_record where tenant_id=? and record_type='REVERSAL' and original_billing_record_id=? and lifecycle<>'CANCELLED' and (?::uuid is null or id<>?::uuid))",Boolean.class,t,original,excluding,excluding));}
    @Override public Optional<CommandReplay> command(UUID t,String scope,String key){return jdbc.query("select request_hash,billing_record_id,result_version from transport_billing_history where tenant_id=? and idempotency_scope=? and idempotency_key=?",rs->rs.next()?Optional.of(new CommandReplay(rs.getString(1),rs.getObject(2,UUID.class),rs.getLong(3))):Optional.empty(),t,scope,key);}
    @Override public void command(UUID t,UUID id,String scope,String key,String hash,UUID actor,long version,OffsetDateTime at){jdbc.update("insert into transport_billing_history(id,tenant_id,billing_record_id,action,actor_id,idempotency_scope,idempotency_key,request_hash,result_version,created_at) values(?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),t,id,"COMMAND_"+scope,actor,scope,key,hash,version,at);}
    @Override public void history(UUID t,UUID id,String action,String from,String to,UUID actor,String detail,OffsetDateTime at){jdbc.update("insert into transport_billing_history(id,tenant_id,billing_record_id,action,from_state,to_state,actor_id,detail,created_at) values(?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),t,id,action,from,to,actor,detail,at);}
    @Override public List<History> history(UUID t,UUID id){return jdbc.query("select id,action,from_state,to_state,actor_id,detail,created_at from transport_billing_history where tenant_id=? and billing_record_id=? order by created_at,id",(rs,n)->new History(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getObject(5,UUID.class),rs.getString(6),rs.getObject(7,OffsetDateTime.class)),t,id);}

    private static void append(StringBuilder sql,List<Object> args,String column,Object value){if(value!=null){sql.append(" and ").append(column).append("=?");args.add(value);}}
    private static void range(StringBuilder sql,List<Object> args,String column,OffsetDateTime from,OffsetDateTime to){if(from!=null){sql.append(" and ").append(column).append(">=?");args.add(from);}if(to!=null){sql.append(" and ").append(column).append("<?");args.add(to);}}

    private void children(TransportBillingRecord b){for(var l:b.lines())jdbc.update("insert into transport_billing_line(id,tenant_id,billing_record_id,category,reason_code,provenance,quantity,unit_rate,amount) values(?,?,?,?,?,?,?,?,?)",l.id(),b.tenantId(),b.id(),l.category().name(),l.reasonCode(),l.provenance(),l.quantity(),l.unitRate(),l.amount());for(var c:b.costCentres())jdbc.update("insert into transport_billing_cost_centre(id,tenant_id,billing_record_id,code,allocation_percent,description,source) values(?,?,?,?,?,?,?)",c.id(),b.tenantId(),b.id(),c.code(),c.allocationPercent(),c.description(),c.source());var x=b.tax();jdbc.update("insert into transport_billing_tax_fact(id,tenant_id,billing_record_id,status,category,jurisdiction_reference,taxable_amount,rate,tax_amount,exemption_reference,provenance,snapshot_hash) values(?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),b.tenantId(),b.id(),x.status().name(),x.category(),x.jurisdictionReference(),x.taxableAmount(),x.rate(),x.taxAmount(),x.exemptionReference(),x.provenance(),x.snapshotHash());}
    private TransportBillingRecord record(ResultSet rs)throws SQLException{UUID t=rs.getObject("tenant_id",UUID.class),id=rs.getObject("id",UUID.class);var lines=jdbc.query("select * from transport_billing_line where tenant_id=? and billing_record_id=? order by id",(x,n)->new Line(x.getObject("id",UUID.class),LineCategory.valueOf(x.getString("category")),x.getString("reason_code"),x.getString("provenance"),x.getBigDecimal("quantity"),x.getBigDecimal("unit_rate"),x.getBigDecimal("amount")),t,id);var centres=jdbc.query("select * from transport_billing_cost_centre where tenant_id=? and billing_record_id=? order by code",(x,n)->new CostCentre(x.getObject("id",UUID.class),x.getString("code"),x.getBigDecimal("allocation_percent"),x.getString("description"),x.getString("source")),t,id);TaxFact tax=jdbc.query("select * from transport_billing_tax_fact where tenant_id=? and billing_record_id=?",x->{if(!x.next())return TaxFact.notSupplied();return new TaxFact(TaxStatus.valueOf(x.getString("status")),x.getString("category"),x.getString("jurisdiction_reference"),x.getBigDecimal("taxable_amount"),x.getBigDecimal("rate"),x.getBigDecimal("tax_amount"),x.getString("exemption_reference"),x.getString("provenance"),x.getString("snapshot_hash"));},t,id);var source=new Source(Source.SourceType.valueOf(rs.getString("source_type")),rs.getObject("source_id",UUID.class),rs.getString("source_business_number"),rs.getString("source_terminal_lifecycle"),rs.getObject("source_completion_time",OffsetDateTime.class),rs.getLong("source_version"),rs.getString("source_snapshot_hash"));var totals=new Totals(rs.getBigDecimal("base_charge"),rs.getBigDecimal("surcharges"),rs.getBigDecimal("penalties"),rs.getBigDecimal("credit_adjustments"),rs.getBigDecimal("subtotal"),rs.getBigDecimal("tax_amount"),rs.getBigDecimal("total_amount"));return new TransportBillingRecord(id,t,rs.getString("billing_number"),RecordType.valueOf(rs.getString("record_type")),rs.getObject("original_billing_record_id",UUID.class),rs.getObject("replacement_of_record_id",UUID.class),source,rs.getObject("customer_id",UUID.class),rs.getString("currency"),Lifecycle.valueOf(rs.getString("lifecycle")),lines,tax,centres,totals,rs.getObject("prepared_by",UUID.class),rs.getObject("approved_by",UUID.class),rs.getObject("approved_at",OffsetDateTime.class),rs.getObject("finalized_at",OffsetDateTime.class),rs.getObject("export_configuration_id",UUID.class),rs.getObject("export_event_id",UUID.class),rs.getString("validation_hash"),Compliance.valueOf(rs.getString("compliance")),rs.getLong("version"),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("updated_at",OffsetDateTime.class));}
}
