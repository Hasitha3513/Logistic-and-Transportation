export interface Trip {
  id: string;
  tripNumber: string;
  customerId?: string | null;
  departmentId?: string | null;
  projectId?: string | null;
  routeId?: string | null;
  originLocationId: string;
  destinationLocationId: string;
  requestedStartTime: string;
  requestedEndTime: string;
  priority: string;
  vehicleId?: string | null;
  driverId?: string | null;
  status: string;
  cargoDescription?: string | null;
  requiredVehicleTypeId?: string | null;
  requiredCapacityKg?: number | null;
  passengerCount?: number | null;
  customerInstructions?: string | null;
  notes?: string | null;
  actualStartTime?: string | null;
  actualEndTime?: string | null;
  startOdometerKm?: number | null;
  endOdometerKm?: number | null;
  completionRemarks?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CustomerReference {
  id: string;
  code: string;
  name: string;
}

export interface LocationReference {
  id: string;
  code: string;
  name: string;
}

export interface VehicleReference {
  id: string;
  registrationNumber: string;
  manufacturer?: string | null;
  model?: string | null;
  typeId?: string | null;
  categoryId?: string | null;
  operationalStatus?: string | null;
  capacityKg?: number | null;
  active?: boolean;
}

export interface DriverReference {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  status?: string | null;
  active?: boolean;
}

export interface AvailabilityReason {
  code: string;
  message: string;
}

export interface ResourceAvailability {
  available: boolean;
  reasons: AvailabilityReason[];
}

export interface RouteReference {
  id: string;
  code: string;
  name: string;
  originLocationId: string;
  destinationLocationId: string;
  plannedDistanceKm: number;
  estimatedDurationMinutes: number;
  active: boolean;
  stopLocationIds: string[];
}

export interface TripHistoryEntry {
  id: string;
  tripId: string;
  fromStatus?: string | null;
  toStatus?: string | null;
  action: string;
  vehicleId?: string | null;
  driverId?: string | null;
  licenseClass?: string | null;
  actor: string;
  details?: string | null;
  occurredAt: string;
}

export interface PagedTrips {
  content: Trip[];
  total: number;
  page: number;
  limit: number;
}

export type TripResponse = Trip[] | PagedTrips;
