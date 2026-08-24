import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { server } from '../test/server';
import { AuthProvider } from '../auth/AuthContext';
import TripOperationalEventsSection from './TripOperationalEventsSection';
import type { Trip } from './types';

const offline = vi.hoisted(() => ({
  enqueue: vi.fn(),
  syncNow: vi.fn(() => Promise.resolve()),
  register: vi.fn<(type: string, callback: unknown) => () => void>(() => () => undefined),
  getForAggregate: vi.fn<() => Promise<unknown[]>>(() => Promise.resolve([])),
}));

vi.mock('../features/offlineSync/OfflineSyncProvider', () => ({
  useOfflineSync: () => ({
    enqueueOperation: offline.enqueue,
    syncNow: offline.syncNow,
    registerPostApply: offline.register,
    getOperationsForAggregate: offline.getForAggregate,
    operationsRevision: 0,
  }),
}));

const activeTrip: Trip = {
  id: '34000000-0000-0000-0000-000000000001',
  tripNumber: 'TRP-2026-0099',
  originLocationId: 'loc-1',
  destinationLocationId: 'loc-2',
  requestedStartTime: '2026-08-19T08:00:00Z',
  requestedEndTime: '2026-08-19T14:00:00Z',
  priority: 'HIGH',
  status: 'IN_PROGRESS',
  vehicleId: 'veh-1',
  driverId: 'drv-1',
};

const sampleEvents = [
  {
    id: 'evt-1',
    tripId: activeTrip.id,
    eventType: 'CHECKPOINT',
    occurredAt: '2026-08-19T08:15:00Z',
    locationDescription: 'Main Gate Checkpoint',
    checkpointType: 'DEPARTURE',
    remarks: 'Dispatched on schedule',
    recordedBy: 'dispatcher.john',
    createdAt: '2026-08-19T08:15:00Z',
    updatedAt: '2026-08-19T08:15:00Z',
  },
  {
    id: 'evt-2',
    tripId: activeTrip.id,
    eventType: 'DELAY',
    occurredAt: '2026-08-19T09:30:00Z',
    locationDescription: 'Highway Interchange 3',
    delayMinutes: 25,
    reason: 'Traffic congestion due to road maintenance',
    remarks: 'Resumed transit',
    recordedBy: 'driver.sam',
    createdAt: '2026-08-19T09:30:00Z',
    updatedAt: '2026-08-19T09:30:00Z',
  },
  {
    id: 'evt-3',
    tripId: activeTrip.id,
    eventType: 'INCIDENT',
    occurredAt: '2026-08-19T10:45:00Z',
    locationDescription: 'Rest Area B',
    incidentSeverity: 'MEDIUM',
    reason: 'Punctured rear tire',
    remarks: 'Replaced with onboard spare tire in 20 minutes',
    recordedBy: 'driver.sam',
    createdAt: '2026-08-19T10:45:00Z',
    updatedAt: '2026-08-19T10:45:00Z',
  },
];

function renderSection(trip: Trip = activeTrip) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ConfigProvider>
        <AntApp>
          <AuthProvider>
            <TripOperationalEventsSection trip={trip} />
          </AuthProvider>
        </AntApp>
      </ConfigProvider>
    </QueryClientProvider>
  );
}

