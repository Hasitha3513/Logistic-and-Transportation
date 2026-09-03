package com.transportlogistics.app.integration.adapters.outbound.file;

import com.transportlogistics.app.integration.ports.outbound.IntegrationEndpointPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GovernedJsonFileExchangeAdapterTest {
    @TempDir Path root;

    @Test
    void deliversByAtomicStagingAndReplaysSameHashIdempotently() throws Exception {
        var adapter = new GovernedJsonFileExchangeAdapter(root.toString());
        UUID exchangeId = UUID.randomUUID();
        String payload = "{\"probe_id\":\"" + UUID.randomUUID() + "\"}";
        String hash = hash(payload);

        var first = adapter.deliver("CONTROLLED_SANDBOX", exchangeId, payload, hash);
        var replay = adapter.deliver("CONTROLLED_SANDBOX", exchangeId, payload, hash);

        assertThat(first).isEqualTo(replay);
        assertThat(Files.readString(root.resolve(exchangeId + ".json"))).isEqualTo(payload);
        assertThat(root.resolve(exchangeId + ".part")).doesNotExist();
    }

    @Test
    void existingMismatchedFileIsNeverOverwritten() throws Exception {
        var adapter = new GovernedJsonFileExchangeAdapter(root.toString());
        UUID exchangeId = UUID.randomUUID();
        Files.writeString(root.resolve(exchangeId + ".json"), "existing", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> adapter.deliver("CONTROLLED_SANDBOX", exchangeId, "replacement",
            hash("replacement")))
            .isInstanceOf(IntegrationEndpointPort.EndpointFailure.class)
            .extracting("code").isEqualTo("INTEGRATION_FILE_INTEGRITY_FAILURE");
        assertThat(Files.readString(root.resolve(exchangeId + ".json"))).isEqualTo("existing");
    }

    @Test
    void onlyAllowlistedAliasCanResolveTheServerConfiguredRoot() {
        var adapter = new GovernedJsonFileExchangeAdapter(root.toString());
        assertThatThrownBy(() -> adapter.probe("../operator-input", UUID.randomUUID()))
            .isInstanceOf(IntegrationEndpointPort.EndpointFailure.class)
            .extracting("code").isEqualTo("INTEGRATION_CONFIGURATION_INVALID");
    }

    @Test
    void temporaryFilesystemFailureIsClassifiedForRetry() throws Exception {
        Path rootIsAFile = root.resolve("not-a-directory");
        Files.writeString(rootIsAFile, "occupied");
        var adapter = new GovernedJsonFileExchangeAdapter(rootIsAFile.toString());
        assertThatThrownBy(() -> adapter.deliver("CONTROLLED_SANDBOX", UUID.randomUUID(), "{}", hash("{}")))
            .isInstanceOfSatisfying(IntegrationEndpointPort.EndpointFailure.class,
                failure -> assertThat(failure.retryable()).isTrue())
            .extracting("code").isEqualTo("INTEGRATION_PROVIDER_UNAVAILABLE");
    }

    @Test
    void payloadLargerThanThirtyTwoKibibytesIsRejectedPermanently() throws Exception {
        var adapter = new GovernedJsonFileExchangeAdapter(root.toString());
        String oversized = "x".repeat(32 * 1024 + 1);

        assertThatThrownBy(() -> adapter.deliver("CONTROLLED_SANDBOX", UUID.randomUUID(), oversized,
            hash(oversized)))
            .isInstanceOfSatisfying(IntegrationEndpointPort.EndpointFailure.class,
                failure -> assertThat(failure.retryable()).isFalse())
            .extracting("code").isEqualTo("INTEGRATION_PAYLOAD_INVALID");
    }

    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
