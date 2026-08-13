CREATE TABLE vehicle_document (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    document_type VARCHAR(80) NOT NULL,
    document_number VARCHAR(160) NOT NULL,
    issue_date DATE,
    expiry_date DATE,
    file_reference VARCHAR(1000),
    mandatory_for_dispatch BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(160) NOT NULL,
    updated_by VARCHAR(160) NOT NULL,
    CONSTRAINT fk_vehicle_document_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT chk_vehicle_document_dates CHECK (issue_date IS NULL OR expiry_date IS NULL OR expiry_date >= issue_date),
    CONSTRAINT chk_vehicle_document_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT chk_vehicle_document_active_status CHECK (
        (active = TRUE AND status = 'ACTIVE') OR (active = FALSE AND status IN ('INACTIVE', 'DELETED'))
    )
);

CREATE INDEX idx_vehicle_document_vehicle ON vehicle_document(vehicle_id);
CREATE INDEX idx_vehicle_document_dispatch ON vehicle_document(vehicle_id, active, mandatory_for_dispatch, expiry_date);
