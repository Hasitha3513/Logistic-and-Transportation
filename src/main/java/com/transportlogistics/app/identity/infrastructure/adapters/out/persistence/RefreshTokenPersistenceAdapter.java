package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.identity.application.ports.out.RefreshTokenStore;
import com.transportlogistics.app.identity.domain.model.IssuedRefreshToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenStore {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenJpaRepository repository;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public IssuedRefreshToken issue(UUID userId, OffsetDateTime expiresAt) {
        return create(userId, expiresAt).token();
    }

    @Override
    @Transactional
    public Optional<Rotation> rotate(String token, OffsetDateTime now, OffsetDateTime newExpiresAt) {
        var existing = repository.findLockedByTokenHash(hash(token)).orElse(null);
        if (existing == null || existing.getRevokedAt() != null || !existing.getExpiresAt().isAfter(now)) {
            return Optional.empty();
        }
        var replacement = create(existing.getUserId(), newExpiresAt);
        existing.setRevokedAt(now);
        existing.setReplacedBy(replacement.id());
        repository.save(existing);
        return Optional.of(new Rotation(existing.getUserId(), replacement.token()));
    }

    @Override
    @Transactional
    public boolean revoke(String token, OffsetDateTime revokedAt) {
        var existing = repository.findLockedByTokenHash(hash(token)).orElse(null);
        if (existing == null || existing.getRevokedAt() != null || !existing.getExpiresAt().isAfter(revokedAt)) {
            return false;
        }
        existing.setRevokedAt(revokedAt);
        repository.save(existing);
        return true;
    }

    private Created create(UUID userId, OffsetDateTime expiresAt) {
        var rawToken = randomToken();
        var entity = new RefreshTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        entity.setCreatedAt(OffsetDateTime.now(java.time.ZoneOffset.UTC));
        entity.setExpiresAt(expiresAt);
        repository.save(entity);
        return new Created(entity.getId(), new IssuedRefreshToken(rawToken, expiresAt));
    }

    private String randomToken() {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private record Created(UUID id, IssuedRefreshToken token) {
    }
}
