-- V22__driver_medical_and_drug_tests.sql
-- Tables for driver medical fitness assessments and substance screening tests

CREATE TABLE driver_medical_record (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    assessment_date DATE NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    fitness_status VARCHAR(32) NOT NULL,
    vision_test_status VARCHAR(32),
    restrictions TEXT,
    examiner_or_provider VARCHAR(255),
    certificate_reference VARCHAR(128),
    remarks TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128),
    updated_by VARCHAR(128),
    CONSTRAINT fk_driver_medical_driver FOREIGN KEY (driver_id) REFERENCES driver (id) ON DELETE CASCADE,
    CONSTRAINT chk_medical_valid_dates CHECK (valid_until >= valid_from)
);

CREATE INDEX idx_driver_medical_driver ON driver_medical_record (driver_id, valid_until DESC);
CREATE INDEX idx_driver_medical_status ON driver_medical_record (driver_id, fitness_status);

CREATE TABLE driver_drug_test (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    test_type VARCHAR(32) NOT NULL,
    scheduled_date DATE NOT NULL,
    sample_collected_at TIMESTAMP WITH TIME ZONE,
    result_date DATE,
    result VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    laboratory_or_provider VARCHAR(255),
    reference_number VARCHAR(128),
    remarks TEXT,
    return_to_duty_required BOOLEAN NOT NULL DEFAULT FALSE,
    return_to_duty_cleared_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(128),
    updated_by VARCHAR(128),
    CONSTRAINT fk_driver_drug_test_driver FOREIGN KEY (driver_id) REFERENCES driver (id) ON DELETE CASCADE
);

CREATE INDEX idx_driver_drug_test_driver ON driver_drug_test (driver_id, scheduled_date DESC);
CREATE INDEX idx_driver_drug_test_result ON driver_drug_test (driver_id, result);

-- Seed permissions
INSERT INTO app_permission (code, description, active) VALUES
    ('DRIVER_MEDICAL_VIEW', 'View driver medical fitness records and certificates', TRUE),
    ('DRIVER_MEDICAL_MANAGE', 'Record, update, and manage driver medical fitness certificates', TRUE),
    ('DRIVER_DRUG_TEST_VIEW', 'View driver substance screening and drug test records', TRUE),
    ('DRIVER_DRUG_TEST_MANAGE', 'Schedule drug tests, record lab results, and perform return-to-duty clearance', TRUE);
