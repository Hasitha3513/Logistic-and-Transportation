import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import App from '../../../../App';
import { appTheme } from '../../../../app/theme/theme';
import { AuthProvider } from '../../../../auth/AuthContext';
import { server } from '../../../../test/server';
import type { CargoManifest, ManifestItem, ManifestItemPayload } from '../types/cargoManifest';

const orderId = '44444444-4444-4444-8444-444444444444';
const lineId = '55555555-5555-4555-8555-555555555555';
const manifestId = '66666666-6666-4666-8666-666666666666';
const itemId = '77777777-7777-4777-8777-777777777777';
const manifest: CargoManifest = { id: manifestId, manifestNumber: 'CM-2026-000001', freightOrderId: orderId, freightOrderNumber: 'FO-2026-000001', finalized: false, items: [], version: 0, createdAt: '2026-08-25T00:00:00Z', updatedAt: '2026-08-25T00:00:00Z', createdBy: 'manager', updatedBy: 'manager' };
const unknownItem: ManifestItem = { id: itemId, freightOrderLineId: lineId, description: 'Pallet cargo', quantity: 2, packingInformation: 'Wrapped', commodityClassification: 'PALLET.CODE', customsApplicable: false, hazardous: false, fragile: null, temperatureSensitive: null };
const order = { id: orderId, orderNumber: 'FO-2026-000001', lines: [{ id: lineId, description: 'Pallet cargo', quantity: 2 }], customerId: '1', originLocationId: '2', destinationLocationId: '3', requestedPickupAt: '2026-09-01T00:00:00Z', requestedDeliveryAt: '2026-09-02T00:00:00Z', serviceLevel: 'STANDARD', priority: 'HIGH', version: 0, createdAt: '2026-08-25T00:00:00Z', updatedAt: '2026-08-25T00:00:00Z', createdBy: 'manager', updatedBy: 'manager' };

function handlers(permissions: string[], value: CargoManifest = manifest, classificationFailure = false) {
  server.use(
    http.get('*/auth/me', () => HttpResponse.json({ id: 'u', username: 'manager', firstName: 'Cargo', lastName: 'Manager', active: true, roles: [], permissions })),
    http.get('*/v1/freight/manifests', () => HttpResponse.json({ content: [value], page: 0, limit: 10, totalElements: 1, totalPages: 1 })),
    http.get('*/v1/freight/manifests/:id/readiness', () => HttpResponse.json(classificationFailure
      ? { ready: false, failures: [{ code: 'SPECIAL_CARGO_CLASSIFICATION_MISSING', field: `items.${itemId}.specialCargoClassification`, message: 'Raw backend classification message' }] }
      : { ready: value.items.length > 0, failures: value.items.length ? [] : [{ code: 'UNMANIFESTED_CARGO', field: 'items', message: 'Pallet cargo has unmanifested quantity 2' }] })),
    http.get('*/v1/freight/manifests/:id', () => HttpResponse.json(value)),
    http.get('*/v1/freight/orders/:id', () => HttpResponse.json(order)),
    http.get('*/v1/freight/orders', () => HttpResponse.json({ content: [order], page: 0, limit: 100, totalElements: 1, totalPages: 1 })),
  );
}

function renderAt(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<ConfigProvider theme={appTheme}><AntApp><QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><AuthProvider><App /></AuthProvider></MemoryRouter></QueryClientProvider></AntApp></ConfigProvider>);
}

async function choose(groupLabel: string, choice: 'Yes' | 'No') {
  const group = screen.getByRole('radiogroup', { name: groupLabel });
  await userEvent.setup().click(within(group).getByText(choice));
}

