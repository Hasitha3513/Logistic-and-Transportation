package com.transportlogistics.app.shared.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

/**
 * Closes the direct identifier lookup gap left by discriminator multi-tenancy.
 * Hibernate applies {@code @TenantId} to generated queries, but a direct entity
 * load by primary key is not tenant-qualified.
 */
public class TenantAwareJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {
    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;
    private final boolean tenantScoped;

    public TenantAwareJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
        this.tenantScoped = TenantScopedEntity.class.isAssignableFrom(entityInformation.getJavaType());
    }

    @Override
    public Optional<T> findById(ID id) {
        if (!tenantScoped) {
            return super.findById(id);
        }

        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(entityInformation.getJavaType());
        var root = query.from(entityInformation.getJavaType());
        var idAttr = entityInformation.getIdAttribute();
        String idName = (idAttr != null) ? idAttr.getName() : "id";
        Predicate identifier = builder.equal(root.get(idName), id);
        Predicate tenant = builder.equal(root.get("tenantId"), currentTenantIdentifier());
        return entityManager.createQuery(query.select(root).where(identifier, tenant)).getResultStream().findFirst();
    }

    @Override
    public boolean existsById(ID id) {
        return tenantScoped ? findById(id).isPresent() : super.existsById(id);
    }

    @Override
    public T getReferenceById(ID id) {
        if (!tenantScoped) {
            return super.getReferenceById(id);
        }
        return findById(id).orElseThrow(() -> new EntityNotFoundException(
                entityInformation.getEntityName() + " not found for current tenant: " + id));
    }

    @Override
    public void deleteById(ID id) {
        if (!tenantScoped) {
            super.deleteById(id);
            return;
        }
        findById(id).ifPresent(entityManager::remove);
    }

    private Object currentTenantIdentifier() {
        return UUID.fromString(entityManager.unwrap(Session.class).getTenantIdentifier());
    }
}
