package com.transportlogistics.app.freight.manifest.application;

import com.transportlogistics.app.freight.manifest.domain.model.*;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import com.transportlogistics.app.freight.manifest.ports.outbound.*;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderLookup;
import com.transportlogistics.app.shared.domain.*;
import java.time.*; import java.util.*;

public final class CargoManifestService implements CargoManifestUseCase {
    private final CargoManifestRepository manifests;
    private final CargoManifestNumberGenerator numbers;
    private final FreightOrderLookup orders;
    private final CargoManifestTransaction transactions;
    private final Clock clock;

    public CargoManifestService(CargoManifestRepository manifests, CargoManifestNumberGenerator numbers, FreightOrderLookup orders, CargoManifestTransaction transactions, Clock clock) {
        this.manifests = manifests;
        this.numbers = numbers;
        this.orders = orders;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public CargoManifest create(CreateCommand command, String actor) {
        return transactions.execute(() -> {
            requireActor(actor);
            var order = order(command.freightOrderId());
            var now = OffsetDateTime.now(clock);
            return manifests.save(new CargoManifest(UUID.randomUUID(), numbers.next(), order.id(), order.orderNumber(), List.of(), 0, now, now, actor, actor, null, null));
        });
    }

    @Override
    public CargoManifest get(UUID id) {
        return manifests.findById(id).orElseThrow(() -> new NotFoundException("CARGO_MANIFEST_NOT_FOUND", "Cargo manifest not found: " + id));
    }

    @Override
    public PageResult<CargoManifest> search(SearchQuery query) {
        return manifests.search(query);
    }

    @Override
    public CargoManifest update(UUID id, UpdateCommand command, String actor) {
        return transactions.execute(() -> manifests.save(requireVersion(get(id), command.version()).update(actor, OffsetDateTime.now(clock))));
    }

    @Override
    public CargoManifest addItem(UUID id, ItemCommand command, String actor) {
        return transactions.execute(() -> {
            var current = requireVersion(get(id), command.version());
            return manifests.save(current.addItem(item(UUID.randomUUID(), command), actor, OffsetDateTime.now(clock)));
        });
    }

    @Override
    public CargoManifest updateItem(UUID id, UUID itemId, ItemCommand command, String actor) {
        return transactions.execute(() -> {
            var current = requireVersion(get(id), command.version());
            return manifests.save(current.updateItem(itemId, item(itemId, command), actor, OffsetDateTime.now(clock)));
        });
    }

    @Override
    public Readiness validate(UUID id) {
        var manifest = get(id);
        return new Readiness(manifest.validate(order(manifest.freightOrderId()).lines()).isEmpty(), manifest.validate(order(manifest.freightOrderId()).lines()));
    }

    @Override
    public CargoManifest finalizeManifest(UUID id, long version, String actor) {
        return transactions.execute(() -> {
            var current = requireVersion(get(id), version);
            var expected = order(current.freightOrderId()).lines();
            return manifests.save(current.finalizeManifest(expected, actor, OffsetDateTime.now(clock)));
        });
    }

    private CargoManifestItem item(UUID id, ItemCommand c) {
        return new CargoManifestItem(
                id,
                c.freightOrderLineId(),
                c.description(),
                c.quantity(),
                c.packingInformation(),
                c.commodityClassification(),
                c.customsApplicable(),
                c.customsInformation(),
                c.hazardous(),
                c.hazardousClassification(),
                c.hazardousDetails(),
                c.fragile(),
                c.temperatureSensitive(),
                c.unitWeight(),
                c.weightUnit(),
                c.length(),
                c.width(),
                c.height(),
                c.dimensionUnit()
        );
    }

    private FreightOrderLookup.OrderReference order(UUID id) {
        return orders.find(id).orElseThrow(() -> new NotFoundException("FREIGHT_ORDER_NOT_FOUND", "Freight order not found: " + id));
    }

    private CargoManifest requireVersion(CargoManifest m, Long version) {
        if (version == null) throw new BusinessRuleException("CARGO_MANIFEST_VERSION_REQUIRED", "Version is required");
        if (version != m.version()) throw new ConflictException("CARGO_MANIFEST_CONCURRENT_UPDATE", "Cargo manifest was changed by another user");
        return m;
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) throw new BusinessRuleException("CARGO_MANIFEST_ACTOR_REQUIRED", "An authenticated actor is required");
    }
}
