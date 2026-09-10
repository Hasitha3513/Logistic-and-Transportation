import { App } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { TrackingDevice } from '../types';
import { DeviceManagement } from './DeviceManagement';

const api = vi.hoisted(() => ({
  devices:vi.fn(),device:vi.fn(),providerConnections:vi.fn(),providerTypes:vi.fn(),
  createDevice:vi.fn(),bindDevice:vi.fn(),associate:vi.fn(),lifecycle:vi.fn(),discoverProviderDevices:vi.fn(),
}));
const listVehicles=vi.hoisted(()=>vi.fn());
vi.mock('../api/trackingApi',()=>({trackingApi:api}));
vi.mock('../../fleet/vehicleMaster/api/vehicleApi',()=>({vehicleApi:{list:listVehicles}}));

const provider={id:'provider-1',providerType:'FLESPI',displayName:'Colombo Telematics',providerAlias:'fleet-primary',safeConfiguration:{},credentialConfigured:true,pollIntervalSeconds:30,pageSize:100,lifecycle:'ACTIVE' as const,testStatus:'PASS' as const,version:3};
const vehicle={id:'vehicle-1',registrationNumber:'WP CAB-1234',categoryId:'category-1',typeId:'type-1',ownershipType:'COMPANY_OWNED' as const,operationalStatus:'AVAILABLE' as const,active:true,manufacturer:'Isuzu',model:'NPR'};
const draft:TrackingDevice={id:'device-1',externalReference:'FMC***130',providerAlias:'fleet-primary',lifecycle:'DRAFT',version:2,currentProviderBinding:{bindingId:'binding-1',providerConnectionId:'provider-1',bindingLifecycle:'ACTIVE',bindingVersion:7,providerDisplayName:'Colombo Telematics',providerType:'FLESPI',providerAlias:'fleet-primary',connectionLifecycle:'ACTIVE',maskedExternalDeviceReference:'FMC***130',safeConfiguration:{}},currentVehicleAssociation:{associationId:'association-1',vehicleId:'vehicle-1',effectiveFrom:'2026-09-10T00:00:00Z'}};

function view(canManage:boolean, device:TrackingDevice=draft){
  api.devices.mockResolvedValue([device]);api.device.mockResolvedValue(device);
  const client=new QueryClient({defaultOptions:{queries:{retry:false}}});
  return render(<MemoryRouter><QueryClientProvider client={client}><App><DeviceManagement canManage={canManage}/></App></QueryClientProvider></MemoryRouter>);
}

async function openDetails(){fireEvent.click(await screen.findByRole('button',{name:/Details/i}));await screen.findByText('Device lifecycle');}

describe('DeviceManagement',()=>{
  beforeEach(()=>{
    vi.clearAllMocks();api.providerConnections.mockResolvedValue({items:[provider],page:0,size:100,total:1});api.providerTypes.mockResolvedValue([{providerType:'FLESPI',capabilities:['POLLING'],supported:true}]);listVehicles.mockResolvedValue([vehicle]);
  });

  it('renders backend binding and Vehicle association after a fresh detail fetch without exposing credentials',async()=>{
    view(false);await openDetails();
    expect(await screen.findByText(/Colombo Telematics · FLESPI · fleet-primary/)).toBeInTheDocument();
    expect(screen.getAllByText(/WP CAB-1234 · Isuzu NPR/).length).toBeGreaterThan(0);
    expect(screen.queryByText(/credentialReference|token|secret/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button',{name:/Bind|Associate|Activate|Disable|Rebind|Retire/i})).not.toBeInTheDocument();
  });

  it('enables explicit activation only when backend detail reports both prerequisites',async()=>{
    view(true);await openDetails();
    expect(screen.getByRole('button',{name:'Activate Device'})).toBeEnabled();
    expect(screen.getByRole('button',{name:'Rebind Provider'})).toBeEnabled();
  });

  it('keeps activation disabled and permits DRAFT association when no association exists',async()=>{
    view(true,{...draft,currentVehicleAssociation:null});await openDetails();
    expect(screen.getByRole('button',{name:'Activate Device'})).toBeDisabled();
    expect(screen.getByRole('button',{name:'Associate Vehicle'})).toBeEnabled();
    expect(screen.getByText('Activation not ready')).toBeInTheDocument();
  });

  it('uses manual entry for FLESPI when DISCOVERY is not advertised',async()=>{
    view(true);await openDetails();fireEvent.click(screen.getByRole('button',{name:'Rebind Provider'}));
    expect(await screen.findByText('This provider requires manual device entry.')).toBeInTheDocument();
    expect(screen.queryByRole('button',{name:'Discover Devices'})).not.toBeInTheDocument();
  });

  it('shows discovery only when the selected provider advertises it',async()=>{
    api.providerTypes.mockResolvedValue([{providerType:'FLESPI',capabilities:['DISCOVERY'],supported:true}]);
    view(true);await openDetails();fireEvent.click(screen.getByRole('button',{name:'Rebind Provider'}));
    expect(await screen.findByRole('button',{name:'Discover Devices'})).toBeInTheDocument();
  });

  it('renders RETIRED as terminal while preserving safe details',async()=>{
    view(true,{...draft,lifecycle:'RETIRED'});await openDetails();
    expect(screen.getByText('Retired device')).toBeInTheDocument();
    expect(screen.queryByRole('button',{name:/Bind|Associate|Activate|Disable|Rebind|Retire/i})).not.toBeInTheDocument();
    expect(screen.getAllByText(/FMC\*\*\*130/).length).toBeGreaterThan(0);
  });

  it('guides an operator to Provider Connections when no ACTIVE provider exists',async()=>{
    api.providerConnections.mockResolvedValue({items:[],page:0,size:100,total:0});api.devices.mockResolvedValue([]);
    view(true);
    expect(await screen.findByText('Create and activate a provider connection before onboarding a device.')).toBeInTheDocument();
    expect(screen.getByRole('link',{name:'Provider Connections'})).toHaveAttribute('href','/tracking/provider-connections');
  });
});
