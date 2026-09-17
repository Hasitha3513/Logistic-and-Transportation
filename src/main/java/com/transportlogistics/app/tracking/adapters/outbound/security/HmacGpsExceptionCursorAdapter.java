package com.transportlogistics.app.tracking.adapters.outbound.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionCursorPort;
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
final class HmacGpsExceptionCursorAdapter implements GpsExceptionCursorPort {
    private static final String PURPOSE="US55_GPS_EXCEPTION_V1";
    private final ObjectMapper json; private final byte[] secret;
    HmacGpsExceptionCursorAdapter(ObjectMapper json,@Value("${app.tracking.gps-exception.cursor-secret:${security.jwt.secret}}") String secret){
        this.json=json; this.secret=secret.getBytes(StandardCharsets.UTF_8);
        if(this.secret.length<32) throw new IllegalArgumentException("GPS exception cursor secret must contain at least 32 bytes");
    }
    @Override public String encode(UUID tenant,String binding,Instant timestamp,UUID id){try{
        String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(new State(tenant,binding,timestamp,id)));
        String signed=PURPOSE+"."+payload; return signed+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(sign(signed));
    }catch(Exception e){throw invalid();}}
    @Override public Position decode(UUID tenant,String binding,String cursor){try{
        String[] p=cursor.split("\\.",-1); if(p.length!=3||!PURPOSE.equals(p[0]))throw invalid(); String signed=p[0]+"."+p[1];
        if(!MessageDigest.isEqual(sign(signed),Base64.getUrlDecoder().decode(p[2])))throw invalid();
        State s=json.readValue(Base64.getUrlDecoder().decode(p[1]),State.class);
        if(!tenant.equals(s.tenantId())||!binding.equals(s.binding()))throw invalid(); return new Position(s.timestamp(),s.id());
    }catch(BusinessRuleException e){throw e;}catch(Exception e){throw invalid();}}
    private byte[] sign(String value)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));}
    private static BusinessRuleException invalid(){return new BusinessRuleException("GPS_EXCEPTION_CURSOR_INVALID","GPS exception cursor is invalid");}
    private record State(UUID tenantId,String binding,Instant timestamp,UUID id){}
}
