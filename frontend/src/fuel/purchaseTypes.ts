export type FuelPurchaseStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'RECEIVED' | 'RECONCILED' | 'CANCELLED';
export type ReconciliationStatus = 'PENDING' | 'RECONCILED' | 'NOT_APPLICABLE';

export interface Vendor { id: string; code: string; name: string; active: boolean }
export interface FuelPrice { id: string; vendorId: string; fuelType: string; effectiveFrom: string; effectiveTo?: string | null; unitPrice: number; currencyCode: string; active: boolean; createdAt: string; updatedAt: string }
export interface FuelPurchase {
  id: string; purchaseNumber: string; vendor: Vendor; fuelStationId?: string | null; fuelType: string;
  purchaseDate: string; invoiceNumber?: string | null; invoiceDate?: string | null; quantity: number; unitPrice: number;
  subtotal: number; taxRate: number; taxAmount: number; otherCharges: number; totalAmount: number; currencyCode: string;
  status: FuelPurchaseStatus; reconciliationStatus: ReconciliationStatus; receivedQuantity?: number | null;
  quantityVariance?: number | null; expectedUnitPrice?: number | null; priceVariance?: number | null;
  destinationFuelStationId?: string | null; deliveryNoteNumber?: string | null; receivedAt?: string | null;
  approvedBy?: string | null; approvedAt?: string | null; reconciledBy?: string | null; reconciledAt?: string | null;
  reconciliationNotes?: string | null; reconciliationReference?: string | null; notes?: string | null;
  createdBy: string; createdAt: string; updatedAt: string;
}
export interface FuelPurchaseHistory { id: string; fuelPurchaseId: string; fromStatus?: FuelPurchaseStatus | null; toStatus: FuelPurchaseStatus; action: string; actorId: string; actor: string; comment?: string | null; quantityVariance?: number | null; priceVariance?: number | null; occurredAt: string }
export interface FuelPurchasePage { content: FuelPurchase[]; page: number; limit: number; totalElements: number; totalPages: number }
export interface FuelPurchasePayload { vendorId: string; fuelStationId?: string | null; fuelType: string; purchaseDate: string; invoiceNumber?: string | null; invoiceDate?: string | null; quantity: number; unitPrice: number; taxRate: number; otherCharges: number; currencyCode: string; notes?: string | null }
export interface FuelPricePayload { vendorId: string; fuelType: string; effectiveFrom: string; effectiveTo?: string | null; unitPrice: number; currencyCode: string; active: boolean }
