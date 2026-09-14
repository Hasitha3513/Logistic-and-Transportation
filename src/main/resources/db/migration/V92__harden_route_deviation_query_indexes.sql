CREATE INDEX idx_tracking_route_deviation_episode_keyset
    ON tracking_route_deviation_episode (
        tenant_id,
        vehicle_id,
        start_source_timestamp DESC,
        id DESC
    );

CREATE INDEX idx_trip_tenant_vehicle_source_assignment
    ON trip (
        tenant_id,
        vehicle_id,
        actual_start_time DESC,
        id DESC
    )
    INCLUDE (
        actual_end_time,
        status,
        driver_id,
        route_id,
        route_version
    )
    WHERE actual_start_time IS NOT NULL
      AND status NOT IN ('CANCELLED', 'REJECTED');
