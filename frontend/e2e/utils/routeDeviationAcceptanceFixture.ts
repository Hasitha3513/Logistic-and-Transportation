import {execFileSync} from 'node:child_process';

const ACCEPTANCE_DATABASE='transport_logistics_acceptance';
const UUID=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export type RouteDeviationFixture={
  highApproveId:string;
  highRejectId:string;
  warningId:string;
  vehicleId:string;
};

export function seedRouteDeviationEvidence(tenantId:string,ruleId:string,routeId:string):RouteDeviationFixture{
  const tenant=checked(tenantId),rule=checked(ruleId),route=checked(routeId);
  const highApproveId=crypto.randomUUID(),highRejectId=crypto.randomUUID(),warningId=crypto.randomUUID(),vehicleId=crypto.randomUUID();
  const sql=`
    BEGIN;
    INSERT INTO tracking_route_deviation_state(
      tenant_id,vehicle_id,stable_state,availability,route_id,route_version,rule_id,
      rule_version,active_episode_id,last_source_timestamp,last_position_id)
    VALUES('${tenant}','${vehicleId}','DEVIATING','AVAILABLE','${route}','REVISION:1',
      '${rule}',1,'${highApproveId}',now(),gen_random_uuid());
    INSERT INTO tracking_route_deviation_episode(
      id,tenant_id,vehicle_id,route_id,route_version,rule_id,rule_version,
      configured_tolerance_meters,effective_tolerance_meters,first_candidate_position_id,
      confirming_position_id,start_source_timestamp,confirmation_source_timestamp,
      end_source_timestamp,maximum_distance_meters,eligible_outside_sample_count,severity,
      review_status,review_version,distance_high_escalated,review_rejected_escalated)
    VALUES
      ('${highApproveId}','${tenant}','${vehicleId}','${route}','REVISION:1','${rule}',1,
       100,110,gen_random_uuid(),gen_random_uuid(),now()-interval '3 minutes',now()-interval '2 minutes',
       null,180,2,'HIGH','PENDING',0,false,false),
      ('${highRejectId}','${tenant}',gen_random_uuid(),'${route}','REVISION:1','${rule}',1,
       100,110,gen_random_uuid(),gen_random_uuid(),now()-interval '8 minutes',now()-interval '7 minutes',
       now()-interval '6 minutes',190,2,'HIGH','PENDING',0,false,false),
      ('${warningId}','${tenant}',gen_random_uuid(),'${route}','REVISION:1','${rule}',1,
       100,110,gen_random_uuid(),gen_random_uuid(),now()-interval '13 minutes',now()-interval '12 minutes',
       now()-interval '11 minutes',140,2,'WARNING','NOT_REQUIRED',0,false,false);
    INSERT INTO tracking_route_deviation_episode(
      id,tenant_id,vehicle_id,route_id,route_version,rule_id,rule_version,
      configured_tolerance_meters,effective_tolerance_meters,first_candidate_position_id,
      confirming_position_id,start_source_timestamp,confirmation_source_timestamp,
      end_source_timestamp,maximum_distance_meters,eligible_outside_sample_count,severity,
      review_status,review_version,distance_high_escalated,review_rejected_escalated)
    SELECT gen_random_uuid(),'${tenant}',gen_random_uuid(),'${route}','REVISION:1','${rule}',1,
      100,110,gen_random_uuid(),gen_random_uuid(),now()-(n||' minutes')::interval,
      now()-((n-1)||' minutes')::interval,now()-((n-2)||' minutes')::interval,
      140,2,'WARNING','NOT_REQUIRED',0,false,false
    FROM generate_series(20,121) AS n;
    COMMIT;`;
  execute(sql);
  return {highApproveId,highRejectId,warningId,vehicleId};
}

function execute(sql:string){
  if(process.env.PGDATABASE!==ACCEPTANCE_DATABASE)throw new Error(`Route-deviation fixtures require PGDATABASE=${ACCEPTANCE_DATABASE}`);
  const user=process.env.PGUSER;if(!user)throw new Error('Route-deviation fixtures require PGUSER');
  execFileSync('docker',['compose','exec','-T','postgres','psql','-X','--no-psqlrc','-U',user,
    '-d',ACCEPTANCE_DATABASE,'-v','ON_ERROR_STOP=1','-c',sql],{env:process.env,stdio:['ignore','pipe','pipe']});
}

function checked(value:string){if(!UUID.test(value))throw new Error('Fixture identifier must be a UUID');return value.toLowerCase();}
