package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryCustomerSubmissionRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliverySelfServiceAccessRepository;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

@Component
public class DeliverySelfServiceJdbcAdapter implements DeliverySelfServiceAccessRepository,
        DeliveryCustomerSubmissionRepository {
    private static final String CUSTOMER_ACTOR = "delivery-self-service";
    private final JdbcTemplate jdbc;
    private final CurrentTenant tenants;

    public DeliverySelfServiceJdbcAdapter(JdbcTemplate jdbc, CurrentTenant tenants) {
        this.jdbc = jdbc;
        this.tenants = tenants;
    }

    @Override
    public Optional<DeliverySelfServiceAccess> findBootstrapByTokenHash(String hash) {
        return one("select * from delivery_self_service_access where token_hash = ?", hash);
    }

    @Override
    public Optional<DeliverySelfServiceAccess> findByTokenHash(String hash) {
        return one("select * from delivery_self_service_access where tenant_id = ? and token_hash = ?",
                tenantId(), hash);
    }

    @Override
    public Optional<DeliverySelfServiceAccess> findByTokenHashForUpdate(String hash) {
        return one("select * from delivery_self_service_access where tenant_id = ? and token_hash = ? for update",
                tenantId(), hash);
    }

    @Override
    public Optional<DeliverySelfServiceAccess> findByIssuanceKeyForUpdate(String key) {
        return one("select * from delivery_self_service_access where tenant_id = ? and issuance_idempotency_key = ? for update",
                tenantId(), key);
    }

    @Override
    public List<DeliverySelfServiceAccess> findActiveForUpdate(UUID deliveryId, UUID customerId, OffsetDateTime now) {
        return jdbc.query("select * from delivery_self_service_access where tenant_id = ? and delivery_order_id = ? "
                        + "and customer_id = ? and revoked_at is null and expires_at > ? order by issued_at limit 5 for update",
                this::access, tenantId(), deliveryId, customerId, now);
    }

    @Override
    public DeliverySelfServiceAccess save(DeliverySelfServiceAccess a) {
        jdbc.execute((ConnectionCallback<Integer>) connection -> {
            Array actions = connection.createArrayOf("varchar", a.allowedActions().stream().map(Enum::name).toArray());
            try (var statement = connection.prepareStatement("""
                insert into delivery_self_service_access
                (id,tenant_id,delivery_order_id,customer_id,recipient_contact_hash,contact_hash_key_version,
                 token_hash,allowed_actions,issuance_idempotency_key,issued_at,expires_at,revoked_at,last_used_at,
                 use_count,version,created_at,updated_at,created_by,updated_by)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                on conflict (id) do update set recipient_contact_hash=excluded.recipient_contact_hash,
                 contact_hash_key_version=excluded.contact_hash_key_version,token_hash=excluded.token_hash,
                 allowed_actions=excluded.allowed_actions,issued_at=excluded.issued_at,expires_at=excluded.expires_at,
                 revoked_at=excluded.revoked_at,updated_at=excluded.updated_at,updated_by=excluded.updated_by,
                 version=delivery_self_service_access.version+1
                """)) {
                statement.setObject(1, a.id()); statement.setObject(2, a.tenantId());
                statement.setObject(3, a.deliveryOrderId()); statement.setObject(4, a.customerId());
                statement.setString(5, a.recipientContactHash()); statement.setString(6, a.contactHashKeyVersion());
                statement.setString(7, a.tokenHash()); statement.setArray(8, actions);
                statement.setString(9, a.issuanceIdempotencyKey()); statement.setObject(10, a.issuedAt());
                statement.setObject(11, a.expiresAt()); statement.setObject(12, a.revokedAt());
                statement.setObject(13, a.lastUsedAt()); statement.setLong(14, a.useCount());
                statement.setLong(15, a.version()); statement.setObject(16, a.issuedAt()); statement.setObject(17, a.issuedAt());
                statement.setString(18, CUSTOMER_ACTOR); statement.setString(19, CUSTOMER_ACTOR);
                return statement.executeUpdate();
            } finally {
                actions.free();
            }
        });
        return findByTokenHash(a.tokenHash()).orElseThrow();
    }

    @Override public void revoke(UUID id, OffsetDateTime at, String reason) {
        jdbc.update("update delivery_self_service_access set revoked_at=?,revocation_reason=?,updated_at=?,updated_by=?,version=version+1 "
                + "where tenant_id=? and id=? and revoked_at is null", at, reason, at, CUSTOMER_ACTOR, tenantId(), id);
    }
    @Override public boolean markUsed(UUID id, OffsetDateTime at) {
        return jdbc.update("update delivery_self_service_access set last_used_at=?,use_count=use_count+1,updated_at=?,updated_by=?,version=version+1 "
                + "where tenant_id=? and id=? and revoked_at is null and expires_at>?",
                at, at, CUSTOMER_ACTOR, tenantId(), id, at) == 1;
    }

    @Override
    public DeliveryCustomerSubmission save(DeliveryCustomerSubmission s) {
        int inserted = jdbc.update("""
            insert into delivery_customer_submission
            (id,tenant_id,delivery_order_id,customer_id,access_id,submission_type,category,description,rating,
             preferred_start_at,preferred_end_at,status,idempotency_key,request_hash,created_at,updated_at,version,
             created_by,updated_by)
            values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on conflict do nothing
            """, s.id(), s.tenantId(), s.deliveryOrderId(), s.customerId(), s.accessId(), s.type().name(),
            s.category(), s.description(), s.rating(), s.preferredStartAt(), s.preferredEndAt(), s.status(),
            s.idempotencyKey(), s.requestHash(), s.createdAt(), s.updatedAt(), s.version(),
            CUSTOMER_ACTOR, CUSTOMER_ACTOR);
        if (inserted == 1) return s;
        var replay = findIdempotent(s.accessId(), s.type(), s.idempotencyKey());
        if (replay.isPresent()) return replay.get();
        if (s.type() == CustomerSubmissionType.FEEDBACK && feedbackExists(s.deliveryOrderId(), s.customerId())) {
            throw new ConflictException("SELF_SERVICE_FEEDBACK_ALREADY_SUBMITTED", "Feedback has already been submitted");
        }
        throw new ConflictException("SELF_SERVICE_CONCURRENT_CHANGE", "Customer request changed concurrently; reload and retry");
    }

    @Override
    public Optional<DeliveryCustomerSubmission> findIdempotent(UUID accessId, CustomerSubmissionType type, String key) {
        return submissionOne("select * from delivery_customer_submission where tenant_id=? and access_id=? "
                + "and submission_type=? and idempotency_key=?", tenantId(), accessId, type.name(), key);
    }
    @Override public boolean feedbackExists(UUID deliveryId, UUID customerId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from delivery_customer_submission "
                + "where tenant_id=? and delivery_order_id=? and customer_id=? and submission_type='FEEDBACK' "
                + "and status<>'SUPERSEDED')", Boolean.class, tenantId(), deliveryId, customerId));
    }
    @Override public long countRecent(UUID deliveryId, OffsetDateTime after) {
        return Optional.ofNullable(jdbc.queryForObject("select count(*) from delivery_customer_submission where tenant_id=? "
                + "and delivery_order_id=? and created_at>=?", Long.class, tenantId(), deliveryId, after)).orElse(0L);
    }
    @Override public List<DeliveryCustomerSubmission> findByDelivery(UUID deliveryId, UUID customerId) {
        return jdbc.query("select * from delivery_customer_submission where tenant_id=? and delivery_order_id=? "
                + "and customer_id=? order by created_at desc limit 100", this::submission, tenantId(), deliveryId, customerId);
    }

    private Optional<DeliverySelfServiceAccess> one(String sql, Object... args) {
        try { return Optional.ofNullable(jdbc.queryForObject(sql, this::access, args)); }
        catch (EmptyResultDataAccessException exception) { return Optional.empty(); }
    }
    private Optional<DeliveryCustomerSubmission> submissionOne(String sql, Object... args) {
        try { return Optional.ofNullable(jdbc.queryForObject(sql, this::submission, args)); }
        catch (EmptyResultDataAccessException exception) { return Optional.empty(); }
    }
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private DeliverySelfServiceAccess access(ResultSet rs, int row) throws SQLException {
        Array array = rs.getArray("allowed_actions");
        Set<SelfServiceAction> actions = new HashSet<>();
        for (Object value : (Object[]) array.getArray()) actions.add(SelfServiceAction.valueOf(value.toString()));
        return new DeliverySelfServiceAccess(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getObject("delivery_order_id", UUID.class), rs.getObject("customer_id", UUID.class),
                rs.getString("recipient_contact_hash"), rs.getString("contact_hash_key_version"),
                rs.getString("token_hash"), Set.copyOf(actions), rs.getString("issuance_idempotency_key"),
                rs.getObject("issued_at", OffsetDateTime.class), rs.getObject("expires_at", OffsetDateTime.class),
                rs.getObject("revoked_at", OffsetDateTime.class), rs.getObject("last_used_at", OffsetDateTime.class),
                rs.getLong("use_count"), rs.getLong("version"));
    }
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private DeliveryCustomerSubmission submission(ResultSet rs, int row) throws SQLException {
        return new DeliveryCustomerSubmission(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getObject("delivery_order_id", UUID.class), rs.getObject("customer_id", UUID.class),
                rs.getObject("access_id", UUID.class), CustomerSubmissionType.valueOf(rs.getString("submission_type")),
                rs.getString("category"), rs.getString("description"), (Integer) rs.getObject("rating"),
                rs.getObject("preferred_start_at", OffsetDateTime.class), rs.getObject("preferred_end_at", OffsetDateTime.class),
                rs.getString("status"), rs.getString("idempotency_key"), rs.getString("request_hash"),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class),
                rs.getLong("version"));
    }
    private UUID tenantId() { return tenants.required().tenantId(); }
}