describe('Cargo manifest pages', () => {
  it('renders permission-aware paginated list', async () => {
    handlers(['CARGO_MANIFEST_VIEW']);
    renderAt('/freight/manifests');
    expect(await screen.findByText('CM-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('UNFINALIZED')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'New cargo manifest' })).not.toBeInTheDocument();
  });

  it('starts new items unclassified and blocks an unselected submission', async () => {
    handlers(['CARGO_MANIFEST_VIEW', 'CARGO_MANIFEST_MANAGE']);
    const user = userEvent.setup();
    renderAt(`/freight/manifests/${manifestId}`);
    await user.click(await screen.findByRole('button', { name: /Add cargo item/ }));
    expect(within(screen.getByRole('radiogroup', { name: 'Fragile' })).getAllByRole('radio').every(input => !(input as HTMLInputElement).checked)).toBe(true);
    expect(within(screen.getByRole('radiogroup', { name: 'Temperature sensitive' })).getAllByRole('radio').every(input => !(input as HTMLInputElement).checked)).toBe(true);
    await user.click(screen.getByRole('button', { name: 'Save cargo item' }));
    expect(await screen.findByText('Select Yes or No for Fragile')).toBeInTheDocument();
    expect(screen.getByText('Select Yes or No for Temperature sensitive')).toBeInTheDocument();
  });

  it('sends explicit Yes and No classifications when creating an item', async () => {
    let sent: ManifestItemPayload | undefined;
    handlers(['CARGO_MANIFEST_VIEW', 'CARGO_MANIFEST_MANAGE']);
    server.use(http.post('*/v1/freight/manifests/:id/items', async ({ request }) => {
      sent = await request.json() as ManifestItemPayload;
      return HttpResponse.json({ ...manifest, items: [{ ...unknownItem, fragile: true, temperatureSensitive: false }] });
    }));
    const user = userEvent.setup();
    renderAt(`/freight/manifests/${manifestId}`);
    await user.click(await screen.findByRole('button', { name: /Add cargo item/ }));
    await user.click(screen.getByRole('combobox', { name: 'Freight Order line' }));
    await user.click(await screen.findByText('Pallet cargo — 2'));
    await user.type(screen.getByRole('textbox', { name: 'Traceable description' }), 'Pallet cargo');
    await user.type(screen.getByRole('textbox', { name: 'Packing information' }), 'Wrapped');
    await user.type(screen.getByRole('textbox', { name: 'Commodity classification' }), 'PALLET.CODE');
    await choose('Fragile', 'Yes');
    await choose('Temperature sensitive', 'No');
    await user.click(screen.getByRole('button', { name: 'Save cargo item' }));
    await waitFor(() => expect(sent).toBeDefined());
    expect(sent?.fragile).toBe(true);
    expect(sent?.temperatureSensitive).toBe(false);
  });

  it('displays explicit special-cargo indicators', async () => {
    handlers(['CARGO_MANIFEST_VIEW'], { ...manifest, items: [{ ...unknownItem, fragile: true, temperatureSensitive: true }] });
    renderAt(`/freight/manifests/${manifestId}`);
    expect(await screen.findByText('FRAGILE')).toBeInTheDocument();
    expect(screen.getByText('TEMPERATURE SENSITIVE')).toBeInTheDocument();
    expect(screen.queryByText('CLASSIFICATION REQUIRED')).not.toBeInTheDocument();
  });

  it('shows historical UNKNOWN as classification required instead of No', async () => {
    handlers(['CARGO_MANIFEST_VIEW'], { ...manifest, items: [unknownItem] }, true);
    renderAt(`/freight/manifests/${manifestId}`);
    expect(await screen.findByText('CLASSIFICATION REQUIRED')).toBeInTheDocument();
    expect(screen.getByText(/Complete Fragile and Temperature-sensitive classification/)).toBeInTheDocument();
  });

  it('allows a manager to classify an editable historical UNKNOWN item', async () => {
    let sent: ManifestItemPayload | undefined;
    const editable = { ...manifest, items: [unknownItem] };
    handlers(['CARGO_MANIFEST_VIEW', 'CARGO_MANIFEST_MANAGE'], editable, true);
    server.use(http.patch('*/v1/freight/manifests/:id/items/:itemId', async ({ request }) => {
      sent = await request.json() as ManifestItemPayload;
      return HttpResponse.json({ ...editable, items: [{ ...unknownItem, fragile: false, temperatureSensitive: true }] });
    }));
    const user = userEvent.setup();
    renderAt(`/freight/manifests/${manifestId}`);
    await user.click(await screen.findByRole('button', { name: /Edit/ }));
    await choose('Fragile', 'No');
    await choose('Temperature sensitive', 'Yes');
    await user.click(screen.getByRole('button', { name: 'Save cargo item' }));
    await waitFor(() => expect(sent).toBeDefined());
    expect(sent?.fragile).toBe(false);
    expect(sent?.temperatureSensitive).toBe(true);
  });

  it('keeps finalized historical UNKNOWN classification visible and read-only', async () => {
    handlers(['CARGO_MANIFEST_VIEW', 'CARGO_MANIFEST_MANAGE', 'CARGO_MANIFEST_FINALIZE'], { ...manifest, finalized: true, items: [unknownItem], finalizedAt: '2026-08-25T01:00:00Z', finalizedBy: 'finalizer' }, true);
    renderAt(`/freight/manifests/${manifestId}`);
    expect(await screen.findByText('CLASSIFICATION REQUIRED')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Edit/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Add cargo item/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Finalize manifest/ })).not.toBeInTheDocument();
  });

  it('preserves view-only RBAC while showing classification', async () => {
    handlers(['CARGO_MANIFEST_VIEW'], { ...manifest, items: [{ ...unknownItem, fragile: false, temperatureSensitive: false }] });
    renderAt(`/freight/manifests/${manifestId}`);
    expect(await screen.findByText('Pallet cargo')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Edit/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Add cargo item/ })).not.toBeInTheDocument();
  });

  it('conditionally presents customs and hazardous inputs', async () => {
    handlers(['CARGO_MANIFEST_VIEW', 'CARGO_MANIFEST_MANAGE']);
    const user = userEvent.setup();
    renderAt(`/freight/manifests/${manifestId}`);
    await user.click(await screen.findByRole('button', { name: /Add cargo item/ }));
    expect(screen.queryByLabelText('Customs information')).not.toBeInTheDocument();
    await user.click(screen.getByRole('checkbox', { name: /Customs information applies/ }));
    expect(screen.getByLabelText('Customs information')).toBeInTheDocument();
    await user.click(screen.getByRole('checkbox', { name: /Hazardous-goods information applies/ }));
    expect(screen.getByLabelText('Hazardous classification')).toBeInTheDocument();
    expect(screen.getByLabelText('Hazardous details')).toBeInTheDocument();
  });

  it('guards the route without view permission', async () => {
    handlers([]);
    renderAt('/freight/manifests');
    expect(await screen.findByText('Select an available module from the navigation.')).toBeInTheDocument();
  });
});
