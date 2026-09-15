package com.transportlogistics.app.tracking.adapters.outbound.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayError;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class HmacJourneyReplayCursorAdapter implements JourneyReplayCursorPort {
    private final ObjectMapper json;
    private final byte[] secret;

    HmacJourneyReplayCursorAdapter(ObjectMapper json,
            @Value("${app.tracking.journey-replay.cursor-secret:${security.jwt.secret}}") String secret) {
        this.json = json;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("Journey replay cursor secret must contain at least 32 bytes");
        }
    }

    @Override
    public String encode(CursorState state) {
        try {
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.writeValueAsBytes(state));
            return payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
        } catch (Exception exception) {
            throw invalid();
        }
    }

    @Override
    public CursorState decode(String opaqueCursor) {
        try {
            String[] parts = opaqueCursor.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]),
                    Base64.getUrlDecoder().decode(parts[1]))) {
                throw invalid();
            }
            return json.readValue(Base64.getUrlDecoder().decode(parts[0]), CursorState.class);
        } catch (JourneyReplayException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private byte[] sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static JourneyReplayException invalid() {
        return new JourneyReplayException(JourneyReplayError.INVALID_CURSOR);
    }
}
