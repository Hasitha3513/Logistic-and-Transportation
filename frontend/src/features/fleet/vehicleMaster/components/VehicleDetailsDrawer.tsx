import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Alert, App as AntApp, Badge, Button, Card, Descriptions, Drawer, Flex, Space, Spin, Tag, Typography } from 'antd';
import { DocumentStatusTag, VehicleStatusTag } from '../../../../components/status/StatusTags';
import { useVehicle, useVehicleDocuments, useVehicleReferences } from '../hooks/useVehicles';
import { vehicleApi } from '../api/vehicleApi';
import type { VehicleDocument } from '../types/vehicle';
import VehicleMaintenanceSection from '../../../../fleet/VehicleMaintenanceSection';
import VehicleReadingsSection from '../../../../fleet/VehicleReadingsSection';
import { VehicleLubricantSection } from '../../../../fleet/VehicleLubricantSection';

interface VehicleDetailsDrawerProps {
  vehicleId?: string;
  open: boolean;
  canManageDocuments: boolean;
  onClose: () => void;
  onAddDocument: () => void;
  onEditDocument: (document: VehicleDocument) => void;
}

const value = (item: unknown, suffix = '') => item == null || item === '' ? '—' : `${String(item)}${suffix}`;

export function VehicleDetailsDrawer({
  vehicleId,
  open,
  canManageDocuments,
  onClose,
  onAddDocument,
  onEditDocument,
}: VehicleDetailsDrawerProps) {
  const { message, modal } = AntApp.useApp();
  const vehicle = useVehicle(vehicleId);
  const documents = useVehicleDocuments(vehicleId);
  const references = useVehicleReferences(open);
  const categoryName = references.categories.data?.find((category) => category.id === vehicle.data?.categoryId)?.name;
  const typeName = references.types.data?.find((type) => type.id === vehicle.data?.typeId)?.name;

  const removeDocument = (document: VehicleDocument) => modal.confirm({
    title: 'Delete vehicle document',
    content: 'The backend will preserve historical audit data.',
    okText: 'Delete',
    okButtonProps: { danger: true },
    onOk: async () => {
      await vehicleApi.deleteDocument(vehicleId!, document.id);
      await documents.refetch();
      void message.success('Vehicle document removed');
    },
  });

  return (
    <Drawer title="Vehicle registry details" width={820} open={open} onClose={onClose} destroyOnHidden>
      {vehicle.isLoading && <Flex justify="center"><Spin aria-label="Loading vehicle details" /></Flex>}
      {vehicle.isError && <Alert type="error" showIcon message="Vehicle details could not be loaded" />}
      {vehicle.data && (
        <Flex vertical gap={20}>
          <Descriptions
            bordered
            size="small"
            column={{ xs: 1, sm: 2 }}
            items={[
              { key: 'registration', label: 'Registration', children: <Typography.Text strong>{vehicle.data.registrationNumber}</Typography.Text> },
              { key: 'status', label: 'Operational status', children: <VehicleStatusTag status={vehicle.data.operationalStatus} /> },
              { key: 'active', label: 'Lifecycle state', children: <Badge status={vehicle.data.active ? 'success' : 'default'} text={vehicle.data.active ? 'Active' : 'Inactive'} /> },
              { key: 'ownership', label: 'Ownership', children: vehicle.data.ownershipType
                ? <Tag>{vehicle.data.ownershipType.replaceAll('_', ' ')}</Tag> : '—' },
              { key: 'manufacturer', label: 'Manufacturer', children: value(vehicle.data.manufacturer) },
              { key: 'model', label: 'Model', children: value(vehicle.data.model) },
              { key: 'year', label: 'Manufacture year', children: value(vehicle.data.manufactureYear) },
              { key: 'chassis', label: 'Chassis number', children: value(vehicle.data.chassisNumber) },
              { key: 'engine', label: 'Engine number', children: value(vehicle.data.engineNumber) },
              { key: 'category', label: 'Category', children: value(categoryName) },
              { key: 'type', label: 'Vehicle type', children: value(typeName) },
              { key: 'capacity', label: 'Capacity', children: value(vehicle.data.capacityKg, ' kg') },
              { key: 'odometer', label: 'Current odometer', children: value(vehicle.data.currentOdometerKm, ' km') },
              { key: 'hours', label: 'Engine hours', children: value(vehicle.data.engineHours, ' h') },
            ]}
          />

          <Card
            size="small"
            title="Vehicle documents"
            extra={canManageDocuments ? <Button size="small" type="primary" icon={<PlusOutlined />} onClick={onAddDocument}>Add</Button> : undefined}
          >
            {documents.isLoading && <Spin size="small" aria-label="Loading vehicle documents" />}
            {documents.isError && <Alert type="error" showIcon message="Vehicle documents could not be loaded" />}
            {!documents.isLoading && !documents.isError && !documents.data?.length && (
              <Typography.Text type="secondary">No compliance records</Typography.Text>
            )}
            <Flex vertical gap={10}>
              {documents.data?.map((document) => (
                <Card
                  key={document.id}
                  size="small"
                  className="resource-related-detail"
                  extra={canManageDocuments ? (
                    <Space>
                      <Button size="small" icon={<EditOutlined />} onClick={() => onEditDocument(document)}>Edit</Button>
                      <Button size="small" danger icon={<DeleteOutlined />} onClick={() => removeDocument(document)}>Delete</Button>
                    </Space>
                  ) : undefined}
                >
                  <Descriptions size="small" column={{ xs: 1, sm: 2 }} items={[
                    { key: 'type', label: 'Document type', children: value(document.documentType) },
                    { key: 'number', label: 'Document number', children: value(document.documentNumber) },
                    { key: 'status', label: 'Status', children: <DocumentStatusTag status={document.status} /> },
                    { key: 'mandatory', label: 'Mandatory for dispatch', children: document.mandatoryForDispatch ? 'Yes' : 'No' },
                    { key: 'issue', label: 'Issue date', children: value(document.issueDate) },
                    { key: 'expiry', label: 'Expiry date', children: value(document.expiryDate) },
                    { key: 'file', label: 'File reference', children: value(document.fileReference) },
                  ]} />
                </Card>
              ))}
            </Flex>
          </Card>

          <VehicleMaintenanceSection vehicleId={vehicle.data.id} />
          <VehicleReadingsSection vehicleId={vehicle.data.id} />
          <VehicleLubricantSection vehicleId={vehicle.data.id} />
        </Flex>
      )}
    </Drawer>
  );
}
