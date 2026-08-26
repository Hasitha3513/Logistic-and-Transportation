CREATE TABLE driver_license (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    license_number VARCHAR(160) NOT NULL UNIQUE,
    license_class VARCHAR(40) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(160) NOT NULL,
    updated_by VARCHAR(160) NOT NULL,
    CONSTRAINT fk_driver_license_driver FOREIGN KEY (driver_id) REFERENCES driver(id),
    CONSTRAINT chk_driver_license_dates CHECK (expiry_date > issue_date),
    CONSTRAINT chk_driver_license_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT chk_driver_license_active_status CHECK (
        (active = TRUE AND status = 'ACTIVE') OR (active = FALSE AND status IN ('INACTIVE', 'DELETED'))
    )
);

CREATE INDEX idx_driver_license_driver ON driver_license(driver_id);
CREATE INDEX idx_driver_license_availability ON driver_license(driver_id, active, license_class, expiry_date);
