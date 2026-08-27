import { expect, test, type APIRequestContext } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { adminLogin, authenticatePage, headers, provisionUser, type AuthTokens } from '../../helpers/notificationTestApi';

const customerId = '10000000-0000-0000-0000-000000000001';
const originLocationId = '20000000-0000-0000-0000-000000000001';
const destinationLocationId = '20000000-0000-0000-0000-000000000002';

async function getOrCreateVehicle(api: APIRequestContext, tokens: AuthTokens): Promise<string> {
  const response = await api.get('/api/vehicles', { headers: headers(tokens) });
  if (response.ok()) {
    const list = await response.json();
    const active = list.find((v: { active: boolean; id: string }) => v.active);
    if (active) return active.id;
  }
  return '30000000-0000-0000-0000-000000000001';
}

async function createOrder(api: APIRequestContext, tokens: AuthTokens, lineCount = 2) {
  const marker = randomUUID().slice(0, 8);
  const lines = Array.from({ length: lineCount }, (_, i) => ({
    description: `Cargo Line ${i + 1} ${marker}`,
    quantity: 2,
  }));
  const response = await api.post('/api/v1/freight/orders', {
    headers: headers(tokens),
    data: {
      customerId,
      originLocationId,
      destinationLocationId,
      requestedPickupAt: '2027-04-01T08:00:00Z',
      requestedDeliveryAt: '2027-04-02T08:00:00Z',
      serviceLevel: `SLA_${marker}`,
      priority: 'HIGH',
      lines,
    },
  });
  expect(response.status(), await response.text()).toBe(201);
  return response.json();
}

