export type DeliveryPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type DeliveryServiceType = 'STANDARD' | 'EXPRESS' | 'SAME_DAY' | 'SCHEDULED';
export type DeliveryStatus = 'DRAFT' | 'READY_FOR_ASSIGNMENT' | 'DELIVERED';

export type PodStatus = 'DRAFT' | 'FINALIZED';
export type PodEvidenceType = 'SIGNATURE' | 'PHOTO' | 'BARCODE';
export interface PodEvidence { id: string; type: PodEvidenceType; barcodeValue?: string | null; contentType?: string | null; contentLength: number; checksum?: string | null; originalFilename?: string | null; captureSource: string; createdAt: string }
export interface ProofOfDelivery { id: string; deliveryOrderId: string; status: PodStatus; deviceCapturedAt?: string | null; latitude?: number | null; longitude?: number | null; accuracyMeters?: number | null; signerName?: string | null; signerRelationship?: string | null; acceptedAt?: string | null; acceptedBy?: string | null; version: number; evidence: PodEvidence[] }

export interface DeliveryOrder {
  id: string; deliveryNumber: string; customerId: string; originLocationId: string; destinationLocationId: string;
  priority: DeliveryPriority; serviceType: DeliveryServiceType; windowStart: string; windowEnd: string;
  instructions: string | null; status: DeliveryStatus; version: number; createdAt: string; updatedAt: string;
  createdBy: string; updatedBy: string;
}
export interface DeliveryOrderPayload {
  customerId: string; originLocationId: string; destinationLocationId: string; priority: DeliveryPriority;
  serviceType: DeliveryServiceType; windowStart: string; windowEnd: string; instructions: string | null; version?: number;
}
export interface DeliveryOrderPage { content: DeliveryOrder[]; page: number; size: number; totalElements: number; totalPages: number }
export interface DeliveryOrderFilters { page?: number; size?: number; search?: string; status?: DeliveryStatus; customerId?: string }
export interface OrganizationReference { id: string; code: string; name: string; active: boolean }
