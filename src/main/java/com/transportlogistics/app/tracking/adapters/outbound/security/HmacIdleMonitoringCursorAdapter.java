package com.transportlogistics.app.tracking.adapters.outbound.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.ports.outbound.IdleMonitoringCursorPort;
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
final class HmacIdleMonitoringCursorAdapter implements IdleMonitoringCursorPort {
    private static final String PURPOSE = "US51_IDLE_MONITORING_V1";
    private final ObjectMapper json;
    private final byte[] secret;

    HmacIdleMonitoringCursorAdapter(ObjectMapper json,
            @Value("${app.tracking.idle-monitoring.cursor-secret:${security.jwt.secret}}") String secret) {
        this.json = json; this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) throw new IllegalArgumentException("Idle monitoring cursor secret must contain at least 32 bytes");
    }
    @Override public String encode(UUID tenantId, String binding, Instant timestamp, UUID id) { try {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(new State(tenantId,binding,timestamp,id)));
        String signed = PURPOSE + "." + payload;
        return signed + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(signed));
    } catch (Exception exception) { throw invalid(); } }
    @Override public Position decode(UUID tenantId, String binding, String cursor) { try {
        String[] parts = cursor.split("\\.",-1); if(parts.length!=3 || !PURPOSE.equals(parts[0])) throw invalid();
        String signed=parts[0]+"."+parts[1];
        if(!MessageDigest.isEqual(sign(signed),Base64.getUrlDecoder().decode(parts[2]))) throw invalid();
        State state=json.readValue(Base64.getUrlDecoder().decode(parts[1]),State.class);
        if(!tenantId.equals(state.tenantId()) || !binding.equals(state.binding())) throw invalid();
        return new Position(state.timestamp(),state.id());
    } catch(BusinessRuleException exception){throw exception;} catch(Exception exception){throw invalid();} }
    private byte[] sign(String value)throws Exception { Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return mac.doFinal(value.getBytes(StandardCharsets.UTF_8)); }
    private static BusinessRuleException invalid(){return new BusinessRuleException("IDLE_MONITOR_CURSOR_INVALID","Idle monitoring cursor is invalid");}
    private record State(UUID tenantId,String binding,Instant timestamp,UUID id) { }
}
