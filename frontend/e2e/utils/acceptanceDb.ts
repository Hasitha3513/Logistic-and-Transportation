import { execFileSync } from 'node:child_process';

const ACCEPTANCE_DATABASE = 'transport_logistics_acceptance';
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export type NegativeSignalState = {
  sourceEventId: string;
  activeCaseCount: number;
};

export type BunkerLedgerState = {
  stock: string;
  movementCount: number;
  latestSequence: number;
};

export type HandoffState = {
  handoffId: string;
  eventId: string;
  status: string;
  attemptCount: number;
  operationsCaseCount: number;
};

export type CorrectionAttemptState = {
  distinctIdempotencyKeys: number;
  successes: number;
  failures: number;
  noopReplays: number;
};

export type FuelCardIndicatorState = {
  indicatorId: string;
  transactionId: string;
  code: string;
  providerTransactionId: string;
  canonicalHash: string;
  localStatus: string;
};

export function latestFuelCardIndicatorState(): FuelCardIndicatorState {
  const row = select(`
    SELECT i.id::text, i.transaction_id::text, i.code,
           t.provider_transaction_id, t.canonical_hash, t.local_status
      FROM fuel_card_transaction_indicator i
      JOIN fuel_card_transaction t
        ON t.tenant_id = i.tenant_id AND t.id = i.transaction_id
     WHERE NOT EXISTS (
       SELECT 1 FROM fuel_exception_case c
        WHERE c.tenant_id = i.tenant_id
          AND c.category = 'FUEL_CARD_POLICY_DEVIATION'
          AND c.source_type = 'FUEL_CARD_INDICATOR'
          AND c.source_id = i.id
          AND c.lifecycle <> 'RESOLVED')
     ORDER BY i.created_at DESC, i.id DESC
     LIMIT 1`);
  return {
    indicatorId: required(row[0], 'indicator_id'), transactionId: required(row[1], 'transaction_id'),
    code: required(row[2], 'indicator_code'), providerTransactionId: required(row[3], 'provider_transaction_id'),
    canonicalHash: required(row[4], 'canonical_hash'), localStatus: required(row[5], 'local_status'),
  };
}

export function fuelCardIndicatorState(indicatorId: string): FuelCardIndicatorState {
  const id = checkedUuid(indicatorId);
  const row = select(`
    SELECT i.id::text, i.transaction_id::text, i.code,
           t.provider_transaction_id, t.canonical_hash, t.local_status
      FROM fuel_card_transaction_indicator i
      JOIN fuel_card_transaction t
        ON t.tenant_id = i.tenant_id AND t.id = i.transaction_id
     WHERE i.id = '${id}'`);
  return {
    indicatorId: required(row[0], 'indicator_id'), transactionId: required(row[1], 'transaction_id'),
    code: required(row[2], 'indicator_code'), providerTransactionId: required(row[3], 'provider_transaction_id'),
    canonicalHash: required(row[4], 'canonical_hash'), localStatus: required(row[5], 'local_status'),
  };
}

export function negativeSignalState(caseId: string): NegativeSignalState {
  const id = checkedUuid(caseId);
  const row = select(`
    SELECT source_event_id::text,
           (SELECT count(*) FROM fuel_exception_case matching
             WHERE matching.tenant_id = c.tenant_id
               AND matching.category = c.category
               AND matching.source_type = c.source_type
               AND matching.source_id = c.source_id
               AND matching.lifecycle <> 'RESOLVED')
      FROM fuel_exception_case c
     WHERE c.id = '${id}'`);
  return { sourceEventId: required(row[0], 'source_event_id'), activeCaseCount: Number(row[1]) };
}

export function bunkerLedgerState(tankId: string): BunkerLedgerState {
  const id = checkedUuid(tankId);
  const row = select(`
    SELECT t.current_stock_liters::text,
           count(m.id),
           coalesce(max(m.ledger_sequence), 0)
      FROM bunker_tank t
      LEFT JOIN bunker_stock_movement m
        ON m.tenant_id = t.tenant_id AND m.tank_id = t.id
     WHERE t.id = '${id}'
     GROUP BY t.current_stock_liters`);
  return { stock: required(row[0], 'tank_stock'), movementCount: Number(row[1]), latestSequence: Number(row[2]) };
}

export function handoffState(exceptionId: string): HandoffState {
  const id = checkedUuid(exceptionId);
  const row = select(`
    SELECT h.id::text, h.handoff_event_id::text, h.status, h.attempt_count,
           (SELECT count(*) FROM operational_exception_case o
             WHERE o.tenant_id = h.tenant_id AND o.source_event_id = h.handoff_event_id)
      FROM fuel_exception_operations_handoff h
     WHERE h.exception_id = '${id}'`);
  return {
    handoffId: required(row[0], 'handoff_id'), eventId: required(row[1], 'handoff_event_id'),
    status: required(row[2], 'handoff_status'), attemptCount: Number(row[3]), operationsCaseCount: Number(row[4]),
  };
}

export function correctionAttemptState(correctionId: string): CorrectionAttemptState {
  const id = checkedUuid(correctionId);
  const row = select(`
    SELECT count(DISTINCT idempotency_key),
           count(*) FILTER (WHERE result = 'SUCCESS'),
           count(*) FILTER (WHERE result = 'FAILED'),
           count(*) FILTER (WHERE result = 'NOOP_REPLAY')
      FROM fuel_exception_correction_attempt
     WHERE correction_id = '${id}'`);
  return {
    distinctIdempotencyKeys: Number(row[0]), successes: Number(row[1]),
    failures: Number(row[2]), noopReplays: Number(row[3]),
  };
}

function select(sql: string): string[] {
  const database = process.env.PGDATABASE;
  if (database !== ACCEPTANCE_DATABASE) {
    throw new Error(`Acceptance DB observability requires PGDATABASE=${ACCEPTANCE_DATABASE}`);
  }
  const normalized = sql.trim();
  if (!normalized.startsWith('SELECT') || normalized.includes(';')) {
    throw new Error('Acceptance DB observability permits one fixed read-only SELECT only');
  }
  const user = process.env.PGUSER;
  if (!user) throw new Error('Acceptance DB observability requires PGUSER');
  const output = execFileSync('docker', ['compose', 'exec', '-T', 'postgres', 'psql', '-X', '--no-psqlrc',
    '-U', user, '-d', database, '-A', '-t', '-F', '\t', '-v', 'ON_ERROR_STOP=1',
    '-c', `BEGIN READ ONLY; ${normalized}; COMMIT`], { encoding: 'utf8', env: process.env, stdio: ['ignore', 'pipe', 'pipe'] });
  const data = output.split('\n').map(value => value.trim()).filter(value => value && value !== 'BEGIN' && value !== 'COMMIT');
  if (data.length !== 1) throw new Error(`Expected exactly one acceptance row, received ${data.length}`);
  return data[0].split('\t');
}

function checkedUuid(value: string): string {
  if (!UUID.test(value)) throw new Error('Acceptance query identifier must be a UUID');
  return value.toLowerCase();
}

function required(value: string | undefined, field: string): string {
  if (!value) throw new Error(`Persisted ${field} is missing`);
  return value;
}
