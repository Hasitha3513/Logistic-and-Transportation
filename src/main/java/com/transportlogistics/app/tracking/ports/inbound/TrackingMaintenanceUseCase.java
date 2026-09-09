package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBinding;
import com.transportlogistics.app.tracking.domain.TrackingModels.ProviderBindingLifecycle;
import com.transportlogistics.app.tracking.domain.TrackingModels.RetentionPolicy;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase.Context;
import java.time.Instant;
import java.util.UUID;

/** Internal administration and repair boundary. It is deliberately not exposed by REST. */
public interface TrackingMaintenanceUseCase {
 ProviderBinding createProviderBinding(Context context,String providerKeyId,String providerAlias,String credentialReference);
 ProviderBinding providerBindingLifecycle(Context context,UUID id,long version,ProviderBindingLifecycle lifecycle);
 ProviderBinding updateProviderCredential(Context context,UUID id,long version,String credentialReference);
 RetentionPolicy configureRetention(Context context,long retentionDurationSeconds,String policyVersion,Instant effectiveAt);
 void rebuildLatest(UUID tenantId,UUID vehicleId);
}
