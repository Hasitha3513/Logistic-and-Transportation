package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tracking.domain.TrackingModels.Association;
import com.transportlogistics.app.tracking.domain.TrackingModels.Device;
import com.transportlogistics.app.tracking.domain.TrackingModels.DeviceLifecycle;
import com.transportlogistics.app.tracking.domain.TrackingModels.State;
import com.transportlogistics.app.tracking.ports.inbound.TrackingUseCase;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.time.*;
import java.util.*;

public final class TrackingService implements TrackingUseCase {
 private final TrackingStore store; private final FleetReportingQuery fleet; private final Clock clock;
 public TrackingService(TrackingStore store,FleetReportingQuery fleet,Clock clock){this.store=store;this.fleet=fleet;this.clock=clock;}
 @Override public Device create(Context c,CreateDevice x){required(x.externalDeviceReference(),"externalDeviceReference");required(x.providerAlias(),"providerAlias");return store.insertDevice(c,x,clock.instant());}
 @Override public Device update(Context c,UUID id,long v,UpdateDevice x){required(x.externalDeviceReference(),"externalDeviceReference");return store.updateDevice(c,id,v,x,clock.instant());}
 @Override public Device lifecycle(Context c,UUID id,long v,DeviceLifecycle s){Device current=store.device(c.tenantId(),id).orElseThrow(()->notFound("TRACKING_DEVICE_NOT_FOUND"));if(current.lifecycle()==DeviceLifecycle.RETIRED||s==DeviceLifecycle.DRAFT)throw new BusinessRuleException("TRACKING_DEVICE_INVALID","Device lifecycle transition is invalid");boolean valid=current.lifecycle()==s||s==DeviceLifecycle.RETIRED||current.lifecycle()==DeviceLifecycle.DRAFT&&s==DeviceLifecycle.ACTIVE||current.lifecycle()==DeviceLifecycle.ACTIVE&&s==DeviceLifecycle.DISABLED||current.lifecycle()==DeviceLifecycle.DISABLED&&s==DeviceLifecycle.ACTIVE;if(!valid)throw new BusinessRuleException("TRACKING_DEVICE_INVALID","Device lifecycle transition is invalid");return store.lifecycle(c,id,v,s,clock.instant());}
 @Override public Device get(UUID t,UUID id,boolean reveal){return mask(store.device(t,id).orElseThrow(()->notFound("TRACKING_DEVICE_NOT_FOUND")),reveal);}
 @Override public List<Device> devices(UUID t,int p,int s,boolean reveal){return store.devices(t,Math.max(0,p),Math.min(Math.max(s,1),100)).stream().map(d->mask(d,reveal)).toList();}
 @Override public Association associate(Context c,UUID d,Associate x){if(fleet.findVehicle(x.vehicleId()).filter(v->v.active()).isEmpty())throw notFound("TRACKING_VEHICLE_NOT_FOUND");return store.associate(c,d,x,clock.instant());}
 @Override public Association endAssociation(Context c,UUID d,UUID a,Instant at){return store.endAssociation(c,d,a,at,clock.instant());}
 @Override public Optional<Association> activeAssociation(UUID t,UUID d){return store.activeAssociation(t,d);}
 @Override public List<State> vehicles(UUID t,int p,int s,Instant now){return store.states(t,Math.max(0,p),Math.min(Math.max(s,1),100),now);}
 @Override public State latest(UUID t,UUID v,Instant now){return store.state(t,v,now).orElseThrow(()->notFound("TRACKING_VEHICLE_NOT_FOUND"));}
 @Override public HistoryPage positions(UUID t,UUID v,Instant f,Instant to,String c,int l){if(f==null||to==null||!f.isBefore(to)||Duration.between(f,to).compareTo(Duration.ofHours(24))>0)throw new BusinessRuleException("TRACKING_POSITION_INVALID","History requires a valid window of at most 24 hours");return store.positions(t,v,f,to,c,Math.min(Math.max(l,1),500));}
 @Override public List<IngestResult> ingest(ProviderContext p,List<PositionCommand> xs,Instant received){if(xs==null||xs.isEmpty()||xs.size()>500)throw new BusinessRuleException("TRACKING_POSITION_INVALID","Batch size must be 1..500");return store.ingest(p,xs,received);}
 private static void required(String x,String n){if(x==null||x.isBlank())throw new BusinessRuleException("TRACKING_POSITION_INVALID",n+" is required");}
 private static NotFoundException notFound(String code){return new NotFoundException(code,"Tracking resource was not found");}
 private static Device mask(Device d,boolean reveal){if(reveal)return d;return new Device(d.id(),d.tenantId(),masked(d.externalReference()),d.providerAlias(),masked(d.hardwareSerialReference()),d.lifecycle(),d.registeredAt(),d.registeredBy(),d.lastSeenAt(),d.version());}
 private static String masked(String s){if(s==null)return null;return s.length()<5?"****":"****"+s.substring(s.length()-4);}
}
