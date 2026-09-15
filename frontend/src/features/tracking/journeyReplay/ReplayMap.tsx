import type { ReplayPoint, ReplayStop } from './types';

const breakPath = (point: ReplayPoint) => point.trust !== 'TRUSTED' || point.qualityFlags.some(flag => ['TIME_GAP', 'UNTRUSTED', 'PARTIAL_RETENTION'].includes(flag));

export function ReplayMap({ points, stops, activeIndex, unavailable=false }:{points:ReplayPoint[];stops:ReplayStop[];activeIndex:number;unavailable?:boolean}) {
  if (unavailable) return <div className="replay-map replay-map--notice" role="status">Map unavailable. Timeline, stops, quality evidence and playback remain available.</div>;
  if (!points.length) return <div className="replay-map replay-map--notice" role="img" aria-label="Journey map with no points">Load a journey to display its path.</div>;
  const longitudes=points.map(p=>p.coordinate.longitude),latitudes=points.map(p=>p.coordinate.latitude);
  const minX=Math.min(...longitudes),maxX=Math.max(...longitudes),minY=Math.min(...latitudes),maxY=Math.max(...latitudes);
  const project=(coordinate:{longitude:number;latitude:number})=>({x:5+90*(coordinate.longitude-minX)/(maxX-minX||1),y:95-90*(coordinate.latitude-minY)/(maxY-minY||1)});
  const segments:ReplayPoint[][]=[];let current:ReplayPoint[]=[];
  points.forEach((point,index)=>{if(index>0&&breakPath(point)){if(current.length)segments.push(current);current=[];}current.push(point)});if(current.length)segments.push(current);
  const active=project(points[Math.min(activeIndex,points.length-1)].coordinate);
  return <svg className="replay-map" role="img" aria-label={`Journey map with ${points.length} chronological points`} viewBox="0 0 100 100">
    <rect width="100" height="100" fill="#f6f8fb"/>
    {segments.map((segment,index)=><polyline key={index} points={segment.map(p=>{const c=project(p.coordinate);return `${c.x},${c.y}`}).join(' ')} fill="none" stroke="#1677ff" strokeWidth="1.5"/>) }
    {stops.map(stop=>{const c=project(stop.centroid);return <circle key={stop.stopId} cx={c.x} cy={c.y} r="2.5" fill="#faad14"><title>{`Stop from ${stop.start} to ${stop.end}`}</title></circle>})}
    <circle cx={active.x} cy={active.y} r="2.5" fill="#cf1322" aria-label="Current playback position"/>
  </svg>;
}
