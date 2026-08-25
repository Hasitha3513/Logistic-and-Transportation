import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import App from '../../../../App';
import { appTheme } from '../../../../app/theme/theme';
import { AuthProvider } from '../../../../auth/AuthContext';
import { server } from '../../../../test/server';
import type { FreightOrder } from '../types/freightOrder';

const customerId = '11111111-1111-4111-8111-111111111111';
const originId = '22222222-2222-4222-8222-222222222222';
const destinationId = '33333333-3333-4333-8333-333333333333';
const order: FreightOrder = { id: '44444444-4444-4444-8444-444444444444', orderNumber: 'FO-2026-000001', customerId,
  originLocationId: originId, destinationLocationId: destinationId, requestedPickupAt: '2026-09-01T08:00:00Z',
  requestedDeliveryAt: '2026-09-02T08:00:00Z', serviceLevel: 'STANDARD', priority: 'HIGH',
  specialHandlingInstructions: 'Keep dry', lines: [{ id: '55555555-5555-4555-8555-555555555555', description: 'Pallets', quantity: 2 }],
  version: 0, createdAt: '2026-08-25T00:00:00Z', updatedAt: '2026-08-25T00:00:00Z', createdBy: 'manager', updatedBy: 'manager' };

function handlers(permissions: string[]) { server.use(
  http.get('*/auth/me', () => HttpResponse.json({ id: 'user-1', username: 'freight.manager', firstName: 'Freight', lastName: 'Manager', active: true, roles: ['FREIGHT'], permissions })),
  http.get('*/v1/freight/orders', () => HttpResponse.json({ content: [order], page: 0, limit: 10, totalElements: 1, totalPages: 1 })),
  http.get('*/v1/freight/orders/:id', () => HttpResponse.json(order)),
  http.get('*/customers', () => HttpResponse.json([{ id: customerId, code: 'CUS-1', name: 'Acme Freight', active: true }])),
  http.get('*/locations', () => HttpResponse.json([{ id: originId, code: 'CMB', name: 'Colombo', active: true }, { id: destinationId, code: 'KDY', name: 'Kandy', active: true }])),
); }
function renderAt(path: string) { const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } }); return render(<ConfigProvider theme={appTheme}><AntApp><QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><AuthProvider><App /></AuthProvider></MemoryRouter></QueryClientProvider></AntApp></ConfigProvider>); }

describe('Freight order pages', () => {
  it('renders server-paginated orders with organization references', async () => { handlers(['FREIGHT_ORDER_VIEW']); renderAt('/freight/orders'); expect(await screen.findByText('FO-2026-000001')).toBeInTheDocument(); expect(screen.getByText('Acme Freight')).toBeInTheDocument(); expect(screen.getByText('Colombo')).toBeInTheDocument(); expect(screen.getByText('Kandy')).toBeInTheDocument(); });
  it('hides manage actions from a view-only actor', async () => { handlers(['FREIGHT_ORDER_VIEW']); renderAt('/freight/orders'); await screen.findByText('FO-2026-000001'); expect(screen.queryByRole('button', { name: 'New freight order' })).not.toBeInTheDocument(); });
  it('renders details and immutable audit metadata', async () => { handlers(['FREIGHT_ORDER_VIEW', 'FREIGHT_ORDER_MANAGE']); renderAt(`/freight/orders/${order.id}`); expect(await screen.findByText('Keep dry')).toBeInTheDocument(); expect(screen.getByText(/Pallets/)).toBeInTheDocument(); expect(screen.getAllByText(/by manager/)).not.toHaveLength(0); expect(screen.getByRole('button', { name: 'Edit order' })).toBeInTheDocument(); });
  it('validates an empty order before mutation', async () => { handlers(['FREIGHT_ORDER_VIEW', 'FREIGHT_ORDER_MANAGE']); const user = userEvent.setup(); renderAt('/freight/orders/new'); await user.click(await screen.findByRole('button', { name: 'Save freight order' })); expect(await screen.findByText('Customer is required')).toBeInTheDocument(); expect(screen.getByText('Description is required')).toBeInTheDocument(); });
  it('adds and removes minimal shipment lines without exposing manifest fields', async () => { handlers(['FREIGHT_ORDER_VIEW', 'FREIGHT_ORDER_MANAGE']); const user = userEvent.setup(); renderAt('/freight/orders/new'); expect(await screen.findByLabelText('Line 1 description')).toBeInTheDocument(); await user.click(screen.getByRole('button', { name: /Add line/ })); expect(screen.getByLabelText('Line 2 description')).toBeInTheDocument(); await user.click(screen.getByRole('button', { name: 'Remove line 2' })); expect(screen.queryByLabelText('Line 2 description')).not.toBeInTheDocument(); expect(screen.queryByText(/hazmat|customs|manifest/i)).not.toBeInTheDocument(); });
  it('edits an order with its optimistic version and preserved shipment lines', async () => { handlers(['FREIGHT_ORDER_VIEW', 'FREIGHT_ORDER_MANAGE']); let submitted: Record<string, unknown> | undefined; server.use(http.patch('*/v1/freight/orders/:id', async ({ request }) => { submitted = await request.json() as Record<string, unknown>; return HttpResponse.json({ ...order, priority: 'URGENT', version: 1, updatedBy: 'editor' }); })); const user = userEvent.setup(); renderAt(`/freight/orders/${order.id}/edit`); const priority = await screen.findByLabelText('Priority code'); await user.clear(priority); await user.type(priority, 'URGENT'); await user.click(screen.getByRole('button', { name: 'Save freight order' })); expect(await screen.findByText('Freight order updated')).toBeInTheDocument(); await waitFor(() => expect(submitted).toMatchObject({ version: 0, priority: 'URGENT', lines: [{ id: order.lines[0].id, description: 'Pallets', quantity: 2 }] })); });
  it('maps backend field errors and conflict messages into the form', async () => { handlers(['FREIGHT_ORDER_VIEW', 'FREIGHT_ORDER_MANAGE']); server.use(http.patch('*/v1/freight/orders/:id', () => HttpResponse.json({ code: 'FREIGHT_ORDER_CONCURRENT_UPDATE', message: 'Freight order was changed by another user', fieldErrors: [{ field: 'priority', message: 'Priority must be reviewed' }] }, { status: 409 }))); const user = userEvent.setup(); renderAt(`/freight/orders/${order.id}/edit`); await screen.findByDisplayValue('HIGH'); await user.click(screen.getByRole('button', { name: 'Save freight order' })); expect(await screen.findByText('Priority must be reviewed')).toBeInTheDocument(); expect(screen.getByText('Freight order was changed by another user')).toBeInTheDocument(); });
  it('guards freight routes without view permission', async () => { handlers([]); renderAt('/freight/orders'); expect(await screen.findByText('Select an available module from the navigation.')).toBeInTheDocument(); });
});
