package com.transportlogistics.app.tracking.application.provider;

public record ProviderDeviceCursor(String externalDeviceReference, ProviderWatermark watermark) {
    public ProviderDeviceCursor {
        if (externalDeviceReference == null || externalDeviceReference.isBlank()
                || externalDeviceReference.length() > 160) {
            throw new IllegalArgumentException("External device reference is invalid");
        }
    }
}
