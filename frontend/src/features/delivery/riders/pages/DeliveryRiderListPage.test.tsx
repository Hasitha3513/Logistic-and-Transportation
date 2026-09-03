import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { DeliveryRiderListPage } from './DeliveryRiderListPage';

vi.mock('../../../../auth/AuthContext', () => ({ useAuth: () => ({ hasPermission: () => true }) }));
vi.mock('../../zones/api/deliveryZoneApi', () => ({ deliveryZoneApi: { list: vi.fn().mockResolvedValue([]) } }));
vi.mock('../api/deliveryRiderApi', () => ({
  deliveryRiderApi: {
    getRiders: vi.fn().mockResolvedValue({ data: [] }),
    getRiderShifts: vi.fn().mockResolvedValue({ data: [] }),
    onboardRider: vi.fn(), updateRider: vi.fn(), activateRider: vi.fn(), deactivateRider: vi.fn(),
    suspendRider: vi.fn(), scheduleShift: vi.fn(), startShift: vi.fn(), endShift: vi.fn(), cancelShift: vi.fn(),
  },
}));

describe('DeliveryRiderListPage transport mode', () => {
  it('renders a required transport mode selector on onboarding', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={client}><DeliveryRiderListPage /></QueryClientProvider>);
    fireEvent.click(screen.getByRole('button', { name: /onboard rider/i }));
    expect(await screen.findByText('Transport Mode')).toBeDefined();
    fireEvent.click(screen.getAllByRole('button', { name: /onboard rider/i })[1]);
    expect(await screen.findByText('Transport mode is required')).toBeDefined();
  });
});
