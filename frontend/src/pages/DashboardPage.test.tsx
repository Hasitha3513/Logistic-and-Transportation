import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { server } from '../test/server';
import { appTheme } from '../app/theme/theme';
import DashboardPage from './DashboardPage';

function renderDashboard(response: object) {
  server.use(http.get('*/dashboard/operations', () => HttpResponse.json(response)));
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}><DashboardPage /></QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('DashboardPage', () => {
  it('displays the current placeholder response honestly without invented metrics', async () => {
    renderDashboard({ date: '2026-08-15', status: 'READY' });

    expect(await screen.findByText('Operations Overview')).toBeInTheDocument();
    expect(screen.getByText('2026-08-15')).toBeInTheDocument();
    expect(screen.getByText('READY')).toBeInTheDocument();
    expect(screen.getAllByText('Not supplied by reporting API').length).toBeGreaterThan(0);
    expect(screen.queryByText('42')).not.toBeInTheDocument();
  });

  it('renders metrics, progress, and alerts exactly when supplied by reporting', async () => {
    renderDashboard({
      date: '2026-08-15', status: 'READY',
      vehicles: { available: 12, allocated: 8, maintenance: 2, outOfService: 1, availabilityPercent: 52 },
      drivers: { available: 9, assigned: 7, availabilityPercent: 56 },
      trips: { draft: 3, pendingApproval: 2, approved: 4, assigned: 5, dispatched: 1, inProgress: 6, completed: 11, completionPercent: 34 },
      alerts: {
        expiringDocuments: [{ id: 'alert-1', title: 'Insurance expires', detail: 'WP-CAB-1234', severity: 'WARNING', dueDate: '2026-08-20' }],
        criticalExceptions: [{ id: 'alert-2', title: 'Trip interrupted', detail: 'TRIP-000123', severity: 'CRITICAL' }],
      },
    });

    expect(await screen.findByText('Insurance expires')).toBeInTheDocument();
    expect(screen.getByText('Trip interrupted')).toBeInTheDocument();
    expect(screen.getByText('52%')).toBeInTheDocument();
    expect(screen.getByText('34%')).toBeInTheDocument();
    expect(screen.getByText('11')).toBeInTheDocument();
  });
});
