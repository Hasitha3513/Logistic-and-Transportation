DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM vehicle_reading
        WHERE source_reference_id IS NOT NULL
          AND correction_of_reading_id IS NULL
        GROUP BY tenant_id, vehicle_id, reading_type, source_type, source_reference_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'V84 cannot enforce vehicle reading source identity: duplicate original source identities exist';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_vehicle_reading_source_identity
    ON vehicle_reading (tenant_id, vehicle_id, reading_type, source_type, source_reference_id)
    WHERE source_reference_id IS NOT NULL
      AND correction_of_reading_id IS NULL;
