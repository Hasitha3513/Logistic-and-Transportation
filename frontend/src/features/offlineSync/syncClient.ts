import axios from 'axios';
import { api } from '../../api/client';
import type {
  OfflineOperation,
  OfflineServerOperation,
  OfflineServerResultStatus,
  OfflineSyncBatchResponse,
  OfflineSyncOperationResult,
} from './types';

const RESULT_STATUSES: readonly OfflineServerResultStatus[] = [
  'APPLIED',
  'ALREADY_APPLIED',
  'REJECTED',
  'CONFLICT',
  'RETRYABLE_ERROR',
];

export type OfflineSyncClientErrorKind = 'NETWORK' | 'HTTP' | 'PROTOCOL';

export class OfflineSyncClientError extends Error {
  constructor(
    readonly kind: OfflineSyncClientErrorKind,
    readonly code: string,
    message: string,
    readonly httpStatus?: number,
  ) {
    super(message);
    this.name = 'OfflineSyncClientError';
  }
}

export interface OfflineSyncClient {
  synchronize(operations: readonly OfflineServerOperation[]): Promise<OfflineSyncBatchResponse>;
}

export function toServerOperation(operation: OfflineOperation): OfflineServerOperation {
  return {
    operationId: operation.operationId,
    operationVersion: operation.operationVersion,
    operationType: operation.operationType,
    aggregateType: operation.aggregateType,
    aggregateId: operation.aggregateId,
    payload: operation.payload,
    clientCreatedAt: operation.clientCreatedAt,
    clientUpdatedAt: operation.clientUpdatedAt,
    clientInstanceId: operation.clientInstanceId,
    idempotencyKey: operation.idempotencyKey,
    baseVersion: operation.baseVersion,
  } as OfflineServerOperation;
}

function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw protocolError('Offline sync response must be an object');
  }
  return value as Record<string, unknown>;
}

function requiredString(value: unknown, field: string): string {
  if (typeof value !== 'string' || value.length === 0) throw protocolError(`${field} must be a string`);
  return value;
}

function nullableString(value: unknown, field: string): string | null {
  if (value === null) return null;
  return requiredString(value, field);
}

function timestamp(value: unknown, field: string): string {
  const parsed = requiredString(value, field);
  if (!Number.isFinite(Date.parse(parsed))) throw protocolError(`${field} must be an ISO-8601 timestamp`);
  return parsed;
}

function resultStatus(value: unknown): OfflineServerResultStatus {
  if (typeof value !== 'string' || !RESULT_STATUSES.includes(value as OfflineServerResultStatus)) {
    throw protocolError('Offline sync result status is unsupported');
  }
  return value as OfflineServerResultStatus;
}

function parseResult(value: unknown, index: number): OfflineSyncOperationResult {
  const source = record(value);
  const currentVersion = source.currentVersion;
  if (currentVersion !== null && (typeof currentVersion !== 'number' || !Number.isInteger(currentVersion))) {
    throw protocolError(`results[${index}].currentVersion must be an integer or null`);
  }
  return {
    operationId: requiredString(source.operationId, `results[${index}].operationId`),
    status: resultStatus(source.status),
    serverTimestamp: timestamp(source.serverTimestamp, `results[${index}].serverTimestamp`),
    aggregateId: requiredString(source.aggregateId, `results[${index}].aggregateId`),
    currentVersion: currentVersion as number | null,
    errorCode: nullableString(source.errorCode, `results[${index}].errorCode`),
    message: nullableString(source.message, `results[${index}].message`),
  };
}

function parseResponse(value: unknown): OfflineSyncBatchResponse {
  const source = record(value);
  if (!Array.isArray(source.results)) throw protocolError('Offline sync results must be an array');
  return {
    serverTimestamp: timestamp(source.serverTimestamp, 'serverTimestamp'),
    results: source.results.map(parseResult),
  };
}

function protocolError(message: string): OfflineSyncClientError {
  return new OfflineSyncClientError('PROTOCOL', 'OFFLINE_SYNC_PROTOCOL_ERROR', message);
}

export class AxiosOfflineSyncClient implements OfflineSyncClient {
  async synchronize(operations: readonly OfflineServerOperation[]): Promise<OfflineSyncBatchResponse> {
    try {
      const response = await api.post<unknown>('/offline-sync/operations', { operations });
      return parseResponse(response.data);
    } catch (error: unknown) {
      if (error instanceof OfflineSyncClientError) throw error;
      if (axios.isAxiosError(error)) {
        if (error.response) {
          throw new OfflineSyncClientError('HTTP', 'OFFLINE_SYNC_HTTP_ERROR', 'Offline sync request failed', error.response.status);
        }
        throw new OfflineSyncClientError('NETWORK', 'OFFLINE_SYNC_NETWORK_ERROR', 'Offline sync backend is unreachable');
      }
      throw protocolError('Offline sync response could not be processed');
    }
  }
}

export const offlineSyncClient = new AxiosOfflineSyncClient();
