import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { appTheme } from '../app/theme/theme';
import { AuthProvider } from '../auth/AuthContext';
import { server } from '../test/server';
import VehicleReadingsSection from './VehicleReadingsSection';

const vehicleId = 'v-123';

const sampleReadings = [
  {
    id: 'r-1',
    vehicleId,
    readingType: 'ODOMETER',
    value: 10000,
    unit: 'KILOMETER',
    meterEpoch: 0,
    sourceType: 'BASELINE',
    sourceReferenceId: null,
    recordedAt: '2026-08-10T00:00:00Z',
    receivedAt: '2026-08-10T00:00:00Z',
    createdBy: 'u-1',
    correctionOfReadingId: null,
    correctionReason: null,
    status: 'ACTIVE',
  },
  {
    id: 'r-2',
    vehicleId,
    readingType: 'ODOMETER',
    value: 10500,
    unit: 'KILOMETER',
    meterEpoch: 0,
    sourceType: 'MANUAL',
    sourceReferenceId: null,
    recordedAt: '2026-08-11T00:00:00Z',
    receivedAt: '2026-08-11T00:00:00Z',
    createdBy: 'u-1',
    correctionOfReadingId: null,
    correctionReason: null,
    status: 'CORRECTED',
  },
  {
    id: 'r-3',
    vehicleId,
    readingType: 'ODOMETER',
    value: 10100,
    unit: 'KILOMETER',
    meterEpoch: 0,
    sourceType: 'MANUAL',
    sourceReferenceId: null,
    recordedAt: '2026-08-11T00:00:00Z',
    receivedAt: '2026-08-11T00:00:00Z',
    createdBy: 'u-1',
    correctionOfReadingId: 'r-2',
    correctionReason: 'Fixed typo in log',
    status: 'CORRECTION',
  },
];

const sampleLatest = {
  vehicleId,
  odometer: {
    id: 'r-3',
    vehicleId,
    readingType: 'ODOMETER',
    value: 10100,
    unit: 'KILOMETER',
    meterEpoch: 0,
    sourceType: 'MANUAL',
    recordedAt: '2026-08-11T00:00:00Z',
  },
  engineHours: {
    id: 'r-4',
    vehicleId,
    readingType: 'ENGINE_HOURS',
    value: 120.5,
    unit: 'HOUR',
    meterEpoch: 0,
    sourceType: 'BASELINE',
    recordedAt: '2026-08-10T00:00:00Z',
  },
};

const sampleResets = [
  {
    id: 'reset-1',
    vehicleId,
    readingType: 'ODOMETER',
    previousReadingId: 'r-3',
    previousMeterValue: 10100,
    newReadingId: 'r-5',
    newMeterValue: 0,
    effectiveAt: '2026-08-12T00:00:00Z',
    reason: 'Speedometer cluster replacement',
    createdBy: 'u-1',
    approvedBy: 'u-1',
    notes: 'Warranty repair',
    createdAt: '2026-08-12T00:00:00Z',
  },
];

const sampleMileageSummary = {
  vehicleId,
  from: '2026-07-17T00:00:00Z',
  to: '2026-08-16T23:59:59Z',
  openingOdometer: 10000,
  closingOdometer: 10500,
  distanceKm: 500,
  openingEngineHours: 100,
  closingEngineHours: 125,
  engineHoursUsed: 25,
  readingCount: 5,
  correctionCount: 1,
  meterResetCount: 0,
  sourceCounts: {
    BASELINE: 1,
    MANUAL: 2,
    TRIP_START: 1,
    TRIP_END: 1,
  },
  coverageStatus: 'COMPLETE',
  coverageReason: null,
};

function setupMocks(permissions: string[], mileageOverride?: Partial<typeof sampleMileageSummary>) {
  server.use(
    http.get('*/auth/me', () =>
      HttpResponse.json({
        id: 'u-1',
        username: 'fleet.admin',
        firstName: 'Fleet',
        lastName: 'Admin',
        active: true,
        roles: ['FLEET_MANAGER'],
        permissions,
      })
    ),
    http.get('*/vehicles/:vehicleId/readings', () =>
      HttpResponse.json({
        content: sampleReadings,
        page: 0,
        limit: 50,
        totalElements: sampleReadings.length,
        totalPages: 1,
      })
    ),
    http.get('*/vehicles/:vehicleId/readings/latest', () =>
      HttpResponse.json(sampleLatest)
    ),
    http.get('*/vehicles/:vehicleId/meter-resets', () =>
      HttpResponse.json(sampleResets)
    ),
    http.get('*/vehicles/:vehicleId/mileage-summary', () =>
      HttpResponse.json({ ...sampleMileageSummary, ...mileageOverride })
    )
  );
}

