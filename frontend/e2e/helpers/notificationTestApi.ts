import { expect, type APIRequestContext, type Page, type TestInfo } from '@playwright/test';
import { randomUUID } from 'node:crypto';

export type AuthTokens = { accessToken: string; refreshToken: string };
export type NotificationRule = { id: string; name: string; enabled: boolean; [key: string]: unknown };
export type NotificationItem = { id: string; eventId: string; title: string; message: string; status: string; channel: string };
export type Delivery = { notificationId: string; ruleId?: string; eventId: string; status: string; attemptCount: number; nextDeliveryAt?: string; terminalFailure: boolean };

export function unique(prefix: string, testInfo: TestInfo): string {
  const browser = testInfo.project.name.replace(/[^a-z0-9]/gi, '').slice(0, 8);
  return `${prefix}-${browser}-${randomUUID().slice(0, 8)}`;
}

export async function login(api: APIRequestContext, username: string, password: string): Promise<AuthTokens> {
  const response = await api.post('/api/auth/login', { data: { username, password } });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json();
}

export async function adminLogin(api: APIRequestContext): Promise<AuthTokens> {
  const username = process.env.E2E_ADMIN_USERNAME;
  const password = process.env.E2E_ADMIN_PASSWORD;
  if (!username || !password) throw new Error('E2E administrator credentials are unavailable');
  return login(api, username, password);
}

export function headers(tokens: AuthTokens) {
  return { Authorization: `Bearer ${tokens.accessToken}` };
}

export async function authenticatePage(page: Page, tokens: AuthTokens) {
  await page.addInitScript(({ accessToken, refreshToken }) => {
    localStorage.setItem('transport.accessToken', accessToken);
    localStorage.setItem('transport.refreshToken', refreshToken);
  }, tokens);
}

export async function provisionUser(api: APIRequestContext, admin: AuthTokens, name: string, permissions: string[]) {
  const roleResponse = await api.post('/api/roles', { headers: headers(admin), data: {
    name: `E2E_${name.toUpperCase()}`, description: 'Notification E2E isolated role', active: true, permissions,
  }});
  expect(roleResponse.ok(), await roleResponse.text()).toBeTruthy();
  const role = await roleResponse.json();
  const username = name.toLowerCase();
  const password = `E2e!Safe-${randomUUID()}`;
  const userResponse = await api.post('/api/users', { headers: headers(admin), data: {
    username, email: `${username}@example.test`, password, firstName: 'E2E', lastName: 'Notification',
    active: true, roleIds: [role.id],
  }});
  expect(userResponse.ok(), await userResponse.text()).toBeTruthy();
  const user = await userResponse.json();
  return { user, role, username, password, tokens: await login(api, username, password) };
}

export async function createRule(api: APIRequestContext, admin: AuthTokens, data: Record<string, unknown>): Promise<NotificationRule> {
  const response = await api.post('/api/notification-rules', { headers: headers(admin), data: {
    description: 'Playwright notification rule', eventType: 'TRIP_DELAY_RECORDED', templateCode: 'TRIP_DELAY',
    severityThreshold: 'INFO', suppressionWindowMinutes: 0, quietHoursEnabled: false, quietDays: [],
    escalationEnabled: false, enabled: true, ...data,
  }});
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json();
}

export async function deleteRule(api: APIRequestContext, admin: AuthTokens, id: string) {
  const response = await api.delete(`/api/notification-rules/${id}`, { headers: headers(admin) });
  if ([204, 404].includes(response.status())) return;
  const disabled = await api.patch(`/api/notification-rules/${id}/disable`, { headers: headers(admin) });
  expect([200, 404]).toContain(disabled.status());
}

export async function triggerDelay(api: APIRequestContext, admin: AuthTokens, reason: string,
  tripId = '60000000-0000-0000-0000-000000000006') {
  const response = await api.post(`/api/trips/${tripId}/delays`, {
    headers: headers(admin), data: { delayMinutes: 17, reason, locationDescription: 'E2E checkpoint' },
  });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json() as Promise<{ id: string }>;
}

export async function triggerIncident(api: APIRequestContext, admin: AuthTokens, description: string) {
  const response = await api.post('/api/trips/60000000-0000-0000-0000-000000000006/incidents', {
    headers: headers(admin), data: { incidentSeverity: 'LOW', description, locationDescription: 'E2E checkpoint' },
  });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json() as Promise<{ id: string }>;
}

export async function createOperationalTrip(api: APIRequestContext, admin: AuthTokens, tripNumber: string): Promise<string> {
  const start = new Date(Date.now() + 20 * 24 * 60 * 60 * 1000);
  const end = new Date(start.getTime() + 8 * 60 * 60 * 1000);
  const created = await api.post('/api/trips', { headers: headers(admin), data: {
    tripNumber, customerId: '10000000-0000-0000-0000-000000000001', departmentId: '11000000-0000-0000-0000-000000000001',
    projectId: '12000000-0000-0000-0000-000000000001', routeId: '50000000-0000-0000-0000-000000000001',
    priority: 'NORMAL', originLocationId: '20000000-0000-0000-0000-000000000001', destinationLocationId: '20000000-0000-0000-0000-000000000002',
    requestedStartTime: start.toISOString(), requestedEndTime: end.toISOString(), requiredVehicleTypeId: '31000000-0000-0000-0000-000000000001',
    requiredCapacityKg: 1000, cargoDescription: 'Notification suppression E2E cargo', passengerCount: 1,
  }});
  expect(created.ok(), await created.text()).toBeTruthy();
  const trip = await created.json();
  for (const action of ['submit', 'approve']) {
    const response = await api.post(`/api/trips/${trip.id}/${action}`, { headers: headers(admin), data: action === 'approve' ? { remarks: 'E2E notification test' } : {} });
    expect(response.ok(), await response.text()).toBeTruthy();
  }
  return trip.id;
}

export async function notifications(api: APIRequestContext, tokens: AuthTokens): Promise<NotificationItem[]> {
  const response = await api.get('/api/notifications?limit=50', { headers: headers(tokens) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json();
}

export async function unreadCount(api: APIRequestContext, tokens: AuthTokens): Promise<number> {
  const response = await api.get('/api/notifications/unread-count', { headers: headers(tokens) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return (await response.json()).unreadCount;
}

export async function deliveries(api: APIRequestContext, admin: AuthTokens): Promise<Delivery[]> {
  const response = await api.get('/api/notification-deliveries?limit=100', { headers: headers(admin) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json();
}

export async function attempts(api: APIRequestContext, admin: AuthTokens, id: string) {
  const response = await api.get(`/api/notification-deliveries/${id}/attempts`, { headers: headers(admin) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.json() as Promise<Array<{ attemptNumber: number; state: string; providerMessageId?: string; errorCode?: string }>>;
}

export async function processEmail(api: APIRequestContext, admin: AuthTokens) {
  const response = await api.post('/api/e2e/notifications/process-email', { headers: headers(admin) });
  expect(response.status(), await response.text()).toBe(204);
}

export async function advanceTime(api: APIRequestContext, admin: AuthTokens, duration = 'PT61S') {
  const response = await api.post(`/api/e2e/notifications/advance?duration=${duration}`, { headers: headers(admin) });
  expect(response.ok(), await response.text()).toBeTruthy();
}

export async function backendNow(api: APIRequestContext, admin: AuthTokens): Promise<Date> {
  const response = await api.get('/api/e2e/notifications/now', { headers: headers(admin) });
  expect(response.ok(), await response.text()).toBeTruthy();
  return new Date((await response.json()).now);
}
