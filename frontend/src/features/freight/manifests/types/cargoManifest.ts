export interface ManifestItem {
  id: string;
  freightOrderLineId: string;
  description: string;
  quantity: number;
  packingInformation: string;
  commodityClassification: string;
  customsApplicable: boolean;
  customsInformation?: string | null;
  hazardous: boolean;
  hazardousClassification?: string | null;
  hazardousDetails?: string | null;
  fragile?: boolean | null;
  temperatureSensitive?: boolean | null;
  unitWeight?: number | null;
  weightUnit?: string | null;
  length?: number | null;
  width?: number | null;
  height?: number | null;
  dimensionUnit?: string | null;
}

export interface CargoManifest {
  id: string;
  manifestNumber: string;
  freightOrderId: string;
  freightOrderNumber: string;
  finalized: boolean;
  items: ManifestItem[];
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  finalizedAt?: string | null;
  finalizedBy?: string | null;
}

export interface ManifestPage {
  content: CargoManifest[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
}

export interface ManifestFailure {
  code: string;
  field: string;
  message: string;
}

export interface ManifestReadiness {
  ready: boolean;
  failures: ManifestFailure[];
}

export interface ManifestItemPayload {
  version: number;
  freightOrderLineId: string;
  description: string;
  quantity: number;
  packingInformation: string;
  commodityClassification: string;
  customsApplicable: boolean;
  customsInformation?: string | null;
  hazardous: boolean;
  hazardousClassification?: string | null;
  hazardousDetails?: string | null;
  fragile: boolean;
  temperatureSensitive: boolean;
  unitWeight?: number | null;
  weightUnit?: string | null;
  length?: number | null;
  width?: number | null;
  height?: number | null;
  dimensionUnit?: string | null;
}

export interface ManifestFilters {
  page: number;
  limit: number;
  search?: string;
  freightOrderId?: string;
  finalized?: boolean;
}
