-- V30__route_history_and_disruptions.sql
-- Route revision history snapshotting and route disruption management

CREATE TABLE route_revision (
    id UUID PRIMARY KEY,
    route_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    origin_location_id UUID NOT NULL,
    destination_location_id UUID NOT NULL,
    planned_distance_km DOUBLE PRECISION NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    changed_by VARCHAR(128) NOT NULL,
    CONSTRAINT fk_route_revision_route FOREIGN KEY (route_id) REFERENCES route (id) ON DELETE CASCADE,
    CONSTRAINT uq_route_revision_number UNIQUE (route_id, revision_number),
    CONSTRAINT chk_route_revision_positive_num CHECK (revision_number > 0)
);

CREATE TABLE route_revision_stop (
    route_revision_id UUID NOT NULL,
    location_id UUID NOT NULL,
    stop_order INTEGER NOT NULL,
    CONSTRAINT pk_route_revision_stop PRIMARY KEY (route_revision_id, stop_order),
    CONSTRAINT fk_route_revision_stop_revision FOREIGN KEY (route_revision_id) REFERENCES route_revision (id) ON DELETE CASCADE
);

CREATE INDEX idx_route_revision_route_id ON route_revision (route_id);

CREATE TABLE route_disruption (
    id UUID PRIMARY KEY,
    route_id UUID NOT NULL,
    disruption_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_until TIMESTAMP WITH TIME ZONE,
    detour_route_id UUID,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(128),
    CONSTRAINT fk_route_disruption_route FOREIGN KEY (route_id) REFERENCES route (id),
    CONSTRAINT fk_route_disruption_detour FOREIGN KEY (detour_route_id) REFERENCES route (id),
    CONSTRAINT chk_route_disruption_window CHECK (effective_until IS NULL OR effective_until > effective_from),
    CONSTRAINT chk_route_disruption_status CHECK (status IN ('ACTIVE', 'RESOLVED')),
    CONSTRAINT chk_route_disruption_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_route_disruption_type CHECK (disruption_type IN ('ROAD_CLOSURE', 'ACCIDENT', 'WEATHER', 'RESTRICTION'))
);

CREATE INDEX idx_route_disruption_route_status ON route_disruption (route_id, status);
CREATE INDEX idx_route_disruption_status ON route_disruption (status);

-- Seed ROUTE_DISRUPTION_MANAGE permission
INSERT INTO app_permission (code, description, active) VALUES
    ('ROUTE_DISRUPTION_MANAGE', 'Create and resolve route disruptions and detour configurations', TRUE);
