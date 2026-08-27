ALTER TABLE trip_status_history ADD COLUMN driver_id UUID;

CREATE INDEX idx_trip_status_history_driver ON trip_status_history(driver_id);
