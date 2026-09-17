import { execFileSync } from 'node:child_process';
const ACCEPTANCE_DATABASE = 'transport_logistics_acceptance';
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function seedGpsExceptionEvidence(tenantId: string) {
  const tenant = checked(tenantId), deviceId = crypto.randomUUID(), vehicleId = crypto.randomUUID(), episodeId = crypto.randomUUID(), evidenceId = crypto.randomUUID(), actorId = crypto.randomUUID();
  const openedAt = new Date(Date.now() - 10 * 60_000).toISOString(), observedAt = new Date(Date.now() - 5 * 60_000).toISOString();
  execute(`INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,hardware_serial_reference,lifecycle,registered_at,registered_by,version,created_at,updated_at) VALUES ('${deviceId}','${tenant}','us55-${deviceId}','FIXTURE',NULL,'ACTIVE','${openedAt}','${actorId}',0,'${openedAt}','${openedAt}'); INSERT INTO tracking_gps_exception_episode(id,tenant_id,tracking_device_id,vehicle_id,exception_type,severity,status,opened_at,last_observed_at,evidence_count,consecutive_recovery_points,version) VALUES ('${episodeId}','${tenant}','${deviceId}','${vehicleId}','PROCESSING_FAILURE','WARNING','OPEN','${openedAt}','${observedAt}',1,0,0); INSERT INTO tracking_gps_exception_evidence(id,tenant_id,episode_id,evidence_identity,assessed_at,trust,ordering_classification,reliability_state,quality_codes,transition) VALUES ('${evidenceId}','${tenant}','${episodeId}','${'a'.repeat(64)}','${observedAt}','UNKNOWN','IN_ORDER','DEGRADED','CLOCK_SKEW','OPENED');`);
  return { episodeId, vehicleId, deviceId, from: new Date(Date.now() - 24 * 60 * 60_000).toISOString(), to: new Date(Date.now() + 60_000).toISOString() };
}

function execute(sql: string) {
  if (process.env.PGDATABASE !== ACCEPTANCE_DATABASE) throw new Error(`GPS exception fixtures require PGDATABASE=${ACCEPTANCE_DATABASE}`);
  const user = process.env.PGUSER; if (!user) throw new Error('GPS exception fixtures require PGUSER');
  execFileSync('docker', ['compose', 'exec', '-T', 'postgres', 'psql', '-X', '--no-psqlrc', '-U', user, '-d', ACCEPTANCE_DATABASE, '-v', 'ON_ERROR_STOP=1', '-c', sql], { env: process.env, stdio: ['ignore', 'pipe', 'pipe'] });
}
function checked(value: string) { if (!UUID.test(value)) throw new Error('Fixture tenant must be a UUID'); return value.toLowerCase(); }
