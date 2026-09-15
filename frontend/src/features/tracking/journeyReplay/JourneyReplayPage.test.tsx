import { QueryClient,QueryClientProvider } from '@tanstack/react-query';
import { fireEvent,render,screen,waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach,describe,expect,it,vi } from 'vitest';
import { journeyReplayApi } from './api';
import JourneyReplayPage from './JourneyReplayPage';
import type { ReplayPoint, ReplayPointsPage, ReplayStopsPage } from './types';

const permissions=new Set(['JOURNEY_REPLAY_VIEW']);
vi.mock('../../../auth/AuthContext',()=>({useAuth:()=>({hasPermission:(permission:string)=>permissions.has(permission)})}));
vi.mock('./api',()=>({journeyReplayApi:{points:vi.fn(),stops:vi.fn()}}));
const point:ReplayPoint={historyId:'p1',vehicleId:'v1',sourceTimestamp:'2026-01-01T00:00:00Z',receivedAt:'2026-01-01T00:00:01Z',coordinate:{latitude:7,longitude:80},trust:'TRUSTED',quality:'GOOD',ordering:'IN_ORDER',qualityFlags:[]};
const page:ReplayPointsPage={items:[point,{...point,historyId:'p2',sourceTimestamp:'2026-01-01T00:01:00Z',coordinate:{latitude:7.1,longitude:80.1},qualityFlags:['TIME_GAP']}],snapshotRecordedAt:'2026-01-01T01:00:00Z',requestedRange:{from:'2026-01-01T00:00:00Z',to:'2026-01-01T01:00:00Z'},coverage:'COMPLETE',missingIntervals:[],truncated:false,browserCeilingWarning:false,unsupportedEvidence:[]};
const renderPage=()=>render(<MemoryRouter><QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><JourneyReplayPage/></QueryClientProvider></MemoryRouter>);
describe('JourneyReplayPage',()=>{
  beforeEach(()=>{permissions.clear();permissions.add('JOURNEY_REPLAY_VIEW');vi.mocked(journeyReplayApi.points).mockResolvedValue(page);vi.mocked(journeyReplayApi.stops).mockResolvedValue({...page,items:[],analyzedPointCount:2,pointCeiling:20000} as ReplayStopsPage)});
  it('loads by POST-backed query and keeps playback paused with accessible controls',async()=>{renderPage();fireEvent.change(screen.getByLabelText('Vehicle ID'),{target:{value:'11111111-1111-4111-8111-111111111111'}});fireEvent.click(screen.getByRole('button',{name:'Load replay'}));expect(await screen.findByRole('img',{name:/Journey map with 2/})).toBeInTheDocument();expect(screen.getByText(/Replay paused at 1x/)).toBeInTheDocument();expect(screen.getByRole('button',{name:'Play replay'})).toBeInTheDocument();expect(screen.queryByText(/Coordinates:/)).not.toBeInTheDocument();fireEvent.click(screen.getByRole('button',{name:'Show coordinates'}));expect(screen.getByText(/Coordinates:/)).toBeInTheDocument();await waitFor(()=>expect(journeyReplayApi.points).toHaveBeenCalled())});
  it('shows incident capability truthfully only to incident-authorized users',async()=>{permissions.add('JOURNEY_REPLAY_INCIDENT_VIEW');renderPage();fireEvent.change(screen.getByLabelText('Vehicle ID'),{target:{value:'11111111-1111-4111-8111-111111111111'}});fireEvent.click(screen.getByRole('button',{name:'Load replay'}));expect(await screen.findByText('Incident overlays are not yet available')).toBeInTheDocument()});
});
