import { Tag, type TagProps } from 'antd';

export interface StatusPresentation {
  color: TagProps['color'];
  label: string;
}

type PresentationMap = Readonly<Record<string, StatusPresentation>>;

const TRIP_STATUS: PresentationMap = {
  DRAFT: { color: 'default', label: 'Draft' },
  SUBMITTED: { color: 'processing', label: 'Submitted' },
  APPROVED: { color: 'cyan', label: 'Approved' },
  REJECTED: { color: 'error', label: 'Rejected' },
  ASSIGNED: { color: 'blue', label: 'Assigned' },
  DISPATCHED: { color: 'purple', label: 'Dispatched' },
  IN_PROGRESS: { color: 'gold', label: 'In progress' },
  COMPLETED: { color: 'success', label: 'Completed' },
  CLOSED: { color: 'default', label: 'Closed' },
  CANCELLED: { color: 'volcano', label: 'Cancelled' },
};

const VEHICLE_STATUS: PresentationMap = {
  AVAILABLE: { color: 'success', label: 'Available' },
  BROKEN_DOWN: { color: 'error', label: 'Broken down' },
  BREAKDOWN: { color: 'error', label: 'Breakdown' },
  OUT_OF_SERVICE: { color: 'error', label: 'Out of service' },
  MAINTENANCE: { color: 'gold', label: 'Maintenance' },
  UNDER_MAINTENANCE: { color: 'gold', label: 'Under maintenance' },
  MAINTENANCE_DUE: { color: 'orange', label: 'Maintenance due' },
  UNAVAILABLE: { color: 'error', label: 'Unavailable' },
};

const DRIVER_STATUS: PresentationMap = {
  AVAILABLE: { color: 'success', label: 'Available' },
  ASSIGNED: { color: 'blue', label: 'Assigned' },
  ON_TRIP: { color: 'processing', label: 'On trip' },
  OFF_DUTY: { color: 'default', label: 'Off duty' },
  UNAVAILABLE: { color: 'error', label: 'Unavailable' },
  SUSPENDED: { color: 'error', label: 'Suspended' },
};

const DOCUMENT_STATUS: PresentationMap = {
  ACTIVE: { color: 'success', label: 'Active' },
  INACTIVE: { color: 'default', label: 'Inactive' },
  DELETED: { color: 'error', label: 'Deleted' },
  EXPIRED: { color: 'error', label: 'Expired' },
  EXPIRING_SOON: { color: 'orange', label: 'Expiring soon' },
};

const PRIORITY: PresentationMap = {
  LOW: { color: 'default', label: 'Low' },
  NORMAL: { color: 'blue', label: 'Normal' },
  MEDIUM: { color: 'cyan', label: 'Medium' },
  HIGH: { color: 'orange', label: 'High' },
  URGENT: { color: 'error', label: 'Urgent' },
  CRITICAL: { color: 'magenta', label: 'Critical' },
};

const FUEL_ISSUE_STATUS: PresentationMap = {
  DRAFT: { color: 'default', label: 'Draft' },
  PENDING_AUTHORIZATION: { color: 'processing', label: 'Pending authorization' },
  AUTHORIZED: { color: 'cyan', label: 'Authorized' },
  ISSUED: { color: 'success', label: 'Issued' },
  CANCELLED: { color: 'volcano', label: 'Cancelled' },
};

const FUEL_PURCHASE_STATUS: PresentationMap = {
  DRAFT: { color: 'default', label: 'Draft' }, SUBMITTED: { color: 'processing', label: 'Submitted' },
  APPROVED: { color: 'cyan', label: 'Approved' }, RECEIVED: { color: 'gold', label: 'Received' },
  RECONCILED: { color: 'success', label: 'Reconciled' }, CANCELLED: { color: 'volcano', label: 'Cancelled' },
};

const BUNKER_TANK_STATUS: PresentationMap = {
  ACTIVE: { color: 'success', label: 'Active' },
  INACTIVE: { color: 'default', label: 'Inactive' },
  DECOMMISSIONED: { color: 'volcano', label: 'Decommissioned' },
};

const BUNKER_STOCK_STATUS: PresentationMap = {
  NORMAL: { color: 'success', label: 'Normal' },
  LOW_STOCK: { color: 'warning', label: 'Low Stock' },
  NEAR_CAPACITY: { color: 'processing', label: 'Near Capacity' },
  OUT_OF_SERVICE: { color: 'error', label: 'Out of Service' },
};

const BUNKER_MOVEMENT_TYPE: PresentationMap = {
  OPENING_BALANCE: { color: 'default', label: 'Opening Balance' },
  PURCHASE_RECEIPT: { color: 'success', label: 'Purchase Receipt' },
  FUEL_ISSUE: { color: 'blue', label: 'Fuel Issue' },
  TRANSFER_IN: { color: 'cyan', label: 'Transfer In' },
  TRANSFER_OUT: { color: 'volcano', label: 'Transfer Out' },
  ADJUSTMENT_IN: { color: 'purple', label: 'Adjustment (In)' },
  ADJUSTMENT_OUT: { color: 'magenta', label: 'Adjustment (Out)' },
};

function presentation(value: string | null | undefined, mapping: PresentationMap): StatusPresentation {
  const normalized = value?.trim().toUpperCase() || 'UNKNOWN';
  return mapping[normalized] ?? {
    color: 'default',
    label: normalized.replaceAll('_', ' ').toLowerCase().replace(/^./, (letter) => letter.toUpperCase()),
  };
}

function PresentationTag({ value, mapping, ...tagProps }: { value?: string | null; mapping: PresentationMap } & Omit<TagProps, 'color' | 'children'>) {
  const item = presentation(value, mapping);
  return <Tag {...tagProps} color={item.color}>{item.label}</Tag>;
}

export const tripStatusPresentation = (status?: string | null) => presentation(status, TRIP_STATUS);
export const vehicleStatusPresentation = (status?: string | null) => presentation(status, VEHICLE_STATUS);
export const driverStatusPresentation = (status?: string | null) => presentation(status, DRIVER_STATUS);
export const documentStatusPresentation = (status?: string | null) => presentation(status, DOCUMENT_STATUS);
export const priorityPresentation = (priority?: string | null) => presentation(priority, PRIORITY);
export const bunkerStockStatusPresentation = (status?: string | null) => presentation(status, BUNKER_STOCK_STATUS);
export const bunkerMovementTypePresentation = (type?: string | null) => presentation(type, BUNKER_MOVEMENT_TYPE);

export function TripStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={TRIP_STATUS} />;
}

export function VehicleStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={VEHICLE_STATUS} />;
}

export function DriverStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={DRIVER_STATUS} />;
}

export function DocumentStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={DOCUMENT_STATUS} />;
}

export function PriorityTag({ priority, ...props }: { priority?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={priority} mapping={PRIORITY} />;
}

export function FuelIssueStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={FUEL_ISSUE_STATUS} />;
}

export function FuelPurchaseStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={FUEL_PURCHASE_STATUS} />;
}

export function BunkerTankStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={BUNKER_TANK_STATUS} />;
}

export function BunkerStockStatusTag({ status, ...props }: { status?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={status} mapping={BUNKER_STOCK_STATUS} />;
}

export function BunkerMovementTypeTag({ type, ...props }: { type?: string | null } & Omit<TagProps, 'color' | 'children'>) {
  return <PresentationTag {...props} value={type} mapping={BUNKER_MOVEMENT_TYPE} />;
}
