import React from 'react';
import { Card, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { RegionalPerformanceItem } from '../types/deliveryAnalytics';

interface DeliveryRegionsTableProps {
  data?: RegionalPerformanceItem[];
  loading?: boolean;
}

export const DeliveryRegionsTable: React.FC<DeliveryRegionsTableProps> = ({
  data = [],
  loading = false,
}) => {
  const formatPercent = (val: number | null) => {
    if (val === null) return 'N/A';
    return `${val.toFixed(1)}%`;
  };

  const columns: ColumnsType<RegionalPerformanceItem> = [
    {
      title: 'Location / Region',
      dataIndex: 'locationName',
      key: 'locationName',
      render: (name: string, record) => (
        <span>
          <strong>{name}</strong>{' '}
          {record.locationCode && <Tag>{record.locationCode}</Tag>}
        </span>
      ),
    },
    {
      title: 'Total Orders',
      dataIndex: 'totalOrders',
      key: 'totalOrders',
      sorter: (a, b) => a.totalOrders - b.totalOrders,
    },
    {
      title: 'Delivered',
      dataIndex: 'deliveredOrders',
      key: 'deliveredOrders',
    },
    {
      title: 'Returned (RTO)',
      dataIndex: 'returnedToBaseOrders',
      key: 'returnedToBaseOrders',
    },
    {
      title: 'Success Rate',
      dataIndex: 'orderSuccessRate',
      key: 'orderSuccessRate',
      render: (rate: number | null) => formatPercent(rate),
      sorter: (a, b) => (a.orderSuccessRate ?? 0) - (b.orderSuccessRate ?? 0),
    },
    {
      title: 'On-Time Rate',
      dataIndex: 'onTimeDeliveryRate',
      key: 'onTimeDeliveryRate',
      render: (rate: number | null) => formatPercent(rate),
      sorter: (a, b) => (a.onTimeDeliveryRate ?? 0) - (b.onTimeDeliveryRate ?? 0),
    },
    {
      title: 'Avg Delay (Mins)',
      dataIndex: 'averageDelayMinutes',
      key: 'averageDelayMinutes',
      render: (delay: number | null) => (delay != null ? `${delay.toFixed(1)}m` : 'N/A'),
    },
    {
      title: 'Failed Attempts',
      dataIndex: 'failedAttemptCount',
      key: 'failedAttemptCount',
    },
  ];

  return (
    <Card title="Regional Delivery Performance" style={{ marginTop: 16 }}>
      <Table
        rowKey={(record) => record.destinationLocationId || record.locationCode}
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{ pageSize: 10 }}
        locale={{ emptyText: 'No regional delivery data found in the selected period' }}
      />
    </Card>
  );
};
