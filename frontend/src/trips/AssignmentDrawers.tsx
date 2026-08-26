import { useMemo, useState } from 'react';
import { CarOutlined, SafetyCertificateOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons';
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import {
  Alert,
  App as AntApp,
  Button,
  Drawer,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  type TableColumnsType,
} from 'antd';
import { api } from '../api/client';
import { DriverStatusTag, VehicleStatusTag } from '../components/status/StatusTags';
import type { DriverReference, ResourceAvailability, Trip, VehicleReference } from './types';

const { Search } = Input;
const { Text } = Typography;

interface AssignmentDrawerProps {
  trip: Trip;
  open: boolean;
  onClose: () => void;
}

interface ApiErrorBody {
  message?: string;
  correlationId?: string;
}

function errorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data;
    return body?.message ? `${body.message}${body.correlationId ? ` (${body.correlationId})` : ''}` : 'The assignment request failed.';
  }
  return 'The assignment request failed.';
}

function EligibilityIndicator({ result, loading, failed, awaitingCriteria }: {
  result?: ResourceAvailability;
  loading: boolean;
  failed: boolean;
  awaitingCriteria?: boolean;
}) {
  if (awaitingCriteria) return <Tag>Enter licence class</Tag>;
  if (loading) return <Tag color="processing">Checking eligibility</Tag>;
  if (failed || !result) return <Tag color="error">Check failed</Tag>;
  if (result.available) return <Tag color="success">Eligible</Tag>;

  const reasonList = (
    <ul className="eligibility-reasons">
      {result.reasons.map((reason) => <li key={reason.code}><strong>{reason.code.replaceAll('_', ' ')}</strong>: {reason.message}</li>)}
    </ul>
  );
  return (
    <Space direction="vertical" size={3}>
      <Tooltip title={reasonList} placement="left"><Tag color="error">Ineligible · {result.reasons.length} reason{result.reasons.length === 1 ? '' : 's'}</Tag></Tooltip>
      <Text type="danger" className="eligibility-summary">{result.reasons[0]?.message}</Text>
    </Space>
  );
}

