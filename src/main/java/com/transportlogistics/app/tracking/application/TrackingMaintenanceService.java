package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBinding;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBindingLifecycle;
import com.transportlogistics.app.tracking.domain.TrackingModels.RetentionPolicy;
import com.transportlogistics.app.tracking.ports.inbound.TrackingMaintenanceUseCase;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Context;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class TrackingMaintenanceService implements TrackingMaintenanceUseCase {
 private final TrackingStore store; private final Clock clock;
 public TrackingMaintenanceService(TrackingStore store,Clock clock){this.store=store;this.clock=clock;}
 @Override public ProviderBinding createProviderBinding(Context c,String key,String alias,String credential){required(key,"providerKeyId");required(alias,"providerAlias");required(credential,"credentialReference");return store.insertProviderBinding(c,key.trim(),alias.trim().toUpperCase(java.util.Locale.ROOT),credential.trim(),clock.instant());}
 @Override public ProviderBinding providerBindingLifecycle(Context c,UUID id,long version,ProviderBindingLifecycle lifecycle){return store.providerBindingLifecycle(c,id,version,lifecycle,clock.instant());}
 @Override public ProviderBinding updateProviderCredential(Context c,UUID id,long version,String credential){required(credential,"credentialReference");return store.updateProviderCredential(c,id,version,credential.trim(),clock.instant());}
 @Override public RetentionPolicy configureRetention(Context c,long seconds,String version,Instant effectiveAt){if(seconds<=0)throw new BusinessRuleException("TRACKING_RETENTION_INVALID","Retention duration must be positive");required(version,"policyVersion");if(effectiveAt==null)throw new BusinessRuleException("TRACKING_RETENTION_INVALID","effectiveAt is required");return store.upsertRetentionPolicy(c,seconds,version.trim(),effectiveAt,clock.instant());}
 @Override public void rebuildLatest(UUID tenantId,UUID vehicleId){store.rebuildLatest(tenantId,vehicleId,clock.instant());}
 private static void required(String value,String field){if(value==null||value.isBlank())throw new BusinessRuleException("TRACKING_PROVIDER_BINDING_INVALID",field+" is required");}
}
