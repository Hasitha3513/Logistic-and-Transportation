export interface FreightOrderLine { id: string; description: string; quantity: number }
export interface FreightOrder {
  id: string; orderNumber: string; customerId: string; originLocationId: string; destinationLocationId: string;
  requestedPickupAt: string; requestedDeliveryAt: string; serviceLevel: string; priority: string;
  specialHandlingInstructions?: string | null; lines: FreightOrderLine[]; version: number;
  createdAt: string; updatedAt: string; createdBy: string; updatedBy: string;
}
export interface FreightOrderPage { content: FreightOrder[]; page: number; limit: number; totalElements: number; totalPages: number }
export interface FreightOrderLinePayload { id?: string; description: string; quantity: number }
export interface FreightOrderPayload {
  customerId: string; originLocationId: string; destinationLocationId: string; requestedPickupAt: string;
  requestedDeliveryAt: string; serviceLevel: string; priority: string; specialHandlingInstructions?: string | null;
  lines: FreightOrderLinePayload[]; version?: number;
}
export interface OrganizationReference { id: string; code: string; name: string; active: boolean }
export interface FreightOrderFilters { page: number; limit: number; search?: string; customerId?: string; pickupFrom?: string; pickupTo?: string; sort?: string; direction?: string }
