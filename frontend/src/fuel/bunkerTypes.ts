export type BunkerTankStatus = 'ACTIVE' | 'INACTIVE' | 'DECOMMISSIONED';

export type BunkerStockStatus = 'NORMAL' | 'LOW_STOCK' | 'NEAR_CAPACITY' | 'OUT_OF_SERVICE';

export type BunkerMovementType =
  | 'OPENING_BALANCE'
  | 'PURCHASE_RECEIPT'
  | 'FUEL_ISSUE'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'ADJUSTMENT_IN'
  | 'ADJUSTMENT_OUT';

export type BunkerReferenceType =
  | 'OPENING_BALANCE'
  | 'FUEL_PURCHASE'
  | 'FUEL_ISSUE'
  | 'INTER_TANK_TRANSFER'
  | 'STOCK_ADJUSTMENT'
  | 'MANUAL';

export interface BunkerTank {
  id: string;
  fuelStationId: string;
  tankCode: string;
  tankName: string;
  fuelType: string;
  capacityLiters: number;
  currentStockLiters: number;
  availableCapacityLiters: number;
  minimumStockLiters: number;
  status: BunkerTankStatus;
  lowStock: boolean;
  commissionedAt?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BunkerTankBalance {
  tankId: string;
  fuelStationId: string;
  tankCode: string;
  tankName: string;
  fuelType: string;
  capacityLiters: number;
  currentStockLiters: number;
  availableCapacityLiters: number;
  minimumStockLiters: number;
  status: BunkerTankStatus;
  stockStatus: BunkerStockStatus | string;
  latestDipQuantityLiters?: number | null;
  latestDipAt?: string | null;
  latestVarianceLiters?: number | null;
}

export interface BunkerStockMovement {
  id: string;
  tankId: string;
  movementType: BunkerMovementType;
  quantityLiters: number;
  resultingBalanceLiters: number;
  referenceType: BunkerReferenceType;
  referenceId?: string | null;
  occurredAt: string;
  createdBy: string;
  reason?: string | null;
  createdAt: string;
}

export interface BunkerStockMovementPage {
  items: BunkerStockMovement[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
}

export interface DipReading {
  id: string;
  tankId: string;
  physicalQuantityLiters: number;
  bookQuantityAtMeasurement: number;
  varianceQuantityLiters: number;
  measuredAt: string;
  measuredBy?: string | null;
  notes?: string | null;
  createdAt: string;
}

export interface StockAdjustment {
  id: string;
  tankId: string;
  quantityDeltaLiters: number;
  reason: string;
  approvedBy?: string | null;
  sourceDipReadingId?: string | null;
  occurredAt: string;
  createdAt: string;
}

export interface BunkerTankCreatePayload {
  fuelStationId: string;
  tankCode: string;
  tankName: string;
  fuelType: string;
  capacityLiters: number;
  minimumStockLiters?: number;
  openingBalanceLiters?: number;
  commissionedAt?: string;
}

export interface BunkerTankUpdatePayload {
  tankName?: string;
  minimumStockLiters?: number;
  status?: BunkerTankStatus;
  active?: boolean;
}

export interface DipReadingPayload {
  physicalQuantityLiters: number;
  notes?: string;
}

export interface StockAdjustmentPayload {
  quantityDeltaLiters: number;
  reason: string;
  sourceDipReadingId?: string;
}

export interface BunkerTransferPayload {
  sourceTankId: string;
  destinationTankId: string;
  quantityLiters: number;
  reason?: string;
}