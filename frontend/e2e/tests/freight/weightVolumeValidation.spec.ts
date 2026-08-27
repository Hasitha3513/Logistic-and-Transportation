import { expect, test, type APIRequestContext } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { adminLogin, authenticatePage, headers, provisionUser, type AuthTokens } from '../../helpers/notificationTestApi';

const customerId = '10000000-0000-0000-0000-000000000001';
const originLocationId = '20000000-0000-0000-0000-000000000001';
const destinationLocationId = '20000000-0000-0000-0000-000000000002';

async function createVehicleWithCapacity(
  api: APIRequestContext,
  tokens: AuthTokens,
  capacity: {
    capacityKg?: number | null;
    tareWeightKg?: number | null;
    grossVehicleWeightKg?: number | null;
    cargoVolumeCapacityM3?: number | null;
    axleCount?: number | null;
    maxAxleLoadKg?: number | null;
  }
) {
  const regNum = `WV-${randomUUID().slice(0, 6).toUpperCase()}`;
  const response = await api.post('/api/vehicles', {
    headers: headers(tokens),
    data: {
      registrationNumber: regNum,
      categoryId: '30000000-0000-0000-0000-000000000001',
      typeId: '31000000-0000-0000-0000-000000000001',
      chassisNumber: `CH-${randomUUID().slice(0, 10).toUpperCase()}`,
      engineNumber: `ENG-${randomUUID().slice(0, 8).toUpperCase()}`,
      manufacturer: 'Volvo',
      model: 'FH16',
      manufactureYear: 2024,
      ownershipType: 'COMPANY_OWNED',
      operationalStatus: 'AVAILABLE',
      capacityKg: capacity.capacityKg ?? null,
      tareWeightKg: capacity.tareWeightKg ?? null,
      grossVehicleWeightKg: capacity.grossVehicleWeightKg ?? null,
      cargoVolumeCapacityM3: capacity.cargoVolumeCapacityM3 ?? null,
      axleCount: capacity.axleCount ?? null,
      maxAxleLoadKg: capacity.maxAxleLoadKg ?? null,
      active: true,
    },
  });
  expect(response.status(), await response.text()).toBe(201);
  return response.json();
}

async function createAndFinalizeManifest(
  api: APIRequestContext,
  tokens: AuthTokens,
  itemCount = 2
) {
  const marker = randomUUID().slice(0, 8);
  const lines = Array.from({ length: itemCount }, (_, i) => ({
    description: `Freight Line ${i + 1} ${marker}`,
    quantity: 2,
  }));

  const oRes = await api.post('/api/v1/freight/orders', {
    headers: headers(tokens),
    data: {
      customerId,
      originLocationId,
      destinationLocationId,
      requestedPickupAt: '2027-05-01T08:00:00Z',
      requestedDeliveryAt: '2027-05-02T08:00:00Z',
      serviceLevel: `SLA_${marker}`,
      priority: 'NORMAL',
      lines,
    },
  });
  expect(oRes.status(), await oRes.text()).toBe(201);
  const order = await oRes.json();

  const mRes = await api.post('/api/v1/freight/manifests', {
    headers: headers(tokens),
    data: { freightOrderId: order.id },
  });
  expect(mRes.status(), await mRes.text()).toBe(201);
  let manifest = await mRes.json();

  for (let i = 0; i < itemCount; i++) {
    const addRes = await api.post(`/api/v1/freight/manifests/${manifest.id}/items`, {
      headers: headers(tokens),
      data: {
        version: manifest.version,
        freightOrderLineId: order.lines[i].id,
        description: `Cargo Item ${i + 1}`,
        quantity: 2,
        packingInformation: 'Wooden Crate',
        commodityClassification: 'GENERAL.CARGO',
        customsApplicable: false,
        hazardous: false,
        fragile: false,
        temperatureSensitive: false,
      },
    });
    expect(addRes.status(), await addRes.text()).toBe(200);
    manifest = await addRes.json();
  }

  const finalizeRes = await api.post(`/api/v1/freight/manifests/${manifest.id}/finalize`, {
    headers: headers(tokens),
    data: { version: manifest.version },
  });
  expect(finalizeRes.status(), await finalizeRes.text()).toBe(200);
  manifest = await finalizeRes.json();
  expect(manifest.finalized).toBe(true);

  return { order, manifest };
}

async function createLoadPlan(
  api: APIRequestContext,
  tokens: AuthTokens,
  manifestId: string,
  vehicleId: string,
  manifestItemIds: string[]
) {
  const placements = manifestItemIds.map((itemId, idx) => ({
    manifestItemId: itemId,
    placementOrder: idx,
    zoneReference: `ZONE-${idx + 1}`,
    stackGroup: `STACK-${idx + 1}`,
    containerReference: `PALLET-${idx + 1}`,
    loadingSequence: idx + 1,
    specialHandlingNotes: 'Standard handling',
  }));

  const response = await api.post('/api/v1/freight/load-plans', {
    headers: headers(tokens),
    data: {
      cargoManifestId: manifestId,
      vehicleId,
      notes: 'US-27 Weight and Volume validation test plan',
      placements,
    },
  });
  expect(response.status(), await response.text()).toBe(201);
  return response.json();
}

