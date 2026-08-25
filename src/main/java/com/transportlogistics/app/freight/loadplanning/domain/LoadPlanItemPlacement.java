package com.transportlogistics.app.freight.loadplanning.domain;

import java.util.UUID;

/**
 * Value object representing placement of a manifest item within a load plan.
 *
 * <p>This is a pure domain object — no Spring, JPA, or framework dependencies.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code placementId} — unique placement identifier</li>
 *   <li>{@code manifestItemId} — reference to the cargo manifest item being placed</li>
 *   <li>{@code placementOrder} — physical position order within the load plan</li>
 *   <li>{@code zoneReference} — logical zone/area within the vehicle (e.g., "FRONT", "REAR", "SIDE-L")</li>
 *   <li>{@code stackGroup} — group identifier for stacked items; items with same group are stacked</li>
 *   <li>{@code containerReference} — pallet/container reference (e.g., "PALLET-A1", "CONTAINER-12")</li>
 *   <li>{@code loadingSequence} — order in which this item should be loaded (1 = first loaded)</li>
 *   <li>{@code specialHandlingNotes} — planner-declared handling notes (fragile, temperature-sensitive, etc.)</li>
 * </ul>
 */
public record LoadPlanItemPlacement(
        UUID placementId,
        UUID manifestItemId,
        int placementOrder,
        String zoneReference,
        String stackGroup,
        String containerReference,
        int loadingSequence,
        String specialHandlingNotes
) {

    public LoadPlanItemPlacement {
        if (manifestItemId == null) {
            throw new IllegalArgumentException("Manifest item ID is required for placement");
        }
        if (placementOrder < 0) {
            throw new IllegalArgumentException("Placement order must be non-negative");
        }
        if (loadingSequence < 0) {
            throw new IllegalArgumentException("Loading sequence must be non-negative");
        }
    }
}
