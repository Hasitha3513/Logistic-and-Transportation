import React, { useState } from 'react';
import { Card, Radio, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useDeliveryAnalyticsTrends } from '../hooks/useDeliveryAnalytics';
import { DeliveryAnalyticsFilters, DeliveryTrendItem } from '../types/deliveryAnalytics';

interface DeliveryTrendsSectionProps {
  filters: DeliveryAnalyticsFilters;
}

export const DeliveryTrendsSection: React.FC<DeliveryTrendsSectionProps> = ({ filters }) => {
  const [granularity, setGranularity] = useState<'DAY' | 'WEEK' | 'MONTH'>('DAY');
  const { data: trends = [], isLoading } = useDeliveryAnalyticsTrends(filters, granularity);

  const columns: ColumnsType<DeliveryTrendItem> = [
    {
      title: 'Time Bucket',
      dataIndex: 'bucketDate',
      key: 'bucketDate',
      render: (date: string) => <strong>{date}</strong>,
    },
    {
      title: 'Created Orders',
      dataIndex: 'totalCreated',
      key: 'totalCreated',
    },
    {
      title: 'Delivered Orders',
      dataIndex: 'delivered',
      key: 'delivered',
      render: (delivered: number) => <Tag color="green">{delivered}</Tag>,
    },
    {
      title: 'On-Time',
      dataIndex: 'onTimeDelivered',
      key: 'onTimeDelivered',
    },
    {
      title: 'Late',
      dataIndex: 'lateDelivered',
      key: 'lateDelivered',
      render: (late: number) => (late > 0 ? <Tag color="volcano">{late}</Tag> : '0'),
    },
    {
      title: 'Failed Attempts',
      dataIndex: 'failedAttempts',
      key: 'failedAttempts',
      render: (fails: number) => (fails > 0 ? <Tag color="orange">{fails}</Tag> : '0'),
    },
    {
      title: 'Returned (RTO)',
      dataIndex: 'returnedToBase',
      key: 'returnedToBase',
      render: (rto: number) => (rto > 0 ? <Tag color="red">{rto}</Tag> : '0'),
    },
  ];

  return (
    <Card
      title="Delivery Volume & Completion Trends"
      style={{ marginTop: 16 }}
      extra={
        <Radio.Group
          value={granularity}
          onChange={(e) => setGranularity(e.target.value)}
          optionType="button"
          buttonStyle="solid"
          size="small"
        >
          <Radio.Button value="DAY">Daily</Radio.Button>
          <Radio.Button value="WEEK">Weekly</Radio.Button>
          <Radio.Button value="MONTH">Monthly</Radio.Button>
        </Radio.Group>
      }
    >
      <Table
        rowKey="bucketDate"
        columns={columns}
        dataSource={trends}
        loading={isLoading}
        pagination={{ pageSize: 10 }}
        locale={{ emptyText: 'No delivery trend records in the selected period' }}
      />
    </Card>
  );
};
