import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { server } from '../test/server';
import { appTheme } from '../app/theme/theme';
import LifecycleActions from './LifecycleActions';
import type { Trip } from './types';

const baseTrip: Trip = {
  id: 'trip-1', tripNumber: 'TRIP-000123', customerId: 'customer-1',
  originLocationId: 'location-1', destinationLocationId: 'location-2',
  requestedStartTime: '2026-08-15T02:30:00Z', requestedEndTime: '2026-08-15T07:30:00Z',
  priority: 'HIGH', vehicleId: 'vehicle-1', driverId: 'driver-1', status: 'DRAFT',
  actualStartTime: '2026-08-15T02:45:00Z', startOdometerKm: 10500,
};

let request: { action: string; body: unknown } | undefined;

function renderActions(status: string, permission: string) {
  request = undefined;
  const trip = { ...baseTrip, status };
  server.use(http.post('*/trips/:tripId/:action', async ({ params, request: apiRequest }) => {
    request = { action: String(params.action), body: await apiRequest.json() };
    return HttpResponse.json(trip);
  }));
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <LifecycleActions trip={trip} hasPermission={(candidate) => candidate === permission} />
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('LifecycleActions', () => {
  it.each([
    ['DRAFT', 'TRIP_SUBMIT', 'Submit', 'Submit trip', 'submit'],
    ['SUBMITTED', 'TRIP_APPROVE', 'Approve', 'Approve trip', 'approve'],
    ['COMPLETED', 'TRIP_CLOSE', 'Close trip', 'Close trip', 'close'],
  ])('confirms the %s lifecycle action', async (status, permission, trigger, confirmation, endpoint) => {
    const user = userEvent.setup();
    renderActions(status, permission);

    await user.click(screen.getByRole('button', { name: trigger }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: confirmation }));

    await waitFor(() => expect(request).toEqual({ action: endpoint, body: {} }));
  });

  it.each([
    ['SUBMITTED', 'TRIP_REJECT', 'Reject', 'Reject trip', 'reject', 'Duplicate request'],
    ['ASSIGNED', 'TRIP_CANCEL', 'Cancel trip', 'Cancel trip', 'cancel', 'Customer cancelled'],
  ])('requires a reason for %s actions', async (status, permission, trigger, confirmation, endpoint, reason) => {
    const user = userEvent.setup();
    renderActions(status, permission);

    await user.click(screen.getByRole('button', { name: trigger }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: confirmation }));
    expect(await within(dialog).findByText('A reason is required')).toBeInTheDocument();
    await user.type(within(dialog).getByLabelText(/reason/i), reason);
    await user.click(within(dialog).getByRole('button', { name: confirmation }));

    await waitFor(() => expect(request).toEqual({ action: endpoint, body: { reason } }));
  });

  it('shows dispatch readiness and submits optional remarks', async () => {
    const user = userEvent.setup();
    renderActions('ASSIGNED', 'TRIP_DISPATCH');

    await user.click(screen.getByRole('button', { name: 'Dispatch' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('Dispatch performs a fresh backend readiness check')).toBeInTheDocument();
    expect(within(dialog).getAllByText('Assigned').length).toBeGreaterThanOrEqual(2);
    await user.type(within(dialog).getByLabelText('Dispatch remarks'), 'Ready at gate');
    await user.click(within(dialog).getByRole('button', { name: 'Dispatch trip' }));

    await waitFor(() => expect(request).toEqual({ action: 'dispatch', body: { remarks: 'Ready at gate' } }));
  });

  it('collects the start odometer while leaving the start timestamp to the server', async () => {
    const user = userEvent.setup();
    renderActions('DISPATCHED', 'TRIP_START');

    await user.click(screen.getByRole('button', { name: 'Start trip' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('The server records the authoritative actual start time')).toBeInTheDocument();
    await user.type(within(dialog).getByRole('spinbutton', { name: 'Start odometer (km)' }), '10600');
    await user.click(within(dialog).getByRole('button', { name: 'Start trip' }));

    await waitFor(() => expect(request).toEqual({ action: 'start', body: { startOdometerKm: 10600 } }));
  });

  it('collects the completion odometer and remarks', async () => {
    const user = userEvent.setup();
    renderActions('IN_PROGRESS', 'TRIP_COMPLETE');

    await user.click(screen.getByRole('button', { name: 'Complete trip' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('The server records the authoritative actual completion time')).toBeInTheDocument();
    await user.type(within(dialog).getByRole('spinbutton', { name: 'End odometer (km)' }), '10720');
    await user.type(within(dialog).getByLabelText('Completion remarks'), 'Delivered intact');
    await user.click(within(dialog).getByRole('button', { name: 'Complete trip' }));

    await waitFor(() => expect(request).toEqual({ action: 'complete', body: { endOdometerKm: 10720, completionRemarks: 'Delivered intact' } }));
  });

  it('invalidates vehicle reading queries on successful trip start', async () => {
    const user = userEvent.setup();
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    const trip = { ...baseTrip, status: 'DISPATCHED', vehicleId: 'veh-123' };

    server.use(http.post('*/trips/:tripId/start', () => HttpResponse.json({ ...trip, status: 'IN_PROGRESS' })));

    render(
      <ConfigProvider theme={appTheme}>
        <AntApp>
          <QueryClientProvider client={queryClient}>
            <LifecycleActions trip={trip} hasPermission={(p) => p === 'TRIP_START'} />
          </QueryClientProvider>
        </AntApp>
      </ConfigProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'Start trip' }));
    const dialog = screen.getByRole('dialog');
    await user.type(within(dialog).getByRole('spinbutton', { name: 'Start odometer (km)' }), '10600');
    await user.click(within(dialog).getByRole('button', { name: 'Start trip' }));

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['vehicle', 'veh-123', 'readings'] }));
      expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['vehicle', 'veh-123', 'readings', 'latest'] }));
    });
  });

  it('renders backend chronology conflict error when start reading is rejected', async () => {
    const user = userEvent.setup();
    const trip = { ...baseTrip, status: 'DISPATCHED' };

    server.use(http.post('*/trips/:tripId/start', () =>
      HttpResponse.json({
        code: 'VEHICLE_READING_CHRONOLOGY_CONFLICT',
        message: 'Recorded odometer conflicts with a later vehicle reading.',
      }, { status: 409 }),
    ));

    render(
      <ConfigProvider theme={appTheme}>
        <AntApp>
          <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <LifecycleActions trip={trip} hasPermission={(p) => p === 'TRIP_START'} />
          </QueryClientProvider>
        </AntApp>
      </ConfigProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'Start trip' }));
    const dialog = screen.getByRole('dialog');
    await user.type(within(dialog).getByRole('spinbutton', { name: 'Start odometer (km)' }), '9500');
    await user.click(within(dialog).getByRole('button', { name: 'Start trip' }));

    expect(await within(dialog).findByText(/Recorded odometer conflicts with a later vehicle reading/i)).toBeInTheDocument();
  });

  it('renders backend decrease error when complete reading is lower than previous', async () => {
    const user = userEvent.setup();
    const trip = { ...baseTrip, status: 'IN_PROGRESS' };

    server.use(http.post('*/trips/:tripId/complete', () =>
      HttpResponse.json({
        code: 'VEHICLE_READING_DECREASE',
        message: 'Reading is below the previous value; use the approved meter-reset workflow if the meter changed',
      }, { status: 409 }),
    ));

    render(
      <ConfigProvider theme={appTheme}>
        <AntApp>
          <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <LifecycleActions trip={trip} hasPermission={(p) => p === 'TRIP_COMPLETE'} />
          </QueryClientProvider>
        </AntApp>
      </ConfigProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'Complete trip' }));
    const dialog = screen.getByRole('dialog');
    await user.type(within(dialog).getByRole('spinbutton', { name: 'End odometer (km)' }), '10400');
    await user.click(within(dialog).getByRole('button', { name: 'Complete trip' }));

    expect(await within(dialog).findByText(/Reading is below the previous value/i)).toBeInTheDocument();
  });
});
