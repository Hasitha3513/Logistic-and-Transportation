package com.transportlogistics.app.identity.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.transportlogistics.app.identity.application.ports.out.AccessTokenService;
import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import com.transportlogistics.app.identity.domain.model.TokenClaims;
import com.transportlogistics.app.identity.domain.model.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Component
class JwtAccessTokenService implements AccessTokenService {
    private final JwtProperties properties;
    private final Clock clock;
    private final byte[] secret;

    JwtAccessTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String issue(User user) {
        var issuedAt = clock.instant();
        var expiresAt = issuedAt.plus(properties.accessTokenTtl());
        var claims = new JWTClaimsSet.Builder()
                .subject(user.username())
                .issuer(properties.issuer())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("uid", user.id().toString())
                .claim("roles", List.copyOf(user.roleNames()))
                .claim("permissions", List.copyOf(user.permissions()))
                .build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims);
        try {
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign access token", ex);
        }
    }

    @Override
    public TokenClaims verify(String token) {
        try {
            var jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                throw invalidToken();
            }
            var claims = jwt.getJWTClaimsSet();
            var now = clock.instant();
            if (!properties.issuer().equals(claims.getIssuer()) || claims.getExpirationTime() == null
                    || !claims.getExpirationTime().toInstant().isAfter(now) || claims.getSubject() == null) {
                throw invalidToken();
            }
            var roles = claims.getStringListClaim("roles");
            var permissions = claims.getStringListClaim("permissions");
            return new TokenClaims(UUID.fromString(claims.getStringClaim("uid")), claims.getSubject(),
                    roles == null ? java.util.Set.of() : new HashSet<>(roles),
                    permissions == null ? java.util.Set.of() : new HashSet<>(permissions),
                    OffsetDateTime.ofInstant(claims.getExpirationTime().toInstant(), ZoneOffset.UTC));
        } catch (ParseException | JOSEException | IllegalArgumentException ex) {
            throw invalidToken();
        }
    }

    @Override
    public long ttlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    private AuthenticationFailedException invalidToken() {
        return new AuthenticationFailedException("Bearer token is invalid or expired");
    }
}
