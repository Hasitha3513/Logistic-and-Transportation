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
      requestedPickupAt: '2027-03-01T08:00:00Z',
      requestedDeliveryAt: '2027-03-02T08:00:00Z',
      serviceLevel: `INS_SLA_${marker}`,
      priority: 'HIGH',
      lines: [{ description: `Insurance Cargo ${marker}`, quantity: 5 }],
    },
  });
  expect(response.status(), await response.text()).toBe(201);
  return response.json();
}

async function createPolicy(api: APIRequestContext, tokens: AuthTokens, orderId: string) {
  const response = await api.post('/api/v1/freight/insurance/policies', {
    headers: headers(tokens),
    data: {
      freightOrderId: orderId,
      insuranceProvider: 'Zurich Cargo Assurance',
      policyType: 'ALL_RISK',
      coverageAmount: 100000.0,
      premiumAmount: 1200.0,
      deductibleAmount: 500.0,
      currencyCode: 'USD',
      validFrom: '2026-01-01T00:00:00Z',
      validUntil: '2027-12-31T23:59:59Z',
      termsAndConditions: 'Standard cargo transit coverage with comprehensive perils.',
    },
  });
  expect(response.status(), await response.text()).toBe(201);
  return response.json();
}

test.describe('US-28 Freight Insurance Management', () => {
  test('E2E-P2-INS-001: Create insurance policy for freight order', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const policy = await createPolicy(request, admin, order.id);

    expect(policy.policyNumber).toMatch(/^POL-\d{4}-\d{6}$/);
    expect(policy.freightOrderId).toBe(order.id);
    expect(policy.status).toBe('ACTIVE');
    expect(policy.coverageAmount).toBe(100000.0);
    expect(policy.currencyCode).toBe('USD');
  });

  test('E2E-P2-INS-002: Update policy details and terms', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const policy = await createPolicy(request, admin, order.id);

    const updateResponse = await request.put(`/api/v1/freight/insurance/policies/${policy.id}`, {
      headers: headers(admin),
      data: {
        coverageAmount: 120000.0,
        premiumAmount: 1500.0,
        deductibleAmount: 500.0,
        validFrom: '2026-01-01T00:00:00Z',
        validUntil: '2027-12-31T23:59:59Z',
        status: 'ACTIVE',
        termsAndConditions: 'Updated endorsement with expedited claims clause.',
        version: policy.version,
      },
    });
    expect(updateResponse.status(), await updateResponse.text()).toBe(200);
    const updated = await updateResponse.json();
    expect(updated.coverageAmount).toBe(120000.0);
    expect(updated.termsAndConditions).toContain('Updated endorsement');
  });

  test('E2E-P2-INS-003: File claim against active policy', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const policy = await createPolicy(request, admin, order.id);

    const claimResponse = await request.post('/api/v1/freight/insurance/claims', {
      headers: headers(admin),
      data: {
        policyId: policy.id,
        incidentDate: '2026-06-15T14:30:00Z',
        description: 'Water ingress damaged high-value electronic components.',
        claimedAmount: 25000.0,
        currencyCode: 'USD',
      },
    });
    expect(claimResponse.status(), await claimResponse.text()).toBe(201);
    const claim = await claimResponse.json();
    expect(claim.claimNumber).toMatch(/^CLM-\d{4}-\d{6}$/);
    expect(claim.policyId).toBe(policy.id);
    expect(claim.status).toBe('OPEN');
    expect(claim.claimedAmount).toBe(25000.0);
    expect(claim.totalSettledAmount).toBe(0);
  });

  test('E2E-P2-INS-004: Assess and approve claim workflow', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const policy = await createPolicy(request, admin, order.id);

    const claimResponse = await request.post('/api/v1/freight/insurance/claims', {
      headers: headers(admin),
      data: {
        policyId: policy.id,
        incidentDate: '2026-06-15T14:30:00Z',
        description: 'Cargo damage during transit',
        claimedAmount: 20000.0,
        currencyCode: 'USD',
      },
    });
    const claim = await claimResponse.json();

    // Assess claim
    const assessResponse = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/assess`, {
      headers: headers(admin),
      data: {
        assessedAmount: 18000.0,
        assessmentNotes: 'Surveyor inspected cargo and approved 18,000 for repair and replacement.',
        version: claim.version,
      },
    });
    expect(assessResponse.status(), await assessResponse.text()).toBe(200);
    const assessed = await assessResponse.json();
    expect(assessed.status).toBe('UNDER_REVIEW');
    expect(assessed.assessedAmount).toBe(18000.0);

    // Approve claim
    const approveResponse = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/approve`, {
      headers: headers(admin),
      data: {
        notes: 'Manager approved surveyor assessment for payout.',
        version: assessed.version,
      },
    });
    expect(approveResponse.status(), await approveResponse.text()).toBe(200);
    const approved = await approveResponse.json();
    expect(approved.status).toBe('APPROVED');
  });

  test('E2E-P2-INS-005: Multi-tranche settlement and over-settlement prevention', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const policy = await createPolicy(request, admin, order.id);

    const claimRes = await request.post('/api/v1/freight/insurance/claims', {
      headers: headers(admin),
      data: {
        policyId: policy.id,
        incidentDate: '2026-06-15T14:30:00Z',
        description: 'Transit loss claim',
        claimedAmount: 10000.0,
        currencyCode: 'USD',
      },
    });
    const claim = await claimRes.json();

    const assessRes = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/assess`, {
      headers: headers(admin),
      data: { assessedAmount: 10000.0, assessmentNotes: 'Full amount validated', version: claim.version },
    });
    const assessed = await assessRes.json();

    const approveRes = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/approve`, {
      headers: headers(admin),
      data: { notes: 'Approved for payout', version: assessed.version },
    });
    let currentClaim = await approveRes.json();

    // Tranche 1: Partial settlement $6,000
    const settle1Res = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/settlements`, {
      headers: headers(admin),
      data: { amount: 6000.0, notes: 'Tranche 1 payment (60%)', version: currentClaim.version },
    });
    expect(settle1Res.status(), await settle1Res.text()).toBe(200);
    currentClaim = await settle1Res.json();
    expect(currentClaim.totalSettledAmount).toBe(6000.0);
    expect(currentClaim.status).toBe('APPROVED'); // Still APPROVED (partial)
    expect(currentClaim.settlements).toHaveLength(1);

    // Over-settlement attempt ($5,000 > remaining $4,000) -> HTTP 400
    const overSettleRes = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/settlements`, {
      headers: headers(admin),
      data: { amount: 5000.0, notes: 'Exceeding remaining amount', version: currentClaim.version },
    });
    expect(overSettleRes.status()).toBe(400);

    // Tranche 2: Remaining settlement $4,000 -> Status transitions to SETTLED
    const settle2Res = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/settlements`, {
      headers: headers(admin),
      data: { amount: 4000.0, notes: 'Tranche 2 final settlement', version: currentClaim.version },
    });
    expect(settle2Res.status(), await settle2Res.text()).toBe(200);
    currentClaim = await settle2Res.json();
    expect(currentClaim.totalSettledAmount).toBe(10000.0);
    expect(currentClaim.status).toBe('SETTLED');
    expect(currentClaim.settlements).toHaveLength(2);
  });

  test('E2E-P2-INS-006: Dispute and reject workflows', async ({ request }) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const policy = await createPolicy(request, admin, order.id);

    const claimRes = await request.post('/api/v1/freight/insurance/claims', {
      headers: headers(admin),
      data: {
        policyId: policy.id,
        incidentDate: '2026-07-10T10:00:00Z',
        description: 'Improper packing damage',
        claimedAmount: 5000.0,
        currencyCode: 'USD',
      },
    });
    const claim = await claimRes.json();

    // Reject claim
    const rejectRes = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/reject`, {
      headers: headers(admin),
      data: { reason: 'Damage caused by insufficient vendor packaging (excluded clause 4.2)', version: claim.version },
    });
    expect(rejectRes.status(), await rejectRes.text()).toBe(200);
    let updated = await rejectRes.json();
    expect(updated.status).toBe('REJECTED');

    // Dispute rejected claim
    const disputeRes = await request.post(`/api/v1/freight/insurance/claims/${claim.id}/dispute`, {
      headers: headers(admin),
      data: { reason: 'Shipper provides packaging compliance certificate', version: updated.version },
    });
    expect(disputeRes.status(), await disputeRes.text()).toBe(200);
    updated = await disputeRes.json();
    expect(updated.status).toBe('DISPUTED');
  });

  test('E2E-P2-INS-007: RBAC enforcement on API and frontend navigation', async ({ page, request }, testInfo) => {
    const admin = await adminLogin(request);
    const order = await createOrder(request, admin);
    const policy = await createPolicy(request, admin, order.id);

    const viewer = await provisionUser(
      request,
      admin,
      `insview-${testInfo.project.name}-${randomUUID().slice(0, 6)}`,
      ['CARGO_INSURANCE_VIEW']
    );

    await authenticatePage(page, viewer.tokens);
    await page.goto('/freight/insurance/policies');
    await expect(page.getByText(policy.policyNumber)).toBeVisible();

    // Viewer should not have manage button
    const forbidden = await request.post('/api/v1/freight/insurance/policies', {
      headers: headers(viewer.tokens),
      data: {
        freightOrderId: order.id,
        insuranceProvider: 'Unauthorized Mutual',
        policyType: 'ALL_RISK',
        coverageAmount: 50000.0,
        premiumAmount: 500.0,
        deductibleAmount: 100.0,
        currencyCode: 'USD',
        validFrom: '2026-01-01T00:00:00Z',
        validUntil: '2026-12-31T23:59:59Z',
      },
    });
    expect(forbidden.status()).toBe(403);

    // Unauthenticated request
    const unauth = await request.get('/api/v1/freight/insurance/policies');
    expect(unauth.status()).toBe(401);
  });
});
