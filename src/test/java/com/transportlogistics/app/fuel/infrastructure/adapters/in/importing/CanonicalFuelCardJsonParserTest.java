package com.transportlogistics.app.fuel.infrastructure.adapters.in.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanonicalFuelCardJsonParserTest {
    private final CanonicalFuelCardJsonParser parser = new CanonicalFuelCardJsonParser(new ObjectMapper());

    @Test void parsesFrozenCanonicalContract() {
        var parsed = parser.parse(valid().getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.schemaVersion()).isEqualTo("FUEL_CARD_TRANSACTIONS_V1");
        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.providerCardReference()).isEqualTo("opaque-1");
            assertThat(transaction.totalAmount()).isEqualByComparingTo("100.00");
        });
    }

    @Test void rejectsUnknownFieldsBomAndTrailingContent() {
        assertThatThrownBy(() -> parser.parse(valid().replace("\"transactions\"", "\"unknown\":1,\"transactions\"")
                .getBytes(StandardCharsets.UTF_8))).hasMessage("FUEL_CARD_IMPORT_INVALID");
        byte[] bom = ("\uFEFF" + valid()).getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> parser.parse(bom)).hasMessage("FUEL_CARD_IMPORT_INVALID");
        assertThatThrownBy(() -> parser.parse((valid() + " {}").getBytes(StandardCharsets.UTF_8)))
                .hasMessage("FUEL_CARD_IMPORT_INVALID");
    }

    private String valid() {
        return """
                {"schemaVersion":"FUEL_CARD_TRANSACTIONS_V1","providerBatchId":"batch-1",
                 "generatedAt":"2026-09-04T10:00:00Z","transactions":[{
                 "providerTransactionId":"tx-1","providerCardReference":"opaque-1",
                 "transactionKind":"PURCHASE","transactionTimestamp":"2026-09-04T09:00:00Z",
                 "fuelType":"DIESEL","quantityLitres":10,"unitPrice":10,"totalAmount":100,
                 "currency":"LKR","providerStatus":"POSTED"}]}
                """;
    }
}
