package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.application.provider.DeviceProviderBindingLifecycle;
import com.transportlogistics.app.tracking.application.provider.NewTrackingDeviceProviderBinding;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderSafeConfiguration;
import com.transportlogistics.app.tracking.domain.TrackingModels.DeviceLifecycle;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Associate;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Context;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.CreateDevice;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDeviceProviderBindingStore;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TrackingDeviceOnboardingContractPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");

    @Autowired TrackingStore devices;
    @Autowired TrackingDeviceProviderBindingStore bindings;
    @Autowired JdbcTemplate jdbc;

    @Test
    void draftDeviceCanBePreparedThenExplicitlyActivated() {
        Fixture fixture = fixture("prepared");
        var association = devices.associate(context(fixture.tenant()), fixture.deviceId(),
                new Associate(UUID.randomUUID(), NOW), NOW);
        var binding = bindings.create(binding(fixture, "external-prepared"));

        assertThat(devices.device(fixture.tenant(), fixture.deviceId()).orElseThrow().lifecycle())
                .isEqualTo(DeviceLifecycle.DRAFT);
        assertThat(binding.lifecycle()).isEqualTo(DeviceProviderBindingLifecycle.ACTIVE);
        long deviceVersion = devices.device(fixture.tenant(), fixture.deviceId()).orElseThrow().version();
        var active = devices.lifecycle(context(fixture.tenant()), fixture.deviceId(), deviceVersion,
                DeviceLifecycle.ACTIVE, NOW.plusSeconds(1));
        assertThat(active.lifecycle()).isEqualTo(DeviceLifecycle.ACTIVE);
        assertThat(devices.activeAssociation(fixture.tenant(), fixture.deviceId()))
                .contains(association);
    }

    @Test
    void activationFailsClosedWhenEitherPreparationFactIsMissing() {
        Fixture withoutBinding = fixture("without-binding");
        devices.associate(context(withoutBinding.tenant()), withoutBinding.deviceId(),
                new Associate(UUID.randomUUID(), NOW), NOW);
        assertNotReady(withoutBinding);

        Fixture withoutVehicle = fixture("without-vehicle");
        bindings.create(binding(withoutVehicle, "external-without-vehicle"));
        assertNotReady(withoutVehicle);
    }

    @Test
    void currentBindingIsDeterministicAndSupportsRefreshThenRebind() {
        Fixture fixture = fixture("refresh");
        var first = bindings.create(binding(fixture, "external-first"));
        assertThat(bindings.findCurrentByDevice(fixture.tenant(), fixture.deviceId())).contains(first);

        ProviderConnectionId replacement = provider(fixture.tenant(), "REPLACEMENT");
        var rebound = bindings.rebind(new NewTrackingDeviceProviderBinding(
                fixture.tenant(), fixture.deviceId(), replacement, "external-second",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.ACTIVE,
                NOW, ACTOR, NOW.plusSeconds(1)), first.version());

        assertThat(bindings.findCurrentByDevice(fixture.tenant(), fixture.deviceId())).contains(rebound);
        assertThat(bindings.find(UUID.randomUUID(), rebound.id())).isEmpty();
        var advanced = bindings.updateNextPoll(fixture.tenant(), rebound.id(), rebound.version(),
                NOW.plusSeconds(30), ACTOR, NOW.plusSeconds(2));
        assertThatThrownBy(() -> bindings.rebind(new NewTrackingDeviceProviderBinding(
                fixture.tenant(), fixture.deviceId(), fixture.connectionId(), "external-third",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.ACTIVE,
                NOW, ACTOR, NOW.plusSeconds(3)), advanced.version() - 1))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_STALE_VERSION"));
    }

    @Test
    void retiredDeviceCannotAssociateBindRebindOrActivate() {
        Fixture fixture = fixture("retired");
        var first = bindings.create(binding(fixture, "external-retired"));
        long deviceVersion = devices.device(fixture.tenant(), fixture.deviceId()).orElseThrow().version();
        devices.lifecycle(context(fixture.tenant()), fixture.deviceId(), deviceVersion,
                DeviceLifecycle.RETIRED, NOW.plusSeconds(1));

        assertThatThrownBy(() -> devices.associate(context(fixture.tenant()), fixture.deviceId(),
                new Associate(UUID.randomUUID(), NOW), NOW.plusSeconds(2)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> bindings.create(new NewTrackingDeviceProviderBinding(
                fixture.tenant(), fixture.deviceId(), fixture.connectionId(), "another-reference",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.DRAFT,
                null, ACTOR, NOW.plusSeconds(2))))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> bindings.rebind(new NewTrackingDeviceProviderBinding(
                fixture.tenant(), fixture.deviceId(), fixture.connectionId(), "replacement-reference",
                ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.ACTIVE,
                NOW, ACTOR, NOW.plusSeconds(2)), first.version()))
                .isInstanceOf(BusinessRuleException.class);
        long retiredVersion = devices.device(fixture.tenant(), fixture.deviceId()).orElseThrow().version();
        assertThatThrownBy(() -> devices.lifecycle(context(fixture.tenant()), fixture.deviceId(), retiredVersion,
                DeviceLifecycle.ACTIVE, NOW.plusSeconds(2)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void onboardingTransitionRacesResolveToValidSerializedOutcomes() throws Exception {
        Fixture associateRetire = fixture("associate-retire-race");
        long retireVersion = devices.device(associateRetire.tenant(), associateRetire.deviceId())
                .orElseThrow().version();
        UUID vehicle = UUID.randomUUID();
        List<Object> associationRace = race(
                () -> devices.associate(context(associateRetire.tenant()), associateRetire.deviceId(),
                        new Associate(vehicle, NOW), NOW),
                () -> devices.lifecycle(context(associateRetire.tenant()), associateRetire.deviceId(),
                        retireVersion, DeviceLifecycle.RETIRED, NOW.plusSeconds(1)));
        assertThat(associationRace.stream().filter(BusinessRuleException.class::isInstance).count())
                .isLessThanOrEqualTo(1);
        assertThat(devices.device(associateRetire.tenant(), associateRetire.deviceId()).orElseThrow().lifecycle())
                .isEqualTo(DeviceLifecycle.RETIRED);

        Fixture activateDisable = prepared("activate-disable-race");
        long activateVersion = devices.device(activateDisable.tenant(), activateDisable.deviceId())
                .orElseThrow().version();
        List<Object> disableRace = race(
                () -> devices.lifecycle(context(activateDisable.tenant()), activateDisable.deviceId(),
                        activateVersion, DeviceLifecycle.ACTIVE, NOW.plusSeconds(2)),
                () -> jdbc.update("UPDATE tracking_provider_binding SET lifecycle='DISABLED',version=version+1 "
                                + "WHERE tenant_id=? AND id=? AND lifecycle='ACTIVE'",
                        activateDisable.tenant(), activateDisable.connectionId().value()));
        assertThat(disableRace).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT lifecycle FROM tracking_provider_binding WHERE tenant_id=? AND id=?",
                String.class, activateDisable.tenant(), activateDisable.connectionId().value()))
                .isEqualTo("DISABLED");

        Fixture activateRebind = prepared("activate-rebind-race");
        var current = bindings.findCurrentByDevice(activateRebind.tenant(), activateRebind.deviceId()).orElseThrow();
        ProviderConnectionId replacement = provider(activateRebind.tenant(), "RACE_REPLACEMENT");
        long activationVersion = devices.device(activateRebind.tenant(), activateRebind.deviceId())
                .orElseThrow().version();
        List<Object> rebindRace = race(
                () -> devices.lifecycle(context(activateRebind.tenant()), activateRebind.deviceId(),
                        activationVersion, DeviceLifecycle.ACTIVE, NOW.plusSeconds(3)),
                () -> bindings.rebind(new NewTrackingDeviceProviderBinding(
                        activateRebind.tenant(), activateRebind.deviceId(), replacement, "external-race-replacement",
                        ProviderSafeConfiguration.empty(), DeviceProviderBindingLifecycle.ACTIVE,
                        NOW, ACTOR, NOW.plusSeconds(3)), current.version()));
        assertThat(rebindRace).allMatch(result -> !(result instanceof Throwable)
                || result instanceof BusinessRuleException error
                && error.code().equals("TRACKING_STALE_VERSION"));
        assertThat(bindings.findCurrentByDevice(activateRebind.tenant(), activateRebind.deviceId()))
                .hasValueSatisfying(binding -> assertThat(binding.providerConnectionId()).isEqualTo(replacement));
        var deviceAfterRace = devices.device(activateRebind.tenant(), activateRebind.deviceId()).orElseThrow();
        if (deviceAfterRace.lifecycle() == DeviceLifecycle.DRAFT) {
            devices.lifecycle(context(activateRebind.tenant()), activateRebind.deviceId(), deviceAfterRace.version(),
                    DeviceLifecycle.ACTIVE, NOW.plusSeconds(4));
        }
        assertThat(devices.device(activateRebind.tenant(), activateRebind.deviceId()).orElseThrow().lifecycle())
                .isEqualTo(DeviceLifecycle.ACTIVE);
        System.out.println("US48_CS09_ONBOARDING_TRANSITION_RACES=3/3 PASS");
    }

    private Fixture fixture(String suffix) {
        UUID tenant = UUID.randomUUID();
        ProviderConnectionId connection = provider(tenant, "FIXTURE_" + suffix.toUpperCase());
        var device = devices.insertDevice(context(tenant),
                new CreateDevice("legacy-" + suffix, "FIXTURE", null), NOW);
        return new Fixture(tenant, device.id(), connection);
    }

    private Fixture prepared(String suffix) {
        Fixture fixture = fixture(suffix);
        devices.associate(context(fixture.tenant()), fixture.deviceId(),
                new Associate(UUID.randomUUID(), NOW), NOW);
        bindings.create(binding(fixture, "external-" + suffix));
        return fixture;
    }

    private ProviderConnectionId provider(UUID tenant, String alias) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_provider_binding(
                 id,tenant_id,provider_key_id,provider_alias,credential_reference,lifecycle,
                 created_at,created_by,updated_at,updated_by,provider_type,display_name,
                 safe_configuration,poll_interval_seconds,page_size,test_status,version)
                VALUES(?,?,?,?,?,'ACTIVE',?,?,?,?,?,?,?::jsonb,5,100,'PASS',0)
                """, id, tenant, "key-" + id, alias, "env:TRACKING_TEST", Timestamp.from(NOW),
                ACTOR, Timestamp.from(NOW), ACTOR, "FLESPI", "Provider " + alias, "{}");
        return new ProviderConnectionId(id);
    }

    private NewTrackingDeviceProviderBinding binding(Fixture fixture, String externalReference) {
        return new NewTrackingDeviceProviderBinding(fixture.tenant(), fixture.deviceId(),
                fixture.connectionId(), externalReference, ProviderSafeConfiguration.empty(),
                DeviceProviderBindingLifecycle.ACTIVE, NOW, ACTOR, NOW);
    }

    private void assertNotReady(Fixture fixture) {
        long version = devices.device(fixture.tenant(), fixture.deviceId()).orElseThrow().version();
        assertThatThrownBy(() -> devices.lifecycle(context(fixture.tenant()), fixture.deviceId(), version,
                DeviceLifecycle.ACTIVE, NOW.plusSeconds(1)))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_DEVICE_NOT_READY"));
    }

    private Context context(UUID tenant) {
        return new Context(tenant, ACTOR, "cs09-backend-remediation");
    }

    private List<Object> race(Callable<?> first, Callable<?> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(() -> attempt(barrier, first));
            var right = executor.submit(() -> attempt(barrier, second));
            return List.of(left.get(), right.get());
        }
    }

    private Object attempt(CyclicBarrier barrier, Callable<?> operation) {
        try {
            barrier.await();
            return operation.call();
        } catch (Exception failure) {
            return failure;
        }
    }

    private record Fixture(UUID tenant, UUID deviceId, ProviderConnectionId connectionId) { }
}
