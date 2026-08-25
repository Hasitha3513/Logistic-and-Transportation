-- V34__load_plan_tables.sql
-- Tables for Load Plan feature (US-26)

CREATE SEQUENCE load_plan_number_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE load_plan (
    id                  UUID            PRIMARY KEY,
    load_plan_number    VARCHAR(60)     NOT NULL UNIQUE,
    cargo_manifest_id   UUID            NOT NULL,
    vehicle_id          UUID            NOT NULL,
    notes               VARCHAR(2000),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(128)    NOT NULL,
    updated_by          VARCHAR(128)    NOT NULL,
    CONSTRAINT fk_load_plan_cargo_manifest FOREIGN KEY (cargo_manifest_id) REFERENCES cargo_manifest(id),
    CONSTRAINT fk_load_plan_vehicle        FOREIGN KEY (vehicle_id)        REFERENCES vehicle(id)
);

CREATE TABLE load_plan_item_placement (
    id                      UUID            PRIMARY KEY,
    load_plan_id            UUID            NOT NULL,
    manifest_item_id        UUID            NOT NULL,
    placement_order         INTEGER         NOT NULL,
    zone_reference          VARCHAR(120),
    stack_group             VARCHAR(120),
    container_reference     VARCHAR(200),
    loading_sequence        INTEGER         NOT NULL,
    special_handling_notes   VARCHAR(500),
    CONSTRAINT fk_item_placement_load_plan   FOREIGN KEY (load_plan_id)     REFERENCES load_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_placement_manifest    FOREIGN KEY (manifest_item_id) REFERENCES cargo_manifest_item(id),
    CONSTRAINT uq_placement_item_per_plan    UNIQUE (load_plan_id, manifest_item_id),
    CONSTRAINT uq_placement_order_per_plan   UNIQUE (load_plan_id, placement_order),
    CONSTRAINT chk_placement_order           CHECK (placement_order >= 0),
    CONSTRAINT chk_loading_sequence          CHECK (loading_sequence >= 0)
);

CREATE INDEX idx_load_plan_manifest  ON load_plan(cargo_manifest_id);
CREATE INDEX idx_load_plan_vehicle   ON load_plan(vehicle_id);
CREATE INDEX idx_item_placement_plan ON load_plan_item_placement(load_plan_id);