describe('TripOperationalEventsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    offline.enqueue.mockResolvedValue({ operationId: 'operation-new', status: 'PENDING' });
    offline.getForAggregate.mockResolvedValue([]);
  });

  it('renders existing operational events timeline with badges and tags', async () => {
    server.use(
      http.get('*/auth/me', () =>
        HttpResponse.json({
          id: '11000000-0000-4000-8000-000000000001',
          username: 'dispatcher.john',
          roles: ['OPERATIONS'],
          permissions: ['TRIP_VIEW', 'TRIP_LOG_VIEW', 'TRIP_LOG_MANAGE'],
        })
      ),
      http.get(`*/trips/${activeTrip.id}/operational-events`, () =>
        HttpResponse.json(sampleEvents)
      )
    );

    renderSection();

    expect(await screen.findByText('En-Route Checkpoints & Operational Events')).toBeInTheDocument();
    expect(await screen.findByText('Departure')).toBeInTheDocument();
    expect(await screen.findByText('Main Gate Checkpoint')).toBeInTheDocument();
    expect(await screen.findByText('Delay: 25 mins')).toBeInTheDocument();
    expect(await screen.findByText('Traffic congestion due to road maintenance')).toBeInTheDocument();
    expect(await screen.findByText('Incident: Medium')).toBeInTheDocument();
    expect(await screen.findByText('Punctured rear tire')).toBeInTheDocument();
  });

  it('renders empty state when no events exist', async () => {
    server.use(
      http.get('*/auth/me', () =>
        HttpResponse.json({
          id: '11000000-0000-4000-8000-000000000001',
          username: 'dispatcher.john',
          roles: ['OPERATIONS'],
          permissions: ['TRIP_VIEW', 'TRIP_LOG_VIEW'],
        })
      ),
      http.get(`*/trips/${activeTrip.id}/operational-events`, () =>
        HttpResponse.json([])
      )
    );

    renderSection();

    expect(await screen.findByText('En-Route Checkpoints & Operational Events')).toBeInTheDocument();
    expect(
      await screen.findByText('No en-route checkpoints, delays, or incidents have been recorded yet.')
    ).toBeInTheDocument();
  });

  it('allows recording a new checkpoint', async () => {
    const user = userEvent.setup();
    let directPostCount = 0;

    server.use(
      http.get('*/auth/me', () =>
        HttpResponse.json({
          id: '11000000-0000-4000-8000-000000000001',
          username: 'dispatcher.john',
          roles: ['OPERATIONS'],
          permissions: ['TRIP_VIEW', 'TRIP_LOG_VIEW', 'TRIP_LOG_MANAGE'],
        })
      ),
      http.get(`*/trips/${activeTrip.id}/operational-events`, () =>
        HttpResponse.json([])
      ),
      http.post(`*/trips/${activeTrip.id}/checkpoints`, async ({ request }) => {
        directPostCount += 1;
        await request.json();
        return HttpResponse.json({
          id: 'evt-new',
          tripId: activeTrip.id,
          eventType: 'CHECKPOINT',
          checkpointType: 'PICKUP',
          locationDescription: 'Dock 4',
          occurredAt: new Date().toISOString(),
          recordedBy: 'dispatcher.john',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }, { status: 201 });
      })
    );

    renderSection();

    const recordBtn = await screen.findByRole('button', { name: /Record Checkpoint/i });
    await user.click(recordBtn);

    expect(await screen.findByText('Record En-Route Checkpoint')).toBeInTheDocument();

    const locationInput = screen.getByPlaceholderText(/e.g. Colombo Port Gate 4/i);
    await user.type(locationInput, 'Dock 4');

    const select = screen.getByLabelText('Checkpoint Type');
    await user.click(select);
    const option = await screen.findByText('Pickup Point');
    await user.click(option);

    const submitBtn = screen.getByRole('button', { name: 'Record Checkpoint' });
    await user.click(submitBtn);

    await waitFor(() => expect(offline.enqueue).toHaveBeenCalledTimes(1));
    expect(offline.enqueue).toHaveBeenCalledWith(expect.objectContaining({
      ownerUserId: '11000000-0000-4000-8000-000000000001',
      operationType: 'TRIP_CHECKPOINT_RECORD',
      aggregateType: 'TRIP',
      aggregateId: activeTrip.id,
      payload: expect.objectContaining({ checkpointType: 'PICKUP', locationDescription: 'Dock 4' }),
    }));
    expect(offline.syncNow).toHaveBeenCalled();
    expect(directPostCount).toBe(0);
  });

  it('shows pending and terminal local operations while hiding reconciled entries', async () => {
    offline.getForAggregate.mockResolvedValue([
      {
        operationId: 'pending-checkpoint', operationType: 'TRIP_CHECKPOINT_RECORD',
        aggregateType: 'TRIP', aggregateId: activeTrip.id, status: 'PENDING',
        payload: { checkpointType: 'DELIVERY', occurredAt: '2026-08-19T12:00:00Z' },
      },
      {
        operationId: 'conflict-delay', operationType: 'TRIP_DELAY_RECORD',
        aggregateType: 'TRIP', aggregateId: activeTrip.id, status: 'CONFLICT',
        payload: { delayMinutes: 15, reason: 'Road closed', occurredAt: '2026-08-19T12:10:00Z' },
        lastErrorMessage: 'Trip status no longer accepts events',
      },
      {
        operationId: 'failed-incident', operationType: 'TRIP_INCIDENT_RECORD',
        aggregateType: 'TRIP', aggregateId: activeTrip.id, status: 'FAILED',
        payload: { incidentSeverity: 'HIGH', description: 'Engine fault', occurredAt: '2026-08-19T12:20:00Z' },
        lastErrorMessage: 'Incident could not be synchronized',
      },
      {
        operationId: 'synced-hidden', operationType: 'TRIP_CHECKPOINT_RECORD',
        aggregateType: 'TRIP', aggregateId: activeTrip.id, status: 'SYNCED',
        payload: { checkpointType: 'ARRIVAL', occurredAt: '2026-08-19T13:00:00Z' },
      },
    ]);
    server.use(
      http.get('*/auth/me', () => HttpResponse.json({
        id: '11000000-0000-4000-8000-000000000001', username: 'dispatcher.john',
        roles: ['OPERATIONS'], permissions: ['TRIP_VIEW', 'TRIP_LOG_VIEW'],
      })),
      http.get(`*/trips/${activeTrip.id}/operational-events`, () => HttpResponse.json([])),
    );

    renderSection();

    expect(await screen.findByText('Pending sync')).toBeInTheDocument();
    expect(await screen.findByText('Trip status no longer accepts events')).toBeInTheDocument();
    expect(await screen.findByText('Incident could not be synchronized')).toBeInTheDocument();
    expect(screen.queryByText('Arrival')).not.toBeInTheDocument();
    expect(offline.getForAggregate).toHaveBeenCalledWith('TRIP', activeTrip.id);
  });
});
