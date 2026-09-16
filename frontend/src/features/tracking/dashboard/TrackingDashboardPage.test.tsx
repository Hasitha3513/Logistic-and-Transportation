import { render,screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach,describe,expect,it,vi } from 'vitest';
import TrackingDashboardPage from './TrackingDashboardPage';
import type { TrackingDashboard } from './types';

const permissions=new Set<string>();
const dashboard:TrackingDashboard={evaluatedAt:'2026-01-01T00:00:00Z',lastSuccessfulRefreshAt:'2026-01-01T00:00:00Z',sourceStatus:'DEGRADED',incidentSourceStatuses:{GEOFENCE:'AVAILABLE'},producerStatuses:{GEOFENCE:'ACCEPTED',SPEED:'FIELD_FIDELITY_PENDING'},summary:{matchingVehicleCount:1,freshnessCounts:{LIVE:1},connectivityCounts:{CONNECTED:1},motionCounts:{MOVING:1}},vehicles:[{vehicleId:'vehicle-1',freshness:'LIVE',connectivity:'CONNECTED',motion:'MOVING',latestTrusted:{sourceTimestamp:'2026-01-01T00:00:00Z',receivedAt:'2026-01-01T00:00:01Z',trust:'TRUSTED',latitude:7,longitude:80,speedKph:12},incidentCounts:{GEOFENCE:1},journeyReplayAvailable:false}],incidents:[{evidenceId:'event-1',type:'GEOFENCE',vehicleId:'vehicle-1',severity:'WARNING',status:'ENTERED',sourceTimestamp:'2026-01-01T00:00:00Z',producerStatus:'ACCEPTED'}],heatMapCells:[{latitude:7,longitude:80,count:1}]};
const query={data:dashboard,isError:false,isLoading:false,isFetching:false,error:null,refetch:vi.fn()};
vi.mock('../../../auth/AuthContext',()=>({useAuth:()=>({hasPermission:(permission:string)=>permissions.has(permission)})}));
vi.mock('../../../notifications/useNotifications',()=>({useUnreadNotificationCount:()=>({data:3})}));
vi.mock('./hooks',()=>({useTrackingDashboard:()=>query}));

describe('TrackingDashboardPage',()=>{
  beforeEach(()=>{permissions.clear();permissions.add('TRACKING_DASHBOARD_VIEW')});
  it('renders truthful degraded state, table-first evidence and producer labels',()=>{render(<MemoryRouter><TrackingDashboardPage/></MemoryRouter>);expect(screen.getByText('Live source degraded')).toBeInTheDocument();expect(screen.getByRole('table',{name:'Tracking dashboard Vehicle evidence'})).toBeInTheDocument();expect(screen.getByText('Field fidelity pending')).toBeInTheDocument();expect(screen.getAllByText('Observed motion').length).toBeGreaterThan(0);expect(screen.getByText(/not engine or idle state/i)).toBeInTheDocument()});
  it('does not expose the page from broad Tracking permissions alone',()=>{permissions.clear();permissions.add('TRACKING_VIEW');render(<MemoryRouter initialEntries={['/tracking/dashboard']}><TrackingDashboardPage/></MemoryRouter>);expect(screen.queryByRole('table',{name:'Tracking dashboard Vehicle evidence'})).not.toBeInTheDocument()});
});
