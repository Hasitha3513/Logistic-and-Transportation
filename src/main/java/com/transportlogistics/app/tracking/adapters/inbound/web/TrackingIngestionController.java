package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.web.ApiError;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.IngestResult;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.PositionCommand;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.ProviderContext;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBindingLifecycle;
import com.transportlogistics.app.integration.IntegrationSecretResolver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/integration/v1/tracking")
public class TrackingIngestionController {
 private final TrackingUseCase use;private final TrackingStore store;private final ObjectMapper json;private final IntegrationSecretResolver secrets;private final TrackingIngressGuard guard;private final MeterRegistry meters;
 public TrackingIngestionController(TrackingUseCase use,TrackingStore store,ObjectMapper json,IntegrationSecretResolver secrets,TrackingIngressGuard guard,MeterRegistry meters){this.use=use;this.store=store;this.json=json;this.secrets=secrets;this.guard=guard;this.meters=meters;}
 @PostMapping(value="/positions",consumes=MediaType.APPLICATION_JSON_VALUE)
 public List<IngestResult> ingest(@RequestHeader(value="X-Tracking-Provider-Key-Id",required=false)String providerKeyId,@RequestHeader(value="X-Tracking-Provider",required=false)String assertedProvider,@RequestHeader(value="X-Tracking-Timestamp",required=false)String epochValue,@RequestHeader(value="X-Tracking-Nonce",required=false)String nonce,@RequestHeader(value="X-Tracking-Signature",required=false)String signature,@RequestBody String body,HttpServletRequest request){
  Timer.Sample sample=Timer.start(meters);String metricProvider="UNKNOWN";try{if(body.getBytes(StandardCharsets.UTF_8).length>1_048_576)throw error("TRACKING_POSITION_INVALID","Request exceeds 1 MiB");if(blank(providerKeyId)||blank(assertedProvider)||blank(epochValue)||blank(nonce)||blank(signature))throw unauthorized();long epoch;try{epoch=Long.parseLong(epochValue);}catch(NumberFormatException e){throw unauthorized();}var binding=store.providerBinding(providerKeyId).filter(b->b.lifecycle()==ProviderBindingLifecycle.ACTIVE).orElseThrow(TrackingIngestionController::unauthorized);metricProvider=binding.providerAlias();if(!binding.providerAlias().equalsIgnoreCase(assertedProvider))throw unauthorized();Instant now=Instant.now();Instant signedAt;try{signedAt=Instant.ofEpochSecond(epoch);}catch(DateTimeException e){throw unauthorized();}if(Duration.between(signedAt,now).abs().compareTo(Duration.ofMinutes(5))>0)throw unauthorized();char[] secret=secrets.resolve(binding.credentialReference()).orElseThrow(TrackingIngestionController::unauthorized);try{String canonical=epoch+"\n"+nonce+"\n"+providerKeyId+"\n"+assertedProvider+"\n"+body;if(!MessageDigest.isEqual(hmac(secret,canonical),hex(signature)))throw unauthorized();}finally{Arrays.fill(secret,'\0');}String nonceHash=sha(nonce);if(!store.reserveNonce(binding.tenantId(),binding.id(),nonceHash,now,now.plus(Duration.ofMinutes(10))))throw unauthorized();
  JsonNode root=json.readTree(body);List<PositionRequest> input;if(root.isArray())input=Arrays.asList(json.treeToValue(root,PositionRequest[].class));else if(root.has("positions"))input=Arrays.asList(json.treeToValue(root.get("positions"),PositionRequest[].class));else input=List.of(json.treeToValue(root,PositionRequest.class));guard.admit(binding.tenantId(),binding.providerAlias(),input.size());return use.ingest(new ProviderContext(binding.tenantId(),binding.providerAlias()),input.stream().map(PositionRequest::command).toList(),now);}catch(ProviderAuthenticationException e){meters.counter("tracking.ingress.provider_auth_failure","provider",metricProvider.toUpperCase(Locale.ROOT)).increment();throw e;}catch(BusinessRuleException e){String metric=e.code().equals("TRACKING_POSITION_CONFLICT")?"conflict":"invalid";meters.counter("tracking.ingress."+metric,"provider",metricProvider.toUpperCase(Locale.ROOT)).increment();if("TRACKING_PROVIDER_UNAUTHORIZED".equals(e.code()))throw unauthorized();throw e;}catch(JsonProcessingException e){meters.counter("tracking.ingress.invalid","provider",metricProvider.toUpperCase(Locale.ROOT)).increment();throw error("TRACKING_POSITION_INVALID","Malformed telemetry JSON");}finally{sample.stop(meters.timer("tracking.ingress.processing.latency","provider",metricProvider.toUpperCase(Locale.ROOT)));}
 }
 public record PositionRequest(UUID deviceId,String providerMessageId,Long providerSequence,Instant sourceTimestamp,BigDecimal latitude,BigDecimal longitude,BigDecimal horizontalAccuracyMeters,BigDecimal speedKph,BigDecimal headingDegrees,BigDecimal altitudeMeters,EngineState engineState,BigDecimal odometerKm,BigDecimal engineHours,Map<String,String> safeMetadata){PositionCommand command(){return new PositionCommand(deviceId,providerMessageId,providerSequence,sourceTimestamp,latitude,longitude,horizontalAccuracyMeters,speedKph,headingDegrees,altitudeMeters,engineState,odometerKm,engineHours,safeMetadata);}}
 private static byte[] hmac(char[] secret,String value){try{Mac m=Mac.getInstance("HmacSHA256");byte[] key=new String(secret).getBytes(StandardCharsets.UTF_8);try{m.init(new SecretKeySpec(key,"HmacSHA256"));return m.doFinal(value.getBytes(StandardCharsets.UTF_8));}finally{Arrays.fill(key,(byte)0);}}catch(Exception e){throw new IllegalStateException(e);}}
 private static byte[] hex(String s){try{return HexFormat.of().parseHex(s);}catch(Exception e){return new byte[0];}}
 private static String sha(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private static boolean blank(String value){return value==null||value.isBlank();}
 @ExceptionHandler(ProviderAuthenticationException.class) ResponseEntity<ApiError> providerAuthentication(ProviderAuthenticationException ignored,HttpServletRequest request){var body=new ApiError(OffsetDateTime.now(),401,"Unauthorized","TRACKING_PROVIDER_UNAUTHORIZED","Provider authentication failed",request.getRequestURI(),(String)request.getAttribute(CorrelationIdFilter.ATTRIBUTE),List.of());return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);}
 private static ProviderAuthenticationException unauthorized(){return new ProviderAuthenticationException();}private static BusinessRuleException error(String c,String m){return new BusinessRuleException(c,m);}private static final class ProviderAuthenticationException extends RuntimeException {}
}
