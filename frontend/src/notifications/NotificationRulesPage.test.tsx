import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthProvider } from '../auth/AuthContext';
import { server } from '../test/server';
import { appTheme } from '../app/theme/theme';
import NotificationRulesPage from './NotificationRulesPage';
import type { NotificationRule } from './types';

const mockRules: NotificationRule[] = [
  {
    id: 'rule-1',
    name: 'Trip Delay Alert',
    description: 'Notify dispatchers on trip delays',
    eventType: 'TRIP_DELAY_RECORDED',
    channel: 'IN_APP',
    recipientType: 'ROLE',
    recipientValue: 'DISPATCHER',
    templateCode: 'TRIP_DELAY',
    quietHoursEnabled: false,
    quietDays: [],
    suppressionWindowMinutes: 15,
    escalationEnabled: false,
    enabled: true,
    severityThreshold: 'WARNING',
    createdAt: '2026-08-19T10:00:00Z',
    updatedAt: '2026-08-19T10:00:00Z',
  },
  {
    id: 'rule-2',
    name: 'Maintenance Email Warning',
    description: 'Email fleet manager on blocked maintenance',
    eventType: 'VEHICLE_MAINTENANCE_BLOCKED',
    channel: 'EMAIL',
    recipientType: 'EMAIL_ADDRESS',
    recipientValue: 'fleet@example.com',
    templateCode: 'VEHICLE_MAINTENANCE_DUE',
    quietHoursEnabled: true,
    quietStartTime: '22:00:00',
    quietEndTime: '06:00:00',
    quietDays: ['MONDAY', 'TUESDAY'],
    suppressionWindowMinutes: 60,
    escalationEnabled: true,
    escalationDelayMinutes: 10,
    escalationRecipientType: 'ROLE',
    escalationRecipientValue: 'OPERATIONS',
    enabled: false,
    severityThreshold: 'CRITICAL',
    createdAt: '2026-08-19T11:00:00Z',
    updatedAt: '2026-08-19T11:00:00Z',
  },
];

