package com.transportlogistics.app.tracking.adapters.outbound.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayError;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopCursorState;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayStopCursorPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class HmacJourneyReplayStopCursorAdapter implements JourneyReplayStopCursorPort {
    private final ObjectMapper json;
    private final byte[] secret;

    HmacJourneyReplayStopCursorAdapter(ObjectMapper json,
            @Value("${app.tracking.journey-replay.cursor-secret:${security.jwt.secret}}") String secret) {
        this.json = json;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) throw new IllegalArgumentException("Cursor secret must contain 32 bytes");
    }

    @Override public String encode(StopCursorState state) {
        try {
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.writeValueAsBytes(state));
            String signed = "STOP." + payload;
            return signed + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(signed));
        } catch (Exception exception) {
            throw invalid();
        }
    }

    @Override public StopCursorState decode(String cursor) {
        try {
            String[] parts = cursor.split("\\.", -1);
            if (parts.length != 3 || !"STOP".equals(parts[0])) throw invalid();
            String signed = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(signed), Base64.getUrlDecoder().decode(parts[2]))) throw invalid();
            return json.readValue(Base64.getUrlDecoder().decode(parts[1]), StopCursorState.class);
        } catch (JourneyReplayException exception) {
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

    private static JourneyReplayException invalid() {
        return new JourneyReplayException(JourneyReplayError.STOP_CURSOR_INVALID);
    }
}
