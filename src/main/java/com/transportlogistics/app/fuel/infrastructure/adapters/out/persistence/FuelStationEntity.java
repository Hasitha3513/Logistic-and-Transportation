package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "fuel_station")
@Getter
@Setter
@NoArgsConstructor
class FuelStationEntity {
    @Id UUID id;
    @Column(nullable = false, unique = true) String code;
    @Column(nullable = false) String name;
    @Enumerated(EnumType.STRING) @Column(name = "station_type", nullable = false) FuelStationType stationType;
    @Column(nullable = false) boolean active;
    @Column(name = "vendor_id") UUID vendorId;
    @Column(name = "location_id") UUID locationId;
}
