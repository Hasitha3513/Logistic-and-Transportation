package com.transportlogistics.app.tracking.adapters.outbound.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardException;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardCursorPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class HmacTrackingDashboardCursorAdapter implements TrackingDashboardCursorPort {
    private static final String PURPOSE = "US54_DASHBOARD_V1";
    private final ObjectMapper json;
    private final byte[] secret;

    HmacTrackingDashboardCursorAdapter(ObjectMapper json,
            @Value("${app.tracking.dashboard.cursor-secret:${security.jwt.secret}}") String secret) {
        this.json = json;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("Tracking dashboard cursor secret must contain at least 32 bytes");
        }
    }

    @Override
    public String encode(CursorState state) {
        try {
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(state));
            String signed = PURPOSE + "." + payload;
            return signed + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(signed));
        } catch (Exception exception) {
            throw invalid();
        }
    }

    @Override
    public CursorState decode(UUID tenantId,
            com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter filter,
            String cursor, Instant now) {
        try {
            String[] parts = cursor.split("\\.", -1);
            if (parts.length != 3 || !PURPOSE.equals(parts[0])) throw invalid();
            String signed = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(signed), Base64.getUrlDecoder().decode(parts[2]))) throw invalid();
            CursorState state = json.readValue(Base64.getUrlDecoder().decode(parts[1]), CursorState.class);
            if (!tenantId.equals(state.tenantId()) || !filter.equals(state.filter())
                    || !state.expiresAt().isAfter(now)) throw invalid();
            return state;
        } catch (TrackingDashboardException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private static TrackingDashboardException invalid() {
        return new TrackingDashboardException("TRACKING_DASHBOARD_CURSOR_INVALID", "Dashboard cursor is invalid or expired");
    }
}
