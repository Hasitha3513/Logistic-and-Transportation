
export class TestDataFactory {
  private static randomSuffix(): string {
    return Math.random().toString(36).substring(2, 7).toUpperCase();
  }

  static createVehiclePayload(overrides: Record<string, unknown> = {}) {
    const suffix = this.randomSuffix();
    return {
      registrationNumber: `WP-CAB-${suffix}`,
      chassisNumber: `CHS-${suffix}`,
      engineNumber: `ENG-${suffix}`,
      categoryId: '30000000-0000-0000-0000-000000000001',
      typeId: '31000000-0000-0000-0000-000000000001',
      manufacturer: 'Isuzu',
      model: 'NPR',
      manufactureYear: 2024,
      ownershipType: 'COMPANY_OWNED',
      operationalStatus: 'AVAILABLE',
      currentOdometerKm: 15000,
      engineHours: 650,
      capacityKg: 5000,
      active: true,
      ...overrides,
    };
  }

  static createDriverPayload(overrides: Record<string, unknown> = {}) {
    const suffix = this.randomSuffix();
    return {
      employeeNumber: `DRV-${suffix}`,
      firstName: 'Samantha',
      lastName: `Perera-${suffix}`,
      phone: '+94 77 555 9999',
      email: `driver.${suffix.toLowerCase()}@example.test`,
      status: 'AVAILABLE',
      active: true,
      ...overrides,
    };
  }

  static createRoutePayload(overrides: Record<string, unknown> = {}) {
    const suffix = this.randomSuffix();
    return {
      code: `RTE-${suffix}`,
      name: `Route Hub to Depot ${suffix}`,
      originLocationId: '20000000-0000-0000-0000-000000000001',
      destinationLocationId: '20000000-0000-0000-0000-000000000002',
      plannedDistanceKm: 120,
      estimatedDurationMinutes: 180,
      stops: ['20000000-0000-0000-0000-000000000004'],
      active: true,
      ...overrides,
    };
  }

  static createTripPayload(overrides: Record<string, unknown> = {}) {
    const suffix = this.randomSuffix();
    return {
      tripNumber: `TRIP-E2E-${suffix}`,
      customerId: '10000000-0000-0000-0000-000000000001',
      departmentId: '11000000-0000-0000-0000-000000000001',
      projectId: '12000000-0000-0000-0000-000000000001',
      routeId: '50000000-0000-0000-0000-000000000001',
      priority: 'NORMAL',
      originLocationId: '20000000-0000-0000-0000-000000000001',
      destinationLocationId: '20000000-0000-0000-0000-000000000002',
      requestedStartTime: '2026-08-25T08:00:00Z',
      requestedEndTime: '2026-08-25T16:00:00Z',
      requiredVehicleTypeId: '31000000-0000-0000-0000-000000000001',
      requiredCapacityKg: 2500,
      cargoDescription: 'Industrial components',
      passengerCount: 1,
      customerInstructions: 'Handle with care',
      notes: 'Priority cargo delivery',
      ...overrides,
    };
  }

  static createFuelPurchasePayload(overrides: Record<string, unknown> = {}) {
    const suffix = this.randomSuffix();
    return {
      purchaseOrderNumber: `PO-E2E-${suffix}`,
      vendorId: '80000000-0000-0000-0000-000000000001',
      fuelStationId: '70000000-0000-0000-0000-000000000001',
      bunkerTankId: '72000000-0000-0000-0000-000000000001',
      fuelType: 'DIESEL',
      quantityLiters: 1500,
      unitPrice: 310.0,
      totalCost: 465000.0,
      currencyCode: 'LKR',
      orderedAt: '2026-08-19T08:00:00Z',
      ...overrides,
    };
  }

  static createFuelIssuePayload(overrides: Record<string, unknown> = {}) {
    const suffix = this.randomSuffix();
    return {
      issueNumber: `ISS-E2E-${suffix}`,
      vehicleId: '32000000-0000-0000-0000-000000000001',
      driverId: '40000000-0000-0000-0000-000000000001',
      fuelStationId: '70000000-0000-0000-0000-000000000001',
      bunkerTankId: '72000000-0000-0000-0000-000000000001',
      fuelType: 'DIESEL',
      quantityLiters: 80,
      odometerKm: 42600,
      issuedAt: '2026-08-19T10:00:00Z',
      ...overrides,
    };
  }
}
