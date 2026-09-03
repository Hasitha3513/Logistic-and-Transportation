import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import DeliverySelfServicePage, { consumeFragment } from './DeliverySelfServicePage';

const projection = {
  deliveryNumber: 'DEL-2026-000070', status: 'Scheduled', explanation: 'Your delivery is scheduled.',
  scheduledStart: '2026-09-03T05:00:00Z', scheduledEnd: '2026-09-03T07:00:00Z', timeZone: 'Asia/Colombo',
  etaFreshness: 'UNAVAILABLE', availableActions: ['TRACK', 'PREFERENCES', 'REPORT_ISSUE'], destination: 'Colombo',
  podAvailability: 'NOT_AVAILABLE', notificationPreferences: { emailEnabled: true, smsEnabled: false,
    maskedEmail: 'c***@example.com', maskedPhone: '+94******000', explicitProfile: false }, submissions: [],
};

describe('DeliverySelfServicePage', () => {
  afterEach(() => { vi.clearAllMocks(); vi.unstubAllGlobals(); window.history.replaceState(null, '', '/'); });

  it('consumes and immediately removes the URL fragment without persistent storage', () => {
    window.history.replaceState(null, '', '/track#access_token=opaque-token');
    const localSpy = vi.spyOn(Storage.prototype, 'setItem');
    expect(consumeFragment()).toBe('opaque-token');
    expect(window.location.hash).toBe('');
    expect(localSpy).not.toHaveBeenCalled();
    localSpy.mockRestore();
  });

  it('renders the public customer shell and uses only DeliveryAccess authentication', async () => {
    window.history.replaceState(null, '', '/track#access_token=abcdefghijklmnopqrstuvwxyzABCDEFGH123456789');
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => projection });
    vi.stubGlobal('fetch', fetchMock);
    render(<DeliverySelfServicePage />);
    expect(await screen.findByText('DEL-2026-000070')).toBeInTheDocument();
    expect(screen.getByText('Delivery self-service')).toBeInTheDocument();
    expect(screen.queryByText('Sign out')).not.toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>).Authorization).toMatch(/^DeliveryAccess /);
    expect(window.location.hash).toBe('');
  });

  it('cannot recover access after a reload because the fragment is gone', () => {
    window.history.replaceState(null, '', '/track');
    render(<DeliverySelfServicePage />);
    expect(screen.getByText('Open your original delivery link')).toBeInTheDocument();
  });
});