async function createAndFinalizeManifest(
  api: APIRequestContext,
  tokens: AuthTokens,
  itemClassifications: Array<{ fragile: boolean | null; temperatureSensitive: boolean | null; specialHandlingNotes?: string }>
) {
  const order = await createOrder(api, tokens, itemClassifications.length);
  const mRes = await api.post('/api/v1/freight/manifests', {
    headers: headers(tokens),
    data: { freightOrderId: order.id },
  });
  expect(mRes.status(), await mRes.text()).toBe(201);
  let manifest = await mRes.json();

  for (let i = 0; i < itemClassifications.length; i++) {
    const c = itemClassifications[i];
    const addRes = await api.post(`/api/v1/freight/manifests/${manifest.id}/items`, {
      headers: headers(tokens),
      data: {
        version: manifest.version,
        freightOrderLineId: order.lines[i].id,
        description: `Item ${i + 1}`,
        quantity: 2,
        packingInformation: 'Standard Box',
        commodityClassification: 'GENERAL.CARGO',
        customsApplicable: false,
        hazardous: false,
        fragile: c.fragile,
        temperatureSensitive: c.temperatureSensitive,
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

test.describe('US-26 Load Planning Dedicated E2E Acceptance Suite', () => {

  test('E2E-P2-LOAD-001: Structured Fragile Cargo Rule', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: true, temperatureSensitive: false },
      { fragile: false, temperatureSensitive: false },
    ]);

    const item1Id = manifest.items[0].id;
    const item2Id = manifest.items[1].id;

    // 1. Create plan with fragile item sharing stackGroup with another placement
    const createRes = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: item1Id, placementOrder: 0, zoneReference: 'FRONT', stackGroup: 'STACK-SHARED', containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: item2Id, placementOrder: 1, zoneReference: 'FRONT', stackGroup: 'STACK-SHARED', containerReference: 'P2', loadingSequence: 2 },
        ],
        notes: 'Fragile stacking test',
      },
    });
    expect(createRes.status(), await createRes.text()).toBe(201);
    let plan = await createRes.json();

    // 2. Validate layout -> reports fragile rule violation
    const valRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-layout`, { headers: headers(admin) });
    expect(valRes.status()).toBe(200);
    const valData = await valRes.json();
    expect(valData.valid).toBe(false);
    expect(valData.violations.map((v: { code: string }) => v.code)).toContain('LOAD_PLAN_FRAGILE_RULE_FAILED');

    // 3. Ready command is blocked
    const readyBlocked = await request.post(`/api/v1/freight/load-plans/${plan.id}/ready`, {
      headers: headers(admin),
      data: { version: plan.version },
    });
    expect(readyBlocked.status()).toBe(400);
    expect((await readyBlocked.json()).code).toBe('LOAD_PLAN_STRUCTURAL_VIOLATIONS');

    // 4. Correct stack configuration (unique stack group for fragile item)
    const updateRes = await request.patch(`/api/v1/freight/load-plans/${plan.id}`, {
      headers: headers(admin),
      data: {
        version: plan.version,
        vehicleId,
        placements: [
          { manifestItemId: item1Id, placementOrder: 0, zoneReference: 'FRONT', stackGroup: 'STACK-SOLO-FRAGILE', containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: item2Id, placementOrder: 1, zoneReference: 'FRONT', stackGroup: 'STACK-STANDARD', containerReference: 'P2', loadingSequence: 2 },
        ],
        notes: 'Corrected stack configuration',
      },
    });
    expect(updateRes.status()).toBe(200);
    plan = await updateRes.json();

    // 5. Layout validation now passes fragile rule
    const valFixedRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-layout`, { headers: headers(admin) });
    expect(valFixedRes.status()).toBe(200);
    const fixedData = await valFixedRes.json();
    expect(fixedData.violations.filter((v: { code: string }) => v.code === 'LOAD_PLAN_FRAGILE_RULE_FAILED')).toHaveLength(0);
  });

  test('E2E-P2-LOAD-002: Structured Temperature-Sensitive Cargo Rule', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: false, temperatureSensitive: true },
      { fragile: false, temperatureSensitive: false },
    ]);

    const tempItem = manifest.items[0].id;
    const stdItem = manifest.items[1].id;

    // Scenario A: no zoneReference for temp item
    const createResA = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: tempItem, placementOrder: 0, zoneReference: null, stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: stdItem, placementOrder: 1, zoneReference: 'AMBIENT-1', stackGroup: null, containerReference: 'P2', loadingSequence: 2 },
        ],
      },
    });
    expect(createResA.status()).toBe(201);
    let plan = await createResA.json();

    const valA = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-layout`, { headers: headers(admin) });
    expect((await valA.json()).violations.map((v: { code: string }) => v.code)).toContain('LOAD_PLAN_TEMPERATURE_RULE_FAILED');

    // Scenario B: temp item shares zone with standard cargo
    const updateResB = await request.patch(`/api/v1/freight/load-plans/${plan.id}`, {
      headers: headers(admin),
      data: {
        version: plan.version,
        vehicleId,
        placements: [
          { manifestItemId: tempItem, placementOrder: 0, zoneReference: 'SHARED-ZONE', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: stdItem, placementOrder: 1, zoneReference: 'SHARED-ZONE', stackGroup: null, containerReference: 'P2', loadingSequence: 2 },
        ],
      },
    });
    expect(updateResB.status()).toBe(200);
    plan = await updateResB.json();

    const valB = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-layout`, { headers: headers(admin) });
    expect((await valB.json()).violations.map((v: { code: string }) => v.code)).toContain('LOAD_PLAN_TEMPERATURE_RULE_FAILED');

    // Scenario C: temp item in dedicated exclusive zone
    const updateResC = await request.patch(`/api/v1/freight/load-plans/${plan.id}`, {
      headers: headers(admin),
      data: {
        version: plan.version,
        vehicleId,
        placements: [
          { manifestItemId: tempItem, placementOrder: 0, zoneReference: 'COLD-ZONE', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: stdItem, placementOrder: 1, zoneReference: 'AMBIENT-ZONE', stackGroup: null, containerReference: 'P2', loadingSequence: 2 },
        ],
      },
    });
    expect(updateResC.status()).toBe(200);
    plan = await updateResC.json();

    const valC = await request.post(`/api/v1/freight/load-plans/${plan.id}/validate-layout`, { headers: headers(admin) });
    const valCData = await valC.json();
    expect(valCData.violations.filter((v: { code: string }) => v.code === 'LOAD_PLAN_TEMPERATURE_RULE_FAILED')).toHaveLength(0);
  });

  test('E2E-P2-LOAD-003: Invalid Draft Can Save But Cannot Become Ready', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: false, temperatureSensitive: false },
      { fragile: false, temperatureSensitive: false },
    ]);

    // Incomplete draft: place only 1 of 2 items
    const createRes = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: manifest.items[0].id, placementOrder: 0, zoneReference: 'BAY-1', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
        ],
        notes: 'Incomplete plan draft',
      },
    });
    expect(createRes.status()).toBe(201);
    const plan = await createRes.json();
    expect(plan.readinessStatus).toBe('DRAFT');
    expect(plan.readyAt).toBeNull();
    expect(plan.readyBy).toBeNull();

    // Mark ready is rejected
    const readyRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/ready`, {
      headers: headers(admin),
      data: { version: plan.version },
    });
    expect(readyRes.status()).toBe(400);
    const err = await readyRes.json();
    expect(err.code).toBe('LOAD_PLAN_STRUCTURAL_VIOLATIONS');

    // Confirm plan remains DRAFT
    const getRes = await request.get(`/api/v1/freight/load-plans/${plan.id}`, { headers: headers(admin) });
    const fetched = await getRes.json();
    expect(fetched.readinessStatus).toBe('DRAFT');
    expect(fetched.readyAt).toBeNull();
    expect(fetched.readyBy).toBeNull();
  });

  test('E2E-P2-LOAD-004: Valid Load Plan Becomes Structurally Ready and Free-Text Non-Authority', async ({ page, request }) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    // Free-text regression check: fragile=false with notes="FRAGILE" is NOT fragile
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: false, temperatureSensitive: false, specialHandlingNotes: 'FRAGILE HANDLE WITH CARE' },
      { fragile: false, temperatureSensitive: false, specialHandlingNotes: 'STANDARD' },
    ]);

    const item1 = manifest.items[0].id;
    const item2 = manifest.items[1].id;

    // Both share stack group, which is valid because neither is structured fragile
    const createRes = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: item1, placementOrder: 0, zoneReference: 'BAY-1', stackGroup: 'STACK-SHARED', containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: item2, placementOrder: 1, zoneReference: 'BAY-1', stackGroup: 'STACK-SHARED', containerReference: 'P2', loadingSequence: 2 },
        ],
        notes: 'Valid structurally sound plan',
      },
    });
    expect(createRes.status()).toBe(201);
    const plan = await createRes.json();

    // UI mark ready
    await authenticatePage(page, admin);
    await page.goto(`/freight/load-plans/${plan.id}`);

    await expect(page.getByRole('heading', { name: plan.loadPlanNumber })).toBeVisible();

    const markReadyBtn = page.getByRole('button', { name: /Mark Structurally Ready/i });
    await expect(markReadyBtn).toBeEnabled();
    await markReadyBtn.click();

    // UI displays STRUCTURALLY READY tag and audit fields
    await expect(page.getByText('STRUCTURALLY READY', { exact: true })).toBeVisible();

    // Reopen / refresh page to prove persistence
    await page.reload();
    await expect(page.getByRole('heading', { name: plan.loadPlanNumber })).toBeVisible();
    await expect(page.getByText('STRUCTURALLY READY', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: /Mark Structurally Ready/i })).toBeDisabled();

    // Verify backend state
    const getRes = await request.get(`/api/v1/freight/load-plans/${plan.id}`, { headers: headers(admin) });
    const fetched = await getRes.json();
    expect(fetched.readinessStatus).toBe('STRUCTURALLY_READY');
    expect(fetched.readyAt).toBeTruthy();
    expect(fetched.readyBy).toBeTruthy();
    expect(fetched.version).toBeGreaterThan(plan.version);
  });

  test('E2E-P2-LOAD-005: Material Edit Invalidates Structural Readiness & Notes-Only Preserves', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: false, temperatureSensitive: false },
      { fragile: false, temperatureSensitive: false },
    ]);

    const item1 = manifest.items[0].id;
    const item2 = manifest.items[1].id;

    const createRes = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: item1, placementOrder: 0, zoneReference: 'BAY-1', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: item2, placementOrder: 1, zoneReference: 'BAY-2', stackGroup: null, containerReference: 'P2', loadingSequence: 2 },
        ],
        notes: 'Initial plan',
      },
    });
    expect(createRes.status()).toBe(201);
    let plan = await createRes.json();

    // Mark ready
    const readyRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/ready`, {
      headers: headers(admin),
      data: { version: plan.version },
    });
    expect(readyRes.status()).toBe(200);
    plan = await readyRes.json();
    expect(plan.readinessStatus).toBe('STRUCTURALLY_READY');

    // 1. Material mutation: change zone of placement
    const materialRes = await request.patch(`/api/v1/freight/load-plans/${plan.id}`, {
      headers: headers(admin),
      data: {
        version: plan.version,
        vehicleId,
        placements: [
          { manifestItemId: item1, placementOrder: 0, zoneReference: 'BAY-CHANGED', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: item2, placementOrder: 1, zoneReference: 'BAY-2', stackGroup: null, containerReference: 'P2', loadingSequence: 2 },
        ],
        notes: 'Material change applied',
      },
    });
    expect(materialRes.status()).toBe(200);
    plan = await materialRes.json();
    expect(plan.readinessStatus).toBe('DRAFT');
    expect(plan.readyAt).toBeNull();
    expect(plan.readyBy).toBeNull();

    // 2. Mark ready again
    const reReadyRes = await request.post(`/api/v1/freight/load-plans/${plan.id}/ready`, {
      headers: headers(admin),
      data: { version: plan.version },
    });
    expect(reReadyRes.status()).toBe(200);
    plan = await reReadyRes.json();
    expect(plan.readinessStatus).toBe('STRUCTURALLY_READY');
    const origReadyAt = plan.readyAt;
    const origReadyBy = plan.readyBy;

    // 3. Notes-only mutation: preserves STRUCTURALLY_READY
    const notesRes = await request.patch(`/api/v1/freight/load-plans/${plan.id}`, {
      headers: headers(admin),
      data: {
        version: plan.version,
        vehicleId,
        placements: [
          { manifestItemId: item1, placementOrder: 0, zoneReference: 'BAY-CHANGED', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
          { manifestItemId: item2, placementOrder: 1, zoneReference: 'BAY-2', stackGroup: null, containerReference: 'P2', loadingSequence: 2 },
        ],
        notes: 'Typo fixed in dispatch notes only',
      },
    });
    expect(notesRes.status()).toBe(200);
    plan = await notesRes.json();
    expect(plan.readinessStatus).toBe('STRUCTURALLY_READY');
    expect(Math.floor(new Date(plan.readyAt).getTime() / 1000)).toBe(Math.floor(new Date(origReadyAt).getTime() / 1000));
    expect(plan.readyBy).toBe(origReadyBy);
    expect(plan.notes).toBe('Typo fixed in dispatch notes only');
  });

  test('E2E-P2-LOAD-006: View-Only User Cannot Mark Ready', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: false, temperatureSensitive: false },
    ]);

    const createRes = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: manifest.items[0].id, placementOrder: 0, zoneReference: 'BAY-1', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
        ],
        notes: 'View only test plan',
      },
    });
    expect(createRes.status()).toBe(201);
    const plan = await createRes.json();

    const viewer = await provisionUser(request, admin, `lpview-${testInfo.project.name}-${randomUUID().slice(0, 6)}`, ['LOAD_PLAN_VIEW']);
    await authenticatePage(page, viewer.tokens);

    await page.goto(`/freight/load-plans/${plan.id}`);
    await expect(page.getByRole('heading', { name: plan.loadPlanNumber })).toBeVisible();
    await expect(page.getByRole('button', { name: /Mark Structurally Ready/i })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Validate Layout/i })).toHaveCount(0);
  });

  test('E2E-P2-LOAD-007: Direct Unauthorized Ready Command Returns 403', async ({ request }, testInfo) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: false, temperatureSensitive: false },
    ]);

    const createRes = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: manifest.items[0].id, placementOrder: 0, zoneReference: 'BAY-1', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
        ],
      },
    });
    expect(createRes.status()).toBe(201);
    const plan = await createRes.json();

    const viewer = await provisionUser(request, admin, `lpview2-${testInfo.project.name}-${randomUUID().slice(0, 6)}`, ['LOAD_PLAN_VIEW']);

    // Direct unauthorized ready command
    const forbidden = await request.post(`/api/v1/freight/load-plans/${plan.id}/ready`, {
      headers: headers(viewer.tokens),
      data: { version: plan.version },
    });
    expect(forbidden.status()).toBe(403);

    // Plan state is unchanged
    const getRes = await request.get(`/api/v1/freight/load-plans/${plan.id}`, { headers: headers(admin) });
    const fetched = await getRes.json();
    expect(fetched.readinessStatus).toBe('DRAFT');
    expect(fetched.readyAt).toBeNull();
    expect(fetched.readyBy).toBeNull();
  });

  test('E2E-P2-LOAD-008: Stale Ready Command Returns 409', async ({ request }) => {
    const admin = await adminLogin(request);
    const vehicleId = await getOrCreateVehicle(request, admin);
    const { manifest } = await createAndFinalizeManifest(request, admin, [
      { fragile: false, temperatureSensitive: false },
    ]);

    const createRes = await request.post('/api/v1/freight/load-plans', {
      headers: headers(admin),
      data: {
        cargoManifestId: manifest.id,
        vehicleId,
        placements: [
          { manifestItemId: manifest.items[0].id, placementOrder: 0, zoneReference: 'BAY-1', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
        ],
        notes: 'Initial draft',
      },
    });
    expect(createRes.status()).toBe(201);
    const planV0 = await createRes.json();
    const staleVersion = planV0.version;

    // Legitimate update increments version to V+1
    const updateRes = await request.patch(`/api/v1/freight/load-plans/${planV0.id}`, {
      headers: headers(admin),
      data: {
        version: staleVersion,
        vehicleId,
        placements: [
          { manifestItemId: manifest.items[0].id, placementOrder: 0, zoneReference: 'BAY-1', stackGroup: null, containerReference: 'P1', loadingSequence: 1 },
        ],
        notes: 'Notes updated to bump version',
      },
    });
    expect(updateRes.status()).toBe(200);
    const planV1 = await updateRes.json();
    expect(planV1.version).toBeGreaterThan(staleVersion);

    // Attempt ready with stale version
    const staleReadyRes = await request.post(`/api/v1/freight/load-plans/${planV0.id}/ready`, {
      headers: headers(admin),
      data: { version: staleVersion },
    });
    expect(staleReadyRes.status()).toBe(409);
    const err = await staleReadyRes.json();
    expect(err.code).toBe('LOAD_PLAN_STALE_VERSION');

    // Confirm plan remains DRAFT
    const getRes = await request.get(`/api/v1/freight/load-plans/${planV0.id}`, { headers: headers(admin) });
    const fetched = await getRes.json();
    expect(fetched.readinessStatus).toBe('DRAFT');
    expect(fetched.readyAt).toBeNull();
    expect(fetched.readyBy).toBeNull();
  });
});
