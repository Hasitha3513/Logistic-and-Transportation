import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import DeliverySelfServicePage, { consumeFragment } from './DeliverySelfServicePage';
import { selfServiceApi, type Projection } from '../api/selfServiceApi';

const projection = {
  deliveryNumber: 'DEL-2026-000070', status: 'Scheduled', explanation: 'Your delivery is scheduled.',
  scheduledStart: '2026-09-03T05:00:00Z', scheduledEnd: '2026-09-03T07:00:00Z', timeZone: 'Asia/Colombo',
  etaFreshness: 'UNAVAILABLE', availableActions: ['TRACK', 'PREFERENCES', 'REPORT_ISSUE'], destination: 'Colombo',
  podAvailability: 'NOT_AVAILABLE', notificationPreferences: { emailEnabled: true, smsEnabled: false,
    maskedEmail: 'c***@example.com', maskedPhone: '+94******000', explicitProfile: false }, submissions: [],
} satisfies Projection;

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
    const track = vi.spyOn(selfServiceApi, 'track').mockResolvedValue(projection);
    renderPage();
    expect(await screen.findByText('DEL-2026-000070')).toBeInTheDocument();
    expect(screen.getByText('Delivery self-service')).toBeInTheDocument();
    expect(screen.queryByText('Sign out')).not.toBeInTheDocument();
    await waitFor(() => expect(track).toHaveBeenCalledWith('abcdefghijklmnopqrstuvwxyzABCDEFGH123456789'));
    expect(window.location.hash).toBe('');
  });

  it('cannot recover access after a reload because the fragment is gone', () => {
    window.history.replaceState(null, '', '/track');
    renderPage();
    expect(screen.getByText('Open your original delivery link')).toBeInTheDocument();
  });

  it('submits initial preferences when the optimistic version is null', async () => {
    const user = userEvent.setup();
    window.history.replaceState(null, '', '/track#access_token=abcdefghijklmnopqrstuvwxyzABCDEFGH123456789');
    vi.spyOn(selfServiceApi, 'track').mockResolvedValue({
      ...projection,
      notificationPreferences: { ...projection.notificationPreferences, version: null },
    });
    const replace = vi.spyOn(selfServiceApi, 'replacePreferences').mockResolvedValue({
      ...projection.notificationPreferences,
      version: 1,
    });
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Save preferences' }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith(
      'abcdefghijklmnopqrstuvwxyzABCDEFGH123456789',
      expect.objectContaining({ emailEnabled: true, smsEnabled: false, version: null }),
    ));
  });
});

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><DeliverySelfServicePage /></QueryClientProvider>);
}
