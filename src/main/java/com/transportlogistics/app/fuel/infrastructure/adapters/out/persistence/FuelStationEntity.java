package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import jakarta.persistence.*;
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
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "station_type", nullable = false)
    private FuelStationType stationType;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "vendor_id")
    private UUID vendorId;
    @Column(name = "location_id")
    private UUID locationId;
}
