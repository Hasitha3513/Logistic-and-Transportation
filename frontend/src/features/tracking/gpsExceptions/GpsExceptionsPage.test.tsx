import { App } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import GpsExceptionsPage from './GpsExceptionsPage';

const permissions = vi.hoisted(() => new Set<string>());
const current = vi.hoisted(() => ({ id: 'session-a' }));
const gpsApi = vi.hoisted(() => ({ episodes: vi.fn(), episode: vi.fn(), evidence: vi.fn(), acknowledge: vi.fn() }));
vi.mock('../../../auth/AuthContext', () => ({ useAuth: () => ({ user: current, hasPermission: (permission: string) => permissions.has(permission) }) }));
vi.mock('./api', () => ({ gpsExceptionApi: gpsApi }));

const episode = { id: 'episode-1', deviceId: 'device-1', vehicleId: 'vehicle-1', type: 'PROCESSING_FAILURE', severity: 'WARNING', status: 'OPEN', openedAt: '2026-09-16T00:00:00Z', lastObservedAt: '2026-09-16T00:01:00Z', resolvedAt: null, evidenceCount: 1, consecutiveRecoveryPoints: 0, version: 3 } as const;

function view() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<MemoryRouter><QueryClientProvider client={client}><App><GpsExceptionsPage /></App></QueryClientProvider></MemoryRouter>);
}

async function queryRange() {
  fireEvent.change(screen.getByLabelText('GPS exception range from'), { target: { value: '2026-09-16T00:00' } });
  fireEvent.change(screen.getByLabelText('GPS exception range to'), { target: { value: '2026-09-17T00:00' } });
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }));
  await screen.findByText('Telemetry processing failure');
}

describe('GpsExceptionsPage', () => {
  beforeEach(() => {
    permissions.clear(); current.id = 'session-a';
    gpsApi.episodes.mockReset().mockResolvedValue({ items: [episode], nextCursor: 'next-token' });
    gpsApi.episode.mockReset().mockResolvedValue(episode);
    gpsApi.evidence.mockReset().mockResolvedValue({ items: [{ id: 'evidence-1', sourceTimestamp: '2026-09-16T00:00:00Z', assessedAt: '2026-09-16T00:00:01Z', trust: 'UNKNOWN', ordering: 'IN_ORDER', reliabilityState: 'DEGRADED', qualityCodes: ['CLOCK_SKEW'], transition: 'OPENED' }] });
    gpsApi.acknowledge.mockReset().mockResolvedValue({ episodeId: 'episode-1', status: 'ACKNOWLEDGED', severity: 'WARNING', version: 4, acknowledgedAt: '2026-09-17T00:00:00Z' });
  });

  it('does not issue VIEW requests or reveal evidence to REVIEW-only users', () => {
    permissions.add('GPS_EXCEPTION_REVIEW'); view();
    expect(screen.getByText('GPS exception access denied')).toBeInTheDocument();
    expect(gpsApi.episodes).not.toHaveBeenCalled();
  });

  it('requires a valid explicit seven-day range and resets opaque pagination when filters are applied', async () => {
    permissions.add('GPS_EXCEPTION_VIEW'); view();
    expect(screen.getByText('Choose a required start and end time.')).toBeInTheDocument();
    await queryRange();
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    await waitFor(() => expect(gpsApi.episodes).toHaveBeenLastCalledWith(expect.objectContaining({ cursor: 'next-token' }), expect.anything()));
    fireEvent.change(screen.getByLabelText('Filter GPS exceptions by vehicle ID'), { target: { value: 'vehicle-2' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }));
    await waitFor(() => expect(gpsApi.episodes).toHaveBeenLastCalledWith(expect.objectContaining({ vehicleId: 'vehicle-2', cursor: undefined }), expect.anything()));
    expect(screen.getByText('Page 1')).toBeInTheDocument();
  });

  it('shows minimized detail and immutable evidence but hides review controls for VIEW-only users', async () => {
    permissions.add('GPS_EXCEPTION_VIEW'); view(); await queryRange();
    fireEvent.click(screen.getByRole('button', { name: 'View GPS exception episode-1' }));
    expect(await screen.findByText('Immutable evidence')).toBeInTheDocument();
    expect(await screen.findByText('Clock skew')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Acknowledge review' })).not.toBeInTheDocument();
    expect(screen.queryByText(/7\.123|secret-token|raw-payload-value/i)).not.toBeInTheDocument();
  });

  it('validates acknowledgement and prevents duplicate pending submission', async () => {
    permissions.add('GPS_EXCEPTION_VIEW'); permissions.add('GPS_EXCEPTION_REVIEW');
    let finish!: (value: unknown) => void;
    gpsApi.acknowledge.mockReturnValue(new Promise(resolve => { finish = resolve; }));
    view(); await queryRange(); fireEvent.click(screen.getByRole('button', { name: 'View GPS exception episode-1' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Acknowledge review' }));
    fireEvent.click(screen.getByRole('button', { name: 'Acknowledge' }));
    expect(await screen.findByText('Reason is required')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Acknowledgement reason'), '  Reviewed evidence  ');
    fireEvent.click(screen.getByRole('button', { name: 'Acknowledge' }));
    await waitFor(() => expect(gpsApi.acknowledge).toHaveBeenCalledTimes(1));
    expect(screen.getByRole('button', { name: 'loading Acknowledge' })).toBeDisabled();
    finish({ episodeId: 'episode-1', status: 'ACKNOWLEDGED', severity: 'WARNING', version: 4, acknowledgedAt: '2026-09-17T00:00:00Z' });
    await waitFor(() => expect(gpsApi.acknowledge).toHaveBeenCalledWith('episode-1', expect.objectContaining({ expectedVersion: 3, reason: 'Reviewed evidence' })));
  });
});
