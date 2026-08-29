package com.transportlogistics.app.delivery.adapters.outbound.storage;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryEvidenceStoragePort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
class LocalDeliveryEvidenceStorageAdapter implements DeliveryEvidenceStoragePort {
    private final Path root;
    LocalDeliveryEvidenceStorageAdapter(@Value("${delivery.evidence.storage-path:./data/delivery-evidence}") String path) {
        this.root = Path.of(path).toAbsolutePath().normalize();
    }
    @Override public StoredEvidence store(UUID tenantId, UUID evidenceId, byte[] content, String originalFilename) {
        String contentType = detectedType(content);
        String reference = tenantId + "/" + evidenceId;
        Path target = resolve(tenantId, reference);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new StoredEvidence(reference, contentType, content.length, sha256(content));
        } catch (IOException error) { throw unavailable(error); }
    }
    @Override public StoredContent read(UUID tenantId, String storageReference) {
        Path target = resolve(tenantId, storageReference);
        try { byte[] bytes = Files.readAllBytes(target); return new StoredContent(bytes, detectedType(bytes), bytes.length); }
        catch (IOException error) { throw unavailable(error); }
    }
    @Override public void delete(UUID tenantId, String storageReference) {
        try { Files.deleteIfExists(resolve(tenantId, storageReference)); }
        catch (IOException error) { throw unavailable(error); }
    }
    private Path resolve(UUID tenantId, String reference) {
        if (reference == null || !reference.startsWith(tenantId + "/")) throw new BusinessRuleException("POD_EVIDENCE_INVALID", "Evidence reference is unavailable");
        Path target = root.resolve(reference).normalize();
        if (!target.startsWith(root.resolve(tenantId.toString()).normalize())) throw new BusinessRuleException("POD_EVIDENCE_INVALID", "Evidence reference is unavailable");
        return target;
    }
    private String detectedType(byte[] bytes) {
        if (bytes == null || bytes.length < 8) throw new BusinessRuleException("POD_MEDIA_TYPE_UNSUPPORTED", "Evidence is not a supported image");
        String type = bytes[0] == (byte)0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47 ? "image/png" :
                bytes[0] == (byte)0xff && bytes[1] == (byte)0xd8 && bytes[2] == (byte)0xff ? "image/jpeg" : null;
        try { if (type == null || ImageIO.read(new ByteArrayInputStream(bytes)) == null) throw new BusinessRuleException("POD_MEDIA_TYPE_UNSUPPORTED", "Evidence is not a decoded PNG or JPEG image"); }
        catch (IOException error) { throw new BusinessRuleException("POD_MEDIA_TYPE_UNSUPPORTED", "Evidence image is malformed"); }
        return type;
    }
    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private DependencyUnavailableException unavailable(IOException cause) { return new DependencyUnavailableException("POD_STORAGE_UNAVAILABLE", "POD evidence storage is unavailable", cause); }
}
