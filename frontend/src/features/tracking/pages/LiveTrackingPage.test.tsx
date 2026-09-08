import { App } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LiveTrackingPage from './LiveTrackingPage';

const permissions = vi.hoisted(() => new Set<string>());
const api = vi.hoisted(() => ({
  vehicles: vi.fn(), devices: vi.fn(), history: vi.fn(), createDevice: vi.fn(), lifecycle: vi.fn(), associate: vi.fn(),
}));
vi.mock('../../../auth/AuthContext', () => ({ useAuth: () => ({ hasPermission:(permission:string)=>permissions.has(permission) }) }));
vi.mock('../api/trackingApi', () => ({ trackingApi:api }));

function view() {
  const client=new QueryClient({defaultOptions:{queries:{retry:false}}});
  return render(<QueryClientProvider client={client}><App><LiveTrackingPage/></App></QueryClientProvider>);
}

describe('LiveTrackingPage', () => {
  beforeEach(() => {
    permissions.clear();api.vehicles.mockResolvedValue([]);api.devices.mockResolvedValue([]);api.history.mockResolvedValue({items:[]});
  });

  it('truthfully shows an empty last-known tracking state', async () => {
    permissions.add('TRACKING_VIEW');view();
    expect(await screen.findByText('No tracked vehicles')).toBeInTheDocument();
    expect(screen.getByText(/15 seconds while this tab is visible/i)).toBeInTheDocument();
  });

  it('shows device management only with the exact permission', async () => {
    permissions.add('TRACKING_VIEW');permissions.add('TRACKING_DEVICE_MANAGE');view();
    fireEvent.click(screen.getByRole('tab',{name:'Tracking devices'}));
    expect(await screen.findByRole('button',{name:/Register device/i})).toBeInTheDocument();
  });
});
