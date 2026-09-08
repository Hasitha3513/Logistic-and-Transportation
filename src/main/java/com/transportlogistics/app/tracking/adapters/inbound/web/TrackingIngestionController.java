package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.web.ApiError;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.IngestResult;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.PositionCommand;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.ProviderContext;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/integration/v1/tracking")
public class TrackingIngestionController {
 private final TrackingUseCase use;private final TrackingStore store;private final ObjectMapper json;private final Environment env;private final TrackingIngressGuard guard;
 public TrackingIngestionController(TrackingUseCase use,TrackingStore store,ObjectMapper json,Environment env,TrackingIngressGuard guard){this.use=use;this.store=store;this.json=json;this.env=env;this.guard=guard;}
 @PostMapping(value="/positions",consumes=MediaType.APPLICATION_JSON_VALUE)
 public List<IngestResult> ingest(@RequestHeader("X-Tracking-Tenant")UUID tenant,@RequestHeader("X-Tracking-Provider")String provider,@RequestHeader("X-Tracking-Timestamp")long epoch,@RequestHeader("X-Tracking-Nonce")String nonce,@RequestHeader("X-Tracking-Signature")String signature,@RequestBody String body,HttpServletRequest request){
  if(body.getBytes(StandardCharsets.UTF_8).length>1_048_576)throw error("TRACKING_POSITION_INVALID","Request exceeds 1 MiB");Instant now=Instant.now();Instant signedAt=Instant.ofEpochSecond(epoch);if(Duration.between(signedAt,now).abs().compareTo(Duration.ofMinutes(5))>0)throw unauthorized();String secret=env.getProperty("app.tracking.provider-secrets."+provider);if(secret==null||secret.isBlank())throw unauthorized();String canonical=epoch+"\n"+nonce+"\n"+tenant+"\n"+body;if(!MessageDigest.isEqual(hmac(secret,canonical),hex(signature)))throw unauthorized();String nonceHash=sha(nonce);if(!store.reserveNonce(tenant,provider,nonceHash,now,now.plus(Duration.ofMinutes(10))))throw unauthorized();
  try{JsonNode root=json.readTree(body);List<PositionRequest> input;if(root.isArray())input=Arrays.asList(json.treeToValue(root,PositionRequest[].class));else if(root.has("positions"))input=Arrays.asList(json.treeToValue(root.get("positions"),PositionRequest[].class));else input=List.of(json.treeToValue(root,PositionRequest.class));guard.admit(tenant,provider,input.size());return use.ingest(new ProviderContext(tenant,provider),input.stream().map(PositionRequest::command).toList(),now);}catch(BusinessRuleException e){if("TRACKING_PROVIDER_UNAUTHORIZED".equals(e.code()))throw unauthorized();throw e;}catch(Exception e){throw error("TRACKING_POSITION_INVALID","Malformed telemetry JSON");}
 }
 public record PositionRequest(UUID deviceId,String providerMessageId,Long providerSequence,Instant sourceTimestamp,BigDecimal latitude,BigDecimal longitude,BigDecimal horizontalAccuracyMeters,BigDecimal speedKph,BigDecimal headingDegrees,BigDecimal altitudeMeters,EngineState engineState,BigDecimal odometerKm,BigDecimal engineHours,Map<String,String> safeMetadata){PositionCommand command(){return new PositionCommand(deviceId,providerMessageId,providerSequence,sourceTimestamp,latitude,longitude,horizontalAccuracyMeters,speedKph,headingDegrees,altitudeMeters,engineState,odometerKm,engineHours,safeMetadata);}}
 private static byte[] hmac(String secret,String value){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return m.doFinal(value.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
 private static byte[] hex(String s){try{return HexFormat.of().parseHex(s);}catch(Exception e){return new byte[0];}}
 private static String sha(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @ExceptionHandler(ProviderAuthenticationException.class) ResponseEntity<ApiError> providerAuthentication(ProviderAuthenticationException ignored,HttpServletRequest request){var body=new ApiError(OffsetDateTime.now(),401,"Unauthorized","TRACKING_PROVIDER_UNAUTHORIZED","Provider authentication failed",request.getRequestURI(),(String)request.getAttribute(CorrelationIdFilter.ATTRIBUTE),List.of());return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);}
 private static ProviderAuthenticationException unauthorized(){return new ProviderAuthenticationException();}private static BusinessRuleException error(String c,String m){return new BusinessRuleException(c,m);}private static final class ProviderAuthenticationException extends RuntimeException {}
}
