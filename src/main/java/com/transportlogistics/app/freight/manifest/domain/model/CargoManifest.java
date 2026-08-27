package com.transportlogistics.app.freight.manifest.domain.model;

import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

public record CargoManifest(UUID id, String manifestNumber, UUID freightOrderId, String freightOrderNumber,
                            List<CargoManifestItem> items, long version,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt, String createdBy, String updatedBy,
                            OffsetDateTime finalizedAt, String finalizedBy) {
    public CargoManifest {
        if (id == null) throw invalid("Manifest id is required");
        manifestNumber = required(manifestNumber, "Manifest number", 60);
        if (freightOrderId == null) throw invalid("Freight order is required");
        freightOrderNumber = required(freightOrderNumber, "Freight order number", 60);
        items = items == null ? List.of() : List.copyOf(items);
        if (version < 0) throw invalid("Version must not be negative");
        if (createdAt == null || updatedAt == null) throw invalid("Audit timestamps are required");
        createdBy = required(createdBy, "Created by", 128);
        updatedBy = required(updatedBy, "Updated by", 128);
        finalizedBy = optional(finalizedBy, 128, "Finalized by");
        if ((finalizedAt == null) != (finalizedBy == null)) throw invalid("Finalization timestamp and actor must be recorded together");
        if (items.stream().map(CargoManifestItem::id).distinct().count() != items.size()) throw invalid("Manifest item ids must be unique");
    }

    public boolean finalized() { return finalizedAt != null; }

    public List<ManifestValidationFailure> validate(List<FreightOrderLookup.ExpectedLine> expectedLines) {
        var failures = new ArrayList<ManifestValidationFailure>();
        if (items.isEmpty()) failures.add(new ManifestValidationFailure("MANIFEST_ITEM_REQUIRED", "items", "At least one cargo item is required"));
        items.forEach(item -> failures.addAll(item.validationFailures()));
        Set<UUID> expectedIds = expectedLines.stream().map(FreightOrderLookup.ExpectedLine::id).collect(Collectors.toSet());
        for (CargoManifestItem item : items) if (!expectedIds.contains(item.freightOrderLineId()))
            failures.add(new ManifestValidationFailure("UNKNOWN_FREIGHT_ORDER_LINE", "items." + item.id() + ".freightOrderLineId", "Cargo item does not reference a line from this Freight Order"));
        Map<UUID, BigDecimal> manifested = items.stream().collect(Collectors.groupingBy(CargoManifestItem::freightOrderLineId,
                Collectors.reducing(BigDecimal.ZERO, CargoManifestItem::quantity, BigDecimal::add)));
        for (FreightOrderLookup.ExpectedLine line : expectedLines) {
            BigDecimal quantity = manifested.getOrDefault(line.id(), BigDecimal.ZERO);
            if (quantity.compareTo(line.quantity()) < 0)
                failures.add(new ManifestValidationFailure("UNMANIFESTED_CARGO", "items", "Freight Order line '" + line.description() + "' has unmanifested quantity " + line.quantity().subtract(quantity).stripTrailingZeros().toPlainString()));
            else if (quantity.compareTo(line.quantity()) > 0)
                failures.add(new ManifestValidationFailure("MANIFEST_QUANTITY_EXCEEDS_ORDER", "items", "Manifested quantity exceeds Freight Order line '" + line.description() + "'"));
        }
        return List.copyOf(failures);
    }

    public CargoManifest update(String actor, OffsetDateTime now) { requireMutable(); return new CargoManifest(id, manifestNumber, freightOrderId, freightOrderNumber, items, version, createdAt, now, createdBy, required(actor,"Updated by",128), null, null); }
    public CargoManifest addItem(CargoManifestItem item, String actor, OffsetDateTime now) { requireMutable(); var changed = new ArrayList<>(items); changed.add(item); return new CargoManifest(id,manifestNumber,freightOrderId,freightOrderNumber,changed,version,createdAt,now,createdBy,actor,null,null); }
    public CargoManifest updateItem(UUID itemId, CargoManifestItem replacement, String actor, OffsetDateTime now) { requireMutable(); if (items.stream().noneMatch(i -> i.id().equals(itemId))) throw new com.transportlogistics.app.shared.domain.NotFoundException("CARGO_MANIFEST_ITEM_NOT_FOUND", "Cargo manifest item not found: " + itemId); var changed=items.stream().map(i -> i.id().equals(itemId) ? replacement : i).toList(); return new CargoManifest(id,manifestNumber,freightOrderId,freightOrderNumber,changed,version,createdAt,now,createdBy,actor,null,null); }
    public CargoManifest finalizeManifest(List<FreightOrderLookup.ExpectedLine> expected, String actor, OffsetDateTime now) { requireMutable(); var failures=validate(expected); if(!failures.isEmpty()){var code=failures.stream().anyMatch(failure->"SPECIAL_CARGO_CLASSIFICATION_MISSING".equals(failure.code()))?"SPECIAL_CARGO_CLASSIFICATION_MISSING":"CARGO_MANIFEST_INCOMPLETE";throw new BusinessRuleException(code, failures.stream().map(ManifestValidationFailure::message).collect(Collectors.joining("; ")));} return new CargoManifest(id,manifestNumber,freightOrderId,freightOrderNumber,items,version,createdAt,now,createdBy,actor,now,actor); }
    private void requireMutable() { if(finalized()) throw new ConflictException("CARGO_MANIFEST_FINALIZED", "Finalized cargo manifest cannot be edited"); }
    private static String required(String value,String field,int max){String result=optional(value,max,field);if(result==null)throw invalid(field+" is required");return result;}
    private static String optional(String value,int max,String field){String result=value==null||value.isBlank()?null:value.trim();if(result!=null&&result.length()>max)throw invalid(field+" must not exceed "+max+" characters");return result;}
    private static BusinessRuleException invalid(String message){return new BusinessRuleException("INVALID_CARGO_MANIFEST",message);}
}
