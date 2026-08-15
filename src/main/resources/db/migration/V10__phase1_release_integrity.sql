ALTER TABLE project
    ADD CONSTRAINT fk_project_department FOREIGN KEY (department_id) REFERENCES department(id);

ALTER TABLE vehicle_type
    ADD CONSTRAINT fk_vehicle_type_category FOREIGN KEY (category_id) REFERENCES vehicle_category(id);

ALTER TABLE vehicle
    ADD CONSTRAINT fk_vehicle_category FOREIGN KEY (category_id) REFERENCES vehicle_category(id);
ALTER TABLE vehicle
    ADD CONSTRAINT fk_vehicle_type FOREIGN KEY (type_id) REFERENCES vehicle_type(id);

ALTER TABLE route
    ADD CONSTRAINT fk_route_origin FOREIGN KEY (origin_location_id) REFERENCES location(id);
ALTER TABLE route
    ADD CONSTRAINT fk_route_destination FOREIGN KEY (destination_location_id) REFERENCES location(id);

ALTER TABLE route_stop
    ADD CONSTRAINT fk_route_stop_location FOREIGN KEY (location_id) REFERENCES location(id);

ALTER TABLE trip
    ADD CONSTRAINT fk_trip_customer FOREIGN KEY (customer_id) REFERENCES customer(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_department FOREIGN KEY (department_id) REFERENCES department(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_project FOREIGN KEY (project_id) REFERENCES project(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_route FOREIGN KEY (route_id) REFERENCES route(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_origin FOREIGN KEY (origin_location_id) REFERENCES location(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_destination FOREIGN KEY (destination_location_id) REFERENCES location(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_required_vehicle_type FOREIGN KEY (required_vehicle_type_id) REFERENCES vehicle_type(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id);
ALTER TABLE trip ADD CONSTRAINT fk_trip_driver FOREIGN KEY (driver_id) REFERENCES driver(id);

ALTER TABLE trip_status_history
    ADD CONSTRAINT fk_trip_history_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id);
ALTER TABLE trip_status_history
    ADD CONSTRAINT fk_trip_history_driver FOREIGN KEY (driver_id) REFERENCES driver(id);

CREATE INDEX idx_trip_vehicle_allocation
    ON trip(vehicle_id, requested_start_time, requested_end_time, status);

CREATE INDEX idx_trip_driver_assignment
    ON trip(driver_id, requested_start_time, requested_end_time, status);

CREATE INDEX idx_trip_route ON trip(route_id);
CREATE INDEX idx_vehicle_category_type ON vehicle(category_id, type_id);
