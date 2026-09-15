import {execFileSync} from 'node:child_process';
const ACCEPTANCE_DATABASE='transport_logistics_acceptance';
const UUID=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function seedJourneyReplayEvidence(tenantId:string){
  const tenant=checked(tenantId),vehicleId=crypto.randomUUID(),deviceId=crypto.randomUUID();
  const base=Date.now()-20*60_000;
  const points=[
    [0,6.9271,79.8612,20],[120,6.9280,79.8620,0],[180,6.92801,79.86201,0],
    [240,6.92800,79.86200,0],[300,6.92801,79.86200,0],[360,6.92800,79.86201,0],
    [420,6.92801,79.86201,0],[660,6.9350,79.8700,25],
  ] as const;
  const values=points.map(([offset,latitude,longitude,speed],index)=>{
    const source=new Date(base+offset*1000).toISOString(),id=crypto.randomUUID();
    const dedupe=(index.toString(16).padStart(2,'0')+'a'.repeat(62)).slice(0,64);
    return `('${tenant}','${source}','${id}','${deviceId}','${vehicleId}','FIXTURE','us53-${id}',${index+1},'${dedupe}','${source}',${latitude},${longitude},5,${speed},'UNKNOWN','TRUSTED','ACCEPTABLE','IN_ORDER','TIMESCALE_RAW_180_DAYS','V86',now()+interval '180 days','{}',1)`;
  }).join(',');
  execute(`INSERT INTO tracking_position_history(tenant_id,source_timestamp,id,device_id,vehicle_id,provider_alias,provider_message_id,provider_sequence,dedupe_identity,received_at,latitude,longitude,horizontal_accuracy_meters,speed_kph,engine_state,trust,quality,ordering_classification,retention_policy,retention_policy_version,retain_until,safe_metadata,event_version) VALUES ${values};`);
  return{vehicleId,from:new Date(base-60_000).toISOString(),to:new Date(base+12*60_000).toISOString()};
}
function execute(sql:string){if(process.env.PGDATABASE!==ACCEPTANCE_DATABASE)throw new Error(`Journey fixtures require PGDATABASE=${ACCEPTANCE_DATABASE}`);const user=process.env.PGUSER;if(!user)throw new Error('Journey fixtures require PGUSER');execFileSync('docker',['compose','exec','-T','postgres','psql','-X','--no-psqlrc','-U',user,'-d',ACCEPTANCE_DATABASE,'-v','ON_ERROR_STOP=1','-c',sql],{env:process.env,stdio:['ignore','pipe','pipe']});}
function checked(value:string){if(!UUID.test(value))throw new Error('Fixture tenant must be a UUID');return value.toLowerCase()}
