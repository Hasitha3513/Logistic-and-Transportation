import type { DashboardVehicle, HeatCell } from './types';

const coordinate=(vehicle:DashboardVehicle)=>vehicle.latestTrusted?.latitude===undefined||vehicle.latestTrusted.longitude===undefined?undefined:{latitude:vehicle.latestTrusted.latitude,longitude:vehicle.latestTrusted.longitude};

export function TrackingDashboardMap({vehicles,heatCells,unavailable=false}:{vehicles:DashboardVehicle[];heatCells:HeatCell[];unavailable?:boolean}){
  const markers=vehicles.map(vehicle=>({vehicle,coordinate:coordinate(vehicle)})).filter(value=>value.coordinate!==undefined) as {vehicle:DashboardVehicle;coordinate:{latitude:number;longitude:number}}[];
  if(unavailable)return <div className="tracking-dashboard__map-notice" role="status">Map unavailable. The fleet table and incident evidence remain available.</div>;
  if(!markers.length&&!heatCells.length)return <div className="tracking-dashboard__map-notice" role="img" aria-label="Fleet map with no authorized positions">No authorized trusted positions are available for the map.</div>;
  const coordinates=[...markers.map(value=>value.coordinate),...heatCells];
  const xs=coordinates.map(value=>value.longitude),ys=coordinates.map(value=>value.latitude);
  const minX=Math.min(...xs),maxX=Math.max(...xs),minY=Math.min(...ys),maxY=Math.max(...ys);
  const project=(value:{longitude:number;latitude:number})=>({x:6+88*(value.longitude-minX)/(maxX-minX||1),y:94-88*(value.latitude-minY)/(maxY-minY||1)});
  return <svg className="tracking-dashboard__map" role="img" aria-label={`Fleet map with ${markers.length} trusted positions`} viewBox="0 0 100 100">
    <rect width="100" height="100" fill="#f6f8fb"/>
    {heatCells.map((cell,index)=>{const point=project(cell);return <circle key={`${cell.latitude}-${cell.longitude}-${index}`} cx={point.x} cy={point.y} r={Math.min(10,3+cell.count)} fill="#fa8c16" opacity="0.24"><title>{`${cell.count} live or recent vehicles`}</title></circle>})}
    {markers.map(({vehicle,coordinate:value})=>{const point=project(value);return <circle key={vehicle.vehicleId} cx={point.x} cy={point.y} r="2.2" fill={vehicle.freshness==='LIVE'?'#389e0d':vehicle.freshness==='RECENT'?'#1677ff':'#8c8c8c'}><title>{`${vehicle.vehicleId}: ${vehicle.freshness} / ${vehicle.connectivity}`}</title></circle>})}
  </svg>;
}
