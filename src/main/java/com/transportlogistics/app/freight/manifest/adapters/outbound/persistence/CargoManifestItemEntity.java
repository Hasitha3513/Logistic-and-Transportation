package com.transportlogistics.app.freight.manifest.adapters.outbound.persistence;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.util.UUID;
@Entity @Table(name="cargo_manifest_item") @Getter @Setter @NoArgsConstructor
class CargoManifestItemEntity {
 @Id UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cargo_manifest_id",nullable=false) CargoManifestEntity manifest;
 @Column(name="freight_order_line_id",nullable=false) UUID freightOrderLineId; @Column(nullable=false,length=500) String description;
 @Column(nullable=false,precision=19,scale=4) BigDecimal quantity; @Column(name="packing_information",nullable=false,length=500) String packingInformation;
 @Column(name="commodity_classification",nullable=false,length=120) String commodityClassification; @Column(name="customs_applicable",nullable=false) boolean customsApplicable;
 @Column(name="customs_information",length=1000) String customsInformation; @Column(nullable=false) boolean hazardous;
 @Column(name="hazardous_classification",length=120) String hazardousClassification; @Column(name="hazardous_details",length=1000) String hazardousDetails;
 @Column(name="fragile") Boolean fragile; @Column(name="temperature_sensitive") Boolean temperatureSensitive;
 @Column(name="item_order",nullable=false) int itemOrder;
}
