package com.transportlogistics.app.tracking.domain.speed;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class SpeedingEpisodeIdentity {
    private SpeedingEpisodeIdentity() {
    }

    public static UUID create(UUID tenantId, UUID vehicleId, UUID ruleId, long ruleVersion,
                              UUID firstCandidatePositionId) {
        String canonical = tenantId + "\n" + vehicleId + "\n" + ruleId + "\n"
                + ruleVersion + "\n" + firstCandidatePositionId;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bytes = ByteBuffer.wrap(hash);
            long most = (bytes.getLong() & 0xffffffffffff0fffL) | 0x0000000000005000L;
            long least = (bytes.getLong() & 0x3fffffffffffffffL) | 0x8000000000000000L;
            return new UUID(most, least);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
