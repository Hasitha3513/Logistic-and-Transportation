import { useMemo, useState } from 'react';
import {
  CopyOutlined,
  DownOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  ScheduleOutlined,
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import { Link } from 'react-router-dom';
import {
  Alert,
  App as AntApp,
  Button,
  Card,
  DatePicker,
  Dropdown,
  Empty,
  Flex,
  Input,
  Select,
  Space,
  Table,
  Tooltip,
  Typography,
  type MenuProps,
  type TableColumnsType,
  type TableProps,
} from 'antd';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PriorityTag, TripStatusTag } from '../components/status/StatusTags';
import type {
  CustomerReference,
  DriverReference,
  LocationReference,
  PagedTrips,
  Trip,
  TripResponse,
  VehicleReference,
} from './types';

const { RangePicker } = DatePicker;
const { Search } = Input;
const { Text, Title } = Typography;

const statusOptions = [
  'DRAFT', 'SUBMITTED', 'APPROVED', 'ASSIGNED', 'DISPATCHED',
  'IN_PROGRESS', 'COMPLETED', 'CLOSED', 'REJECTED', 'CANCELLED',
].map((value) => ({ value, label: value.replaceAll('_', ' ') }));

const dateTime = new Intl.DateTimeFormat('en-GB', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

function isPaged(response?: TripResponse): response is PagedTrips {
  return Boolean(response && !Array.isArray(response) && Array.isArray(response.content));
}

function referenceMap<T extends { id: string }>(values?: T[]) {
  return new Map((values ?? []).map((value) => [value.id, value]));
}

function shortId(id?: string | null) {
  return id ? id.slice(0, 8).toUpperCase() : '—';
}

export default function TripListPage() {
  const { message } = AntApp.useApp();
  const { hasPermission } = useAuth();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<string>();
  const [period, setPeriod] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [sortField, setSortField] = useState<string>();
  const [sortOrder, setSortOrder] = useState<'ascend' | 'descend'>();

  const tripQuery = useQuery({
    queryKey: ['trips', { page, pageSize, search, status, period, sortField, sortOrder }],
    queryFn: async () => (await api.get<TripResponse>('/trips', {
      params: {
        page: page - 1,
        limit: pageSize,
        query: search || undefined,
        status,
        from: period?.[0]?.startOf('day').toISOString(),
        to: period?.[1]?.endOf('day').toISOString(),
        sort: sortField,
        direction: sortOrder === 'descend' ? 'desc' : sortOrder ? 'asc' : undefined,
      },
    })).data,
    placeholderData: (previous) => previous,
  });
  const customers = useQuery({ queryKey: ['customers', 'references'], queryFn: async () => (await api.get<CustomerReference[]>('/customers')).data });
  const locations = useQuery({ queryKey: ['locations', 'references'], queryFn: async () => (await api.get<LocationReference[]>('/locations')).data });
  const vehicles = useQuery({
    queryKey: ['vehicles', 'references'],
    queryFn: async () => (await api.get<VehicleReference[]>('/vehicles')).data,
    enabled: hasPermission('VEHICLE_VIEW'),
  });
  const drivers = useQuery({
    queryKey: ['drivers', 'references'],
    queryFn: async () => (await api.get<DriverReference[]>('/drivers')).data,
    enabled: hasPermission('DRIVER_VIEW'),
  });

  const customerById = useMemo(() => referenceMap(customers.data), [customers.data]);
  const locationById = useMemo(() => referenceMap(locations.data), [locations.data]);
  const vehicleById = useMemo(() => referenceMap(vehicles.data), [vehicles.data]);
  const driverById = useMemo(() => referenceMap(drivers.data), [drivers.data]);
  const pagedTrips = isPaged(tripQuery.data) ? tripQuery.data : undefined;
  const serverPaged = Boolean(pagedTrips);

  const filteredTrips = useMemo(() => {
    if (!tripQuery.data) return [];
    if (isPaged(tripQuery.data)) return tripQuery.data.content;
    const normalizedSearch = search.trim().toLowerCase();
    const from = period?.[0]?.startOf('day').valueOf();
    const to = period?.[1]?.endOf('day').valueOf();
    const matching = tripQuery.data.filter((trip) => {
      const customerName = customerById.get(trip.customerId ?? '')?.name ?? '';
      const matchesSearch = !normalizedSearch
        || trip.tripNumber.toLowerCase().includes(normalizedSearch)
        || customerName.toLowerCase().includes(normalizedSearch)
        || (trip.cargoDescription ?? '').toLowerCase().includes(normalizedSearch);
      const plannedStart = new Date(trip.requestedStartTime).getTime();
      return matchesSearch && (!status || trip.status === status)
        && (!from || plannedStart >= from) && (!to || plannedStart <= to);
    });
    if (sortField && sortOrder) {
      matching.sort((left, right) => {
        const leftValue = String(left[sortField as keyof Trip] ?? '');
        const rightValue = String(right[sortField as keyof Trip] ?? '');
        const comparison = leftValue.localeCompare(rightValue);
        return sortOrder === 'descend' ? -comparison : comparison;
      });
    }
    return matching;
  }, [customerById, period, search, sortField, sortOrder, status, tripQuery.data]);

  const displayedTrips = serverPaged
    ? filteredTrips
    : filteredTrips.slice((page - 1) * pageSize, page * pageSize);
  const total = pagedTrips?.total ?? filteredTrips.length;

  const copy = async (value: string, label: string) => {
    try {
      await navigator.clipboard.writeText(value);
      void message.success(`${label} copied`);
    } catch {
      void message.error(`${label} could not be copied`);
    }
  };

  const actions = (trip: Trip): MenuProps['items'] => [
    { key: 'view', icon: <EyeOutlined />, label: <Link to={`/trips/${trip.id}`}>View details</Link> },
    ...(hasPermission('TRIP_UPDATE') && trip.status === 'DRAFT' ? [{ key: 'edit', icon: <EditOutlined />, label: <Link to={`/trips/${trip.id}/edit`}>Edit trip</Link> }] : []),
    { key: 'copy-number', icon: <CopyOutlined />, label: 'Copy trip number', onClick: () => void copy(trip.tripNumber, 'Trip number') },
    { key: 'copy-id', icon: <CopyOutlined />, label: 'Copy trip ID', onClick: () => void copy(trip.id, 'Trip ID') },
  ];

  const columns: TableColumnsType<Trip> = [
    {
      title: 'Trip Number', dataIndex: 'tripNumber', key: 'tripNumber', fixed: 'left', width: 150,
      sorter: true, sortOrder: sortField === 'tripNumber' ? sortOrder : undefined,
      render: (value: string, trip) => <Link to={`/trips/${trip.id}`}><Text strong>{value}</Text></Link>,
    },
    { title: 'Customer', dataIndex: 'customerId', key: 'customer', width: 180, responsive: ['md'], render: (id) => customerById.get(id)?.name ?? shortId(id) },
    { title: 'Origin', dataIndex: 'originLocationId', key: 'origin', width: 160, render: (id) => locationById.get(id)?.name ?? shortId(id) },
    { title: 'Destination', dataIndex: 'destinationLocationId', key: 'destination', width: 160, render: (id) => locationById.get(id)?.name ?? shortId(id) },
    {
      title: 'Planned Start', dataIndex: 'requestedStartTime', key: 'requestedStartTime', width: 180,
      sorter: true, sortOrder: sortField === 'requestedStartTime' ? sortOrder : undefined,
      render: (value: string) => dateTime.format(new Date(value)),
    },
    { title: 'Planned End', dataIndex: 'requestedEndTime', key: 'plannedEnd', width: 180, responsive: ['xl'], render: (value: string) => dateTime.format(new Date(value)) },
    { title: 'Priority', dataIndex: 'priority', key: 'priority', width: 105, responsive: ['lg'], render: (value: string) => <PriorityTag priority={value} /> },
    { title: 'Vehicle', dataIndex: 'vehicleId', key: 'vehicle', width: 145, responsive: ['xl'], render: (id) => vehicleById.get(id)?.registrationNumber ?? shortId(id) },
    { title: 'Driver', dataIndex: 'driverId', key: 'driver', width: 170, responsive: ['xl'], render: (id) => { const driver = driverById.get(id); return driver ? `${driver.firstName} ${driver.lastName}` : shortId(id); } },
    { title: 'Status', dataIndex: 'status', key: 'status', width: 125, fixed: 'right', render: (value: string) => <TripStatusTag status={value} /> },
    {
      title: 'Actions', key: 'actions', fixed: 'right', width: 95, align: 'center',
      render: (_, trip) => (
        <Dropdown menu={{ items: actions(trip) }} placement="bottomRight" trigger={['click']}>
          <Tooltip title="Trip actions"><Button type="text">More <DownOutlined /></Button></Tooltip>
        </Dropdown>
      ),
    },
  ];

  const handleTableChange: NonNullable<TableProps<Trip>['onChange']> = (pagination, _filters, sorter) => {
    const activeSorter = Array.isArray(sorter) ? sorter[0] : sorter;
    setPage(pagination.current ?? 1);
    setPageSize(pagination.pageSize ?? 10);
    setSortField(typeof activeSorter?.field === 'string' ? activeSorter.field : undefined);
    setSortOrder(activeSorter?.order ?? undefined);
  };

  const resetPage = () => setPage(1);

  return (
    <Flex vertical gap={18}>
      <Flex justify="space-between" align="flex-start" wrap gap={16}>
        <div>
          <Title level={3} className="trip-list__title">Trip operations</Title>
          <Text type="secondary">Review planned movement, assignments, and lifecycle status.</Text>
        </div>
        <Space>
          {hasPermission('TRIP_CREATE') && <Link to="/trips/new"><Button type="primary" icon={<PlusOutlined />}>Create trip</Button></Link>}
          <Tooltip title="Refresh trip data">
            <Button icon={<ReloadOutlined />} onClick={() => void tripQuery.refetch()} loading={tripQuery.isFetching}>Refresh</Button>
          </Tooltip>
        </Space>
      </Flex>

      <Card className="trip-filters" variant="borderless">
        <Flex wrap gap={12} align="center">
          <Search
            className="trip-filters__search"
            placeholder="Search trip number, customer, or cargo"
            allowClear
            onSearch={(value) => { setSearch(value); resetPage(); }}
          />
          <Select
            className="trip-filters__status"
            placeholder="All statuses"
            allowClear
            options={statusOptions}
            onChange={(value) => { setStatus(value); resetPage(); }}
          />
          <RangePicker
            className="trip-filters__period"
            showTime
            value={period}
            onChange={(value) => { setPeriod(value); resetPage(); }}
          />
        </Flex>
      </Card>

      {tripQuery.isError && (
        <Alert type="error" showIcon message="Trips could not be loaded" description="Retry the request or contact support if the problem continues." />
      )}

      <Card className="trip-table-card" variant="borderless">
        <Table<Trip>
          aria-label="Trips"
          rowKey="id"
          columns={columns}
          dataSource={displayedTrips}
          loading={tripQuery.isLoading}
          scroll={{ x: 1500 }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No trips match the current filters" /> }}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            pageSizeOptions: [10, 20, 50],
            showTotal: (count, range) => `${range[0]}–${range[1]} of ${count} trips`,
          }}
          onChange={handleTableChange}
        />
        {!serverPaged && !tripQuery.isLoading && (
          <Space className="trip-table-card__mode" size={6}>
            <ScheduleOutlined />
            <Text type="secondary">Client pagination fallback — the current API returns an unpaged list.</Text>
          </Space>
        )}
      </Card>
    </Flex>
  );
}