function renderSection() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider theme={appTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <AuthProvider>
              <VehicleReadingsSection vehicleId={vehicleId} vehicleRegistration="WP-CAB-1234" />
            </AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  );
}

describe('VehicleReadingsSection', () => {
  it('renders authoritative metrics and readings ledger with status tags', async () => {
    setupMocks(['VEHICLE_READING_VIEW']);
    renderSection();

    expect(await screen.findByText('Vehicle Readings, Mileage & Utilization')).toBeInTheDocument();
    expect(screen.getByText('Authoritative Odometer')).toBeInTheDocument();
    expect(screen.getByText('Authoritative Engine Hours')).toBeInTheDocument();

    // Table rows and status badges
    expect(await screen.findByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Corrected')).toBeInTheDocument();
    expect(screen.getByText('Correction')).toBeInTheDocument();
    expect(screen.getByText(/Fixed typo in log/)).toBeInTheDocument();
  });

  it('renders period mileage and utilization summary tab with complete coverage', async () => {
    setupMocks(['VEHICLE_READING_VIEW']);
    const user = userEvent.setup();
    renderSection();

    const mileageTab = await screen.findByText('Period Mileage & Utilization');
    await user.click(mileageTab);

    expect(await screen.findByText('Complete Coverage')).toBeInTheDocument();
    expect(screen.getByText('Distance Traveled')).toBeInTheDocument();
    expect(screen.getByText('Engine Hours Used')).toBeInTheDocument();
    expect(screen.getByText('Readings in Period')).toBeInTheDocument();
    expect(screen.getByText('Reading Sources Breakdown')).toBeInTheDocument();
  });

  it('renders partial coverage warning when coverage status is PARTIAL', async () => {
    setupMocks(['VEHICLE_READING_VIEW'], {
      coverageStatus: 'PARTIAL',
      coverageReason: 'No opening reading prior to period start',
    });
    const user = userEvent.setup();
    renderSection();

    const mileageTab = await screen.findByText('Period Mileage & Utilization');
    await user.click(mileageTab);

    expect(await screen.findByText('Partial Coverage')).toBeInTheDocument();
    expect(screen.getByText('Partial Period Coverage')).toBeInTheDocument();
    expect(screen.getByText(/No opening reading prior to period start/)).toBeInTheDocument();
  });

  it('allows authorized user to open correction modal and submits correction', async () => {
    let capturedPayload: Record<string, unknown> | undefined;
    setupMocks(['VEHICLE_READING_VIEW', 'VEHICLE_READING_CORRECT']);
    server.use(
      http.post('*/vehicles/:vehicleId/readings/:readingId/correct', async ({ request }) => {
        capturedPayload = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleReadings[0], value: 10020, status: 'CORRECTION' }, { status: 201 });
      })
    );

    const user = userEvent.setup();
    renderSection();

    const correctBtn = (await screen.findAllByText('Correct'))[0];
    await user.click(correctBtn);

    expect(await screen.findByText(/Correct Reading #r-1/)).toBeInTheDocument();

    const valueInput = screen.getByPlaceholderText('e.g. 10250.0');
    await user.clear(valueInput);
    await user.type(valueInput, '10020');

    const reasonInput = screen.getByPlaceholderText(/Mandatory audit explanation/);
    await user.type(reasonInput, 'Adjusted baseline reading');

    const submitBtn = screen.getByRole('button', { name: 'OK' });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(capturedPayload).toMatchObject({
        value: 10020,
        reason: 'Adjusted baseline reading',
      });
    });
  });

  it('allows authorized user to record meter replacement', async () => {
    let capturedResetPayload: Record<string, unknown> | undefined;
    setupMocks(['VEHICLE_READING_VIEW', 'VEHICLE_READING_RESET_METER']);
    server.use(
      http.post('*/vehicles/:vehicleId/meter-resets', async ({ request }) => {
        capturedResetPayload = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(sampleResets[0], { status: 201 });
      })
    );

    const user = userEvent.setup();
    renderSection();

    const resetBtn = await screen.findByText('Record Meter Replacement');
    await user.click(resetBtn);

    expect(await screen.findByText(/Record Meter Replacement - WP-CAB-1234/)).toBeInTheDocument();

    const reasonInput = screen.getByPlaceholderText(/Cluster replaced after hardware failure/);
    await user.type(reasonInput, 'Cluster burned out and replaced');

    const submitBtn = screen.getByRole('button', { name: 'OK' });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(capturedResetPayload).toMatchObject({
        readingType: 'ODOMETER',
        newMeterValue: 0,
        reason: 'Cluster burned out and replaced',
      });
    });
  });
});
