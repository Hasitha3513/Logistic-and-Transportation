package com.transportlogistics.app.tracking.domain.routedeviation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class RouteDeviationEpisodeIdentity {
    private RouteDeviationEpisodeIdentity() { }
    public static UUID create(UUID tenant,UUID vehicle,UUID trip,UUID route,RouteVersion routeVersion,
                              UUID rule,long ruleVersion,UUID firstPosition){
        String canonical=tenant+"|"+vehicle+"|"+(trip==null?"NONE":trip)+"|"+route+"|"+routeVersion.value()+"|"+rule+"|"+ruleVersion+"|"+firstPosition;
        return uuid(canonical);
    }
    public static UUID escalation(UUID tenant,UUID episode,String reason){return uuid(tenant+"|"+episode+"|"+reason);}
    private static UUID uuid(String value){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));h[6]=(byte)((h[6]&0x0f)|0x50);h[8]=(byte)((h[8]&0x3f)|0x80);java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(h);return new UUID(b.getLong(),b.getLong());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
