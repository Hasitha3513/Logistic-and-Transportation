import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../../api/client';
import { journeyReplayApi } from './api';

vi.mock('../../../api/client',()=>({api:{post:vi.fn()}}));
describe('journeyReplayApi',()=>{
  beforeEach(()=>vi.mocked(api.post).mockReset());
  it('keeps sensitive selectors and ranges in POST bodies',async()=>{
    vi.mocked(api.post).mockResolvedValue({data:{items:[]}});
    const query={vehicleId:'11111111-1111-4111-8111-111111111111',from:'2026-01-01T00:00:00Z',to:'2026-01-01T01:00:00Z'};
    await journeyReplayApi.points(query);
    expect(api.post).toHaveBeenCalledWith('/v1/tracking/journey-replays/points/query',query,{signal:undefined});
    expect(String(vi.mocked(api.post).mock.calls[0][0])).not.toContain(query.vehicleId);
  });
});
