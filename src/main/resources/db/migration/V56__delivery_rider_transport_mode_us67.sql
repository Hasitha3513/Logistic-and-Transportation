-- US-67 additive Rider transport-mode authority. Existing rows remain nullable
-- until explicitly configured; new application onboarding requires a value.
ALTER TABLE delivery_rider
    ADD COLUMN transport_mode VARCHAR(20);

ALTER TABLE delivery_rider
    ADD CONSTRAINT chk_delivery_rider_transport_mode
    CHECK (transport_mode IS NULL OR transport_mode IN ('BICYCLE', 'MOTORBIKE', 'VAN', 'CAR', 'WALKER'));

CREATE INDEX idx_delivery_rider_transport_mode
    ON delivery_rider (tenant_id, transport_mode)
    WHERE transport_mode IS NOT NULL;
