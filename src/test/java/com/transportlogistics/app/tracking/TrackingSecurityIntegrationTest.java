package com.transportlogistics.app.tracking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase;
import com.transportlogistics.app.tracking.ports.inbound.TrackingProviderManagementUseCase;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBinding;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBindingLifecycle;
import com.transportlogistics.app.tracking.domain.TrackingModels.Device;
import com.transportlogistics.app.tracking.domain.TrackingModels.DeviceLifecycle;
import com.transportlogistics.app.tracking.application.provider.*;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.tracking.provider-secrets.FIXTURE=test-secret")
@AutoConfigureMockMvc
@Import(TrackingSecurityIntegrationTest.Stubs.class)
class TrackingSecurityIntegrationTest {
    private static final UUID TENANT = UUID.fromString("48000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE = UUID.fromString("48000000-0000-0000-0000-000000000002");

    @Autowired MockMvc mvc;
    @Autowired CurrentTenant currentTenant;
    @Autowired TrackingUseCase useCase;
    @Autowired TrackingProviderManagementUseCase providerManagementUseCase;
    @Autowired TrackingStore store;
    @Autowired IntegrationSecretResolver secretResolver;
    private static final UUID BINDING_ID = UUID.fromString("48000000-0000-0000-0000-000000000074");
    private static final String KEY_ID = "security-fixture-key";
    private static final String PROVIDER = "FIXTURE";
    private static final String SECRET = "test-secret";

    @TestConfiguration(proxyBeanMethods = false)
    static class Stubs {
        @Bean @Primary CurrentTenant trackingTestTenant() { return org.mockito.Mockito.mock(CurrentTenant.class); }
        @Bean @Primary TrackingUseCase trackingTestUseCase() { return org.mockito.Mockito.mock(TrackingUseCase.class); }
        @Bean("trackingTestProviderManagementUseCase") @Primary
        TrackingProviderManagementUseCase trackingProviderManagementUseCase() {
            return org.mockito.Mockito.mock(TrackingProviderManagementUseCase.class);
        }
        @Bean @Primary TrackingStore trackingTestStore() { return org.mockito.Mockito.mock(TrackingStore.class); }
        @Bean @Primary IntegrationSecretResolver trackingTestSecrets() { return org.mockito.Mockito.mock(IntegrationSecretResolver.class); }
    }

    @BeforeEach
    void setup() {
        reset(useCase, providerManagementUseCase, store, secretResolver);
        var context = new TenantExecutionContext(TENANT, UUID.randomUUID(), "tracking.operator", "tracking-test");
        when(currentTenant.current()).thenReturn(Optional.of(context));
        when(currentTenant.required()).thenReturn(context);
        when(useCase.vehicles(any(), anyInt(), anyInt(), any())).thenReturn(List.of());
        when(useCase.devices(any(), anyInt(), anyInt(), anyBoolean())).thenReturn(List.of());
        when(store.providerBinding(KEY_ID)).thenReturn(Optional.of(binding(ProviderBindingLifecycle.ACTIVE)));
        when(store.reserveNonce(any(), any(), anyString(), any(), any())).thenReturn(true);
        when(secretResolver.resolve("env:TRACKING_SECURITY_TEST")).thenReturn(Optional.of(SECRET.toCharArray()));
        when(useCase.ingest(any(), any(), any())).thenReturn(List.of());
    }

    @Test @WithMockUser(authorities = "RANDOM_AUTHORITY")
    void literalApiRoutesDenyUnrelatedAuthority() throws Exception {
        mvc.perform(get("/api/v1/tracking/vehicles").contextPath("/api")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tracking/vehicles/{id}/positions", VEHICLE).contextPath("/api")
                .param("from", Instant.now().minusSeconds(60).toString()).param("to", Instant.now().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/devices").contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities = "TRACKING_VIEW")
    void viewCannotReadHistoryOrManageDevices() throws Exception {
        mvc.perform(get("/api/v1/tracking/vehicles").contextPath("/api")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/tracking/vehicles/{id}/positions", VEHICLE).contextPath("/api")
                .param("from", Instant.now().minusSeconds(60).toString()).param("to", Instant.now().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/devices").contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tracking/provider-types").contextPath("/api"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities = "TRACKING_DEVICE_MANAGE")
    void deviceManageAllowsLiteralProviderManagementRoute() throws Exception {
        when(providerManagementUseCase.providerTypes()).thenReturn(List.of());
        mvc.perform(get("/api/v1/tracking/provider-types").contextPath("/api"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(authorities = {"TRACKING_VIEW", "TRACKING_DEVICE_MANAGE"})
    void deviceDetailReturnsSafeCurrentBindingNeededForRefreshRecovery() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UUID currentBindingId = UUID.randomUUID();
        Device device = new Device(deviceId, TENANT, "full-device-reference", "FLESPI", null,
                DeviceLifecycle.DRAFT, Instant.EPOCH, UUID.randomUUID(), null, 3);
        when(useCase.get(TENANT, deviceId, true)).thenReturn(device);
        when(useCase.activeAssociation(TENANT, deviceId)).thenReturn(Optional.empty());
        when(providerManagementUseCase.currentBinding(TENANT, deviceId)).thenReturn(Optional.of(
                new TrackingDeviceProviderBinding(currentBindingId, TENANT, deviceId,
                        new ProviderConnectionId(connectionId), "provider-device-reference",
                        ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.ACTIVE,
                        null, null, null, Instant.EPOCH, UUID.randomUUID(), Instant.EPOCH,
                        UUID.randomUUID(), 7)));
        when(providerManagementUseCase.get(TENANT, new ProviderConnectionId(connectionId)))
                .thenReturn(new TrackingProviderConnection(new ProviderConnectionId(connectionId), TENANT,
                        "provider-key", "FLESPI", "env:SECRET", ProviderType.of("FLESPI"),
                        "Fleet Flespi", null, ProviderSafeConfiguration.empty(), 5, 100,
                        ProviderConnectionLifecycle.ACTIVE, ProviderConnectionTestStatus.PASS,
                        null, null, null, null, null, null, null, Instant.EPOCH, UUID.randomUUID(),
                        Instant.EPOCH, UUID.randomUUID(), 4));

        mvc.perform(get("/api/v1/tracking/devices/{id}", deviceId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProviderBinding.bindingId")
                        .value(currentBindingId.toString()))
                .andExpect(jsonPath("$.currentProviderBinding.providerConnectionId")
                        .value(connectionId.toString()))
                .andExpect(jsonPath("$.currentProviderBinding.bindingLifecycle").value("ACTIVE"))
                .andExpect(jsonPath("$.currentProviderBinding.bindingVersion").value(7))
                .andExpect(jsonPath("$.currentProviderBinding.maskedExternalDeviceReference")
                        .value("****ence"))
                .andExpect(jsonPath("$.currentProviderBinding.credentialReference").doesNotExist());
    }

    @Test @WithMockUser(authorities = "TRACKING_VIEW")
    void viewOnlyDeviceDetailDoesNotExposeManagementBindingVersion() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(useCase.get(TENANT, deviceId, false)).thenReturn(new Device(deviceId, TENANT, "****ence",
                "FLESPI", null, DeviceLifecycle.DRAFT, Instant.EPOCH, UUID.randomUUID(), null, 0));
        when(useCase.activeAssociation(TENANT, deviceId)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/tracking/devices/{id}", deviceId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProviderBinding").doesNotExist());
    }

    @Test
    void unauthenticatedCannotReachAnyLiteralProviderManagementRoute() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/v1/tracking/provider-types").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/tracking/provider-connections").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tracking/provider-connections").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/tracking/provider-connections/{id}", id).contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        for (String action : List.of("test", "activate", "disable", "retire")) {
            mvc.perform(post("/api/v1/tracking/provider-connections/{id}/{action}", id, action)
                            .contextPath("/api").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/tracking/provider-connections/{id}/devices/discover", id)
                        .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(authorities = "TRACKING_HISTORY_VIEW")
    void historyViewCannotUseProviderOrDeviceManagementRoutes() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/v1/tracking/provider-connections").contextPath("/api"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/provider-connections").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/devices/{id}/provider-bindings", id)
                        .contextPath("/api").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tracking/devices/{id}/retire", id)
                        .contextPath("/api").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(store, atLeastOnce()).auditDenied(any(), anyString(), anyString(), any());
    }

    @Test @WithMockUser(authorities = "TRACKING_HISTORY_VIEW")
    void historyPermissionAllowsLiteralHistoryRoute() throws Exception {
        when(useCase.positions(any(), any(), any(), any(), anyString(), anyInt()))
                .thenReturn(new TrackingUseCase.HistoryPage(List.of(), null));
        mvc.perform(get("/api/v1/tracking/vehicles/{id}/positions", VEHICLE).contextPath("/api")
                .param("from", Instant.now().minusSeconds(60).toString()).param("to", Instant.now().toString())
                .param("cursor", "cursor"))
                .andExpect(status().isOk());
    }

    @Test void validSignedProviderRequestSucceedsWithoutTenantAuthorityHeader() throws Exception { performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isOk()); }
    @Test void invalidSignatureIsSanitizedUnauthorized() throws Exception { performSigned(KEY_ID,PROVIDER,"wrong",Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void expiredTimestampIsUnauthorized() throws Exception { performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().minusSeconds(301).getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void futureTimestampIsUnauthorized() throws Exception { performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().plusSeconds(600).getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void nonceReplayIsUnauthorized() throws Exception { when(store.reserveNonce(any(),any(),anyString(),any(),any())).thenReturn(false);performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),"replayed",null).andExpect(status().isUnauthorized()); }
    @Test void unknownBindingIsUnauthorized() throws Exception { when(store.providerBinding("unknown")).thenReturn(Optional.empty());performSigned("unknown",PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void disabledBindingIsUnauthorized() throws Exception { when(store.providerBinding(KEY_ID)).thenReturn(Optional.of(binding(ProviderBindingLifecycle.DISABLED)));performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void draftBindingIsUnauthorized() throws Exception { when(store.providerBinding(KEY_ID)).thenReturn(Optional.of(binding(ProviderBindingLifecycle.DRAFT)));performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void retiredBindingIsUnauthorized() throws Exception { when(store.providerBinding(KEY_ID)).thenReturn(Optional.of(binding(ProviderBindingLifecycle.RETIRED)));performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void wrongCredentialIsUnauthorized() throws Exception { when(secretResolver.resolve("env:TRACKING_SECURITY_TEST")).thenReturn(Optional.of("different".toCharArray()));performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void providerAliasMismatchIsUnauthorized() throws Exception { performSigned(KEY_ID,"OTHER",SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test void wrongDeviceAndTenantMappingFailsClosed() throws Exception { when(useCase.ingest(any(),any(),any())).thenThrow(new com.transportlogistics.app.shared.domain.BusinessRuleException("TRACKING_PROVIDER_UNAUTHORIZED","mismatch"));performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),null).andExpect(status().isUnauthorized()); }
    @Test @WithMockUser(authorities="TRACKING_DEVICE_MANAGE") void humanJwtWithoutProviderCredentialCannotAuthenticateIngress() throws Exception { mvc.perform(post("/api/integration/v1/tracking/positions").contextPath("/api").contentType(MediaType.APPLICATION_JSON).content(positionBody())).andExpect(status().isUnauthorized()); }
    @Test void callerTenantHeaderCannotOverrideTrustedBindingTenant() throws Exception { performSigned(KEY_ID,PROVIDER,SECRET,Instant.now().getEpochSecond(),UUID.randomUUID().toString(),UUID.randomUUID()).andExpect(status().isOk()); }

    private org.springframework.test.web.servlet.ResultActions performSigned(String key,String provider,String secret,long epoch,String nonce,UUID assertedTenant)throws Exception{String body=positionBody();String canonical=epoch+"\n"+nonce+"\n"+key+"\n"+provider+"\n"+body;var builder=post("/api/integration/v1/tracking/positions").contextPath("/api").contentType(MediaType.APPLICATION_JSON).header("X-Tracking-Provider-Key-Id",key).header("X-Tracking-Provider",provider).header("X-Tracking-Timestamp",epoch).header("X-Tracking-Nonce",nonce).header("X-Tracking-Signature",hmac(secret,canonical));if(assertedTenant!=null)builder.header("X-Tracking-Tenant",assertedTenant);return mvc.perform(builder.content(body));}
    private static String positionBody(){return "{\"deviceId\":\"48000000-0000-0000-0000-000000000099\",\"sourceTimestamp\":\""+Instant.now()+"\",\"latitude\":6.9271,\"longitude\":79.8612,\"horizontalAccuracyMeters\":5}";}
    private static String hmac(String secret,String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static ProviderBinding binding(ProviderBindingLifecycle lifecycle){return new ProviderBinding(BINDING_ID,TENANT,KEY_ID,PROVIDER,"env:TRACKING_SECURITY_TEST",lifecycle,Instant.EPOCH,UUID.randomUUID(),Instant.EPOCH,UUID.randomUUID(),0);}
}