export function VehicleAssignmentDrawer({ trip, open, onClose }: AssignmentDrawerProps) {
  const { message, notification } = AntApp.useApp();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [selectedId, setSelectedId] = useState<string>();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const vehicles = useQuery({
    queryKey: ['vehicles', 'assignment-candidates'],
    queryFn: async () => (await api.get<VehicleReference[]>('/vehicles')).data,
    enabled: open,
  });
  const candidates = useMemo(() => {
    const query = search.trim().toLowerCase();
    return (vehicles.data ?? []).filter((vehicle) => !query || [vehicle.registrationNumber, vehicle.manufacturer, vehicle.model]
      .some((value) => value?.toLowerCase().includes(query)));
  }, [search, vehicles.data]);
  const checks = useQueries({ queries: candidates.map((vehicle) => ({
    queryKey: ['vehicle-availability', vehicle.id, trip.id, trip.requestedStartTime, trip.requestedEndTime],
    queryFn: async () => (await api.get<ResourceAvailability>(`/vehicles/${vehicle.id}/availability`, { params: {
      from: trip.requestedStartTime,
      to: trip.requestedEndTime,
      requiredVehicleTypeId: trip.requiredVehicleTypeId || undefined,
      requiredCapacityKg: trip.requiredCapacityKg ?? undefined,
      excludeTripId: trip.id,
    } })).data,
    enabled: open,
    retry: false,
  })) });
  const availabilityById = new Map(candidates.map((vehicle, index) => [vehicle.id, checks[index]]));
  const selectedVehicle = candidates.find((vehicle) => vehicle.id === selectedId);
  const selectedAvailability = selectedId ? availabilityById.get(selectedId)?.data : undefined;
  const assign = useMutation({
    mutationFn: async () => (await api.post<Trip>(`/trips/${trip.id}/assign-vehicle`, { vehicleId: selectedId })).data,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', trip.id] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ]);
      void message.success('Vehicle assigned');
      setConfirmOpen(false);
      setSelectedId(undefined);
      onClose();
    },
    onError: (error) => notification.error({ message: 'Vehicle assignment failed', description: errorMessage(error), duration: 6 }),
  });

  const columns: TableColumnsType<VehicleReference> = [
    { title: 'Vehicle', dataIndex: 'registrationNumber', key: 'registrationNumber', render: (value, vehicle) => <Space direction="vertical" size={0}><Text strong>{value}</Text><Text type="secondary">{[vehicle.manufacturer, vehicle.model].filter(Boolean).join(' ') || '—'}</Text></Space> },
    { title: 'Operational status', dataIndex: 'operationalStatus', key: 'status', responsive: ['md'], render: (value) => <VehicleStatusTag status={value} /> },
    { title: 'Capacity', dataIndex: 'capacityKg', key: 'capacity', responsive: ['lg'], render: (value) => value != null ? `${value.toLocaleString()} kg` : '—' },
    { title: 'Eligibility', key: 'eligibility', width: 260, render: (_, vehicle) => { const check = availabilityById.get(vehicle.id); return <EligibilityIndicator result={check?.data} loading={Boolean(check?.isLoading)} failed={Boolean(check?.isError)} />; } },
  ];

  return (
    <>
      <Drawer title={<Space><CarOutlined />Assign vehicle</Space>} open={open} onClose={onClose} width={900}
        footer={<FlexFooter cancel={onClose} continueDisabled={!selectedAvailability?.available} onContinue={() => { assign.reset(); setConfirmOpen(true); }} />}>
        <Space direction="vertical" size={16} className="assignment-drawer-content">
          <Alert type="info" showIcon message="Eligibility is evaluated by Spring Boot" description={`Results use ${trip.requestedStartTime} through ${trip.requestedEndTime} and exclude this trip from conflict checks.`} />
          {assign.isError && <Alert type="error" showIcon message="Vehicle could not be assigned" description={errorMessage(assign.error)} />}
          {vehicles.isError && <Alert type="error" showIcon message="Vehicle candidates could not be loaded" />}
          <Search prefix={<SearchOutlined />} allowClear placeholder="Search registration, manufacturer, or model" onSearch={setSearch} onChange={(event) => setSearch(event.target.value)} />
          <Table<VehicleReference>
            rowKey="id" columns={columns} dataSource={candidates} loading={vehicles.isLoading} pagination={{ pageSize: 8, hideOnSinglePage: true }}
            rowSelection={{ type: 'radio', selectedRowKeys: selectedId ? [selectedId] : [], onChange: (keys) => setSelectedId(String(keys[0])),
              getCheckboxProps: (vehicle) => ({ disabled: !availabilityById.get(vehicle.id)?.data?.available, 'aria-label': `Select ${vehicle.registrationNumber}` }) }}
            scroll={{ x: 720 }} locale={{ emptyText: 'No vehicles match the search' }}
          />
        </Space>
      </Drawer>
      <Modal title="Confirm vehicle assignment" open={confirmOpen} confirmLoading={assign.isPending}
        okText="Confirm assignment" onOk={() => assign.mutate()} onCancel={() => setConfirmOpen(false)}>
        {assign.isError && <Alert className="assignment-confirm-error" type="error" showIcon message="Vehicle could not be assigned" description={errorMessage(assign.error)} />}
        <Alert type="warning" showIcon message={`Assign ${selectedVehicle?.registrationNumber ?? 'selected vehicle'} to ${trip.tripNumber}?`} description="The backend will repeat eligibility and overlap validation before saving." />
      </Modal>
    </>
  );
}

