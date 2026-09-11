package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelCardRepository;
import com.transportlogistics.app.fuel.domain.model.FuelCard;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class FuelCardPersistenceAdapter implements FuelCardRepository {
    private final FuelCardJpaRepository cards; private final FuelCardBindingJpaRepository bindings;
    private final FuelCardRestrictionJpaRepository restrictions;
    private final FuelCardAuditJpaRepository audits;
    FuelCardPersistenceAdapter(FuelCardJpaRepository cards, FuelCardBindingJpaRepository bindings,
                               FuelCardRestrictionJpaRepository restrictions, FuelCardAuditJpaRepository audits) {
        this.cards=cards; this.bindings=bindings; this.restrictions=restrictions; this.audits=audits;
    }
    @Override public FuelCard save(FuelCard card) {
        FuelCardJpaEntity e = cards.findByTenantIdAndId(card.tenantId(), card.id()).orElseGet(FuelCardJpaEntity::new);
        e.setId(card.id()); e.setTenantId(card.tenantId()); e.setProviderId(card.providerId()); e.setAlias(card.alias());
        e.setProviderCardReference(card.providerCardReference()); e.setProviderReferenceHash(hash(card.providerCardReference()));
        e.setMaskedIdentifier(card.maskedIdentifier()); e.setLastFour(card.lastFour());
        e.setExpiryMonth((short) card.expiryMonth()); e.setExpiryYear((short) card.expiryYear());
        e.setStatus(card.status()); e.setCreatedBy(card.createdBy());
        e.setCreatedAt(card.createdAt()); e.setUpdatedAt(card.updatedAt());
        return map(cards.saveAndFlush(e));
    }
    @Override public Optional<FuelCard> find(UUID tenantId, UUID id) { return cards.findByTenantIdAndId(tenantId,id).map(this::map); }
    @Override public List<FuelCard> list(UUID tenantId,FuelCardUseCase.Search search) {
        Specification<FuelCardJpaEntity> specification = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            if (search.status() != null) predicates.add(builder.equal(root.get("status"), search.status()));
            if (search.providerId() != null) predicates.add(builder.equal(root.get("providerId"), search.providerId()));
            var expiry = builder.sum(builder.prod(root.<Integer>get("expiryYear"), 100), root.<Integer>get("expiryMonth"));
            if (search.expiryFrom() != null) predicates.add(builder.greaterThanOrEqualTo(expiry, search.expiryFrom()));
            if (search.expiryTo() != null) predicates.add(builder.lessThanOrEqualTo(expiry, search.expiryTo()));
            if (search.bindingType() != null || search.bindingId() != null) {
                var binding = query.subquery(UUID.class); var bindingRoot = binding.from(FuelCardBindingJpaEntity.class);
                var bindingPredicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                bindingPredicates.add(builder.equal(bindingRoot.get("tenantId"), tenantId));
                bindingPredicates.add(builder.equal(bindingRoot.get("cardId"), root.get("id")));
                bindingPredicates.add(builder.isNull(bindingRoot.get("effectiveTo")));
                if (search.bindingType() != null) bindingPredicates.add(builder.equal(bindingRoot.get("bindingType"), search.bindingType().toUpperCase(java.util.Locale.ROOT)));
                if (search.bindingId() != null) bindingPredicates.add(builder.equal(bindingRoot.get("bindingId"), search.bindingId()));
                binding.select(bindingRoot.get("id")).where(bindingPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
                predicates.add(builder.exists(binding));
            }
            if (search.reviewRequired() != null) {
                var review = query.subquery(UUID.class); var transactionRoot = review.from(FuelCardTransactionJpaEntity.class);
                review.select(transactionRoot.get("id")).where(builder.equal(transactionRoot.get("tenantId"), tenantId),
                        builder.equal(transactionRoot.get("cardId"), root.get("id")),
                        builder.equal(transactionRoot.get("localStatus"), "REVIEW_REQUIRED"));
                predicates.add(search.reviewRequired() ? builder.exists(review) : builder.not(builder.exists(review)));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return cards.findAll(specification, cardPage(search)).stream().map(this::map).toList();
    }
    @Override public boolean referenceExists(UUID t,UUID p,String r) { return cards.existsByTenantIdAndProviderIdAndProviderCardReference(t,p,r); }
    @Override public Optional<FuelCard> findByProviderReference(UUID t,UUID p,String r) { return cards.findByTenantIdAndProviderIdAndProviderCardReference(t,p,r).map(this::map); }
    @Override public boolean hasActiveBinding(UUID t,UUID c) { return bindings.findByTenantIdAndCardIdAndEffectiveToIsNull(t,c).isPresent(); }
    @Override public boolean hasRestriction(UUID t,UUID c) { return restrictions.existsByTenantIdAndCardId(t,c); }
    @Override public Optional<FuelCardUseCase.Restriction> restriction(UUID t,UUID c) { return restrictions.findByTenantIdAndCardId(t,c).map(this::map); }
    @Override public Optional<FuelCardUseCase.Binding> activeBinding(UUID t,UUID c) { return bindings.findByTenantIdAndCardIdAndEffectiveToIsNull(t,c).map(this::map); }
    @Override @Transactional public FuelCardUseCase.Binding replaceBinding(UUID t,UUID c,FuelCardUseCase.Bind x,UUID actor,OffsetDateTime now) {
        bindings.findByTenantIdAndCardIdAndEffectiveToIsNull(t,c).ifPresent(old->{ old.setEffectiveTo(now); bindings.save(old); });
        var e=new FuelCardBindingJpaEntity(); e.setId(UUID.randomUUID()); e.setTenantId(t); e.setCardId(c);
        e.setBindingType(x.bindingType().toUpperCase(java.util.Locale.ROOT)); e.setBindingId(x.bindingId());
        e.setEffectiveFrom(now); e.setReason(x.reason().trim()); e.setChangedBy(actor); e.setCreatedAt(now);
        return map(bindings.save(e));
    }
    @Override public List<FuelCardUseCase.Binding> bindings(UUID t,UUID c) {
        return bindings.findByTenantIdAndCardIdOrderByEffectiveFromDesc(t,c).stream().map(this::map).toList();
    }
    @Override @Transactional public FuelCardUseCase.Restriction replaceRestriction(UUID t,UUID c,FuelCardUseCase.Restrict x,UUID actor,OffsetDateTime now) {
        var e=restrictions.findByTenantIdAndCardId(t,c).orElseGet(()->{var n=new FuelCardRestrictionJpaEntity();n.setId(UUID.randomUUID());n.setTenantId(t);n.setCardId(c);return n;});
        e.setCurrency(x.currency().toUpperCase(java.util.Locale.ROOT)); e.setMaxTransactionAmount(x.maxTransactionAmount());
        e.setMaxDailyAmount(x.maxDailyAmount()); e.setMaxMonthlyAmount(x.maxMonthlyAmount()); e.setMaxDailyLitres(x.maxDailyLitres());
        e.setAllowedFuelTypes(String.join("\n",x.allowedFuelTypes()));
        e.setAllowedStationReferences(String.join("\n",x.allowedStationReferences()==null?java.util.Set.of():x.allowedStationReferences()));
        e.setChangedBy(actor); e.setChangedAt(now); return map(restrictions.save(e));
    }
    @Override public void audit(UUID t, UUID c, UUID transactionId, String action, String result,
                                String reason, UUID actor, OffsetDateTime now) {
        var e = new FuelCardAuditJpaEntity(); e.setId(UUID.randomUUID()); e.setTenantId(t); e.setCardId(c);
        e.setTransactionId(transactionId); e.setAction(action); e.setResult(result);
        e.setReasonCode(reason == null ? null : reason.substring(0, Math.min(80, reason.length())));
        e.setActorId(actor); e.setCreatedAt(now); audits.save(e);
    }
    @Override public List<FuelCardUseCase.History> history(UUID t, UUID c, int page, int limit) {
        return audits.findByTenantIdAndCardIdOrderByCreatedAtDesc(t, c, PageRequest.of(page, limit)).stream()
                .map(e -> new FuelCardUseCase.History(e.getId(), e.getAction(), e.getResult(),
                        e.getReasonCode(), e.getActorId(), e.getCreatedAt())).toList();
    }
    private FuelCard map(FuelCardJpaEntity e){return new FuelCard(e.getId(),e.getTenantId(),e.getProviderId(),e.getAlias(),e.getProviderCardReference(),e.getMaskedIdentifier(),e.getLastFour(),e.getExpiryMonth(),e.getExpiryYear(),e.getStatus(),e.getVersion(),e.getCreatedBy(),e.getCreatedAt(),e.getUpdatedAt());}
    private FuelCardUseCase.Binding map(FuelCardBindingJpaEntity e){return new FuelCardUseCase.Binding(e.getId(),e.getBindingType(),e.getBindingId(),e.getEffectiveFrom(),e.getEffectiveTo(),e.getReason());}
    private FuelCardUseCase.Restriction map(FuelCardRestrictionJpaEntity e){return new FuelCardUseCase.Restriction(e.getCurrency(),e.getMaxTransactionAmount(),e.getMaxDailyAmount(),e.getMaxMonthlyAmount(),e.getMaxDailyLitres(),split(e.getAllowedFuelTypes()),split(e.getAllowedStationReferences()),e.getVersion());}
    private static java.util.Set<String> split(String s){return s==null||s.isBlank()?java.util.Set.of():new LinkedHashSet<>(Arrays.asList(s.split("\\n")));}
    private static PageRequest cardPage(FuelCardUseCase.Search search) {
        var direction = "asc".equalsIgnoreCase(search.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var field = switch (search.sort() == null ? "createdAt" : search.sort()) {
            case "maskedIdentifier" -> "maskedIdentifier"; case "expiry" -> "expiryYear";
            case "status" -> "status"; default -> "createdAt";
        };
        var sort = Sort.by(direction, field);
        if ("expiry".equals(search.sort())) sort = sort.and(Sort.by(direction, "expiryMonth"));
        return PageRequest.of(search.page(), search.limit(), sort.and(Sort.by(Sort.Direction.ASC, "id")));
    }
    private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
