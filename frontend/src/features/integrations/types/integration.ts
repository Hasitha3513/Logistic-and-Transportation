export type IntegrationLifecycle = 'DRAFT' | 'ACTIVE' | 'DISABLED';
export type IntegrationHealth = 'UNKNOWN' | 'HEALTHY' | 'DEGRADED' | 'UNAVAILABLE' | 'AUTH_FAILED';
export type MappingFormat = 'STRING' | 'ISO_DATE_TIME' | 'DECIMAL' | 'BOOLEAN' | 'UUID' | 'ENUM';

export interface IntegrationRule {
  sourceField?: string;
  targetField: string;
  defaultValue?: string;
  format: MappingFormat;
  omitIfNull: boolean;
  required: boolean;
}

export interface IntegrationMapping {
  id: string;
  mappingKey: string;
  mappingVersion: number;
  sourceContract: string;
  sourceVersion: number;
  targetSchema: string;
  targetVersion: number;
  definitionHash: string;
  lifecycle: 'ACTIVE' | 'SUPERSEDED';
  rules: IntegrationRule[];
}

export interface Integration {
  id: string;
  name: string;
  type: 'FILE_EXCHANGE';
  protocol: 'FILE_JSON_V1';
  direction: 'OUTBOUND';
  endpointAlias: string;
  credentialConfigured: boolean;
  credentialReferenceLabel?: string;
  dataClassification: 'INTERNAL_OPERATIONAL_NON_SENSITIVE';
  retryPolicy: 'US73_BOUNDED_V1';
  lifecycle: IntegrationLifecycle;
  health: IntegrationHealth;
  lastTestedAt?: string;
  lastSuccessfulExchangeAt?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  mapping: IntegrationMapping;
}

export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

export interface IntegrationPayload {
  name: string;
  type?: 'FILE_EXCHANGE';
  protocol?: 'FILE_JSON_V1';
  direction?: 'OUTBOUND';
  endpointAlias: string;
  credentialReference?: string;
  dataClassification?: 'INTERNAL_OPERATIONAL_NON_SENSITIVE';
  version?: number;
  mapping: Omit<IntegrationMapping, 'id' | 'mappingVersion' | 'definitionHash' | 'lifecycle'>;
}

export interface IntegrationAttempt {
  attemptNumber: number; startedAt: string; completedAt: string; latencyMillis: number;
  outcome: 'SUCCEEDED' | 'RETRYABLE_FAILURE' | 'PERMANENT_FAILURE'; errorCode?: string;
  externalCorrelationId?: string; targetFilename?: string;
}

export interface IntegrationExchange {
  id: string; sourceEventId: string; sourceEventType: string; mappingVersionId: string;
  mappingDefinitionHash: string; payloadHash: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'RETRY_SCHEDULED' | 'SUCCEEDED' | 'FAILED';
  attemptCount: number; nextAttemptAt: string; externalCorrelationId?: string; targetFilename?: string;
  lastErrorCode?: string; createdAt: string; updatedAt: string; completedAt?: string; attempts: IntegrationAttempt[];
}
