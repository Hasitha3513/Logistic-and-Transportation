package com.transportlogistics.app.freight.loadplanning.domain;

import java.util.UUID;

/**
 * Pure domain value object capturing planning-grade facts of a manifest item.
 */
public record ManifestItemFact(
        UUID itemId,
        boolean hazardous,
        Boolean fragile,
        Boolean temperatureSensitive
) {
    public ManifestItemFact(UUID itemId, boolean hazardous) {
        this(itemId, hazardous, null, null);
    }
}
