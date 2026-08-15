import { useMemo, useState } from 'react';
import {
  ArrowLeftOutlined,
  CarOutlined,
  EnvironmentOutlined,
  EditOutlined,
  HistoryOutlined,
  ReloadOutlined,
  ScheduleOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  App as AntApp,
  Badge,
  Button,
  Card,
  Descriptions,
  Divider,
  Empty,
  Flex,
  Modal,
  Select,
  Space,
  Spin,
  Tabs,
  Timeline,
  Typography,
  type TabsProps,
} from 'antd';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PriorityTag, TripStatusTag } from '../components/status/StatusTags';
import { DriverAssignmentDrawer, VehicleAssignmentDrawer } from './AssignmentDrawers';
import LifecycleActions from './LifecycleActions';
import type {
  CustomerReference,
  DriverReference,
  LocationReference,
  RouteReference,
  Trip,
  TripHistoryEntry,
  VehicleReference,
} from './types';

const { Paragraph, Text, Title } = Typography;
const lifecycle = ['DRAFT', 'SUBMITTED', 'APPROVED', 'ASSIGNED', 'DISPATCHED', 'IN_PROGRESS', 'COMPLETED', 'CLOSED'];
const alternativeStates = new Set(['REJECTED', 'CANCELLED', 'INTERRUPTED']);
const dateTime = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' });

function formatDate(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : dateTime.format(date);
}

function referenceMap<T extends { id: string }>(values?: T[]) {
  return new Map((values ?? []).map((value) => [value.id, value]));
}

function shortId(value?: string | null) {
  return value ? value.slice(0, 8).toUpperCase() : '—';
}

function FutureSection({ title, onExplain }: { title: string; onExplain: (title: string) => void }) {
  return (
    <Card variant="borderless" className="trip-detail-card">
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={
          <Space direction="vertical" size={8}>
            <Space><Text strong>{title}</Text><Badge status="default" text="Backend support pending" /></Space>
            <Text type="secondary">This section will activate when the corresponding trip API is available.</Text>
            <Button onClick={() => onExplain(title)}>About this section</Button>
          </Space>
        }
      />
    </Card>
  );
}

