ALTER TABLE bunker_stock_movement ADD COLUMN ledger_sequence BIGINT;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM bunker_stock_movement m LEFT JOIN bunker_tank t ON t.id = m.tank_id
               WHERE t.id IS NULL OR m.tenant_id <> t.tenant_id) THEN
        RAISE EXCEPTION 'V65 bunker ledger canonicalization failed: orphan or cross-tenant tank ownership mismatch';
    END IF;
    IF EXISTS (SELECT 1 FROM bunker_stock_movement
               WHERE quantity_liters IS NULL OR quantity_liters <= 0
                  OR resulting_balance_liters IS NULL OR resulting_balance_liters < 0) THEN
        RAISE EXCEPTION 'V65 bunker ledger canonicalization failed: invalid movement quantity or balance';
    END IF;
    IF EXISTS (SELECT 1 FROM bunker_stock_movement WHERE reference_id IS NOT NULL
               GROUP BY tenant_id, tank_id, movement_type, reference_type, reference_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'V65 bunker ledger canonicalization failed: logical duplicate movement reference';
    END IF;
    IF EXISTS (SELECT 1 FROM bunker_tank t WHERE t.current_stock_liters <> 0
               AND NOT EXISTS (SELECT 1 FROM bunker_stock_movement m WHERE m.tank_id = t.id)) THEN
        RAISE EXCEPTION 'V65 bunker ledger canonicalization failed: non-zero tank has no movement history';
    END IF;
END $$;

WITH canonical_order AS (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY tenant_id, tank_id ORDER BY occurred_at ASC, created_at ASC, id ASC
    ) AS sequence_value
    FROM bunker_stock_movement
)
UPDATE bunker_stock_movement m SET ledger_sequence = c.sequence_value
FROM canonical_order c WHERE m.id = c.id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM bunker_stock_movement WHERE ledger_sequence IS NULL OR ledger_sequence <= 0)
       OR EXISTS (SELECT 1 FROM bunker_stock_movement GROUP BY tenant_id, tank_id
                  HAVING MIN(ledger_sequence) <> 1 OR MAX(ledger_sequence) <> COUNT(*)
                     OR COUNT(DISTINCT ledger_sequence) <> COUNT(*)) THEN
        RAISE EXCEPTION 'V65 bunker ledger canonicalization failed: invalid or non-contiguous sequence';
    END IF;
    IF EXISTS (
        SELECT 1 FROM bunker_tank t
        JOIN LATERAL (
            SELECT m.resulting_balance_liters FROM bunker_stock_movement m
            WHERE m.tenant_id = t.tenant_id AND m.tank_id = t.id
            ORDER BY m.ledger_sequence DESC LIMIT 1
        ) tail ON TRUE
        WHERE tail.resulting_balance_liters <> t.current_stock_liters
    ) THEN
        RAISE EXCEPTION 'V65 bunker ledger canonicalization failed: canonical ledger tail does not match tank stock';
    END IF;
END $$;

ALTER TABLE bunker_stock_movement
    ALTER COLUMN ledger_sequence SET NOT NULL,
    ADD CONSTRAINT uq_bunker_movement_ledger_sequence UNIQUE (tenant_id, tank_id, ledger_sequence);

CREATE INDEX idx_bunker_movement_tenant_tank_sequence
    ON bunker_stock_movement (tenant_id, tank_id, ledger_sequence DESC);
