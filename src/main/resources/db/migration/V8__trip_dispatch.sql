ALTER TABLE trip_status_history ADD COLUMN license_class VARCHAR(40);

CREATE TABLE trip_dispatch (
    trip_id UUID PRIMARY KEY,
    dispatched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    dispatched_by VARCHAR(160) NOT NULL,
    remarks VARCHAR(1000),
    CONSTRAINT fk_trip_dispatch_trip FOREIGN KEY (trip_id) REFERENCES trip(id)
);
