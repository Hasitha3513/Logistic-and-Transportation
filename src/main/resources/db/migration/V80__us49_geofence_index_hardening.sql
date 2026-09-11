CREATE INDEX idx_tracking_geofence_job_global_due
 ON tracking_geofence_evaluation_job(next_attempt_at,tenant_id,position_id)
 INCLUDE(status,lease_until);

CREATE INDEX idx_tracking_geofence_active_bbox_upper
 ON tracking_geofence(tenant_id,max_longitude)
 INCLUDE(max_latitude,min_longitude,min_latitude,id)
 WHERE lifecycle='ACTIVE';
