import React from 'react';
import { Card, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { FailureReasonBreakdownItem } from '../types/deliveryAnalytics';

const { Text } = Typography;

interface DeliveryFailuresTableProps {
  data?: FailureReasonBreakdownItem[];
  loading?: boolean;
}

export const DeliveryFailuresTable: React.FC<DeliveryFailuresTableProps> = ({
  data = [],
  loading = false,
}) => {
  const columns: ColumnsType<FailureReasonBreakdownItem> = [
    {
      title: 'Failure Reason',
      dataIndex: 'failureReason',
      key: 'failureReason',
      render: (reason: string) => {
        const readable = reason.replace(/_/g, ' ');
        let color = 'default';
        if (reason === 'CUSTOMER_UNAVAILABLE') color = 'orange';
        if (reason === 'WRONG_ADDRESS') color = 'volcano';
        if (reason === 'CUSTOMER_REFUSED') color = 'red';
        if (reason === 'DAMAGED_CARGO') color = 'magenta';
        if (reason === 'ACCESS_RESTRICTED') color = 'gold';
        return <Tag color={color}>{readable}</Tag>;
      },
    },
    {
      title: 'Failed Attempts',
      dataIndex: 'count',
      key: 'count',
      sorter: (a, b) => a.count - b.count,
    },
    {
      title: 'Share of Failures',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (pct: number) => `${pct.toFixed(1)}%`,
      sorter: (a, b) => a.percentage - b.percentage,
    },
    {
      title: 'Redelivery Eligible',
      dataIndex: 'redeliveryEligibleCount',
      key: 'redeliveryEligibleCount',
    },
    {
      title: 'Return to Base',
      dataIndex: 'returnToBaseCount',
      key: 'returnToBaseCount',
    },
    {
      title: 'Escalated',
      dataIndex: 'escalatedCount',
      key: 'escalatedCount',
    },
  ];

  return (
    <Card title="Failure Reasons Breakdown" style={{ marginTop: 16 }}>
      <Table
        rowKey="failureReason"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={false}
        locale={{ emptyText: 'No delivery failures recorded in the selected period' }}
      />
    </Card>
  );
};
