import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../test/server';
import { AuthProvider } from '../auth/AuthContext';
import TripOperationalEventsSection from './TripOperationalEventsSection';
import type { Trip } from './types';

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
  it('renders existing operational events timeline with badges and tags', async () => {
    server.use(
      http.get('*/auth/me', () =>
        HttpResponse.json({
          id: 'user-1',
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
          id: 'user-1',
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
    let createdPayload: unknown;

    server.use(
      http.get('*/auth/me', () =>
        HttpResponse.json({
          id: 'user-1',
          username: 'dispatcher.john',
          roles: ['OPERATIONS'],
          permissions: ['TRIP_VIEW', 'TRIP_LOG_VIEW', 'TRIP_LOG_MANAGE'],
        })
      ),
      http.get(`*/trips/${activeTrip.id}/operational-events`, () =>
        HttpResponse.json([])
      ),
      http.post(`*/trips/${activeTrip.id}/checkpoints`, async ({ request }) => {
        createdPayload = await request.json();
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

    await waitFor(() => expect(createdPayload).toBeDefined());
  });
});
