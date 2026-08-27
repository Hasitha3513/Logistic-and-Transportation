import { PlusOutlined, ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { App as AntApp, Alert, Button, Card, Flex, Input, Select, Space, Tag, Typography } from 'antd';
import axios from 'axios';
import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import ResourceEditorModal, { type ResourceField, type ResourceValues } from '../../../../pages/ResourceEditorModal';
import { VehicleDetailsDrawer } from '../components/VehicleDetailsDrawer';
import { VehicleEditorModal } from '../components/VehicleEditorModal';
import { VehicleTable } from '../components/VehicleTable';
import { useDeactivateVehicle, useVehicleReferences, useVehicles, vehicleKeys } from '../hooks/useVehicles';
import type {
  Vehicle,
  VehicleApiError,
  VehicleDocument,
  VehicleOperationalStatus,
  VehicleOwnershipType,
} from '../types/vehicle';

const documentFields: ResourceField[] = [
  { name: 'documentType', label: 'Document type', required: true },
  { name: 'documentNumber', label: 'Document number', required: true },
  { name: 'issueDate', label: 'Issue date', kind: 'date' },
  { name: 'expiryDate', label: 'Expiry date', kind: 'date' },
  { name: 'fileReference', label: 'File reference / URL' },
  { name: 'mandatoryForDispatch', label: 'Mandatory for dispatch', kind: 'switch' },
  { name: 'status', label: 'Status', kind: 'select', required: true, options: [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'INACTIVE', label: 'Inactive' },
  ] },
  { name: 'active', label: 'Active', kind: 'switch' },
];

interface DocumentEditorState {
  document?: VehicleDocument;
}