function renderPage(permissions = ['NOTIFICATION_RULE_VIEW', 'NOTIFICATION_RULE_MANAGE'], deliveries: Record<string, unknown>[] = []) {
  server.use(
    http.get('*/auth/me', () => HttpResponse.json({
      id: 'user-1',
      username: 'ops.manager',
      email: 'ops@example.com',
      firstName: 'Ops',
      lastName: 'Manager',
      active: true,
      roles: ['OPERATIONS'],
      permissions,
    })),
    http.get('*/notification-rules', () => HttpResponse.json(mockRules)),
    http.get('*/notification-event-catalogue', () => HttpResponse.json([
      { eventType: 'TRIP_DELAY_RECORDED', owningModule: 'trip', defaultSeverity: 'WARNING', supportedChannels: ['IN_APP', 'EMAIL'], templateCodes: ['TRIP_DELAY'], requiredVariables: [], optionalVariables: [] },
    ])),
    http.get('*/notification-templates', () => HttpResponse.json([
      { id: 'template-1', code: 'TRIP_DELAY', name: 'Trip delay', eventType: 'TRIP_DELAY_RECORDED', channel: 'IN_APP', body: 'Trip {{tripNumber}} delayed', version: 1, active: true, createdAt: '2026-08-19T10:00:00Z', updatedAt: '2026-08-19T10:00:00Z' },
    ])),
    http.get('*/notification-deliveries', () => HttpResponse.json(deliveries)),
    http.get('*/notification-deliveries/:id/attempts', () => HttpResponse.json([
      { id: 'attempt-1', attemptNumber: 1, state: 'FAILED', dueAt: '2026-08-19T12:00:00Z', completedAt: '2026-08-19T12:01:00Z', errorCategory: 'TRANSIENT', errorCode: 'SMTP_TIMEOUT', errorMessage: 'Provider timed out' },
    ])),
    http.post('*/notification-rules', async ({ request }) => {
      const body = await request.json() as Record<string, unknown>;
      return HttpResponse.json({
        id: 'rule-new',
        ...body,
        createdAt: '2026-08-19T12:00:00Z',
        updatedAt: '2026-08-19T12:00:00Z',
      }, { status: 201 });
    }),
    http.patch('*/notification-rules/:id/disable', () => HttpResponse.json({
      ...mockRules[0],
      enabled: false,
    })),
    http.delete('*/notification-rules/:id', () => new HttpResponse(null, { status: 204 })),
  );

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });

  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <AuthProvider>
              <NotificationRulesPage />
            </AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('NotificationRulesPage', () => {
  it('renders notification rules list with table columns', async () => {
    renderPage();

    expect(await screen.findByText('Trip Delay Alert')).toBeInTheDocument();
    expect(screen.getByText('Notify dispatchers on trip delays')).toBeInTheDocument();
    expect(screen.getByText('TRIP_DELAY_RECORDED')).toBeInTheDocument();
    expect(screen.getByText('DISPATCHER')).toBeInTheDocument();
    expect(screen.getByText('Maintenance Email Warning')).toBeInTheDocument();
    expect(screen.getByText('fleet@example.com')).toBeInTheDocument();
  });

  it('renders access denied message when user lacks NOTIFICATION_RULE_VIEW', async () => {
    renderPage(['DASHBOARD_VIEW']);

    expect(await screen.findByText('Access Denied')).toBeInTheDocument();
    expect(screen.getByText(/You do not have the NOTIFICATION_RULE_VIEW permission/)).toBeInTheDocument();
  });

  it('hides create and edit actions when user has only NOTIFICATION_RULE_VIEW', async () => {
    renderPage(['NOTIFICATION_RULE_VIEW']);

    expect(await screen.findByText('Trip Delay Alert')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Create Notification Rule' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Edit Trip Delay Alert/i })).not.toBeInTheDocument();
  });

  it('opens create modal and creates a new rule', async () => {
    const user = userEvent.setup();
    renderPage();

    const createBtn = await screen.findByRole('button', { name: 'Create Notification Rule' });
    await user.click(createBtn);

    expect(await screen.findByText('Create Notification Rule', { selector: '.ant-modal-title' })).toBeInTheDocument();

    const nameInput = screen.getByLabelText('Rule Name');
    await user.type(nameInput, 'High Incident Alert');
    await user.type(screen.getByLabelText('Role Name'), 'OPERATIONS');
    expect(await screen.findByText('Trip {{tripNumber}} delayed')).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', { name: 'Create Rule' });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(screen.queryByText('Create Notification Rule', { selector: '.ant-modal-title' })).not.toBeInTheDocument();
    });
  });

  it('shows compact suppression, quiet-hours, and escalation policy summaries', async () => {
    renderPage();
    expect(await screen.findByText('Suppress 15 min')).toBeInTheDocument();
    expect(screen.getByText('Quiet MONDAY, TUESDAY 22:00:00-06:00:00')).toBeInTheDocument();
    expect(screen.getByText('Fallback ROLE OPERATIONS after 10 min')).toBeInTheDocument();
  });

  it('shows pending, sent, and terminal failed delivery health without manual retry', async () => {
    const user = userEvent.setup();
    renderPage(undefined, [
      { notificationId: 'delivery-1', eventId: 'event-1', eventType: 'TRIP_DELAY_RECORDED', channel: 'EMAIL', status: 'PENDING', attemptCount: 1, nextDeliveryAt: '2026-08-19T13:00:00Z', terminalFailure: false, escalationLevel: 0, createdAt: '2026-08-19T12:00:00Z', recipient: 'o***@example.com' },
      { notificationId: 'delivery-2', eventId: 'event-2', eventType: 'TRIP_DELAY_RECORDED', channel: 'EMAIL', status: 'SENT', attemptCount: 1, terminalFailure: false, escalationLevel: 0, createdAt: '2026-08-19T12:00:00Z', sentAt: '2026-08-19T12:01:00Z', recipient: 'o***@example.com' },
      { notificationId: 'delivery-3', eventId: 'event-3', eventType: 'TRIP_DELAY_RECORDED', channel: 'EMAIL', status: 'FAILED', attemptCount: 3, terminalFailure: true, escalationLevel: 1, parentNotificationId: 'parent-1', createdAt: '2026-08-19T12:00:00Z', recipient: 'o***@example.com' },
    ]);
    await user.click(await screen.findByRole('tab', { name: 'Delivery Diagnostics' }));
    expect(await screen.findByText('PENDING')).toBeInTheDocument();
    expect(screen.getByText('SENT')).toBeInTheDocument();
    expect(screen.getByText('Terminal')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument();
  });

  it('loads sanitized delivery attempt history for a rule manager', async () => {
    const user = userEvent.setup();
    renderPage(undefined, [{ notificationId: 'delivery-1', eventId: 'event-1', eventType: 'TRIP_DELAY_RECORDED', channel: 'EMAIL', status: 'FAILED', attemptCount: 1, terminalFailure: true, escalationLevel: 0, createdAt: '2026-08-19T12:00:00Z', recipient: 'o***@example.com' }]);
    await user.click(await screen.findByRole('tab', { name: 'Delivery Diagnostics' }));
    await user.click(await screen.findByRole('button', { name: /View attempts/ }));
    expect(await screen.findByText('Provider timed out')).toBeInTheDocument();
    expect(screen.getByText('TRANSIENT')).toBeInTheDocument();
  });

  it('toggles rule enable/disable status', async () => {
    const user = userEvent.setup();
    renderPage();

    const toggle = await screen.findByLabelText('Toggle rule Trip Delay Alert');
    expect(toggle).toBeInTheDocument();
    await user.click(toggle);

    await waitFor(() => {
      expect(screen.getByText('Trip Delay Alert')).toBeInTheDocument();
    });
  });
});