test.describe('US-27 Weight, Volume and Vehicle Capacity Validation Dedicated Acceptance Suite', () => {

  test('E2E-P2-WV-001: Execute Weight and Volume Validation API and verify structured response', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicle = await createVehicleWithCapacity(request, admin, {
      capacityKg: 5000,
      tareWeightKg: 3500,
      grossVehicleWeightKg: 8500,
      cargoVolumeCapacityM3: 25.5,
      axleCount: 2,
      maxAxleLoadKg: 4500,
    });

    const { manifest } = await createAndFinalizeManifest(request, admin, 2);
    const itemIds = manifest.items.map((i: { id: string }) => i.id);
    const plan = await createLoadPlan(request, admin, manifest.id, vehicle.id, itemIds);

    // Call validate-weight-volume endpoint
    const valRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-weight-volume`, {
      headers: headers(admin),
    });
    expect(valRes.status()).toBe(200);
    const result = await valRes.json();

    expect(result.loadPlanId).toBe(plan.id);
    expect(result.overallOutcome).toBe('INCOMPLETE'); // Incomplete due to manifest items lacking weight/dimensions in MVP
    expect(result.payloadCapacityKg).toBe(5000);
    expect(result.tareWeightKg).toBe(3500);
    expect(result.grossWeightLimitKg).toBe(8500);
    expect(result.volumeCapacityM3).toBe(25.5);
    expect(result.missingData).toContain('CARGO_ITEM_WEIGHT_DATA_MISSING');
    expect(result.missingData).toContain('CARGO_ITEM_DIMENSIONS_DATA_MISSING');
    expect(result.missingData).toContain('VEHICLE_AXLE_LIMITS_UNAVAILABLE');
    expect(result.violations.length).toBeGreaterThan(0);
  });

  test('E2E-P2-WV-002: Incomplete Vehicle Capacity master data returns explicit missing fact diagnostics', async ({ request }) => {
    const admin = await adminLogin(request);
    // Vehicle missing capacityKg, tareWeightKg, grossVehicleWeightKg, cargoVolumeCapacityM3
    const vehicle = await createVehicleWithCapacity(request, admin, {
      capacityKg: null,
      tareWeightKg: null,
      grossVehicleWeightKg: null,
      cargoVolumeCapacityM3: null,
      axleCount: null,
      maxAxleLoadKg: null,
    });

    const { manifest } = await createAndFinalizeManifest(request, admin, 1);
    const itemIds = manifest.items.map((i: { id: string }) => i.id);
    const plan = await createLoadPlan(request, admin, manifest.id, vehicle.id, itemIds);

    const valRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-weight-volume`, {
      headers: headers(admin),
    });
    expect(valRes.status()).toBe(200);
    const result = await valRes.json();

    expect(result.overallOutcome).toBe('INCOMPLETE');
    expect(result.payloadCapacityKg).toBeNull();
    expect(result.volumeCapacityM3).toBeNull();
    expect(result.tareWeightKg).toBeNull();
    expect(result.grossWeightLimitKg).toBeNull();

    // Verify explicit missing data identifiers
    expect(result.missingData).toContain('VEHICLE_PAYLOAD_CAPACITY_MISSING');
    expect(result.missingData).toContain('VEHICLE_VOLUME_CAPACITY_UNAVAILABLE');
    expect(result.missingData).toContain('VEHICLE_GVW_DATA_MISSING');
    expect(result.violations).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ code: 'VEHICLE_PAYLOAD_CAPACITY_UNAVAILABLE' }),
        expect.objectContaining({ code: 'VEHICLE_VOLUME_CAPACITY_UNAVAILABLE' }),
        expect.objectContaining({ code: 'VEHICLE_GVW_DATA_UNAVAILABLE' }),
      ])
    );
  });

  test('E2E-P2-WV-003: Read-Only Guarantee — Validation does not mutate Load Plan state or US-26 readiness', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicle = await createVehicleWithCapacity(request, admin, {
      capacityKg: 5000,
      tareWeightKg: 3000,
      grossVehicleWeightKg: 8000,
      cargoVolumeCapacityM3: 20,
      axleCount: 2,
      maxAxleLoadKg: 4000,
    });

    const { manifest } = await createAndFinalizeManifest(request, admin, 1);
    const itemIds = manifest.items.map((i: { id: string }) => i.id);
    const plan = await createLoadPlan(request, admin, manifest.id, vehicle.id, itemIds);

    expect(plan.readinessStatus).toBe('DRAFT');
    expect(plan.version).toBe(0);

    // Execute validation 3 consecutive times
    for (let i = 0; i < 3; i++) {
      const valRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-weight-volume`, {
        headers: headers(admin),
      });
      expect(valRes.status()).toBe(200);
    }

    // Inspect plan directly to verify no mutation occurred
    const fetchRes = await request.get(`/api/v1/freight/load-plans/${plan.id}`, {
      headers: headers(admin),
    });
    expect(fetchRes.status()).toBe(200);
    const fetchedPlan = await fetchRes.json();

    expect(fetchedPlan.readinessStatus).toBe('DRAFT');
    expect(fetchedPlan.version).toBe(0);
    expect(fetchedPlan.readyAt).toBeNull();
    expect(fetchedPlan.readyBy).toBeNull();
  });

  test('E2E-P2-WV-004: RBAC enforcement — unauthorized and unauthenticated requests are denied', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicle = await createVehicleWithCapacity(request, admin, {
      capacityKg: 5000,
      tareWeightKg: 3000,
      grossVehicleWeightKg: 8000,
      cargoVolumeCapacityM3: 20,
    });

    const { manifest } = await createAndFinalizeManifest(request, admin, 1);
    const itemIds = manifest.items.map((i: { id: string }) => i.id);
    const plan = await createLoadPlan(request, admin, manifest.id, vehicle.id, itemIds);

    // 1. Unauthenticated request -> 401
    const unauthRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-weight-volume`);
    expect(unauthRes.status()).toBe(401);

    // 2. User with no load plan permissions -> 403
    const { tokens: noPermTokens } = await provisionUser(request, admin, `wv_viewer_${randomUUID().slice(0, 6)}`, ['VEHICLE_VIEW']);
    const deniedRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-weight-volume`, {
      headers: headers(noPermTokens),
    });
    expect(deniedRes.status()).toBe(403);

    // 3. User with LOAD_PLAN_VIEW only -> 403
    const { tokens: viewOnlyTokens } = await provisionUser(request, admin, `wv_viewonly_${randomUUID().slice(0, 6)}`, ['LOAD_PLAN_VIEW']);
    const viewOnlyRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-weight-volume`, {
      headers: headers(viewOnlyTokens),
    });
    expect(viewOnlyRes.status()).toBe(403);

    // 4. User with LOAD_PLAN_MANAGE -> 200 (authorized to execute validation command)
    const { tokens: managerTokens } = await provisionUser(request, admin, `wv_planman_${randomUUID().slice(0, 6)}`, ['LOAD_PLAN_MANAGE']);
    const allowedRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-weight-volume`, {
      headers: headers(managerTokens),
    });
    expect(allowedRes.status()).toBe(200);
  });

  test('E2E-P2-WV-005: Frontend UI renders capacity validation card, metrics, and actionable alerts', async ({ page, request }) => {
    const admin = await adminLogin(request);
    const vehicle = await createVehicleWithCapacity(request, admin, {
      capacityKg: 6000,
      tareWeightKg: 3500,
      grossVehicleWeightKg: 9500,
      cargoVolumeCapacityM3: 28.0,
      axleCount: 2,
      maxAxleLoadKg: 5000,
    });

    const { manifest } = await createAndFinalizeManifest(request, admin, 1);
    const itemIds = manifest.items.map((i: { id: string }) => i.id);
    const plan = await createLoadPlan(request, admin, manifest.id, vehicle.id, itemIds);

    await authenticatePage(page, admin);
    await page.goto(`/freight/load-plans/${plan.id}`);

    // Wait for details page to render
    await expect(page.getByRole('heading', { name: plan.loadPlanNumber })).toBeVisible();

    // Click "Validate Weight & Volume" button
    const validateBtn = page.getByRole('button', { name: /Validate Weight & Volume/i });
    await expect(validateBtn).toBeVisible();
    await validateBtn.click();

    // Verify Weight, Volume & Capacity card is displayed
    await expect(page.getByText('Weight, Volume & Capacity Validation (US-27)')).toBeVisible();

    // Verify Vehicle capacity facts are rendered properly
    await expect(page.getByText('6000 kg')).toBeVisible(); // Payload Capacity
    await expect(page.getByText('28 m³')).toBeVisible(); // Volume Capacity
    await expect(page.getByText('9500 kg')).toBeVisible(); // GVW Limit
    await expect(page.getByText('3500 kg')).toBeVisible(); // Tare Weight

    // Verify missing measurement badges and diagnostics
    await expect(page.getByText('CARGO_ITEM_WEIGHT_DATA_MISSING')).toBeVisible();
    await expect(page.getByText('CARGO_ITEM_DIMENSIONS_DATA_MISSING')).toBeVisible();
    await expect(page.getByText('LOAD_WEIGHT_DATA_MISSING: Cargo item weight measurements are unavailable to compute total cargo weight')).toBeVisible();
  });

  test('E2E-P2-WV-006: Non-existent Load Plan ID returns 404', async ({ request }) => {
    const admin = await adminLogin(request);
    const nonExistentId = randomUUID();
    const res = await request.post(`/api/v1/freight/load-plans/${nonExistentId}/validate-weight-volume`, {
      headers: headers(admin),
    });
    expect(res.status()).toBe(404);
  });
});
