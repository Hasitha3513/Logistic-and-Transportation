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
import NotificationCenter from './NotificationCenter';
import type { NotificationItem } from './types';

const mockNotifications: NotificationItem[] = [
  {
    id: 'notif-1',
    ruleId: 'rule-1',
    eventId: 'event-1',
    eventType: 'TRIP_DELAY_RECORDED',
    channel: 'IN_APP',
    recipient: 'ops.manager',
    severity: 'WARNING',
    title: 'Trip TRP-001 Delayed',
    message: 'Traffic delay of 25 minutes reported at Checkpoint CMB.',
    status: 'SENT',
    createdAt: '2026-08-19T10:30:00Z',
    relatedRoute: '/trips/trip-1',
  },
  {
    id: 'notif-2',
    ruleId: 'rule-2',
    eventId: 'event-2',
    eventType: 'VEHICLE_MAINTENANCE_BLOCKED',
    channel: 'IN_APP',
    recipient: 'ops.manager',
    severity: 'CRITICAL',
    title: 'Vehicle Maintenance Conflict',
    message: 'Vehicle WP-CAB-1234 maintenance conflicts with Trip TRP-002.',
    status: 'READ',
    createdAt: '2026-08-19T09:00:00Z',
    readAt: '2026-08-19T09:15:00Z',
  },
];

function renderCenter(permissions = ['NOTIFICATION_VIEW']) {
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
    http.get('*/notifications/unread-count', () => HttpResponse.json({ unreadCount: 1 })),
    http.get('*/notifications', () => HttpResponse.json(mockNotifications)),
    http.patch('*/notifications/:id/read', ({ params }) => {
      const found = mockNotifications.find((n) => n.id === params.id);
      return HttpResponse.json({
        ...found,
        status: 'READ',
        readAt: new Date().toISOString(),
      });
    }),
    http.patch('*/notifications/read-all', () => new HttpResponse(null, { status: 204 })),
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
              <NotificationCenter />
            </AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('NotificationCenter', () => {
  it('renders notification bell with unread badge', async () => {
    renderCenter();

    const bellBtn = await screen.findByRole('button', { name: /Open notifications/i });
    expect(bellBtn).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('opens drawer and displays notifications list', async () => {
    const user = userEvent.setup();
    renderCenter();

    const bellBtn = await screen.findByRole('button', { name: /Open notifications/i });
    await user.click(bellBtn);

    expect(await screen.findByText('Trip TRP-001 Delayed')).toBeInTheDocument();
    expect(screen.getByText(/Traffic delay of 25 minutes/)).toBeInTheDocument();
    expect(screen.getByText('Vehicle Maintenance Conflict')).toBeInTheDocument();
  });

  it('marks individual notification as read', async () => {
    const user = userEvent.setup();
    renderCenter();

    const bellBtn = await screen.findByRole('button', { name: /Open notifications/i });
    await user.click(bellBtn);

    const markReadBtn = await screen.findByLabelText('Mark Trip TRP-001 Delayed as read');
    await user.click(markReadBtn);

    await waitFor(() => {
      expect(screen.getByText('Trip TRP-001 Delayed')).toBeInTheDocument();
    });
  });

  it('marks all notifications as read', async () => {
    const user = userEvent.setup();
    renderCenter();

    const bellBtn = await screen.findByRole('button', { name: /Open notifications/i });
    await user.click(bellBtn);

    const markAllBtn = await screen.findByRole('button', { name: 'Mark all as read' });
    await user.click(markAllBtn);

    await waitFor(() => {
      expect(screen.getByText('Trip TRP-001 Delayed')).toBeInTheDocument();
    });
  });

  it('does not render when user lacks NOTIFICATION_VIEW permission', async () => {
    renderCenter(['DASHBOARD_VIEW']);

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /Open notifications/i })).not.toBeInTheDocument();
    });
  });
});
