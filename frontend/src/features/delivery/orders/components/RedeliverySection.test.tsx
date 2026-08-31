import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RedeliverySection } from './RedeliverySection';
import type { DeliveryOrder } from '../types/deliveryOrder';
import * as useAuthModule from '../../../../auth/AuthContext';
import * as useRedeliveryModule from '../hooks/useRedelivery';

vi.mock('../../../../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../hooks/useRedelivery', () => ({
  useRedelivery: vi.fn(),
}));

describe('RedeliverySection Component', () => {
  let queryClient: QueryClient;

  const mockDelivery: DeliveryOrder = {
    id: '20000000-0000-0000-0000-000000000001',
    deliveryNumber: 'DEL-2026-000001',
    customerId: 'cust-1',
    originLocationId: 'loc-1',
    destinationLocationId: 'loc-2',
    priority: 'NORMAL',
    serviceType: 'STANDARD',
    windowStart: '2026-08-31T10:00:00Z',
    windowEnd: '2026-08-31T14:00:00Z',
    instructions: 'Handle with care',
    status: 'FAILED_ATTEMPT',
    version: 1,
    createdAt: '2026-08-31T09:00:00Z',
    updatedAt: '2026-08-31T09:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  };

  const defaultMockRedelivery = {
    history: [],
    isLoadingHistory: false,
    refetchHistory: vi.fn(),
    getSuggestions: vi.fn(),
    isGettingSuggestions: false,
    suggestions: [],
    scheduleRedelivery: vi.fn(),
    isScheduling: false,
    rescheduleRedelivery: vi.fn(),
    isRescheduling: false,
  };

  beforeEach(() => {
    queryClient = new QueryClient();
    vi.mocked(useAuthModule.useAuth).mockReturnValue({
      hasPermission: vi.fn().mockReturnValue(true),
      user: { username: 'admin' },
    } as unknown as ReturnType<typeof useAuthModule.useAuth>);
    vi.mocked(useRedeliveryModule.useRedelivery).mockReturnValue(
      defaultMockRedelivery as unknown as ReturnType<typeof useRedeliveryModule.useRedelivery>
    );
  });

  it('renders re-delivery section and action button for eligible FAILED_ATTEMPT order', () => {
    render(
      <QueryClientProvider client={queryClient}>
        <RedeliverySection delivery={mockDelivery} />
      </QueryClientProvider>
    );

    expect(screen.getByTestId('redelivery-section')).toBeInTheDocument();
    expect(screen.getByTestId('schedule-redelivery-btn')).toBeInTheDocument();
    expect(screen.getByText(/Order is eligible for Re-Delivery/i)).toBeInTheDocument();
  });

  it('hides schedule button when user lacks DELIVERY_REDELIVERY_SCHEDULE permission', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue({
      hasPermission: vi.fn((perm: string) => perm === 'DELIVERY_REDELIVERY_VIEW'),
      user: { username: 'viewer' },
    } as unknown as ReturnType<typeof useAuthModule.useAuth>);

    render(
      <QueryClientProvider client={queryClient}>
        <RedeliverySection delivery={mockDelivery} />
      </QueryClientProvider>
    );

    expect(screen.getByTestId('redelivery-section')).toBeInTheDocument();
    expect(screen.queryByTestId('schedule-redelivery-btn')).not.toBeInTheDocument();
  });

  it('renders terminal alert and hides schedule button when status is RETURN_TO_BASE', () => {
    const rtoDelivery: DeliveryOrder = { ...mockDelivery, status: 'RETURN_TO_BASE' };

    render(
      <QueryClientProvider client={queryClient}>
        <RedeliverySection delivery={rtoDelivery} />
      </QueryClientProvider>
    );

    expect(screen.getByText(/Order Returned to Base/i)).toBeInTheDocument();
    expect(screen.queryByTestId('schedule-redelivery-btn')).not.toBeInTheDocument();
  });

  it('renders Reschedule button when order is in READY_FOR_ASSIGNMENT with active confirmed schedule', () => {
    const readyDelivery: DeliveryOrder = { ...mockDelivery, status: 'READY_FOR_ASSIGNMENT' };
    const mockWithSchedule = {
      ...defaultMockRedelivery,
      history: [
        {
          id: 'sched-1',
          deliveryOrderId: readyDelivery.id,
          deliveryAttemptId: 'att-1',
          schedulingMethod: 'AGENT_ASSISTED',
          preferredStartTime: null,
          preferredEndTime: null,
          customerPreferenceNotes: null,
          scheduledStartTime: '2026-09-01T09:00:00Z',
          scheduledEndTime: '2026-09-01T13:00:00Z',
          status: 'CONFIRMED',
          scheduledBy: 'dispatcher',
          scheduledAt: '2026-08-31T10:00:00Z',
          supersededAt: null,
          supersededBy: null,
          supersedeReason: null,
          createdAt: '2026-08-31T10:00:00Z',
          updatedAt: '2026-08-31T10:00:00Z',
        },
      ],
    };
    vi.mocked(useRedeliveryModule.useRedelivery).mockReturnValue(
      mockWithSchedule as unknown as ReturnType<typeof useRedeliveryModule.useRedelivery>
    );

    render(
      <QueryClientProvider client={queryClient}>
        <RedeliverySection delivery={readyDelivery} />
      </QueryClientProvider>
    );

    expect(screen.getByTestId('reschedule-redelivery-btn')).toBeInTheDocument();
  });
});