export function DriverAssignmentDrawer({ trip, open, onClose }: AssignmentDrawerProps) {
  const { message, notification } = AntApp.useApp();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [licenseClass, setLicenseClass] = useState('');
  const [selectedId, setSelectedId] = useState<string>();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const drivers = useQuery({
    queryKey: ['drivers', 'assignment-candidates'],
    queryFn: async () => (await api.get<DriverReference[]>('/drivers')).data,
    enabled: open,
  });
  const candidates = useMemo(() => {
    const query = search.trim().toLowerCase();
    return (drivers.data ?? []).filter((driver) => !query || [driver.employeeNumber, driver.firstName, driver.lastName]
      .some((value) => value.toLowerCase().includes(query)));
  }, [drivers.data, search]);
  const requiredClass = licenseClass.trim();
  const checks = useQueries({ queries: candidates.map((driver) => ({
    queryKey: ['driver-availability', driver.id, trip.id, trip.requestedStartTime, trip.requestedEndTime, requiredClass],
    queryFn: async () => (await api.get<ResourceAvailability>(`/drivers/${driver.id}/availability`, { params: {
      from: trip.requestedStartTime,
      to: trip.requestedEndTime,
      requiredLicenseClass: requiredClass,
      excludeTripId: trip.id,
    } })).data,
    enabled: open && Boolean(requiredClass),
    retry: false,
  })) });
  const availabilityById = new Map(candidates.map((driver, index) => [driver.id, checks[index]]));
  const selectedDriver = candidates.find((driver) => driver.id === selectedId);
  const selectedAvailability = selectedId ? availabilityById.get(selectedId)?.data : undefined;
  const assign = useMutation({
    mutationFn: async () => (await api.post<Trip>(`/trips/${trip.id}/assign-driver`, { driverId: selectedId, requiredLicenseClass: requiredClass })).data,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', trip.id] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
      ]);
      void message.success('Driver assigned');
      setConfirmOpen(false);
      setSelectedId(undefined);
      onClose();
    },
    onError: (error) => notification.error({ message: 'Driver assignment failed', description: errorMessage(error), duration: 6 }),
  });

  const columns: TableColumnsType<DriverReference> = [
    { title: 'Driver', key: 'driver', render: (_, driver) => <Space direction="vertical" size={0}><Text strong>{driver.firstName} {driver.lastName}</Text><Text type="secondary">{driver.employeeNumber}</Text></Space> },
    { title: 'Operational status', dataIndex: 'status', key: 'status', responsive: ['md'], render: (value) => <DriverStatusTag status={value} /> },
    { title: 'Required licence', key: 'license', responsive: ['lg'], render: () => <Space><SafetyCertificateOutlined /><Tag>{requiredClass || 'Not set'}</Tag></Space> },
    { title: 'Eligibility', key: 'eligibility', width: 260, render: (_, driver) => { const check = availabilityById.get(driver.id); return <EligibilityIndicator result={check?.data} loading={Boolean(check?.isLoading)} failed={Boolean(check?.isError)} awaitingCriteria={!requiredClass} />; } },
  ];

  return (
    <>
      <Drawer title={<Space><UserOutlined />Assign driver</Space>} open={open} onClose={onClose} width={900}
        footer={<FlexFooter cancel={onClose} continueDisabled={!requiredClass || !selectedAvailability?.available} onContinue={() => { assign.reset(); setConfirmOpen(true); }} />}>
        <Space direction="vertical" size={16} className="assignment-drawer-content">
          <Alert type="info" showIcon message="Licence eligibility is evaluated by Spring Boot" description="A single qualifying licence must satisfy the requested class and the entire trip period. React only displays the returned result." />
          {assign.isError && <Alert type="error" showIcon message="Driver could not be assigned" description={errorMessage(assign.error)} />}
          {drivers.isError && <Alert type="error" showIcon message="Driver candidates could not be loaded" />}
          <div className="assignment-filter-grid">
            <Search prefix={<SearchOutlined />} allowClear placeholder="Search employee number or driver name" onSearch={setSearch} onChange={(event) => setSearch(event.target.value)} />
            <Input value={licenseClass} onChange={(event) => { setLicenseClass(event.target.value.toUpperCase()); setSelectedId(undefined); }} placeholder="Required licence class" maxLength={20} />
          </div>
          {!requiredClass && <Alert type="warning" showIcon message="Enter the required licence class to request eligibility results" />}
          <Table<DriverReference>
            rowKey="id" columns={columns} dataSource={candidates} loading={drivers.isLoading} pagination={{ pageSize: 8, hideOnSinglePage: true }}
            rowSelection={{ type: 'radio', selectedRowKeys: selectedId ? [selectedId] : [], onChange: (keys) => setSelectedId(String(keys[0])),
              getCheckboxProps: (driver) => ({ disabled: !availabilityById.get(driver.id)?.data?.available, 'aria-label': `Select ${driver.firstName} ${driver.lastName}` }) }}
            scroll={{ x: 720 }} locale={{ emptyText: 'No drivers match the search' }}
          />
        </Space>
      </Drawer>
      <Modal title="Confirm driver assignment" open={confirmOpen} confirmLoading={assign.isPending}
        okText="Confirm assignment" onOk={() => assign.mutate()} onCancel={() => setConfirmOpen(false)}>
        {assign.isError && <Alert className="assignment-confirm-error" type="error" showIcon message="Driver could not be assigned" description={errorMessage(assign.error)} />}
        <Alert type="warning" showIcon message={`Assign ${selectedDriver ? `${selectedDriver.firstName} ${selectedDriver.lastName}` : 'selected driver'} to ${trip.tripNumber}?`} description={`The backend will repeat licence class ${requiredClass || '—'} and scheduling validation before saving.`} />
      </Modal>
    </>
  );
}

function FlexFooter({ cancel, onContinue, continueDisabled }: { cancel: () => void; onContinue: () => void; continueDisabled: boolean }) {
  return <Space className="assignment-drawer-footer"><Button onClick={cancel}>Cancel</Button><Button type="primary" disabled={continueDisabled} onClick={onContinue}>Continue</Button></Space>;
}
