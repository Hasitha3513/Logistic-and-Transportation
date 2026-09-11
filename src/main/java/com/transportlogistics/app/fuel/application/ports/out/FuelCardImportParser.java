package com.transportlogistics.app.fuel.application.ports.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FuelCardImportParser {
    ParsedBatch parse(byte[] bytes);
    record ParsedBatch(String schemaVersion, String providerBatchId, OffsetDateTime generatedAt, String fileHash,
                       List<ParsedTransaction> transactions) {}
    record ParsedTransaction(String providerTransactionId, String providerCardReference, String canonicalHash,
                             String transactionKind, String originalProviderTransactionId,
                             OffsetDateTime transactionTimestamp, OffsetDateTime postedTimestamp,
                             String stationReference, String fuelType, BigDecimal quantityLitres,
                             BigDecimal unitPrice, BigDecimal totalAmount, String currency,
                             String providerVehicleReference, String providerDriverReference, UUID tripId,
                             String providerStatus) {}
}
