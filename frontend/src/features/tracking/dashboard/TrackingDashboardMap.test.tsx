import { render,screen } from '@testing-library/react';
import { describe,expect,it } from 'vitest';
import { TrackingDashboardMap } from './TrackingDashboardMap';
import type { DashboardVehicle } from './types';

const vehicle:DashboardVehicle={vehicleId:'vehicle-1',freshness:'LIVE',connectivity:'CONNECTED',motion:'MOVING',latestTrusted:{sourceTimestamp:'2026-01-01T00:00:00Z',receivedAt:'2026-01-01T00:00:01Z',trust:'TRUSTED',latitude:7,longitude:80,speedKph:20},incidentCounts:{},journeyReplayAvailable:false};
describe('TrackingDashboardMap',()=>{
  it('renders authorized trusted positions and density without external map coupling',()=>{render(<TrackingDashboardMap vehicles={[vehicle]} heatCells={[{latitude:7,longitude:80,count:1}]}/>);expect(screen.getByRole('img',{name:'Fleet map with 1 trusted positions'})).toBeInTheDocument()});
  it('keeps a table-first status when the map fails',()=>{render(<TrackingDashboardMap vehicles={[vehicle]} heatCells={[]} unavailable/>);expect(screen.getByRole('status')).toHaveTextContent('fleet table and incident evidence remain available')});
});
