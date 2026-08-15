import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { appTheme } from '../app/theme/theme';
import { AuthProvider } from '../auth/AuthContext';
import { server } from '../test/server';

const baseIssue = {
  id: 'fuel-1', voucherNumber: 'FUEL-2026-000001', vehicle: { id: 'vehicle-1' }, trip: { id: 'trip-1' },
  driver: { id: 'driver-1' }, fuelType: 'DIESEL', quantity: 40, unitPrice: 320, totalAmount: 12800,
  station: { id: 'station-1', code: 'FUEL-CMB', name: 'Colombo Hub Fuel Point', stationType: 'INTERNAL', active: true },
  odometer: 15100, issueDateTime: '2026-08-15T06:00:00Z', status: 'DRAFT', requestedBy: 'user-1',
  createdAt: '2026-08-15T06:00:00Z', updatedAt: '2026-08-15T06:00:00Z',
};

function handlers(permissions: string[], issue = baseIssue) {
  server.use(
    http.get('*/auth/me', () => HttpResponse.json({ id: 'user-1', username: 'fuel.operator', firstName: 'Fuel', lastName: 'Operator', active: true, roles: ['FUEL'], permissions })),
    http.get('*/fuel-issues', () => HttpResponse.json({ content: [issue], page: 0, limit: 10, totalElements: 1, totalPages: 1 })),
    http.get('*/fuel-issues/:id', () => HttpResponse.json(issue)),
    http.get('*/fuel-issues/:id/history', () => HttpResponse.json([{ id: 'history-1', fuelIssueId: 'fuel-1', fromStatus: null, toStatus: issue.status, action: 'CREATE', actorId: 'user-1', actor: 'fuel.operator', occurredAt: issue.createdAt }])),
    http.get('*/fuel-stations', () => HttpResponse.json([issue.station])),
    http.get('*/vehicles', () => HttpResponse.json([{ id: 'vehicle-1', registrationNumber: 'WP-CAB-1234' }])),
    http.get('*/trips', () => HttpResponse.json([{ id: 'trip-1', tripNumber: 'TRIP-000123', status: 'ASSIGNED', vehicleId: 'vehicle-1', driverId: 'driver-1' }])),
  );
}

function renderAt(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<ConfigProvider theme={appTheme}><AntApp><QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><AuthProvider><App /></AuthProvider></MemoryRouter></QueryClientProvider></AntApp></ConfigProvider>);
}

