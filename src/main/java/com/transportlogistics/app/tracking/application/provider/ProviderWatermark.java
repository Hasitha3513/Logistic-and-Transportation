package com.transportlogistics.app.tracking.application.provider;

import java.time.Instant;

public record ProviderWatermark(Instant sourceTimestamp, String messageIdentity) {
    public ProviderWatermark {
        if (messageIdentity != null && (messageIdentity.isBlank() || messageIdentity.length() > 160)) {
            throw new IllegalArgumentException("Watermark message identity is invalid");
        }
    }
}
