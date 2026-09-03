package com.transportlogistics.app.freight.manifest.adapters.outbound.persistence;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime; import java.util.*;
@Entity @Table(name="cargo_manifest") @Getter @Setter @NoArgsConstructor
class CargoManifestEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
 @Id UUID id; @Column(name="manifest_number",nullable=false,unique=true,length=60) String manifestNumber;
 @Column(name="freight_order_id",nullable=false) UUID freightOrderId; @Column(name="freight_order_number",nullable=false,length=60) String freightOrderNumber;
 @Version @Column(nullable=false) long version; @Column(name="created_at",nullable=false) OffsetDateTime createdAt; @Column(name="updated_at",nullable=false) OffsetDateTime updatedAt;
 @Column(name="created_by",nullable=false,length=128) String createdBy; @Column(name="updated_by",nullable=false,length=128) String updatedBy;
 @Column(name="finalized_at") OffsetDateTime finalizedAt; @Column(name="finalized_by",length=128) String finalizedBy;
 @OneToMany(mappedBy="manifest",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER) @OrderBy("itemOrder ASC") List<CargoManifestItemEntity> items=new ArrayList<>();
 void replaceItems(List<CargoManifestItemEntity> values){items.clear();values.forEach(item->{item.setManifest(this);items.add(item);});}
}