describe('Fuel Issue MVP pages', () => {
  it('loads the server-paginated fuel issue list', async () => {
    handlers(['FUEL_ISSUE_VIEW']);
    renderAt('/fuel/issues');
    expect(await screen.findByText('FUEL-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('Colombo Hub Fuel Point')).toBeInTheDocument();
    expect(screen.getByText('Draft')).toBeInTheDocument();
  });

  it('validates positive quantity on the create form', async () => {
    handlers(['FUEL_ISSUE_VIEW', 'FUEL_ISSUE_CREATE']);
    const user = userEvent.setup(); renderAt('/fuel/issues/new');
    await screen.findByRole('heading', { name: 'Create fuel issue' });
    await user.click(screen.getByRole('button', { name: 'Save draft' }));
    expect(await screen.findByText('Quantity must be greater than zero')).toBeInTheDocument();
  });

  it('selects a trip, adopts its assignment, and creates a draft', async () => {
    let payload: Record<string, unknown> | undefined;
    handlers(['FUEL_ISSUE_VIEW', 'FUEL_ISSUE_CREATE']);
    server.use(http.post('*/fuel-issues', async ({ request }) => { payload = await request.json() as Record<string, unknown>; return HttpResponse.json(baseIssue, { status: 201 }); }));
    const user = userEvent.setup(); renderAt('/fuel/issues/new');
    await screen.findByRole('heading', { name: 'Create fuel issue' });
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]); await user.click(await screen.findByText('WP-CAB-1234'));
    await user.click(selects[1]); await user.click(await screen.findByText('TRIP-000123'));
    await user.click(selects[2]); await user.click(await screen.findByText(/FUEL-CMB/));
    const quantity = screen.getAllByRole('spinbutton')[0];
    await user.clear(quantity);
    await user.type(quantity, '40');
    await user.click(screen.getByRole('button', { name: 'Save draft' }));
    await waitFor(() => expect(payload).toMatchObject({ vehicleId: 'vehicle-1', tripId: 'trip-1', driverId: 'driver-1', quantity: 40 }));
  }, 15_000);

  it('does not expose authorization to an actor without its permission', async () => {
    handlers(['FUEL_ISSUE_VIEW'], { ...baseIssue, status: 'PENDING_AUTHORIZATION' });
    renderAt('/fuel/issues/fuel-1');
    await screen.findAllByText('FUEL-2026-000001');
    expect(screen.queryByRole('button', { name: 'Authorize' })).not.toBeInTheDocument();
  });

  it('submits a draft only after confirmation', async () => {
    let submitted = false;
    handlers(['FUEL_ISSUE_VIEW', 'FUEL_ISSUE_SUBMIT']);
    server.use(http.post('*/fuel-issues/:id/submit', () => { submitted = true; return HttpResponse.json({ ...baseIssue, status: 'PENDING_AUTHORIZATION' }); }));
    const user = userEvent.setup(); renderAt('/fuel/issues/fuel-1');
    await user.click(await screen.findByRole('button', { name: /Submit/ }));
    expect(submitted).toBe(false);
    await user.click(screen.getByRole('button', { name: 'OK' }));
    await waitFor(() => expect(submitted).toBe(true));
  });

  it('shows backend business rejection without mutating client-side eligibility', async () => {
    handlers(['FUEL_ISSUE_VIEW', 'FUEL_ISSUE_AUTHORIZE'], { ...baseIssue, status: 'PENDING_AUTHORIZATION' });
    server.use(http.post('*/fuel-issues/:id/authorize', () => HttpResponse.json({ code: 'FUEL_LIMIT_EXCEEDED', message: 'Configured fuel limit exceeded' }, { status: 409 })));
    const user = userEvent.setup(); renderAt('/fuel/issues/fuel-1');
    await user.click(await screen.findByRole('button', { name: /Authorize/ }));
    await user.click(screen.getByRole('button', { name: 'OK' }));
    expect(await screen.findByText('Configured fuel limit exceeded')).toBeInTheDocument();
  });

  it('records an authorized issue after confirmation', async () => {
    let issued = false;
    handlers(['FUEL_ISSUE_VIEW', 'FUEL_ISSUE_ISSUE'], { ...baseIssue, status: 'AUTHORIZED' });
    server.use(http.post('*/fuel-issues/:id/issue', () => { issued = true; return HttpResponse.json({ ...baseIssue, status: 'ISSUED' }); }));
    const user = userEvent.setup(); renderAt('/fuel/issues/fuel-1');
    await user.click(await screen.findByRole('button', { name: /Record issue/ }));
    await user.click(screen.getByRole('button', { name: 'OK' }));
    await waitFor(() => expect(issued).toBe(true));
  });

  it('requires a cancellation reason before enabling cancellation', async () => {
    handlers(['FUEL_ISSUE_VIEW', 'FUEL_ISSUE_CANCEL']);
    const user = userEvent.setup(); renderAt('/fuel/issues/fuel-1');
    await user.click(await screen.findByRole('button', { name: /Cancel$/ }));
    expect(screen.getByRole('button', { name: 'Cancel fuel issue' })).toBeDisabled();
    await user.type(screen.getByLabelText('Cancellation reason'), 'Entered in error');
    expect(screen.getByRole('button', { name: 'Cancel fuel issue' })).toBeEnabled();
  });

  it('renders issued records read-only', async () => {
    handlers(['FUEL_ISSUE_VIEW', 'FUEL_ISSUE_UPDATE', 'FUEL_ISSUE_CANCEL'], { ...baseIssue, status: 'ISSUED' });
    renderAt('/fuel/issues/fuel-1');
    expect(await screen.findByText('This operational record is read-only and retained for audit.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();
  });

  it('guards fuel routes when view permission is absent', async () => {
    handlers([]);
    renderAt('/fuel/issues');
    expect(await screen.findByText('Select an available module from the navigation.')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Fuel issues' })).not.toBeInTheDocument();
  });
});
