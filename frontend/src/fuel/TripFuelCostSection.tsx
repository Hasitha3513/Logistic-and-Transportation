import React from 'react';
import { Alert, Card, Empty, Flex, Skeleton, Space, Statistic, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { FireOutlined, DashboardOutlined, DollarOutlined, LineChartOutlined, WarningOutlined } from '@ant-design/icons';
import { useAuth } from '../auth/AuthContext';
import { useTripFuelCost, type TripFuelCostLineResponse, type PricingSource } from './useTripFuelCost';

const { Text } = Typography;
const dateTime = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' });

function formatDate(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : dateTime.format(date);
}

function PricingSourceTag({ source }: { source: PricingSource }) {
  switch (source) {
    case 'EXPLICIT_ISSUE_PRICE':
      return <Tag color="blue">Issue Price</Tag>;
    case 'PRICE_CATALOGUE':
      return <Tag color="cyan">Price Catalogue</Tag>;
    case 'UNPRICED':
      return <Tag color="error">Unpriced</Tag>;
    default:
      return <Tag>{source}</Tag>;
  }
}

export default function TripFuelCostSection({ tripId }: { tripId: string }) {
  const { hasPermission } = useAuth();
  const { data: cost, isLoading, isError, error } = useTripFuelCost(tripId);

  if (!hasPermission('FUEL_COST_VIEW') && !hasPermission('FUEL_ISSUE_VIEW')) {
    return (
      <Alert
        type="warning"
        showIcon
        message="Permission Required"
        description="You do not have permission to view fuel cost information (requires FUEL_COST_VIEW or FUEL_ISSUE_VIEW)."
      />
    );
  }

  if (isLoading) {
    return <Skeleton active paragraph={{ rows: 6 }} />;
  }

  if (isError || !cost) {
    return (
      <Alert
        type="error"
        showIcon
        message="Unable to Load Fuel Cost"
        description={error instanceof Error ? error.message : 'An error occurred while calculating fuel cost for this trip.'}
      />
    );
  }

  const columns: ColumnsType<TripFuelCostLineResponse> = [
    {
      title: 'Voucher',
      dataIndex: 'voucherNumber',
      key: 'voucherNumber',
      render: (val: string) => <Text strong>{val}</Text>,
    },
    {
      title: 'Issued At',
      dataIndex: 'issuedAt',
      key: 'issuedAt',
      render: (val: string) => formatDate(val),
    },
    {
      title: 'Fuel Type',
      dataIndex: 'fuelType',
      key: 'fuelType',
      render: (val: string) => <Tag color="geekblue">{val}</Tag>,
    },
    {
      title: 'Quantity (L)',
      dataIndex: 'quantityLiters',
      key: 'quantityLiters',
      align: 'right',
      render: (val: number) => val?.toFixed(3) ?? '0.000',
    },
    {
      title: 'Unit Price',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      align: 'right',
      render: (val: number | null, record) =>
        val != null ? `${record.currencyCode} ${val.toFixed(2)}` : <Text type="danger">Unpriced</Text>,
    },
    {
      title: 'Line Cost',
      dataIndex: 'lineCost',
      key: 'lineCost',
      align: 'right',
      render: (val: number | null, record) =>
        val != null ? (
          <Text strong>{`${record.currencyCode} ${val.toFixed(2)}`}</Text>
        ) : (
          <Text type="danger">â€”</Text>
        ),
    },
    {
      title: 'Pricing Source',
      dataIndex: 'pricingSource',
      key: 'pricingSource',
      render: (source: PricingSource) => <PricingSourceTag source={source} />,
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {cost.calculationStatus === 'PARTIAL' && cost.unpricedIssueCount > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          message="Incomplete Pricing Data"
          description={`Fuel cost calculation is partial because ${cost.unpricedIssueCount} fuel issue transaction(s) lack an authoritative unit price.`}
        />
      )}

      {cost.calculationStatus === 'PARTIAL' && cost.tripDistanceKm == null && cost.fuelIssueCount > 0 && (
        <Alert
          type="info"
          showIcon
          icon={<DashboardOutlined />}
          message="Authoritative Distance Pending"
          description="Cost per kilometer cannot be calculated until authoritative trip start and end odometer readings are recorded by Fleet."
        />
      )}

      <Flex wrap="wrap" gap={16}>
        <Card variant="borderless" style={{ flex: '1 1 200px', background: '#fafafa' }}>
          <Statistic
            title="Total Fuel Cost"
            value={cost.totalFuelCost}
            prefix={<DollarOutlined />}
            suffix={cost.currencyCode}
            precision={2}
          />
        </Card>
        <Card variant="borderless" style={{ flex: '1 1 200px', background: '#fafafa' }}>
          <Statistic
            title="Total Fuel Quantity"
            value={cost.totalFuelQuantityLiters}
            prefix={<FireOutlined />}
            suffix="L"
            precision={3}
          />
        </Card>
        <Card variant="borderless" style={{ flex: '1 1 200px', background: '#fafafa' }}>
          <Statistic
            title="Trip Distance"
            value={cost.tripDistanceKm ?? undefined}
            prefix={<DashboardOutlined />}
            suffix={cost.tripDistanceKm != null ? 'km' : undefined}
            precision={3}
          />
          {cost.tripDistanceKm == null && <Text type="secondary">Distance unavailable</Text>}
        </Card>
        <Card variant="borderless" style={{ flex: '1 1 200px', background: '#fafafa' }}>
          <Statistic
            title="Cost / KM"
            value={cost.costPerKm ?? undefined}
            prefix={<LineChartOutlined />}
            suffix={cost.costPerKm != null ? `${cost.currencyCode}/km` : undefined}
            precision={2}
          />
          {cost.costPerKm == null && <Text type="secondary">â€”</Text>}
        </Card>
        {cost.litersPer100Km != null && (
          <Card variant="borderless" style={{ flex: '1 1 200px', background: '#fafafa' }}>
            <Statistic
              title="Consumption"
              value={cost.litersPer100Km}
              suffix="L/100km"
              precision={2}
            />
          </Card>
        )}
      </Flex>

      <Card
        variant="borderless"
        title={
          <Flex justify="space-between" align="center">
            <Space>
              <FireOutlined />
              <span>Contributing Fuel Transactions</span>
              <Tag color={cost.calculationStatus === 'COMPLETE' ? 'success' : 'warning'}>
                {cost.calculationStatus}
              </Tag>
            </Space>
            <Text type="secondary">{cost.fuelIssueCount} issue(s)</Text>
          </Flex>
        }
      >
        {cost.lines.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="No fuel transactions have been issued for this trip."
          />
        ) : (
          <Table<TripFuelCostLineResponse>
            rowKey="fuelIssueId"
            columns={columns}
            dataSource={cost.lines}
            pagination={false}
            size="small"
          />
        )}
      </Card>
    </Space>
  );
}