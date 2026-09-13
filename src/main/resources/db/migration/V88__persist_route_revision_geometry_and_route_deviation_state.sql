ALTER TABLE route_revision
    ADD CONSTRAINT uq_route_revision_tenant_identity UNIQUE (tenant_id, id);

CREATE TABLE route_revision_geometry (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    route_revision_id UUID NOT NULL,
    route_id UUID NOT NULL,
    route_version VARCHAR(120) NOT NULL,
    point_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_route_revision_geometry PRIMARY KEY (tenant_id, id),
    CONSTRAINT uq_route_revision_geometry_version UNIQUE (tenant_id, route_id, route_version),
    CONSTRAINT uq_route_revision_geometry_revision UNIQUE (tenant_id, route_revision_id),
    CONSTRAINT fk_route_revision_geometry_revision FOREIGN KEY (tenant_id, route_revision_id)
        REFERENCES route_revision(tenant_id, id),
    CONSTRAINT ck_route_revision_geometry_version CHECK (route_version ~ '^REVISION:[1-9][0-9]*$'),
    CONSTRAINT ck_route_revision_geometry_count CHECK (point_count BETWEEN 2 AND 2000)
);

CREATE TABLE route_revision_geometry_point (
    tenant_id UUID NOT NULL,
    geometry_id UUID NOT NULL,
    point_order INTEGER NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    latitude NUMERIC(10,7) NOT NULL,
    CONSTRAINT pk_route_revision_geometry_point PRIMARY KEY (tenant_id, geometry_id, point_order),
    CONSTRAINT fk_route_revision_geometry_point FOREIGN KEY (tenant_id, geometry_id)
        REFERENCES route_revision_geometry(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_route_revision_geometry_point_order CHECK (point_order BETWEEN 0 AND 1999),
    CONSTRAINT ck_route_revision_geometry_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_route_revision_geometry_latitude CHECK (latitude BETWEEN -90 AND 90)
);

CREATE INDEX idx_route_revision_geometry_lookup
    ON route_revision_geometry(tenant_id, route_id, route_version);

CREATE OR REPLACE FUNCTION reject_route_revision_geometry_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Published route revision geometry is immutable';
END;
$$;

CREATE TRIGGER trg_route_revision_geometry_immutable
BEFORE UPDATE OR DELETE ON route_revision_geometry
FOR EACH ROW EXECUTE FUNCTION reject_route_revision_geometry_mutation();

CREATE TRIGGER trg_route_revision_geometry_point_immutable
BEFORE UPDATE OR DELETE ON route_revision_geometry_point
FOR EACH ROW EXECUTE FUNCTION reject_route_revision_geometry_mutation();

CREATE TABLE tracking_route_deviation_rule (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    route_id UUID NOT NULL,
    route_version VARCHAR(120) NOT NULL,
    configured_tolerance_meters NUMERIC(12,3) NOT NULL,
    lifecycle VARCHAR(16) NOT NULL,
    rule_version BIGINT NOT NULL DEFAULT 0,
    manage_version BIGINT NOT NULL DEFAULT 0,
    effective_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_tracking_route_deviation_rule PRIMARY KEY (tenant_id, id),
    CONSTRAINT uq_tracking_route_deviation_rule_identity UNIQUE (tenant_id, route_id, route_version, id),
    CONSTRAINT ck_tracking_route_deviation_rule_version CHECK (route_version ~ '^REVISION:[1-9][0-9]*$'),
    CONSTRAINT ck_tracking_route_deviation_rule_tolerance CHECK (configured_tolerance_meters BETWEEN 10 AND 5000),
    CONSTRAINT ck_tracking_route_deviation_rule_lifecycle CHECK (lifecycle IN ('DRAFT','ACTIVE','DISABLED','RETIRED')),
    CONSTRAINT ck_tracking_route_deviation_rule_versions CHECK (rule_version >= 0 AND manage_version >= 0),
    CONSTRAINT ck_tracking_route_deviation_rule_active CHECK (lifecycle <> 'ACTIVE' OR (rule_version >= 1 AND effective_at IS NOT NULL))
);

CREATE UNIQUE INDEX uq_tracking_route_deviation_rule_active
    ON tracking_route_deviation_rule(tenant_id, route_id, route_version) WHERE lifecycle = 'ACTIVE';

CREATE TABLE tracking_route_deviation_state (
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    stable_state VARCHAR(16) NOT NULL,
    availability VARCHAR(48) NOT NULL,
    current_trip_id UUID,
    route_id UUID,
    route_version VARCHAR(120),
    rule_id UUID,
    rule_version BIGINT NOT NULL DEFAULT 0,
    configured_tolerance_meters NUMERIC(12,3),
    effective_tolerance_meters NUMERIC(12,3),
    candidate_position_id UUID,
    candidate_source_timestamp TIMESTAMPTZ,
    candidate_trip_id UUID,
    candidate_driver_id UUID,
    candidate_route_id UUID,
    candidate_route_version VARCHAR(120),
    candidate_rule_id UUID,
    candidate_rule_version BIGINT,
    candidate_configured_tolerance_meters NUMERIC(12,3),
    candidate_effective_tolerance_meters NUMERIC(12,3),
    candidate_distance_meters NUMERIC(12,3),
    candidate_longitude NUMERIC(10,7),
    candidate_latitude NUMERIC(10,7),
    candidate_accuracy_meters NUMERIC(10,3),
    active_episode_id UUID,
    last_source_timestamp TIMESTAMPTZ,
    last_position_id UUID,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_tracking_route_deviation_state PRIMARY KEY (tenant_id, vehicle_id),
    CONSTRAINT ck_tracking_route_deviation_state CHECK (stable_state IN ('UNKNOWN','ON_ROUTE','DEVIATING')),
    CONSTRAINT ck_tracking_route_deviation_state_rule_version CHECK (rule_version >= 0),
    CONSTRAINT ck_tracking_route_deviation_state_route_version CHECK (route_version IS NULL OR route_version ~ '^REVISION:[1-9][0-9]*$'),
    CONSTRAINT ck_tracking_route_deviation_candidate_version CHECK (candidate_route_version IS NULL OR candidate_route_version ~ '^REVISION:[1-9][0-9]*$'),
    CONSTRAINT ck_tracking_route_deviation_candidate_longitude CHECK (candidate_longitude IS NULL OR candidate_longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_tracking_route_deviation_candidate_latitude CHECK (candidate_latitude IS NULL OR candidate_latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_tracking_route_deviation_candidate_accuracy CHECK (candidate_accuracy_meters IS NULL OR candidate_accuracy_meters BETWEEN 0 AND 1000),
    CONSTRAINT ck_tracking_route_deviation_candidate_complete CHECK (
      (candidate_position_id IS NULL
        AND candidate_source_timestamp IS NULL AND candidate_route_id IS NULL
        AND candidate_route_version IS NULL AND candidate_rule_id IS NULL
        AND candidate_rule_version IS NULL AND candidate_configured_tolerance_meters IS NULL
        AND candidate_effective_tolerance_meters IS NULL AND candidate_distance_meters IS NULL
        AND candidate_longitude IS NULL AND candidate_latitude IS NULL
        AND candidate_accuracy_meters IS NULL)
      OR
      (candidate_position_id IS NOT NULL
        AND candidate_source_timestamp IS NOT NULL AND candidate_route_id IS NOT NULL
        AND candidate_route_version IS NOT NULL AND candidate_rule_id IS NOT NULL
        AND candidate_rule_version IS NOT NULL AND candidate_configured_tolerance_meters IS NOT NULL
        AND candidate_effective_tolerance_meters IS NOT NULL AND candidate_distance_meters IS NOT NULL
        AND candidate_longitude IS NOT NULL AND candidate_latitude IS NOT NULL
        AND candidate_accuracy_meters IS NOT NULL)
    ),
    CONSTRAINT ck_tracking_route_deviation_state_lock CHECK (lock_version >= 0)
);

CREATE INDEX idx_tracking_route_deviation_state_source
    ON tracking_route_deviation_state(tenant_id, vehicle_id, last_source_timestamp DESC);

CREATE TABLE tracking_route_deviation_episode (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    trip_id UUID,
    driver_id UUID,
    route_id UUID NOT NULL,
    route_version VARCHAR(120) NOT NULL,
    rule_id UUID NOT NULL,
    rule_version BIGINT NOT NULL,
    configured_tolerance_meters NUMERIC(12,3) NOT NULL,
    effective_tolerance_meters NUMERIC(12,3) NOT NULL,
    first_candidate_position_id UUID NOT NULL,
    confirming_position_id UUID NOT NULL,
    start_source_timestamp TIMESTAMPTZ NOT NULL,
    confirmation_source_timestamp TIMESTAMPTZ NOT NULL,
    end_source_timestamp TIMESTAMPTZ,
    maximum_distance_meters NUMERIC(12,3) NOT NULL,
    eligible_outside_sample_count INTEGER NOT NULL,
    severity VARCHAR(16) NOT NULL,
    review_status VARCHAR(16) NOT NULL,
    review_version BIGINT NOT NULL DEFAULT 0,
    terminal_outcome VARCHAR(32),
    disruption_id UUID,
    distance_high_escalated BOOLEAN NOT NULL DEFAULT FALSE,
    review_rejected_escalated BOOLEAN NOT NULL DEFAULT FALSE,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_tracking_route_deviation_episode PRIMARY KEY (tenant_id, id),
    CONSTRAINT ck_tracking_route_deviation_episode_version CHECK (route_version ~ '^REVISION:[1-9][0-9]*$'),
    CONSTRAINT ck_tracking_route_deviation_episode_versions CHECK (rule_version >= 1 AND review_version >= 0 AND lock_version >= 0),
    CONSTRAINT ck_tracking_route_deviation_episode_counts CHECK (eligible_outside_sample_count >= 2),
    CONSTRAINT ck_tracking_route_deviation_episode_distance CHECK (configured_tolerance_meters BETWEEN 10 AND 5000 AND effective_tolerance_meters >= configured_tolerance_meters AND maximum_distance_meters > effective_tolerance_meters),
    CONSTRAINT ck_tracking_route_deviation_episode_severity CHECK (severity IN ('WARNING','HIGH')),
    CONSTRAINT ck_tracking_route_deviation_episode_review CHECK (review_status IN ('NOT_REQUIRED','PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_tracking_route_deviation_episode_outcome CHECK (terminal_outcome IS NULL OR terminal_outcome IN ('RETURNED_TO_ROUTE','SUPERSEDED','TRIP_ENDED_UNRESOLVED')),
    CONSTRAINT ck_tracking_route_deviation_episode_time CHECK (confirmation_source_timestamp >= start_source_timestamp AND (end_source_timestamp IS NULL OR end_source_timestamp >= confirmation_source_timestamp))
);

CREATE UNIQUE INDEX uq_tracking_route_deviation_episode_open
    ON tracking_route_deviation_episode(tenant_id, vehicle_id) WHERE end_source_timestamp IS NULL;
CREATE INDEX idx_tracking_route_deviation_episode_history
    ON tracking_route_deviation_episode(tenant_id, vehicle_id, confirmation_source_timestamp DESC, id DESC);

CREATE TABLE tracking_route_deviation_review (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    episode_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    note VARCHAR(500),
    reviewer_id UUID NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL,
    review_version BIGINT NOT NULL,
    compensates_review_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_tracking_route_deviation_review PRIMARY KEY (tenant_id, id),
    CONSTRAINT fk_tracking_route_deviation_review_episode FOREIGN KEY (tenant_id, episode_id)
        REFERENCES tracking_route_deviation_episode(tenant_id, id),
    CONSTRAINT fk_tracking_route_deviation_review_compensates FOREIGN KEY (tenant_id, compensates_review_id)
        REFERENCES tracking_route_deviation_review(tenant_id, id),
    CONSTRAINT uq_tracking_route_deviation_review_version UNIQUE (tenant_id, episode_id, review_version),
    CONSTRAINT ck_tracking_route_deviation_review_status CHECK (status IN ('APPROVED','REJECTED')),
    CONSTRAINT ck_tracking_route_deviation_review_reason CHECK (reason IN ('AUTHORIZED_DETOUR','ROAD_CLOSURE','TRAFFIC_DIVERSION','OPERATIONAL_NECESSITY','UNKNOWN')),
    CONSTRAINT ck_tracking_route_deviation_review_version CHECK (review_version >= 1),
    CONSTRAINT ck_tracking_route_deviation_review_unknown CHECK (reason <> 'UNKNOWN' OR char_length(note) BETWEEN 10 AND 500)
);

CREATE INDEX idx_tracking_route_deviation_review_history
    ON tracking_route_deviation_review(tenant_id, episode_id, review_version);
