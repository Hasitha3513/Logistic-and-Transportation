import { expect, test, type APIRequestContext } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { adminLogin, authenticatePage, headers, provisionUser, type AuthTokens } from '../../helpers/notificationTestApi';

const customerId = '10000000-0000-0000-0000-000000000001';
const originLocationId = '20000000-0000-0000-0000-000000000001';
const destinationLocationId = '20000000-0000-0000-0000-000000000002';

async function createOrder(api: APIRequestContext, tokens: AuthTokens) {
  const marker = randomUUID().slice(0, 8);
  const response = await api.post('/api/v1/freight/orders', {
    headers: headers(tokens),
    data: {
      customerId,
      originLocationId,
      destinationLocationId,
      requestedPickupAt: '2027-04-01T08:00:00Z',
      requestedDeliveryAt: '2027-04-02T08:00:00Z',
      serviceLevel: `CEX_SLA_${marker}`,
      priority: 'HIGH',
      lines: [{ description: `Cargo ${marker}`, quantity: 10 }],
    },
  });
  expect(response.status(), await response.text()).toBe(201);
  return response.json();
}

async function recordException(
  api: APIRequestContext,
  tokens: AuthTokens,
  orderId: string,
  type: string,
  description: string,
  severity: string = 'MEDIUM',
  restriction?: string
) {
  const response = await api.post('/api/v1/freight/exceptions', {
    headers: headers(tokens),
    data: {
      exceptionType: type,
      severity,
      freightOrderId: orderId,
      description,
      restriction,
      impact: 'Operational delay risk',
      correctiveAction: 'Inspect and isolate',
    },
  });
  expect(response.status(), await response.text()).toBe(201);
  return response.json();
}

