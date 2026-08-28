import { useState } from 'react';
import { Button, Card, Col, DatePicker, Form, Input, Row, Select, Space, Statistic, Table, Tag, Typography } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { freightReportApi } from '../api/freightReportApi';
import { useFreightReportShipments, useFreightReportSummary } from '../hooks/useFreightReports';
import type { FreightReportFilter, FreightShipment } from '../types';

const initialFilter: FreightReportFilter = {
  fromDate: dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
  toDate: dayjs().format('YYYY-MM-DD'),
};

export default function FreightReportsPage() {
  const { hasPermission } = useAuth();
  const [filter, setFilter] = useState(initialFilter);
  const [page, setPage] = useState(0);
  const summary = useFreightReportSummary(filter);
  const shipments = useFreightReportShipments(filter, page, 20);

  if (!hasPermission('FREIGHT_REPORT_VIEW')) return <Navigate to="/workspace" replace />;

  const exportReport = async () => {
    const blob = await freightReportApi.exportCsv(filter);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'freight-report.csv';
    link.click();
    URL.revokeObjectURL(url);
  };

  const columns = [
    { title: 'Order', dataIndex: 'orderNumber' },
    { title: 'Manifest', dataIndex: 'manifestNumber', render: (value?: string) => value ?? '—' },
    { title: 'Load Plan', dataIndex: 'loadPlanNumber', render: (value?: string) => value ?? '—' },
    { title: 'Weight (kg)', dataIndex: 'cargoWeightKg', render: (value?: number) => value ?? '—' },
    { title: 'Volume (m³)', dataIndex: 'cargoVolumeM3', render: (value?: number) => value ?? '—' },
    { title: 'Payload %', dataIndex: 'payloadUtilizationPercent', render: (value?: number) => value ?? '—' },
    { title: 'Compliance', dataIndex: 'complianceOutcome', render: (value: FreightShipment['complianceOutcome']) => (
      <Tag color={value === 'PASS' ? 'green' : value === 'FAIL' ? 'red' : 'gold'}>{value}</Tag>
    ) },
  ];

  return <Space direction="vertical" size="large" style={{ width: '100%' }}>
    <Card>
      <Form layout="vertical" onFinish={(values: { dates: [Dayjs, Dayjs]; customerId?: string; freightOrderId?: string; loadPlanStatus?: string }) => {
        setPage(0);
        setFilter({ fromDate: values.dates[0].format('YYYY-MM-DD'), toDate: values.dates[1].format('YYYY-MM-DD'),
          customerId: values.customerId || undefined, freightOrderId: values.freightOrderId || undefined,
          loadPlanStatus: values.loadPlanStatus || undefined });
      }} initialValues={{ dates: [dayjs(initialFilter.fromDate), dayjs(initialFilter.toDate)] }}>
        <Row gutter={16} align="bottom">
          <Col xs={24} md={8}><Form.Item name="dates" label="Report period" rules={[{ required: true }]}><DatePicker.RangePicker style={{ width: '100%' }} /></Form.Item></Col>
          <Col xs={24} md={5}><Form.Item name="customerId" label="Customer ID"><Input allowClear /></Form.Item></Col>
          <Col xs={24} md={5}><Form.Item name="freightOrderId" label="Freight order ID"><Input allowClear /></Form.Item></Col>
          <Col xs={24} md={3}><Form.Item name="loadPlanStatus" label="Load plan"><Select allowClear options={[{ value: 'DRAFT' }, { value: 'STRUCTURALLY_READY' }]} /></Form.Item></Col>
          <Col xs={24} md={3}><Form.Item><Button type="primary" htmlType="submit">Apply</Button></Form.Item></Col>
        </Row>
      </Form>
    </Card>
    <Row gutter={16}>
      <Col xs={12} md={4}><Card><Statistic title="Orders" value={summary.data?.freightOrders ?? 0} /></Card></Col>
      <Col xs={12} md={4}><Card><Statistic title="Manifests" value={summary.data?.manifests ?? 0} /></Card></Col>
      <Col xs={12} md={4}><Card><Statistic title="Load plans" value={summary.data?.loadPlans ?? 0} /></Card></Col>
      <Col xs={12} md={4}><Card><Statistic title="Claims" value={summary.data?.claims ?? 0} /></Card></Col>
      <Col xs={12} md={4}><Card><Statistic title="Exceptions" value={summary.data?.cargoExceptions ?? 0} /></Card></Col>
      <Col xs={12} md={4}><Card><Statistic title="Incomplete" value={summary.data?.complianceOutcomes.INCOMPLETE ?? 0} /></Card></Col>
    </Row>
    <Card title="Shipment and capacity utilization" extra={hasPermission('FREIGHT_REPORT_EXPORT') && <Button icon={<DownloadOutlined />} onClick={exportReport}>Export CSV</Button>}>
      {(summary.isError || shipments.isError) && <Typography.Text type="danger">Freight report data could not be loaded.</Typography.Text>}
      <Table<FreightShipment> rowKey={(row) => `${row.freightOrderId}-${row.manifestNumber ?? 'none'}-${row.loadPlanNumber ?? 'none'}`}
        columns={columns} dataSource={shipments.data?.content} loading={shipments.isLoading}
        pagination={{ current: page + 1, pageSize: 20, total: shipments.data?.totalElements,
          onChange: (next) => setPage(next - 1), showSizeChanger: false }} />
    </Card>
  </Space>;
}
