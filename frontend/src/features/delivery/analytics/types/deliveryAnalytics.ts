export interface DeliveryAnalyticsPeriod {
  from: string;
  to: string;
}

export interface DeliveryAnalyticsSummary {
  period: DeliveryAnalyticsPeriod;
  totalOrders: number;
  activeOrders: number;
  terminalCompletedOrders: number;
  deliveredOrders: number;
  returnedToBaseOrders: number;
  orderSuccessRate: number | null;
  firstAttemptSuccessRate: number | null;
  onTimeDeliveredOrders: number;
  lateDeliveredOrders: number;
  onTimeDeliveryRate: number | null;
  lateDeliveryRate: number | null;
  averageDelayMinutes: number | null;
  totalFailedAttempts: number;
  averageFailedAttemptsPerOrder: number;
  redeliveredOrders: number;
  redeliveryRate: number | null;
  redeliverySuccessRate: number | null;
  returnToBaseRate: number | null;
}

export interface FailureReasonBreakdownItem {
  failureReason: string;
  count: number;
  percentage: number;
  redeliveryEligibleCount: number;
  returnToBaseCount: number;
  escalatedCount: number;
}

export interface RegionalPerformanceItem {
  destinationLocationId: string | null;
  locationCode: string;
  locationName: string;
  city: string;
  totalOrders: number;
  deliveredOrders: number;
  returnedToBaseOrders: number;
  orderSuccessRate: number | null;
  onTimeDeliveredOrders: number;
  onTimeDeliveryRate: number | null;
  averageDelayMinutes: number | null;
  failedAttemptCount: number;
}

export interface DeliveryTrendItem {
  bucketDate: string;
  totalCreated: number;
  delivered: number;
  failedAttempts: number;
  returnedToBase: number;
  onTimeDelivered: number;
  lateDelivered: number;
}

export interface DeliveryAnalyticsFilters {
  from?: string;
  to?: string;
  serviceType?: string;
  priority?: string;
  destinationLocationId?: string;
}
