export type DeliveryExceptionType =
  | 'DAMAGED_DELIVERY'
  | 'WRONG_ADDRESS'
  | 'PARTIAL_DELIVERY'
  | 'OTP_MISMATCH'
  | 'RECIPIENT_REFUSAL';

export type DeliveryExceptionSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type DeliveryExceptionStatus = 'OPEN' | 'UNDER_INVESTIGATION' | 'RESOLVED' | 'CANCELLED';

export type DeliveryExceptionResolutionCode =
  | 'RETURN_TO_BASE_APPROVED'
  | 'ACCEPTED_AS_IS'
  | 'REDELIVERY_APPROVED'
  | 'ADDRESS_CORRECTED'
  | 'PARTIAL_ACCEPTED_CLOSE'
  | 'OTP_OVERRIDDEN_BY_MANAGER'
  | 'NEW_OTP_REQUESTED'
  | 'REFUSAL_CONFIRMED_RTO';

export type DeliveryFailureDisposition =
  | 'RETURN_TO_BASE_REQUIRED'
  | 'REDELIVERY_ELIGIBLE'
  | 'ESCALATED';

export interface DeliveryExceptionEvidence {
  id: string;
  storageReference: string;
  detectedContentType: string;
  contentLength: number;
  sha256Checksum: string;
  originalFilename?: string;
  createdBy: string;
  createdAt: string;
}

export interface DeliveryExceptionResolution {
  resolutionCode: DeliveryExceptionResolutionCode;
  resolutionNotes: string;
  followUpDisposition?: DeliveryFailureDisposition;
  resolvedAt: string;
  resolvedBy: string;
}

export interface DeliveryExceptionCase {
  id: string;
  deliveryOrderId: string;
  deliveryAttemptId?: string;
  exceptionType: DeliveryExceptionType;
  severity: DeliveryExceptionSeverity;
  status: DeliveryExceptionStatus;
  description: string;
  correctedLocationId?: string;
  otpAttemptReference?: string;
  deliveredItemsDescription?: string;
  undeliveredItemsDescription?: string;
  quantityDelivered?: number;
  quantityUndelivered?: number;
  resolution?: DeliveryExceptionResolution;
  version: number;
  reportedAt: string;
  reportedBy: string;
  resolvedAt?: string;
  resolvedBy?: string;
  evidence: DeliveryExceptionEvidence[];
}

export interface ReportExceptionPayload {
  deliveryAttemptId?: string;
  exceptionType: DeliveryExceptionType;
  severity?: DeliveryExceptionSeverity;
  description: string;
  correctedLocationId?: string;
  otpAttemptReference?: string;
  deliveredItemsDescription?: string;
  undeliveredItemsDescription?: string;
  quantityDelivered?: number;
  quantityUndelivered?: number;
  evidenceList?: { originalFilename: string; base64Content: string }[];
}

export interface ResolveExceptionPayload {
  expectedVersion: number;
  resolutionCode: DeliveryExceptionResolutionCode;
  resolutionNotes: string;
  correctedLocationId?: string;
  followUpDisposition?: DeliveryFailureDisposition;
}

export interface CancelExceptionPayload {
  expectedVersion: number;
  reason?: string;
}
