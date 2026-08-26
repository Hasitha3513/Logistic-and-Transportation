export const vehicleOperationalStatuses = [
  'AVAILABLE',
  'ALLOCATED',
  'MAINTENANCE',
  'OUT_OF_SERVICE',
  'BROKEN_DOWN',
] as const;

export const vehicleOwnershipTypes = ['COMPANY_OWNED', 'LEASED'] as const;

export type VehicleOperationalStatus = typeof vehicleOperationalStatuses[number];
export type VehicleOwnershipType = typeof vehicleOwnershipTypes[number];

export interface Vehicle {
  id: string;
  registrationNumber: string;
  chassisNumber?: string | null;
  engineNumber?: string | null;
  categoryId: string;
  typeId: string;
  manufacturer?: string | null;
  model?: string | null;
  manufactureYear?: number | null;
  ownershipType: VehicleOwnershipType;
  operationalStatus: VehicleOperationalStatus;
  currentOdometerKm?: number | null;
  engineHours?: number | null;
  capacityKg?: number | null;
  active: boolean;
}

export type VehicleInput = Omit<Vehicle, 'id'>;

export interface VehicleCategoryReference {
  id: string;
  name: string;
  active?: boolean;
}

export interface VehicleTypeReference {
  id: string;
  categoryId: string;
  name: string;
  active?: boolean;
}

export interface VehicleDocument {
  id: string;
  vehicleId?: string;
  documentType?: string;
  documentNumber?: string;
  issueDate?: string | null;
  expiryDate?: string | null;
  fileReference?: string | null;
  mandatoryForDispatch?: boolean;
  status?: string;
  active?: boolean;
}

export interface VehicleFilters {
  search?: string;
  ownershipType?: VehicleOwnershipType;
  operationalStatus?: VehicleOperationalStatus;
}

export interface VehicleApiError {
  code?: string;
  message?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
}