export default function TripDetailsPage() {
  const { tripId } = useParams();
  const { hasPermission } = useAuth();
  const { message } = AntApp.useApp();
  const queryClient = useQueryClient();
  const [vehicleDrawerOpen, setVehicleDrawerOpen] = useState(false);
  const [driverDrawerOpen, setDriverDrawerOpen] = useState(false);
  const [futureSection, setFutureSection] = useState<string>();
  const [routeModalOpen, setRouteModalOpen] = useState(false);
  const [selectedRouteId, setSelectedRouteId] = useState<string>();

  const tripQuery = useQuery({
    queryKey: ['trip', tripId],
    queryFn: async () => (await api.get<Trip>(`/trips/${tripId}`)).data,
    enabled: Boolean(tripId),
  });
  const historyQuery = useQuery({
    queryKey: ['trip', tripId, 'status-history'],
    queryFn: async () => (await api.get<TripHistoryEntry[]>(`/trips/${tripId}/status-history`)).data,
    enabled: Boolean(tripId),
  });
  const customers = useQuery({ queryKey: ['customers', 'references'], queryFn: async () => (await api.get<CustomerReference[]>('/customers')).data });
  const locations = useQuery({ queryKey: ['locations', 'references'], queryFn: async () => (await api.get<LocationReference[]>('/locations')).data });
  const vehicles = useQuery({
    queryKey: ['vehicles', 'references'], queryFn: async () => (await api.get<VehicleReference[]>('/vehicles')).data,
    enabled: hasPermission('VEHICLE_VIEW'),
  });
  const drivers = useQuery({
    queryKey: ['drivers', 'references'], queryFn: async () => (await api.get<DriverReference[]>('/drivers')).data,
    enabled: hasPermission('DRIVER_VIEW'),
  });
  const routeQuery = useQuery({
    queryKey: ['route', tripQuery.data?.routeId],
    queryFn: async () => (await api.get<RouteReference>(`/routes/${tripQuery.data?.routeId}`)).data,
    enabled: Boolean(tripQuery.data?.routeId && hasPermission('ROUTE_VIEW')),
  });
  const routesQuery = useQuery({
    queryKey: ['routes', 'assignment'],
    queryFn: async () => (await api.get<RouteReference[]>('/routes', { params: { active: true } })).data,
    enabled: routeModalOpen && hasPermission('ROUTE_VIEW'),
  });
  const assignRoute = useMutation({
    mutationFn: async (routeId: string) => (await api.post<Trip>(`/trips/${tripId}/assign-route`, { routeId })).data,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', tripId] }),
        queryClient.invalidateQueries({ queryKey: ['trip', tripId, 'status-history'] }),
      ]);
      setRouteModalOpen(false);
      setSelectedRouteId(undefined);
      void message.success('Route assigned');
    },
    onError: () => void message.error('Route could not be assigned'),
  });

  const customerById = useMemo(() => referenceMap(customers.data), [customers.data]);
  const locationById = useMemo(() => referenceMap(locations.data), [locations.data]);
  const vehicleById = useMemo(() => referenceMap(vehicles.data), [vehicles.data]);
  const driverById = useMemo(() => referenceMap(drivers.data), [drivers.data]);
  const trip = tripQuery.data;
  const vehicle = trip?.vehicleId ? vehicleById.get(trip.vehicleId) : undefined;
  const driver = trip?.driverId ? driverById.get(trip.driverId) : undefined;
  const customer = trip?.customerId ? customerById.get(trip.customerId) : undefined;
  const origin = trip?.originLocationId ? locationById.get(trip.originLocationId) : undefined;
  const destination = trip?.destinationLocationId ? locationById.get(trip.destinationLocationId) : undefined;
  const assignmentsComplete = Boolean(trip?.vehicleId && trip?.driverId);

  if (tripQuery.isLoading) {
    return <Flex className="trip-detail-loading" justify="center" align="center"><Spin size="large" aria-label="Loading trip details" /></Flex>;
  }

  if (tripQuery.isError || !trip) {
    return (
      <Space direction="vertical" size={16} className="trip-detail-error">
        <Alert type="error" showIcon message="Trip details could not be loaded" description="The trip may no longer exist, or the service is temporarily unavailable." />
        <Link to="/trips"><Button icon={<ArrowLeftOutlined />}>Back to trips</Button></Link>
      </Space>
    );
  }

  const overview = (
      <Space direction="vertical" size={18} className="trip-detail-section">
      {!assignmentsComplete && ['APPROVED', 'ASSIGNED', 'DISPATCHED'].includes(trip.status) && (
        <Alert
          type="warning"
          showIcon
          message="Operational assignments are incomplete"
          description="A vehicle and driver are required before dispatch. Eligibility must be revalidated by the backend at dispatch time."
        />
      )}
      <Card title="Core trip information" variant="borderless" className="trip-detail-card">
        <Descriptions column={{ xs: 1, sm: 2, lg: 3 }} bordered size="small">
          <Descriptions.Item label="Trip number"><Text strong>{trip.tripNumber}</Text></Descriptions.Item>
          <Descriptions.Item label="Status"><TripStatusTag status={trip.status} /></Descriptions.Item>
          <Descriptions.Item label="Priority"><PriorityTag priority={trip.priority} /></Descriptions.Item>
          <Descriptions.Item label="Customer">{customer?.name ?? shortId(trip.customerId)}</Descriptions.Item>
          <Descriptions.Item label="Origin">{origin?.name ?? shortId(trip.originLocationId)}</Descriptions.Item>
          <Descriptions.Item label="Destination">{destination?.name ?? shortId(trip.destinationLocationId)}</Descriptions.Item>
          <Descriptions.Item label="Planned start">{formatDate(trip.requestedStartTime)}</Descriptions.Item>
          <Descriptions.Item label="Planned end">{formatDate(trip.requestedEndTime)}</Descriptions.Item>
          <Descriptions.Item label="Required capacity">{trip.requiredCapacityKg != null ? `${trip.requiredCapacityKg.toLocaleString()} kg` : '—'}</Descriptions.Item>
          <Descriptions.Item label="Passengers">{trip.passengerCount ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Cargo" span={2}>{trip.cargoDescription || '—'}</Descriptions.Item>
          <Descriptions.Item label="Customer instructions" span={3}>{trip.customerInstructions || '—'}</Descriptions.Item>
          <Descriptions.Item label="Notes" span={3}>{trip.notes || '—'}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="Execution details" variant="borderless" className="trip-detail-card">
        <Descriptions column={{ xs: 1, md: 2 }}>
          <Descriptions.Item label="Actual start">{formatDate(trip.actualStartTime)}</Descriptions.Item>
          <Descriptions.Item label="Actual end">{formatDate(trip.actualEndTime)}</Descriptions.Item>
          <Descriptions.Item label="Start odometer">{trip.startOdometerKm != null ? `${trip.startOdometerKm.toLocaleString()} km` : '—'}</Descriptions.Item>
          <Descriptions.Item label="End odometer">{trip.endOdometerKm != null ? `${trip.endOdometerKm.toLocaleString()} km` : '—'}</Descriptions.Item>
          <Descriptions.Item label="Completion remarks" span={2}>{trip.completionRemarks || '—'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </Space>
  );

  const assignments = (
    <div className="trip-summary-grid">
      <Card variant="borderless" className="trip-detail-card" title={<Space><CarOutlined />Vehicle</Space>}>
        <Badge status={trip.vehicleId ? 'success' : 'warning'} text={trip.vehicleId ? 'Assigned' : 'Not assigned'} />
        <Divider />
        <Title level={4}>{vehicle?.registrationNumber ?? (trip.vehicleId ? shortId(trip.vehicleId) : 'No vehicle')}</Title>
        {!hasPermission('VEHICLE_VIEW') && trip.vehicleId && <Alert type="info" showIcon message="Vehicle details require VEHICLE_VIEW permission" />}
        {hasPermission('TRIP_ASSIGN_VEHICLE') && (
          <Button className="assignment-action" type="primary" onClick={() => setVehicleDrawerOpen(true)} disabled={!hasPermission('VEHICLE_AVAILABILITY_VIEW')}>
            {trip.vehicleId ? 'Change vehicle' : 'Assign vehicle'}
          </Button>
        )}
        {hasPermission('TRIP_ASSIGN_VEHICLE') && !hasPermission('VEHICLE_AVAILABILITY_VIEW') && <Alert type="info" showIcon message="Vehicle selection requires VEHICLE_AVAILABILITY_VIEW permission" />}
      </Card>
      <Card variant="borderless" className="trip-detail-card" title={<Space><UserOutlined />Driver</Space>}>
        <Badge status={trip.driverId ? 'success' : 'warning'} text={trip.driverId ? 'Assigned' : 'Not assigned'} />
        <Divider />
        <Title level={4}>{driver ? `${driver.firstName} ${driver.lastName}` : (trip.driverId ? shortId(trip.driverId) : 'No driver')}</Title>
        {driver && <Text type="secondary">{driver.employeeNumber}</Text>}
        {!hasPermission('DRIVER_VIEW') && trip.driverId && <Alert type="info" showIcon message="Driver details require DRIVER_VIEW permission" />}
        {hasPermission('TRIP_ASSIGN_DRIVER') && (
          <Button className="assignment-action" type="primary" onClick={() => setDriverDrawerOpen(true)} disabled={!hasPermission('DRIVER_AVAILABILITY_VIEW')}>
            {trip.driverId ? 'Change driver' : 'Assign driver'}
          </Button>
        )}
        {hasPermission('TRIP_ASSIGN_DRIVER') && !hasPermission('DRIVER_AVAILABILITY_VIEW') && <Alert type="info" showIcon message="Driver selection requires DRIVER_AVAILABILITY_VIEW permission" />}
      </Card>
      <Card variant="borderless" className="trip-detail-card" title="Readiness">
        <Badge status={assignmentsComplete ? 'success' : 'warning'} text={assignmentsComplete ? 'Assignment data complete' : 'Assignment data incomplete'} />
        <Paragraph type="secondary" className="trip-detail-card__copy">Eligibility and scheduling conflicts remain authoritative on the backend and are rechecked during dispatch.</Paragraph>
      </Card>
    </div>
  );

  const route = routeQuery.data;
  const routeSection = (
    <Space direction="vertical" size={16} className="trip-detail-section">
      {!trip.routeId && <Alert type="warning" showIcon message="No route assigned" description="Assigning a route is required for a complete operational plan." />}
      {trip.routeId && !hasPermission('ROUTE_VIEW') && <Alert type="info" showIcon message="Route details require ROUTE_VIEW permission" />}
      {routeQuery.isError && <Alert type="error" showIcon message="Assigned route could not be loaded" />}
      {route && (
        <Card variant="borderless" className="trip-detail-card" title={<Space><EnvironmentOutlined />{route.name}</Space>} extra={<Badge status={route.active ? 'success' : 'default'} text={route.active ? 'Active' : 'Inactive'} />}>
          <Descriptions column={{ xs: 1, sm: 2, lg: 3 }}>
            <Descriptions.Item label="Route code">{route.code}</Descriptions.Item>
            <Descriptions.Item label="Distance">{route.plannedDistanceKm.toLocaleString()} km</Descriptions.Item>
            <Descriptions.Item label="Estimated duration">{route.estimatedDurationMinutes} minutes</Descriptions.Item>
          </Descriptions>
          <Divider />
          <Timeline items={[
            { color: 'green', children: <><Text strong>{locationById.get(route.originLocationId)?.name ?? shortId(route.originLocationId)}</Text><br /><Text type="secondary">Origin</Text></> },
            ...route.stopLocationIds.map((id, index) => ({ color: 'blue', children: <><Text strong>{locationById.get(id)?.name ?? shortId(id)}</Text><br /><Text type="secondary">Stop {index + 1}</Text></> })),
            { color: 'green', children: <><Text strong>{locationById.get(route.destinationLocationId)?.name ?? shortId(route.destinationLocationId)}</Text><br /><Text type="secondary">Destination</Text></> },
          ]} />
        </Card>
      )}
      {hasPermission('TRIP_ASSIGN_ROUTE') && (
        <Button type="primary" icon={<EnvironmentOutlined />} disabled={!hasPermission('ROUTE_VIEW') || !['DRAFT', 'APPROVED', 'ASSIGNED'].includes(trip.status)}
          onClick={() => { setSelectedRouteId(trip.routeId ?? undefined); setRouteModalOpen(true); }}>
          {trip.routeId ? 'Change route' : 'Assign route'}
        </Button>
      )}
    </Space>
  );

  const currentLifecycleIndex = lifecycle.indexOf(trip.status);
  const lifecycleSection = (
    <Card variant="borderless" className="trip-detail-card">
      {alternativeStates.has(trip.status) && <Alert className="trip-lifecycle-alert" type="warning" showIcon message={`Trip entered the alternative ${trip.status.replaceAll('_', ' ').toLowerCase()} state`} />}
      <Timeline items={lifecycle.map((status, index) => ({
        color: index < currentLifecycleIndex ? 'green' : index === currentLifecycleIndex ? 'blue' : 'gray',
        children: <Space><TripStatusTag status={status} />{index === currentLifecycleIndex && <Text type="secondary">Current state</Text>}</Space>,
      }))} />
    </Card>
  );

  const historySection = (
    <Card variant="borderless" className="trip-detail-card">
      {historyQuery.isError && <Alert type="error" showIcon message="Trip history could not be loaded" />}
      {!historyQuery.isLoading && !historyQuery.isError && !historyQuery.data?.length && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No history has been recorded" />}
      <Timeline pending={historyQuery.isLoading ? 'Loading history' : undefined} items={historyQuery.data?.map((entry) => ({
        color: 'blue',
        dot: <HistoryOutlined />,
        children: (
          <div className="trip-history-entry">
            <Space wrap><Text strong>{entry.action.replaceAll('_', ' ')}</Text>{entry.toStatus && <TripStatusTag status={entry.toStatus} />}</Space>
            <Text type="secondary">{formatDate(entry.occurredAt)} · {entry.actor}</Text>
            {entry.details && <Paragraph>{entry.details}</Paragraph>}
          </div>
        ),
      }))} />
    </Card>
  );

  const tabItems: TabsProps['items'] = [
    { key: 'overview', label: 'Overview', children: overview },
    { key: 'assignments', label: 'Assignments', children: assignments },
    { key: 'route', label: 'Route', children: routeSection },
    { key: 'lifecycle', label: 'Lifecycle', children: lifecycleSection },
    { key: 'history', label: 'History', children: historySection },
    { key: 'logs', label: <Space size={6}>Trip Logs<Badge count="Future" /></Space>, children: <FutureSection title="Trip Logs" onExplain={setFutureSection} /> },
    { key: 'exceptions', label: <Space size={6}>Exceptions<Badge count="Future" /></Space>, children: <FutureSection title="Exceptions" onExplain={setFutureSection} /> },
  ];

  return (
    <Flex vertical gap={18}>
      <Flex justify="space-between" align="flex-start" wrap gap={16}>
        <Space direction="vertical" size={4}>
          <Link to="/trips"><Button type="link" className="trip-detail-back" icon={<ArrowLeftOutlined />}>Back to trips</Button></Link>
          <Space wrap><Title level={3} className="trip-detail-title">{trip.tripNumber}</Title><TripStatusTag status={trip.status} /></Space>
          <Text type="secondary">{origin?.name ?? shortId(trip.originLocationId)} → {destination?.name ?? shortId(trip.destinationLocationId)}</Text>
        </Space>
        <Space>
          {hasPermission('TRIP_UPDATE') && trip.status === 'DRAFT' && <Link to={`/trips/${trip.id}/edit`}><Button icon={<EditOutlined />}>Edit trip</Button></Link>}
          <Button icon={<ReloadOutlined />} loading={tripQuery.isFetching || historyQuery.isFetching} onClick={() => { void tripQuery.refetch(); void historyQuery.refetch(); }}>Refresh</Button>
        </Space>
      </Flex>

      <div className="trip-summary-grid">
        <Card variant="borderless" className="trip-detail-card trip-summary-card" title="Operational state"><Badge status={assignmentsComplete ? 'processing' : 'warning'} text={assignmentsComplete ? 'Assignments ready' : 'Action required'} /></Card>
        <Card variant="borderless" className="trip-detail-card trip-summary-card" title="Planned movement"><Space><ScheduleOutlined /><Text>{formatDate(trip.requestedStartTime)}</Text></Space></Card>
        <Card variant="borderless" className="trip-detail-card trip-summary-card" title="Route"><Space><EnvironmentOutlined /><Text>{route?.name ?? (trip.routeId ? 'Assigned route' : 'Not assigned')}</Text></Space></Card>
      </div>

      <LifecycleActions trip={trip} hasPermission={hasPermission} />

      <Card variant="borderless" className="trip-detail-tabs"><Tabs items={tabItems} destroyOnHidden /></Card>

      <VehicleAssignmentDrawer trip={trip} open={vehicleDrawerOpen} onClose={() => setVehicleDrawerOpen(false)} />
      <DriverAssignmentDrawer trip={trip} open={driverDrawerOpen} onClose={() => setDriverDrawerOpen(false)} />

      <Modal title={futureSection} open={Boolean(futureSection)} footer={<Button type="primary" onClick={() => setFutureSection(undefined)}>Close</Button>} onCancel={() => setFutureSection(undefined)}>
        <Alert type="info" showIcon message="Future backend-supported section" description={`${futureSection ?? 'This section'} will display operational records once a dedicated API is available.`} />
      </Modal>
      <Modal title="Assign route" open={routeModalOpen} okText="Assign"
        okButtonProps={{ disabled: !selectedRouteId }} confirmLoading={assignRoute.isPending}
        onOk={() => selectedRouteId && assignRoute.mutate(selectedRouteId)} onCancel={() => setRouteModalOpen(false)}>
        <Space direction="vertical" className="route-assignment-modal" size={12}>
          <Alert type="info" showIcon message="Route endpoints must match the trip origin and destination." />
          <Select value={selectedRouteId} onChange={setSelectedRouteId} loading={routesQuery.isLoading}
            placeholder="Select an active route" showSearch optionFilterProp="label"
            options={(routesQuery.data ?? []).map((candidate) => ({ value: candidate.id,
              label: `${candidate.code} — ${candidate.name}` }))} />
        </Space>
      </Modal>
    </Flex>
  );
}
