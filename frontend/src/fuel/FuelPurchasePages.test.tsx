import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { appTheme } from '../app/theme/theme';
import { AuthProvider } from '../auth/AuthContext';
import { server } from '../test/server';
import type { FuelPurchase } from './purchaseTypes';

const purchase: FuelPurchase = {
  id:'purchase-1',purchaseNumber:'FP-2026-000001',vendor:{id:'vendor-1',code:'V-1',name:'Ceylon Fuel',active:true},
  fuelType:'DIESEL',purchaseDate:'2026-08-16',invoiceNumber:'INV-100',invoiceDate:'2026-08-16',quantity:100,
  unitPrice:300,subtotal:30000,taxRate:10,taxAmount:3000,otherCharges:500,totalAmount:33500,currencyCode:'LKR',
  status:'DRAFT',reconciliationStatus:'PENDING',expectedUnitPrice:295,priceVariance:5,createdBy:'user-1',
  createdAt:'2026-08-16T00:00:00Z',updatedAt:'2026-08-16T00:00:00Z',
};
function handlers(permissions:string[],current=purchase){server.use(
 http.get('*/auth/me',()=>HttpResponse.json({id:'user-1',username:'fuel.manager',firstName:'Fuel',lastName:'Manager',active:true,roles:['FUEL'],permissions})),
 http.get('*/fuel-purchases',()=>HttpResponse.json({content:[current],page:0,limit:10,totalElements:1,totalPages:1})),
 http.get('*/fuel-purchases/:id',()=>HttpResponse.json(current)),
 http.get('*/fuel-purchases/:id/history',()=>HttpResponse.json([{id:'h1',fuelPurchaseId:'purchase-1',fromStatus:null,toStatus:current.status,action:'CREATED',actorId:'user-1',actor:'fuel.manager',occurredAt:current.createdAt}])),
 http.get('*/vendors',()=>HttpResponse.json([current.vendor])),http.get('*/fuel-stations',()=>HttpResponse.json([])),http.get('*/fuel-prices',()=>HttpResponse.json([])));
}
function renderAt(path:string){const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});return render(<ConfigProvider theme={appTheme}><AntApp><QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><AuthProvider><App/></AuthProvider></MemoryRouter></QueryClientProvider></AntApp></ConfigProvider>)}

describe('Fuel purchase pages',()=>{
 it('renders server-paginated purchase data and backend totals',async()=>{handlers(['FUEL_PURCHASE_VIEW','FUEL_PRICE_VIEW']);renderAt('/fuel/purchases');expect(await screen.findByText('FP-2026-000001')).toBeInTheDocument();expect(screen.getByText('Ceylon Fuel')).toBeInTheDocument();expect(screen.getByText('Draft')).toBeInTheDocument();});
 it('does not expose approve without permission',async()=>{handlers(['FUEL_PURCHASE_VIEW'],{...purchase,status:'SUBMITTED'});renderAt('/fuel/purchases/purchase-1');await screen.findAllByText('FP-2026-000001');expect(screen.queryByRole('button',{name:'Approve'})).not.toBeInTheDocument();});
 it('shows receipt quantity variance in the receive modal',async()=>{handlers(['FUEL_PURCHASE_VIEW','FUEL_PURCHASE_RECEIVE'],{...purchase,status:'APPROVED'});const user=userEvent.setup();renderAt('/fuel/purchases/purchase-1');await user.click(await screen.findByRole('button',{name:'Receive'}));expect(await screen.findByText(/Invoice quantity: 100/)).toBeInTheDocument();expect(screen.getByText(/Variance: 0/)).toBeInTheDocument();});
 it('shows only the permitted draft lifecycle controls',async()=>{handlers(['FUEL_PURCHASE_VIEW','FUEL_PURCHASE_UPDATE','FUEL_PURCHASE_SUBMIT','FUEL_PURCHASE_CANCEL']);renderAt('/fuel/purchases/purchase-1');expect(await screen.findByRole('button',{name:'Edit'})).toBeInTheDocument();expect(screen.getByRole('button',{name:'Submit'})).toBeInTheDocument();expect(screen.getByRole('button',{name:'Cancel'})).toBeInTheDocument();expect(screen.queryByRole('button',{name:'Approve'})).not.toBeInTheDocument();});
 it('validates a new draft before calling the backend',async()=>{handlers(['FUEL_PURCHASE_VIEW','FUEL_PURCHASE_CREATE']);const user=userEvent.setup();renderAt('/fuel/purchases/new');await user.click(await screen.findByRole('button',{name:'Save draft'}));expect(await screen.findByText('Vendor is required')).toBeInTheDocument();expect(screen.getAllByText(/expected number to be >0/i)).toHaveLength(2);});
 it('updates the non-authoritative monetary preview',async()=>{handlers(['FUEL_PURCHASE_VIEW','FUEL_PURCHASE_CREATE']);const user=userEvent.setup();renderAt('/fuel/purchases/new');const amounts=await screen.findAllByRole('spinbutton');for(const [input,value] of [[amounts[0],'10'],[amounts[1],'20'],[amounts[2],'10'],[amounts[3],'5']] as const){await user.clear(input);await user.type(input,value)}expect(screen.getByText('Subtotal').closest('.ant-statistic')).toHaveTextContent('200.00');expect(screen.getByText('Tax').closest('.ant-statistic')).toHaveTextContent('20.00');expect(screen.getByText('Total').closest('.ant-statistic')).toHaveTextContent('225.00');});
 it('opens reconciliation with the backend variance summary',async()=>{handlers(['FUEL_PURCHASE_VIEW','FUEL_PURCHASE_RECONCILE'],{...purchase,status:'RECEIVED',receivedQuantity:98,quantityVariance:-2});const user=userEvent.setup();renderAt('/fuel/purchases/purchase-1');await user.click(await screen.findByRole('button',{name:'Reconcile'}));expect(await screen.findByText('Reconcile fuel purchase')).toBeInTheDocument();expect(screen.getAllByText('-2')).not.toHaveLength(0);expect(screen.getAllByText('33500')).not.toHaveLength(0);});
 it('renders reconciled purchases view-only with variance',async()=>{handlers(['FUEL_PURCHASE_VIEW','FUEL_PURCHASE_UPDATE','FUEL_PURCHASE_CANCEL'],{...purchase,status:'RECONCILED',reconciliationStatus:'RECONCILED',receivedQuantity:98,quantityVariance:-2,reconciliationReference:'REC-1'});renderAt('/fuel/purchases/purchase-1');expect(await screen.findByText('Reconciled')).toBeInTheDocument();expect(screen.queryByRole('button',{name:'Edit'})).not.toBeInTheDocument();expect(screen.queryByRole('button',{name:'Cancel'})).not.toBeInTheDocument();expect(screen.getByText('REC-1')).toBeInTheDocument();});
 it('renders the permission-protected fuel price form',async()=>{handlers(['FUEL_PRICE_VIEW','FUEL_PRICE_MANAGE']);const user=userEvent.setup();renderAt('/fuel/prices');await user.click(await screen.findByRole('button',{name:'Add price'}));expect(await screen.findByText('Add fuel price')).toBeInTheDocument();expect(screen.getAllByText('Effective from')).not.toHaveLength(0);expect(screen.getAllByText('Unit price')).not.toHaveLength(0);});
 it('guards purchase routes without view permission',async()=>{handlers([]);renderAt('/fuel/purchases');expect(await screen.findByText('Select an available module from the navigation.')).toBeInTheDocument();});
});
