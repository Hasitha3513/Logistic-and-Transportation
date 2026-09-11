import { z } from 'zod';
import type { GeofenceVertex } from '../types';

const vertex=z.object({longitude:z.number().finite().min(-180).max(180),latitude:z.number().finite().min(-90).max(90)});
const same=(a:GeofenceVertex,b:GeofenceVertex)=>a.longitude===b.longitude&&a.latitude===b.latitude;
const area=(points:GeofenceVertex[])=>Math.abs(points.reduce((sum,p,index)=>{const next=points[(index+1)%points.length];return sum+p.longitude*next.latitude-next.longitude*p.latitude;},0)/2);
export const geofenceSchema=z.object({name:z.string().trim().min(1).max(160),type:z.enum(['DEPOT','CUSTOMER_SITE','UNAUTHORIZED_ZONE']),locationId:z.string().trim().optional(),polygon:z.array(vertex).min(3).max(100),alertPolicy:z.object({alertOnEntry:z.boolean(),alertOnExit:z.boolean()})}).superRefine((value,ctx)=>{
 if(value.type!=='UNAUTHORIZED_ZONE'&&!value.locationId)ctx.addIssue({code:'custom',path:['locationId'],message:'Location is required for depot and customer-site geofences'});
 if(value.type==='UNAUTHORIZED_ZONE'&&value.locationId)ctx.addIssue({code:'custom',path:['locationId'],message:'Unauthorized zones cannot reference a location'});
 if(value.polygon.some((point,index)=>index>0&&same(point,value.polygon[index-1])))ctx.addIssue({code:'custom',path:['polygon'],message:'Consecutive vertices must be different'});
 if(value.polygon.length>2&&same(value.polygon[0],value.polygon.at(-1)!))ctx.addIssue({code:'custom',path:['polygon'],message:'Enter an open ring; do not repeat the first vertex'});
 if(value.polygon.length>2&&area(value.polygon)===0)ctx.addIssue({code:'custom',path:['polygon'],message:'Polygon must enclose a non-zero area'});
 if(value.type==='UNAUTHORIZED_ZONE'&&!value.alertPolicy.alertOnEntry)ctx.addIssue({code:'custom',path:['alertPolicy','alertOnEntry'],message:'Unauthorized-zone entry alerts are mandatory'});
});
export type GeofenceFormValues=z.infer<typeof geofenceSchema>;
