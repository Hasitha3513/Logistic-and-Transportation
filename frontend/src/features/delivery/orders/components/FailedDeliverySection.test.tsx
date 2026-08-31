import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { App as AntApp } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FailedDeliverySection } from './FailedDeliverySection';
import type { DeliveryOrder } from '../types/deliveryOrder';
import type { DeliveryFailureHistory } from '../types/failedDelivery';

let mockHasPermission = vi.fn(() => true);

vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: () => ({
    hasPermission: mockHasPermission,
    user: { id: 'user-1', username: 'delivery.manager' },
  }),
}));

const mockHistory: DeliveryFailureHistory = {
  deliveryId: '03cd51bf-7ae3-44bd-8202-817fef87341d',
  totalAttempts: 1,
  attempts: [
    {
      id: 'att-1',
      deliveryId: '03cd51bf-7ae3-44bd-8202-817fef87341d',
      attemptNumber: 1,
      attemptTimestamp: '2026-08-31T10:00:00Z',
      failureReason: 'CUSTOMER_UNAVAILABLE',
      notes: 'No answer at reception desk',
      disposition: 'REDELIVERY_ELIGIBLE',
      contactAttempts: [
        {
          id: 'contact-1',
          deliveryAttemptId: 'att-1',
          channel: 'PHONE',
          contactTimestamp: '2026-08-31T10:00:00Z',
          outcome: 'NO_ANSWER',
          notes: 'Phone rang with no answer',
          recordedBy: 'driver1',
          recordedAt: '2026-08-31T10:00:00Z',
        },
      ],
      recordedBy: 'driver1',
      recordedAt: '2026-08-31T10:00:00Z',
    },
  ],
  escalations: [
    {
      id: 'esc-1',
      deliveryId: '03cd51bf-7ae3-44bd-8202-817fef87341d',
      reason: 'Urgent cargo escalation',
      status: 'OPEN',
      escalatedBy: 'manager1',
      escalatedAt: '2026-08-31T10:05:00Z',
    },
  ],
};

vi.mock('../../../../api/client', () => ({
  api: {
    get: vi.fn((url: string) => {
      if (url.includes('/attempts')) {
        return Promise.resolve({ data: mockHistory });
      }
      return Promise.resolve({ data: {} });
    }),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
  },
}));

const testDelivery: DeliveryOrder = {
  id: '03cd51bf-7ae3-44bd-8202-817fef87341d',
  deliveryNumber: 'DEL-2026-000001',
  customerId: 'cust-1',
  originLocationId: 'loc-1',
  destinationLocationId: 'loc-2',
  priority: 'NORMAL',
  serviceType: 'STANDARD',
  windowStart: '2026-08-31T09:00:00Z',
  windowEnd: '2026-08-31T17:00:00Z',
  instructions: 'Handle with care',
  status: 'READY_FOR_ASSIGNMENT',
  version: 1,
  createdAt: '2026-08-31T08:00:00Z',
  updatedAt: '2026-08-31T08:00:00Z',
  createdBy: 'admin',
  updatedBy: 'admin',
};

const renderComponent = (delivery = testDelivery) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <FailedDeliverySection delivery={delivery} />
      </AntApp>
    </QueryClientProvider>
  );
};

describe('FailedDeliverySection Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasPermission = vi.fn(() => true);
  });

  it('renders section title and attempt recording form for eligible delivery order', async () => {
    renderComponent();
    expect(screen.getByText(/Failed Delivery Management/i)).toBeInTheDocument();
    expect(screen.getByText(/Record Failed Delivery Attempt/i)).toBeInTheDocument();
    expect(screen.getByTestId('btn-submit-failed-attempt')).toBeInTheDocument();
  });

  it('renders attempt history and escalation items', async () => {
    renderComponent();
    expect(await screen.findByText(/Attempt #1/i)).toBeInTheDocument();
    expect(screen.getByText(/CUSTOMER_UNAVAILABLE/i)).toBeInTheDocument();
    expect(screen.getByText(/No answer at reception desk/i)).toBeInTheDocument();
    expect(screen.getByText(/Urgent cargo escalation/i)).toBeInTheDocument();
  });

  it('shows delivered completion warning and hides recording form when delivery is DELIVERED', async () => {
    const deliveredOrder: DeliveryOrder = { ...testDelivery, status: 'DELIVERED' };
    renderComponent(deliveredOrder);
    expect(screen.getByText(/Delivery Order Completed/i)).toBeInTheDocument();
    expect(screen.queryByTestId('btn-submit-failed-attempt')).not.toBeInTheDocument();
  });

  it('returns null when user lacks both view and record permissions', () => {
    mockHasPermission = vi.fn(() => false);
    const { container } = renderComponent();
    expect(container.querySelector('.ant-card')).toBeNull();
  });
});
