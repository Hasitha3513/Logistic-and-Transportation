import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DeliveryAnalyticsKpiCards } from './DeliveryAnalyticsKpiCards';
import { DeliveryAnalyticsSummary } from '../types/deliveryAnalytics';

describe('DeliveryAnalyticsKpiCards', () => {
  it('renders correctly formatted values with percentages and counts', () => {
    const summary: DeliveryAnalyticsSummary = {
      period: { from: '2026-08-01', to: '2026-08-31' },
      totalOrders: 100,
      activeOrders: 10,
      terminalCompletedOrders: 90,
      deliveredOrders: 80,
      returnedToBaseOrders: 10,
      orderSuccessRate: 88.89,
      firstAttemptSuccessRate: 75.0,
      onTimeDeliveredOrders: 70,
      lateDeliveredOrders: 10,
      onTimeDeliveryRate: 87.5,
      lateDeliveryRate: 12.5,
      averageDelayMinutes: 35.5,
      totalFailedAttempts: 25,
      averageFailedAttemptsPerOrder: 0.25,
      redeliveredOrders: 15,
      redeliveryRate: 15.0,
      redeliverySuccessRate: 80.0,
      returnToBaseRate: 11.11,
    };

    render(<DeliveryAnalyticsKpiCards summary={summary} />);

    expect(screen.getByText('88.9%')).toBeInTheDocument();
    expect(screen.getByText('(80/90)')).toBeInTheDocument();
    expect(screen.getByText('87.5%')).toBeInTheDocument();
    expect(screen.getByText('75.0%')).toBeInTheDocument();
    expect(screen.getByText('15.0%')).toBeInTheDocument();
    expect(screen.getByText(/Avg Late Delay: 35.5 mins/)).toBeInTheDocument();
  });

  it('renders N/A for null rates when dataset has zero completions', () => {
    const emptySummary: DeliveryAnalyticsSummary = {
      period: { from: '2026-08-01', to: '2026-08-31' },
      totalOrders: 0,
      activeOrders: 0,
      terminalCompletedOrders: 0,
      deliveredOrders: 0,
      returnedToBaseOrders: 0,
      orderSuccessRate: null,
      firstAttemptSuccessRate: null,
      onTimeDeliveredOrders: 0,
      lateDeliveredOrders: 0,
      onTimeDeliveryRate: null,
      lateDeliveryRate: null,
      averageDelayMinutes: null,
      totalFailedAttempts: 0,
      averageFailedAttemptsPerOrder: 0,
      redeliveredOrders: 0,
      redeliveryRate: null,
      redeliverySuccessRate: null,
      returnToBaseRate: null,
    };

    render(<DeliveryAnalyticsKpiCards summary={emptySummary} />);

    const naElements = screen.getAllByText('N/A');
    expect(naElements.length).toBeGreaterThanOrEqual(4);
  });
});
