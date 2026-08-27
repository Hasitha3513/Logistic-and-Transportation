CREATE TABLE route_stop (
    route_id UUID NOT NULL,
    stop_order INT NOT NULL,
    location_id UUID NOT NULL,
    PRIMARY KEY (route_id, stop_order),
    CONSTRAINT fk_route_stop_route FOREIGN KEY (route_id) REFERENCES route(id) ON DELETE CASCADE,
    CONSTRAINT uq_route_stop_location UNIQUE (route_id, location_id)
);

CREATE INDEX idx_route_search ON route(active, origin_location_id, destination_location_id);
