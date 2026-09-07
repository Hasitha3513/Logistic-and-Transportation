-- Draft lines may legitimately be prepared more than once. Released-source duplication
-- is enforced during validation against non-superseded released batches.
DROP INDEX IF EXISTS uq_driver_payroll_regular_source;
DROP INDEX IF EXISTS uq_driver_payroll_correction_source;

CREATE INDEX idx_driver_payroll_regular_source
    ON driver_payroll_input_line(tenant_id, driver_id, trip_id, category)
    WHERE original_line_id IS NULL;
CREATE INDEX idx_driver_payroll_correction_source
    ON driver_payroll_input_line(tenant_id, driver_id, trip_id, category, original_line_id)
    WHERE original_line_id IS NOT NULL;
