CREATE INDEX idx_tracking_speed_episode_dashboard_recent
    ON tracking_speed_episode (
        tenant_id,
        confirmation_source_timestamp DESC,
        id DESC
    );
