import React, { useState } from 'react';
import dayjs, { type Dayjs } from 'dayjs';
import { Alert, Button, DatePicker, Drawer, Flex, Select, Space, Table, Tag, Tooltip, Typography } from 'antd';
import { HistoryOutlined, ReloadOutlined } from '@ant-design/icons';
import type { DeliveryFilters, NotificationDelivery, NotificationStatus } from './types';
import { useNotificationDeliveries, useNotificationDeliveryAttempts } from './useNotificationRules';

const STATUS_COLOR: Record<NotificationStatus, string> = { PENDING: 'processing', SENT: 'success', FAILED: 'error', READ: 'blue' };
const displayTime = (value?: string) => value ? new Date(value).toLocaleString() : '—';

export const NotificationDeliveryDiagnostics: React.FC<{ eventTypes: string[]; canManage: boolean }> = ({ eventTypes, canManage }) => {
  const [filters, setFilters] = useState<DeliveryFilters>({ limit: 100 });
  const [selectedId, setSelectedId] = useState<string>();
  const deliveries = useNotificationDeliveries(filters);
  const attempts = useNotificationDeliveryAttempts(selectedId);
  const dateRange = filters.from && filters.to ? [dayjs(filters.from), dayjs(filters.to)] as [Dayjs, Dayjs] : null;

  return <>
    <Flex justify="space-between" align="center" wrap gap={12} style={{ marginBottom: 16 }}>
      <Space wrap>
        <Select allowClear placeholder="Status" aria-label="Delivery status filter" style={{ width: 150 }}
          options={['PENDING', 'SENT', 'FAILED', 'READ'].map((value) => ({ value, label: value }))}
          onChange={(status?: NotificationStatus) => setFilters((current) => ({ ...current, status }))} />
        <Select allowClear showSearch placeholder="Event" aria-label="Delivery event filter" style={{ minWidth: 260 }}
          options={eventTypes.map((value) => ({ value, label: value }))}
          onChange={(eventType?: string) => setFilters((current) => ({ ...current, eventType }))} />
        <DatePicker.RangePicker showTime value={dateRange} aria-label="Delivery date range"
          onChange={(range) => setFilters((current) => ({ ...current,
            from: range?.[0]?.toISOString(), to: range?.[1]?.toISOString() }))} />
      </Space>
      <Button icon={<ReloadOutlined />} onClick={() => deliveries.refetch()}>Refresh</Button>
    </Flex>
    {deliveries.isError && <Alert type="error" showIcon message="Delivery diagnostics could not be loaded" style={{ marginBottom: 16 }} />}
    <Table rowKey="notificationId" loading={deliveries.isLoading} dataSource={deliveries.data ?? []} scroll={{ x: 1050 }} pagination={{ pageSize: 10 }}
      locale={{ emptyText: 'No deliveries match the selected filters.' }} columns={[
        { title: 'Status', dataIndex: 'status', render: (status: NotificationStatus, row: NotificationDelivery) => <Space>
          <Tag color={STATUS_COLOR[status]}>{status}</Tag>{row.terminalFailure && <Tag color="red">Terminal</Tag>}</Space> },
        { title: 'Event', dataIndex: 'eventType', ellipsis: true },
        { title: 'Channel', dataIndex: 'channel', render: (value: string) => <Tag>{value}</Tag> },
        { title: 'Recipient', dataIndex: 'recipient', render: (value?: string) => value ?? '—' },
        { title: 'Attempts', dataIndex: 'attemptCount' },
        { title: 'Next retry', dataIndex: 'nextDeliveryAt', render: displayTime },
        { title: 'Created', dataIndex: 'createdAt', render: displayTime },
        { title: 'Sent', dataIndex: 'sentAt', render: displayTime },
        { title: 'Escalation', render: (_: unknown, row: NotificationDelivery) => row.escalationLevel > 0
          ? <Tooltip title={row.parentNotificationId ? `Parent ${row.parentNotificationId}` : undefined}><Tag color="purple">Level {row.escalationLevel}</Tag></Tooltip> : '—' },
        { title: 'History', render: (_: unknown, row: NotificationDelivery) => canManage
          ? <Button type="link" icon={<HistoryOutlined />} aria-label={`View attempts for ${row.eventType}`} onClick={() => setSelectedId(row.notificationId)}>Attempts</Button>
          : <Typography.Text type="secondary">Manager only</Typography.Text> },
      ]} />
    <Drawer title="Delivery Attempt History" width={720} open={Boolean(selectedId)} onClose={() => setSelectedId(undefined)}>
      {attempts.isError && <Alert type="error" showIcon message="Attempt history could not be loaded" />}
      <Table rowKey="id" loading={attempts.isLoading} dataSource={attempts.data ?? []} pagination={false} scroll={{ x: 900 }} columns={[
        { title: '#', dataIndex: 'attemptNumber' }, { title: 'State', dataIndex: 'state', render: (value: string) => <Tag>{value}</Tag> },
        { title: 'Due', dataIndex: 'dueAt', render: displayTime }, { title: 'Started', dataIndex: 'startedAt', render: displayTime },
        { title: 'Completed', dataIndex: 'completedAt', render: displayTime }, { title: 'Category', dataIndex: 'errorCategory', render: (value?: string) => value ?? '—' },
        { title: 'Error', render: (_: unknown, row) => row.errorMessage || row.errorCode || '—' },
        { title: 'Provider ID', dataIndex: 'providerMessageId', render: (value?: string) => value ?? '—' },
      ]} />
    </Drawer>
  </>;
};
