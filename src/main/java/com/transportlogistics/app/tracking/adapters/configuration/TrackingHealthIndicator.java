package com.transportlogistics.app.tracking.adapters.configuration;

import com.transportlogistics.app.integration.IntegrationSecretResolver;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("tracking")
final class TrackingHealthIndicator implements HealthIndicator {
 private final TrackingStore store;private final Clock clock;private final AtomicInteger stale=new AtomicInteger();private final AtomicInteger offline=new AtomicInteger();private final AtomicLong latestEpoch=new AtomicLong();
 private final IntegrationSecretResolver secrets;
 TrackingHealthIndicator(TrackingStore store,IntegrationSecretResolver secrets,Clock clock,MeterRegistry meters){this.store=store;this.secrets=secrets;this.clock=clock;meters.gauge("tracking.devices.stale",stale);meters.gauge("tracking.devices.offline",offline);meters.gauge("tracking.ingress.latest_successful_epoch",latestEpoch);}
 @Override public Health health(){var snapshot=store.health(clock.instant());int resolvable=0;for(var binding:store.activeProviderBindings()){var secret=secrets.resolve(binding.credentialReference());if(secret.isPresent()){resolvable++;java.util.Arrays.fill(secret.get(),'\0');}}stale.set(snapshot.staleDevices());offline.set(snapshot.offlineDevices());latestEpoch.set(snapshot.latestSuccessfulIngest()==null?0:snapshot.latestSuccessfulIngest().getEpochSecond());long lag=snapshot.latestSuccessfulIngest()==null?-1:Math.max(0,Duration.between(snapshot.latestSuccessfulIngest(),clock.instant()).toSeconds());var builder=snapshot.configuredBindings()>0&&snapshot.activeBindings()>0&&resolvable==snapshot.activeBindings()?Health.up():Health.down();return builder.withDetail("configured",snapshot.configuredBindings()>0).withDetail("activeBindings",snapshot.activeBindings()).withDetail("resolvableBindings",resolvable).withDetail("lastSuccessfulIngest",snapshot.latestSuccessfulIngest()).withDetail("ingestionLagSeconds",lag).withDetail("staleDeviceCount",snapshot.staleDevices()).withDetail("offlineDeviceCount",snapshot.offlineDevices()).build();}
}
