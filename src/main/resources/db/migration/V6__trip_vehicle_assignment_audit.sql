CREATE TABLE trip_status_history (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    action VARCHAR(80) NOT NULL,
    vehicle_id UUID,
    actor VARCHAR(160) NOT NULL,
    details VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_trip_status_history_trip FOREIGN KEY (trip_id) REFERENCES trip(id)
);

CREATE INDEX idx_trip_status_history_trip_time ON trip_status_history(trip_id, occurred_at);
