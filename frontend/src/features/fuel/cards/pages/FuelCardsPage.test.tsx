import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import FuelCardsPage from './FuelCardsPage';

vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: () => ({ hasPermission: (permission: string) => permission === 'FUEL_CARD_IMPORT' }),
}));
vi.mock('../hooks/useFuelCards', () => ({
  useFuelCardDetail: () => ({ bindings: { data: [] }, history: { data: [] } }),
  useFuelCards: () => ({
    cards: { data: [{ id: 'card-1', providerId: 'provider-1', alias: 'Vehicle card',
      maskedIdentifier: '**** 4242', lastFour: '4242', expiryMonth: 12, expiryYear: 2027,
      status: 'ACTIVE', providerSyncStatus: 'NOT_CONFIGURED', version: 1,
      createdAt: '2026-09-04T00:00:00Z', updatedAt: '2026-09-04T00:00:00Z' }], isLoading: false },
    transactions: { data: [], isLoading: false }, imports: { data: [], isLoading: false },
    upload: { mutate: vi.fn(), isPending: false },
  }),
}));

describe('FuelCardsPage', () => {
  it('shows only masked card identity and honest local provider state', () => {
    render(<FuelCardsPage />);
    expect(screen.getByText('**** 4242')).toBeInTheDocument();
    expect(screen.getByText('Provider synchronization not configured')).toBeInTheDocument();
    expect(screen.queryByText('opaque-provider-reference')).not.toBeInTheDocument();
  });
});
