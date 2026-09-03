package com.transportlogistics.app.integration.adapters.outbound.file;

import com.transportlogistics.app.integration.ports.outbound.IntegrationEndpointPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Component
public class GovernedJsonFileExchangeAdapter implements IntegrationEndpointPort {
    public static final String CONTROLLED_SANDBOX = "CONTROLLED_SANDBOX";
    private static final int MAX_BYTES = 32 * 1024;
    private final Path configuredRoot;

    public GovernedJsonFileExchangeAdapter(
            @Value("${app.integration.file.controlled-sandbox-root:./data/integration-sandbox}") String root) {
        this.configuredRoot = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public void probe(String endpointAlias, UUID probeId) {
        Path root = resolveRoot(endpointAlias);
        String stem = ".probe-" + probeId;
        Path part = safeTarget(root, stem + ".part");
        Path completed = safeTarget(root, stem + ".json");
        try {
            writeNew(part, "{\"probeType\":\"CONTROLLED_SANDBOX\"}".getBytes(StandardCharsets.UTF_8));
            atomicMove(part, completed);
            Files.delete(completed);
        } catch (IOException exception) {
            deleteQuietly(part);
            deleteQuietly(completed);
            throw new EndpointFailure("INTEGRATION_PROVIDER_UNAVAILABLE",
                "Controlled filesystem endpoint is unavailable", true, exception);
        }
    }

    @Override
    public DeliveryResult deliver(String endpointAlias, UUID exchangeId, String canonicalPayload,
                                  String payloadHash) {
        byte[] bytes = canonicalPayload.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) {
            throw new EndpointFailure("INTEGRATION_PAYLOAD_INVALID", "Canonical payload exceeds 32 KiB", false);
        }
        Path root = resolveRoot(endpointAlias);
        String filename = exchangeId + ".json";
        Path target = safeTarget(root, filename);
        Path part = safeTarget(root, exchangeId + ".part");
        try {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(target) || !payloadHash.equals(hash(Files.readAllBytes(target)))) {
                    throw new EndpointFailure("INTEGRATION_FILE_INTEGRITY_FAILURE",
                        "Existing exchange file does not match the accepted payload", false);
                }
                return new DeliveryResult("file:" + exchangeId, filename);
            }
            deleteQuietly(part);
            writeNew(part, bytes);
            atomicMove(part, target);
            setFilePermissions(target);
            return new DeliveryResult("file:" + exchangeId, filename);
        } catch (EndpointFailure failure) {
            throw failure;
        } catch (IOException exception) {
            deleteQuietly(part);
            throw new EndpointFailure("INTEGRATION_PROVIDER_UNAVAILABLE",
                "Controlled filesystem delivery failed", true, exception);
        }
    }

    private Path resolveRoot(String endpointAlias) {
        if (!CONTROLLED_SANDBOX.equals(endpointAlias)) {
            throw new EndpointFailure("INTEGRATION_CONFIGURATION_INVALID", "Endpoint alias is not allow-listed", false);
        }
        try {
            if (Files.exists(configuredRoot, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(configuredRoot)) {
                throw unsafe();
            }
            Files.createDirectories(configuredRoot);
            setDirectoryPermissions(configuredRoot);
            Path real = configuredRoot.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (Files.isSymbolicLink(real) || !Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS)) throw unsafe();
            return real;
        } catch (EndpointFailure failure) {
            throw failure;
        } catch (IOException exception) {
            throw new EndpointFailure("INTEGRATION_PROVIDER_UNAVAILABLE",
                "Controlled filesystem root is unavailable", true, exception);
        }
    }

    private Path safeTarget(Path root, String filename) {
        if (!filename.matches("[.a-zA-Z0-9-]+")) throw unsafe();
        Path target = root.resolve(filename).normalize();
        if (!root.equals(target.getParent()) || !target.startsWith(root)) throw unsafe();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(target)) throw unsafe();
        return target;
    }

    private EndpointFailure unsafe() {
        return new EndpointFailure("INTEGRATION_CONFIGURATION_INVALID", "Unsafe filesystem endpoint", false);
    }

    private void writeNew(Path file, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        setFilePermissions(file);
    }

    private void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException("Atomic move is not supported by the configured endpoint", exception);
        }
    }

    private void setDirectoryPermissions(Path directory) {
        setPermissions(directory, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE));
    }

    private void setFilePermissions(Path file) {
        setPermissions(file, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ));
    }

    private void setPermissions(Path path, Set<PosixFilePermission> permissions) {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
            // POSIX permissions are best-effort on non-POSIX filesystems; path containment remains mandatory.
        }
    }

    private String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The original safe error is returned; the randomized probe/staging path contains no business payload.
        }
    }
}