test.describe('US-30 Cargo Exception Management', () => {
  test('E2E-P2-CEX-001: Record damage exception against freight order (AC1)', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const exc = await recordException(
      request,
      admin,
      order.id,
      'DAMAGE',
      'Pallet box torn and contents exposed during transit',
      'HIGH'
    );

    expect(exc.exceptionNumber).toMatch(/^CEX-\d{4}-\d{6}$/);
    expect(exc.exceptionType).toBe('DAMAGE');
    expect(exc.status).toBe('OPEN');
    expect(exc.severity).toBe('HIGH');
    expect(exc.freightOrderId).toBe(order.id);
  });

  test('E2E-P2-CEX-002: Record partial shipment and seal tampering exceptions (AC1)', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);

    const partialExc = await recordException(
      request,
      admin,
      order.id,
      'PARTIAL_SHIPMENT',
      'Received 8 of 10 cartons. 2 cartons missing from origin dispatch.',
      'MEDIUM'
    );
    expect(partialExc.exceptionType).toBe('PARTIAL_SHIPMENT');
    expect(partialExc.status).toBe('OPEN');

    const sealExc = await recordException(
      request,
      admin,
      order.id,
      'SEAL_TAMPERING',
      'Container bolt seal SL-9844 broken prior to gate arrival.',
      'CRITICAL',
      'Hold in security quarantine bay'
    );
    expect(sealExc.exceptionType).toBe('SEAL_TAMPERING');
    expect(sealExc.severity).toBe('CRITICAL');
    expect(sealExc.status).toBe('OPEN');
    expect(sealExc.restriction).toBe('Hold in security quarantine bay');
  });

  test('E2E-P2-CEX-003: Apply operational hold and restriction on hazardous material (AC2)', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);

    const exc = await recordException(
      request,
      admin,
      order.id,
      'HAZARDOUS_MATERIAL',
      'Unlabeled chemical solvent detected in consignment.',
      'CRITICAL'
    );

    const holdResp = await request.post(`/api/v1/freight/exceptions/${exc.id}/hold`, {
      headers: headers(admin),
      data: {
        restriction: 'Immediate quarantine at chemical containment bay',
        reason: 'Hazmat MSDS certificate missing',
        version: exc.version,
      },
    });
    expect(holdResp.status(), await holdResp.text()).toBe(200);
    const held = await holdResp.json();

    expect(held.status).toBe('HELD');
    expect(held.restriction).toBe('Immediate quarantine at chemical containment bay');
    expect(held.history).toHaveLength(1);
    expect(held.history[0].action).toBe('HOLD_APPLIED');
  });

  test('E2E-P2-CEX-004: Escalate unmanifested cargo exception to management (AC2)', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);

    const exc = await recordException(
      request,
      admin,
      order.id,
      'UNMANIFESTED_CARGO',
      'Extra pallet without shipping document discovered in truck.',
      'HIGH'
    );

    const escalateResp = await request.post(`/api/v1/freight/exceptions/${exc.id}/escalate`, {
      headers: headers(admin),
      data: {
        reason: 'Cargo origin cannot be traced; requires freight manager escalation',
        version: exc.version,
      },
    });
    expect(escalateResp.status(), await escalateResp.text()).toBe(200);
    const escalated = await escalateResp.json();

    expect(escalated.status).toBe('ESCALATED');
    expect(escalated.history).toHaveLength(1);
    expect(escalated.history[0].action).toBe('ESCALATED');
  });

  test('E2E-P2-CEX-005: Release hold after safety clearance and verify state transition (AC2)', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);

    const exc = await recordException(
      request,
      admin,
      order.id,
      'WEIGHT_DISCREPANCY',
      'Gross weight exceeds waybill declared weight by 450 kg.',
      'MEDIUM'
    );

    // Hold
    const holdResp = await request.post(`/api/v1/freight/exceptions/${exc.id}/hold`, {
      headers: headers(admin),
      data: {
        restriction: 'Weight check hold at weighbridge',
        reason: 'Re-weigh required',
        version: exc.version,
      },
    });
    const held = await holdResp.json();
    expect(held.status).toBe('HELD');

    // Release
    const releaseResp = await request.post(`/api/v1/freight/exceptions/${exc.id}/release`, {
      headers: headers(admin),
      data: {
        reason: 'Tare weight re-calibrated; net weight conforms to limits',
        version: held.version,
      },
    });
    expect(releaseResp.status(), await releaseResp.text()).toBe(200);
    const released = await releaseResp.json();

    expect(released.status).toBe('OPEN');
    expect(released.restriction).toBeNull();
    expect(released.history).toHaveLength(2);
    expect(released.history[1].action).toBe('RELEASED');
  });

  test('E2E-P2-CEX-006: Resolve cargo exception with resolution outcome and verify retained audit history (AC3)', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);

    const exc = await recordException(
      request,
      admin,
      order.id,
      'DAMAGE',
      'Pallet carton water damaged',
      'HIGH'
    );

    // Hold first
    const holdResp = await request.post(`/api/v1/freight/exceptions/${exc.id}/hold`, {
      headers: headers(admin),
      data: {
        restriction: 'Dry bay hold',
        reason: 'Inspection pending',
        version: exc.version,
      },
    });
    const held = await holdResp.json();

    // Resolve
    const resolveResp = await request.post(`/api/v1/freight/exceptions/${exc.id}/resolve`, {
      headers: headers(admin),
      data: {
        resolution: 'Goods inspected, repackaged into moisture-proof wrap and cleared',
        correctiveAction: 'Replaced outer shrink wrap and pallet base',
        reason: 'Passed QA inspection',
        version: held.version,
      },
    });
    expect(resolveResp.status(), await resolveResp.text()).toBe(200);
    const resolved = await resolveResp.json();

    expect(resolved.status).toBe('RESOLVED');
    expect(resolved.resolution).toContain('repackaged into moisture-proof wrap');
    expect(resolved.resolvedAt).not.toBeNull();
    expect(resolved.resolvedBy).not.toBeNull();

    // Verify retained resolution history (AC3)
    expect(resolved.history).toHaveLength(2);
    expect(resolved.history[0].action).toBe('HOLD_APPLIED');
    expect(resolved.history[1].action).toBe('RESOLVED');
    expect(resolved.history[1].details).toContain('repackaged into moisture-proof wrap');
  });

  test('E2E-P2-CEX-007: Reject invalid exception and confirm closed state (AC3)', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);

    const exc = await recordException(
      request,
      admin,
      order.id,
      'SEAL_TAMPERING',
      'Suspected seal mark discrepancy',
      'LOW'
    );

    const rejectResp = await request.post(`/api/v1/freight/exceptions/${exc.id}/reject`, {
      headers: headers(admin),
      data: {
        reason: 'Investigation confirmed seal serial matches shipping bill amendment',
        version: exc.version,
      },
    });
    expect(rejectResp.status(), await rejectResp.text()).toBe(200);
    const rejected = await rejectResp.json();

    expect(rejected.status).toBe('REJECTED');
    expect(rejected.history).toHaveLength(1);
    expect(rejected.history[0].action).toBe('REJECTED');

    // Attempting further mutation should fail with conflict
    const invalidHold = await request.post(`/api/v1/freight/exceptions/${exc.id}/hold`, {
      headers: headers(admin),
      data: {
        restriction: 'Cannot hold',
        version: rejected.version,
      },
    });
    expect(invalidHold.status()).toBe(409);
  });

  test('E2E-P2-CEX-008: UI flow: browse list, filter by type/status, navigate to detail (AC1, AC2, AC3)', async ({
    page,
    request,
  }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const exc = await recordException(
      request,
      admin,
      order.id,
      'DAMAGE',
      'UI verification exception for damage handling',
      'HIGH',
      'Hold in Loading Bay A'
    );

    await authenticatePage(page, admin);
    await page.goto('/freight/exceptions');

    // Verify exception appears in table
    await expect(page.locator(`text=${exc.exceptionNumber}`)).toBeVisible({ timeout: 15000 });
    await expect(page.locator('text=Damage').first()).toBeVisible();

    // Click to navigate to details
    await page.click(`text=${exc.exceptionNumber}`);
    await expect(page.locator(`text=${exc.exceptionNumber}`)).toBeVisible();
    await expect(page.locator('text=UI verification exception for damage handling')).toBeVisible();
    await expect(page.locator('text=Hold in Loading Bay A')).toBeVisible();
    await expect(page.locator('text=Resolution & Workflow Audit History')).toBeVisible();
  });
});
