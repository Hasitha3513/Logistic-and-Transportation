import { beforeEach,describe,expect,it,vi } from 'vitest';
const http=vi.hoisted(()=>({post:vi.fn()}));
vi.mock('../../../api/client',()=>({api:http}));
import { trackingDashboardApi } from './api';
import type { DashboardQuery } from './types';

describe('trackingDashboardApi',()=>{
  beforeEach(()=>http.post.mockReset().mockResolvedValue({data:{vehicles:[]}}));
  it('keeps filters and cursors in the body-only query contract',async()=>{
    const query:DashboardQuery={freshness:['LIVE'],includeHeatMap:true,includeIncidents:true,pageSize:100,cursor:'opaque'};
    await trackingDashboardApi.query(query);
    expect(http.post).toHaveBeenCalledWith('/v1/tracking/dashboard/query',query,{signal:undefined});
  });
});
