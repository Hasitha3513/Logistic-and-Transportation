package com.transportlogistics.app.tracking.domain.geofence;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class GeofenceTransitionIdentity {
    private GeofenceTransitionIdentity() {
    }

    public static UUID create(UUID tenantId, UUID geofenceId, long definitionVersion,
                              UUID vehicleId, GeofenceMembership fromState,
                              GeofenceMembership toState, UUID confirmingPositionId) {
        MessageDigest digest = sha256();
        putUuid(digest, tenantId);
        putUuid(digest, geofenceId);
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(definitionVersion).array());
        putUuid(digest, vehicleId);
        digest.update((byte) fromState.ordinal());
        digest.update((byte) toState.ordinal());
        putUuid(digest, confirmingPositionId);
        byte[] value = digest.digest();
        ByteBuffer bytes = ByteBuffer.wrap(value);
        long mostSignificant = bytes.getLong();
        long leastSignificant = bytes.getLong();
        mostSignificant = mostSignificant & 0xffffffffffff0fffL | 0x0000000000005000L;
        leastSignificant = leastSignificant & 0x3fffffffffffffffL | 0x8000000000000000L;
        return new UUID(mostSignificant, leastSignificant);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static void putUuid(MessageDigest digest, UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Transition identity facts are required");
        }
        digest.update(ByteBuffer.allocate(16)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits())
                .array());
    }
}
