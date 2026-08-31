import { api } from '../../../../api/client';

export type DeliveryRiderType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACTOR' | 'GIG';
export type DeliveryRiderStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type DeliveryRiderAvailability = 'AVAILABLE' | 'BUSY' | 'OFF_DUTY' | 'UNAVAILABLE';
export type DeliveryRiderShiftStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type DeliveryRiderAssignmentStatus = 'ACTIVE' | 'REASSIGNED' | 'UNASSIGNED' | 'DELIVERED' | 'FAILED';

export interface DeliveryRider {
  id: string;
  tenantId: string;
  riderCode: string;
  driverId: string;
  riderType: DeliveryRiderType;
  primaryZoneId: string;
  secondaryZoneIds: string[];
  maxConcurrentDeliveries: number;
  status: DeliveryRiderStatus;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface DriverSummary {
  id: string;
  employeeNumber?: string;
  firstName?: string;
  lastName?: string;
  status?: string;
  active: boolean;
}

export interface DeliveryRiderShift {
  id: string;
  tenantId: string;
  riderId: string;
  shiftDate: string;
  startTime: string;
  endTime: string;
  deliverySlotId?: string;
  maxCapacity: number;
  status: DeliveryRiderShiftStatus;
  actualStartTime?: string;
  actualEndTime?: string;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface DeliveryOrderRiderAssignment {
  id: string;
  tenantId: string;
  deliveryOrderId: string;
  riderId: string;
  status: DeliveryRiderAssignmentStatus;
  isOverride: boolean;
  overrideReason?: string;
  assignedAt: string;
  assignedBy: string;
  unassignedAt?: string;
  unassignedBy?: string;
  version: number;
}

export interface DeliveryRiderSummary {
  id: string;
  riderCode: string;
  driverId: string;
  driver?: DriverSummary;
  riderType: DeliveryRiderType;
  status: DeliveryRiderStatus;
  availability: DeliveryRiderAvailability;
  primaryZoneId: string;
  secondaryZoneIds: string[];
  activeWorkload: number;
  maxConcurrentDeliveries: number;
  currentShift?: DeliveryRiderShift;
}

export interface OnboardRiderPayload {
  riderCode?: string;
  driverId: string;
  riderType: DeliveryRiderType;
  primaryZoneId: string;
  secondaryZoneIds?: string[];
  maxConcurrentDeliveries?: number;
}

export interface UpdateRiderProfilePayload {
  riderType?: DeliveryRiderType;
  primaryZoneId?: string;
  secondaryZoneIds?: string[];
  maxConcurrentDeliveries?: number;
  expectedVersion: number;
}

export interface ScheduleShiftPayload {
  shiftDate: string;
  startTime: string;
  endTime: string;
  deliverySlotId?: string;
  maxCapacity?: number;
}

export interface AssignRiderPayload {
  riderId: string;
  isOverride?: boolean;
  overrideReason?: string;
}

export interface ReassignRiderPayload {
  newRiderId: string;
  isOverride?: boolean;
  overrideReason?: string;
}

export const deliveryRiderApi = {
  getRiders: (params?: { zoneId?: string; status?: DeliveryRiderStatus; search?: string }) => {
    return api.get<DeliveryRider[]>('/api/v1/deliveries/riders', { params });
  },

  getRider: (riderId: string) => {
    return api.get<DeliveryRider>(`/api/v1/deliveries/riders/${riderId}`);
  },

  onboardRider: (payload: OnboardRiderPayload) => {
    return api.post<DeliveryRider>('/api/v1/deliveries/riders', payload);
  },

  updateRider: (riderId: string, payload: UpdateRiderProfilePayload) => {
    return api.put<DeliveryRider>(`/api/v1/deliveries/riders/${riderId}`, payload);
  },

  activateRider: (riderId: string) => {
    return api.patch<void>(`/api/v1/deliveries/riders/${riderId}/activate`);
  },

  deactivateRider: (riderId: string) => {
    return api.patch<void>(`/api/v1/deliveries/riders/${riderId}/deactivate`);
  },

  suspendRider: (riderId: string) => {
    return api.patch<void>(`/api/v1/deliveries/riders/${riderId}/suspend`);
  },

  // Shifts
  getRiderShifts: (riderId: string, startDate?: string, endDate?: string) => {
    return api.get<DeliveryRiderShift[]>(`/api/v1/deliveries/riders/${riderId}/shifts`, {
      params: { startDate, endDate },
    });
  },

  scheduleShift: (riderId: string, payload: ScheduleShiftPayload) => {
    return api.post<DeliveryRiderShift>(`/api/v1/deliveries/riders/${riderId}/shifts`, payload);
  },

  startShift: (riderId: string, shiftId: string) => {
    return api.patch<void>(`/api/v1/deliveries/riders/${riderId}/shifts/${shiftId}/start`);
  },

  endShift: (riderId: string, shiftId: string) => {
    return api.patch<void>(`/api/v1/deliveries/riders/${riderId}/shifts/${shiftId}/end`);
  },

  cancelShift: (riderId: string, shiftId: string) => {
    return api.patch<void>(`/api/v1/deliveries/riders/${riderId}/shifts/${shiftId}/cancel`);
  },

  // Availability query
  getAvailableRiders: (zoneId?: string, date?: string, slotId?: string) => {
    return api.get<DeliveryRiderSummary[]>('/api/v1/deliveries/riders/available', {
      params: { zoneId, date, slotId },
    });
  },

  // Order Assignments
  getOrderAssignmentHistory: (deliveryOrderId: string) => {
    return api.get<DeliveryOrderRiderAssignment[]>(`/api/v1/deliveries/orders/${deliveryOrderId}/rider-assignments`);
  },

  assignRiderToOrder: (deliveryOrderId: string, payload: AssignRiderPayload) => {
    return api.post<DeliveryOrderRiderAssignment>(`/api/v1/deliveries/orders/${deliveryOrderId}/assign-rider`, payload);
  },

  reassignRiderForOrder: (deliveryOrderId: string, payload: ReassignRiderPayload) => {
    return api.post<DeliveryOrderRiderAssignment>(`/api/v1/deliveries/orders/${deliveryOrderId}/reassign-rider`, payload);
  },

  unassignRiderFromOrder: (deliveryOrderId: string) => {
    return api.post<void>(`/api/v1/deliveries/orders/${deliveryOrderId}/unassign-rider`);
  },
};
