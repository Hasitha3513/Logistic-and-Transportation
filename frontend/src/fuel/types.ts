export type FuelIssueStatus = 'DRAFT' | 'PENDING_AUTHORIZATION' | 'AUTHORIZED' | 'ISSUED' | 'CANCELLED';

export interface FuelStation {
  id: string;
  code: string;
  name: string;
  stationType: 'INTERNAL' | 'EXTERNAL';
  active: boolean;
  vendorId?: string | null;
  locationId?: string | null;
}

export interface FuelIssue {
  id: string;
  voucherNumber: string;
  vehicle: { id: string };
  trip?: { id: string } | null;
  driver?: { id: string } | null;
  fuelType: string;
  quantity: number;
  unitPrice?: number | null;
  totalAmount?: number | null;
  station: FuelStation;
  odometer?: number | null;
  engineHours?: number | null;
  issueDateTime: string;
  status: FuelIssueStatus;
  requestedBy?: string | null;
  authorizedBy?: string | null;
  authorizationDateTime?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FuelIssueHistory {
  id: string;
  fuelIssueId: string;
  fromStatus?: FuelIssueStatus | null;
  toStatus: FuelIssueStatus;
  action: string;
  actorId: string;
  actor: string;
  comment?: string | null;
  occurredAt: string;
}

export interface FuelIssuePage {
  content: FuelIssue[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
}

export interface FuelIssuePayload {
  vehicleId: string;
  tripId?: string | null;
  driverId?: string | null;
  fuelType: string;
  quantity: number;
  unitPrice?: number | null;
  stationId: string;
  odometer?: number | null;
  engineHours?: number | null;
  issueDateTime: string;
  notes?: string | null;
}