export default function VehicleListPage() {
  const { message, modal, notification } = AntApp.useApp();
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [ownership, setOwnership] = useState<VehicleOwnershipType>();
  const [status, setStatus] = useState<VehicleOperationalStatus>();
  const [editingVehicle, setEditingVehicle] = useState<Vehicle | 'create'>();
  const [documentEditor, setDocumentEditor] = useState<DocumentEditorState>();
  const vehicles = useVehicles();
  const references = useVehicleReferences();
  const deactivate = useDeactivateVehicle();
  const selectedVehicleId = searchParams.get('vehicleId') ?? undefined;

  const filteredVehicles = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    return (vehicles.data ?? []).filter((vehicle) => {
      const matchesSearch = !normalized || [
        vehicle.registrationNumber,
        vehicle.chassisNumber,
        vehicle.engineNumber,
        vehicle.manufacturer,
        vehicle.model,
      ].some((item) => item?.toLowerCase().includes(normalized));
      return matchesSearch
        && (!ownership || vehicle.ownershipType === ownership)
        && (!status || vehicle.operationalStatus === status);
    });
  }, [ownership, search, status, vehicles.data]);

  const classificationByTypeId = useMemo(() => {
    const categoryNames = new Map((references.categories.data ?? []).map((category) => [category.id, category.name]));
    return new Map((references.types.data ?? []).map((type) => [type.id, {
      category: categoryNames.get(type.categoryId) ?? 'Unknown category',
      type: type.name,
    }]));
  }, [references.categories.data, references.types.data]);

  const openDetails = (vehicle: Vehicle) => {
    const next = new URLSearchParams(searchParams);
    next.set('vehicleId', vehicle.id);
    setSearchParams(next, { replace: true });
  };

  const closeDetails = () => {
    const next = new URLSearchParams(searchParams);
    next.delete('vehicleId');
    setSearchParams(next, { replace: true });
  };

  const deactivateVehicle = (vehicle: Vehicle) => modal.confirm({
    title: 'Deactivate Vehicle registry',
    content: 'This preserves Vehicle history but removes the record from operational use.',
    okText: 'Deactivate',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await deactivate.mutateAsync(vehicle.id);
        closeDetails();
        void message.success('Vehicle registry deactivated');
      } catch (error) {
        const body = axios.isAxiosError<VehicleApiError>(error) ? error.response?.data : undefined;
        notification.error({
          message: 'Vehicle could not be deactivated',
          description: body?.message ?? 'The backend rejected this lifecycle action.',
        });
      }
    },
  });

  const fullControl = ['VEHICLE_CREATE', 'VEHICLE_UPDATE', 'VEHICLE_STATUS_UPDATE', 'VEHICLE_DOCUMENT_MANAGE']
    .every(hasPermission);

  return (
    <Flex vertical gap={18}>
      <Flex align="flex-start" justify="space-between" gap={16} wrap>
        <Space direction="vertical" size={4}>
          <Typography.Text type="secondary">Live vehicle master data from the fleet module.</Typography.Text>
          <Tag icon={<SafetyCertificateOutlined />} color={fullControl ? 'success' : 'warning'}>
            {fullControl ? 'Full management access' : 'Read-only access'}
          </Tag>
        </Space>
        <Space wrap>
          {hasPermission('VEHICLE_CREATE') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setEditingVehicle('create')}>Create</Button>
          )}
          <Button icon={<ReloadOutlined />} loading={vehicles.isFetching} onClick={() => void vehicles.refetch()}>Refresh</Button>
        </Space>
      </Flex>

      <Card size="small" className="vehicle-master-filters">
        <Flex gap={12} wrap>
          <Input.Search
            aria-label="Search vehicles"
            allowClear
            placeholder="Search registration, chassis, engine or model"
            onSearch={setSearch}
            onChange={(event) => { if (!event.target.value) setSearch(''); }}
            style={{ minWidth: 260, flex: 1 }}
          />
          <Select<VehicleOwnershipType>
            aria-label="Filter by ownership"
            allowClear
            placeholder="Ownership"
            value={ownership}
            onChange={setOwnership}
            style={{ minWidth: 180 }}
            options={[
              { value: 'COMPANY_OWNED', label: 'Company owned' },
              { value: 'LEASED', label: 'Leased / rental' },
            ]}
          />
          <Select<VehicleOperationalStatus>
            aria-label="Filter by operational status"
            allowClear
            placeholder="Operational status"
            value={status}
            onChange={setStatus}
            style={{ minWidth: 190 }}
            options={[
              { value: 'AVAILABLE', label: 'Available' },
              { value: 'ALLOCATED', label: 'Allocated' },
              { value: 'MAINTENANCE', label: 'Maintenance' },
              { value: 'OUT_OF_SERVICE', label: 'Out of service' },
              { value: 'BROKEN_DOWN', label: 'Broken down' },
            ]}
          />
        </Flex>
      </Card>

      {vehicles.isError && (
        <Alert type="error" showIcon message="Vehicle registry could not be loaded"
          description="Check your permission and backend connection, then retry." />
      )}

      <Card className="resource-list-card">
        <VehicleTable
          vehicles={filteredVehicles}
          classificationByTypeId={classificationByTypeId}
          loading={vehicles.isLoading}
          canEdit={hasPermission('VEHICLE_UPDATE')}
          canDeactivate={hasPermission('VEHICLE_STATUS_UPDATE')}
          onView={openDetails}
          onEdit={setEditingVehicle}
          onDeactivate={deactivateVehicle}
        />
      </Card>

      {editingVehicle && (
        <VehicleEditorModal
          key={editingVehicle === 'create' ? 'create' : editingVehicle.id}
          open
          vehicle={editingVehicle === 'create' ? undefined : editingVehicle}
          onClose={() => setEditingVehicle(undefined)}
        />
      )}

      <VehicleDetailsDrawer
        vehicleId={selectedVehicleId}
        open={Boolean(selectedVehicleId)}
        canManageDocuments={hasPermission('VEHICLE_DOCUMENT_MANAGE')}
        onClose={closeDetails}
        onAddDocument={() => setDocumentEditor({})}
        onEditDocument={(document) => setDocumentEditor({ document })}
      />

      {documentEditor && selectedVehicleId && (
        <ResourceEditorModal
          open
          title={documentEditor.document ? 'Edit vehicle document' : 'Add vehicle document'}
          endpoint={`/vehicles/${selectedVehicleId}/documents${documentEditor.document ? `/${documentEditor.document.id}` : ''}`}
          method={documentEditor.document ? 'patch' : 'post'}
          fields={documentFields}
          initial={documentEditor.document ? { ...documentEditor.document } as ResourceValues : undefined}
          queryKey="vehicles-page-compliance"
          onClose={() => {
            setDocumentEditor(undefined);
            void queryClient.invalidateQueries({ queryKey: vehicleKeys.documents(selectedVehicleId) });
          }}
        />
      )}
    </Flex>
  );
}
