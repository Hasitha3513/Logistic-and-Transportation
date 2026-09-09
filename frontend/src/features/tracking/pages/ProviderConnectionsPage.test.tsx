import { App, Modal } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ProviderConnectionsPage from './ProviderConnectionsPage';

const state=vi.hoisted(()=>({manage:true,empty:false,lifecycle:'ACTIVE',capabilities:['POLLING','HISTORY'] as string[]}));
const calls=vi.hoisted(()=>({create:vi.fn(),update:vi.fn(),test:vi.fn(),lifecycle:vi.fn(),discover:vi.fn()}));
const connection=()=>({id:'00000000-0000-0000-0000-000000000048',providerType:'FLESPI',displayName:'Colombo fleet',providerAlias:'FLESPI_LK',endpointUri:'https://flespi.io',safeConfiguration:{region:'lk'},credentialConfigured:true,pollIntervalSeconds:5,pageSize:500,lifecycle:state.lifecycle,testStatus:'PASS',lastTestedAt:'2026-09-09T08:00:00Z',lastSuccessfulPollAt:undefined,lastProviderMessageAt:undefined,lastErrorCategory:undefined,nextPollAt:undefined,version:2});
vi.mock('../../../auth/AuthContext',()=>({useAuth:()=>({hasPermission:(permission:string)=>state.manage&&permission==='TRACKING_DEVICE_MANAGE'})}));
vi.mock('../hooks/useProviderConnections',()=>({
 useProviderConnections:()=>({data:{items:state.empty?[]:[connection()],page:0,size:20,total:state.empty?0:1},isLoading:false,isFetching:false,isError:false,refetch:vi.fn()}),
 useProviderTypes:()=>({data:[{providerType:'FLESPI',supported:true,capabilities:state.capabilities}],isLoading:false}),
 useProviderConnection:()=>({data:connection(),isError:false}),
 useProviderConnectionMutations:()=>({create:{mutateAsync:calls.create,isPending:false},update:{mutateAsync:calls.update,isPending:false},test:{mutateAsync:calls.test,isPending:false},lifecycle:{mutateAsync:calls.lifecycle,isPending:false},discover:{mutateAsync:calls.discover,isPending:false}}),
}));
const view=()=>render(<MemoryRouter initialEntries={['/tracking/provider-connections']}><QueryClientProvider client={new QueryClient()}><App><ProviderConnectionsPage/></App></QueryClientProvider></MemoryRouter>);

describe('ProviderConnectionsPage',()=>{
 beforeEach(()=>{state.manage=true;state.empty=false;state.lifecycle='ACTIVE';state.capabilities=['POLLING','HISTORY'];Object.values(calls).forEach(mock=>mock.mockReset());calls.test.mockResolvedValue({connection:connection(),status:'PASS'});calls.lifecycle.mockResolvedValue(connection());});
 it('renders realistic CS06 list data without credential reference or secret',()=>{view();expect(screen.getByText('Colombo fleet')).toBeInTheDocument();expect(screen.getByText('Configured')).toBeInTheDocument();expect(screen.queryByText('env:FLESPI_TOKEN')).not.toBeInTheDocument();expect(screen.queryByText(/secret/i)).not.toBeInTheDocument();});
 it('renders a useful authorized empty state',()=>{state.empty=true;view();expect(screen.getByText('No provider connections configured')).toBeInTheDocument();expect(screen.getAllByRole('button',{name:'Add Provider Connection'}).length).toBeGreaterThan(0);});
 it('redirects users without management permission and exposes no controls',()=>{state.manage=false;view();expect(screen.queryByRole('button',{name:'Add Provider Connection'})).not.toBeInTheDocument();});
 it('loads provider types into the create form and validates required values',async()=>{view();fireEvent.click(screen.getByRole('button',{name:/Add Provider Connection/}));expect(await screen.findByRole('combobox',{name:'Provider type'})).toBeInTheDocument();fireEvent.click(screen.getByRole('button',{name:'Save'}));expect(await screen.findAllByRole('alert')).not.toHaveLength(0);expect(calls.create).not.toHaveBeenCalled();});
 it('runs a safe connection test',async()=>{view();fireEvent.click(screen.getByRole('button',{name:/Test Connection/}));await waitFor(()=>expect(calls.test).toHaveBeenCalledWith(connection().id));expect(await screen.findByText('Connection successful')).toBeInTheDocument();});
 it('maps a failed connection test without raw provider detail',async()=>{calls.test.mockResolvedValue({connection:connection(),status:'AUTH_FAILED',detailCode:'provider-internal-body'});view();fireEvent.click(screen.getByRole('button',{name:/Test Connection/}));expect(await screen.findByText('Authentication failed')).toBeInTheDocument();expect(screen.queryByText('provider-internal-body')).not.toBeInTheDocument();});
 it('offers disable for ACTIVE and activate for DISABLED',()=>{const first=view();expect(screen.getByRole('button',{name:'Disable'})).toBeInTheDocument();first.unmount();state.lifecycle='DISABLED';view();expect(screen.getByRole('button',{name:'Activate'})).toBeInTheDocument();});
 it('requires a lifecycle confirmation before disabling',()=>{const confirm=vi.spyOn(Modal,'confirm').mockImplementation(()=>({destroy:vi.fn(),update:vi.fn()}));view();fireEvent.click(screen.getByRole('button',{name:'Disable'}));expect(confirm).toHaveBeenCalledWith(expect.objectContaining({title:'Disable provider connection?',okText:'Disable'}));confirm.mockRestore();});
 it('shows the safe stale-version reload guidance',async()=>{calls.update.mockRejectedValue({isAxiosError:true,response:{data:{code:'TRACKING_STALE_VERSION',message:'unsafe backend detail'}}});view();fireEvent.click(screen.getByRole('button',{name:'Edit'}));fireEvent.click(await screen.findByRole('button',{name:'Save'}));expect(await screen.findByText(/changed elsewhere.*latest version/i)).toBeInTheDocument();expect(screen.queryByText('unsafe backend detail')).not.toBeInTheDocument();});
 it('hides mutable, test, discovery and lifecycle actions for RETIRED',()=>{state.lifecycle='RETIRED';state.capabilities=['DISCOVERY'];view();expect(screen.queryByRole('button',{name:'Edit'})).not.toBeInTheDocument();expect(screen.queryByRole('button',{name:/Test Connection/})).not.toBeInTheDocument();expect(screen.queryByRole('button',{name:'Retire'})).not.toBeInTheDocument();expect(screen.queryByRole('button',{name:'Discover Devices'})).not.toBeInTheDocument();});
 it('gates discovery on the backend capability',()=>{view();fireEvent.click(screen.getByRole('button',{name:/Details/}));expect(screen.queryByRole('button',{name:/Discover Devices/})).not.toBeInTheDocument();fireEvent.click(screen.getByRole('button',{name:'Close'}));state.capabilities=['POLLING','DISCOVERY'];fireEvent.click(screen.getByRole('button',{name:/Details/}));expect(screen.getByRole('button',{name:/Discover Devices/})).toBeInTheDocument();});
});
