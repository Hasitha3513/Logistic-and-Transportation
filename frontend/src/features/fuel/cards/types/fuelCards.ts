export interface FuelCard {
  id: string; providerId: string; alias: string; maskedIdentifier: string; lastFour?: string;
  expiryMonth: number; expiryYear: number; status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'BLOCKED' | 'EXPIRED' | 'CANCELLED';
  providerSyncStatus: 'NOT_CONFIGURED'; version: number; createdAt: string; updatedAt: string;
}
export interface FuelCardTransaction {
  id: string; providerTransactionId: string; cardId: string; transactionTimestamp: string;
  fuelType: string; quantityLitres: number; totalAmount: number; currency: string;
  localStatus: string; indicators: string[]; reconciledPurchaseId?: string; version?: number;
  originalProviderTransactionId?: string; transactionKind?: string;
}
export interface FuelCardImportBatch {
  id: string; providerBatchId: string; transactionCount: number; importedCount: number;
  reviewCount: number; createdAt: string;
}
export interface FuelCardBinding { id: string; bindingType: 'VEHICLE' | 'DRIVER'; bindingId: string; effectiveFrom: string; effectiveTo?: string; reason: string }
export interface FuelCardRestriction {
  currency: string; maxTransactionAmount: number; maxDailyAmount: number; maxMonthlyAmount: number;
  maxDailyLitres: number; allowedFuelTypes: string[]; allowedStationReferences: string[]; version: number;
}
export interface FuelCardHistory { id: string; action: string; result: string; reasonCode?: string; actorId: string; createdAt: string }
export interface CreateFuelCard {
  providerId: string; alias: string; providerCardReference: string; maskedIdentifier: string;
  lastFour?: string; expiryMonth: number; expiryYear: number;
}
