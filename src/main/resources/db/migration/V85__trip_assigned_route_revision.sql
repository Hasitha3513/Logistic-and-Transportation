ALTER TABLE trip
    ADD COLUMN route_version VARCHAR(120);

ALTER TABLE trip
    ADD CONSTRAINT chk_trip_route_version_assignment
    CHECK (
        (route_id IS NULL AND route_version IS NULL)
        OR (route_id IS NOT NULL AND (
            route_version IS NULL
            OR route_version ~ '^REVISION:[1-9][0-9]*$'
        ))
    );
