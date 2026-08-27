import React, { useState } from 'react';
import { Button, Card, Col, Form, Input, Row, Select, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, EyeOutlined, WarningOutlined, FilterOutlined, ReloadOutlined } from '@ant-design/icons';
import { useAuth } from '../../../../auth/AuthContext';
import { Navigate, useNavigate } from 'react-router-dom';
import { useCargoExceptions } from '../hooks/useCargoExceptions';
import type { CargoException, ExceptionSeverity, ExceptionStatus, ExceptionType } from '../types';

const { Title, Text } = Typography;

export const CargoExceptionListPage: React.FC = () => {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();

  const [filterType, setFilterType] = useState<ExceptionType | undefined>(undefined);
  const [filterStatus, setFilterStatus] = useState<ExceptionStatus | undefined>(undefined);
  const [filterOrder, setFilterOrder] = useState<string>('');

  const { data: exceptions, isLoading, isError, refetch } = useCargoExceptions({
    type: filterType,
    status: filterStatus,
    freightOrderId: filterOrder.trim() || undefined,
  });

  if (!hasPermission('CARGO_EXCEPTION_VIEW')) {
    return <Navigate to="/workspace" replace />;
  }

  const getStatusColor = (status: ExceptionStatus) => {
    switch (status) {
      case 'OPEN':
        return 'blue';
      case 'HELD':
        return 'warning';
      case 'ESCALATED':
        return 'purple';
      case 'RESOLVED':
        return 'success';
      case 'REJECTED':
        return 'red';
      default:
        return 'default';
    }
  };

  const getSeverityColor = (severity: ExceptionSeverity) => {
    switch (severity) {
      case 'LOW':
        return 'default';
      case 'MEDIUM':
        return 'blue';
      case 'HIGH':
        return 'orange';
      case 'CRITICAL':
        return 'red';
      default:
        return 'default';
    }
  };

  const getTypeLabel = (type: ExceptionType) => {
    switch (type) {
      case 'DAMAGE':
        return 'Damage';
      case 'PARTIAL_SHIPMENT':
        return 'Partial Shipment';
      case 'WEIGHT_DISCREPANCY':
        return 'Weight Discrepancy';
      case 'HAZARDOUS_MATERIAL':
        return 'Hazardous Material';
      case 'UNMANIFESTED_CARGO':
        return 'Unmanifested Cargo';
      case 'SEAL_TAMPERING':
        return 'Seal Tampering';
      default:
        return type;
    }
  };

  const columns = [
    {
      title: 'Exception #',
      dataIndex: 'exceptionNumber',
      key: 'exceptionNumber',
      render: (text: string, record: CargoException) => (
        <a onClick={() => navigate(`/freight/exceptions/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'exceptionType',
      key: 'exceptionType',
      render: (type: ExceptionType) => <Tag color="geekblue">{getTypeLabel(type)}</Tag>,
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      render: (severity: ExceptionSeverity) => (
        <Tag color={getSeverityColor(severity)}>{severity}</Tag>
      ),
    },
    {
      title: 'Freight Order ID',
      dataIndex: 'freightOrderId',
      key: 'freightOrderId',
      render: (id: string) => (
        <a onClick={() => navigate(`/freight/orders/${id}`)}>
          {id.substring(0, 8)}...
        </a>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: ExceptionStatus) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      ),
    },
    {
      title: 'Restriction / Impact',
      key: 'restriction',
      render: (_: unknown, record: CargoException) => (
        <Text type={record.restriction ? 'danger' : 'secondary'} ellipsis style={{ maxWidth: 200 }}>
          {record.restriction || record.impact || '—'}
        </Text>
      ),
    },
    {
      title: 'Created At',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (text: string) => new Date(text).toLocaleString(),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: CargoException) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/freight/exceptions/${record.id}`)}
          >
            View
          </Button>
        </Space>
      ),
    },
  ];

  if (isError) {
    message.error('Failed to load cargo exceptions');
  }

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>
              <WarningOutlined style={{ marginRight: 8, color: '#fa8c16' }} />
              Cargo Exceptions
            </Title>
            <Text type="secondary">
              Manage damages, partials, weight gaps, hazardous materials, unmanifested cargo and seal tampering
            </Text>
          </div>
          {hasPermission('CARGO_EXCEPTION_MANAGE') && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/freight/exceptions/new')}
            >
              Record Exception
            </Button>
          )}
        </div>

        <Card size="small" style={{ marginBottom: 16, background: '#fafafa' }}>
          <Form layout="inline">
            <Row gutter={[16, 16]} style={{ width: '100%' }}>
              <Col xs={24} sm={8} md={6}>
                <Form.Item label="Type" style={{ width: '100%', marginBottom: 0 }}>
                  <Select
                    allowClear
                    placeholder="All Types"
                    value={filterType}
                    onChange={(val) => setFilterType(val)}
                    style={{ width: '100%' }}
                  >
                    <Select.Option value="DAMAGE">Damage</Select.Option>
                    <Select.Option value="PARTIAL_SHIPMENT">Partial Shipment</Select.Option>
                    <Select.Option value="WEIGHT_DISCREPANCY">Weight Discrepancy</Select.Option>
                    <Select.Option value="HAZARDOUS_MATERIAL">Hazardous Material</Select.Option>
                    <Select.Option value="UNMANIFESTED_CARGO">Unmanifested Cargo</Select.Option>
                    <Select.Option value="SEAL_TAMPERING">Seal Tampering</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col xs={24} sm={8} md={6}>
                <Form.Item label="Status" style={{ width: '100%', marginBottom: 0 }}>
                  <Select
                    allowClear
                    placeholder="All Statuses"
                    value={filterStatus}
                    onChange={(val) => setFilterStatus(val)}
                    style={{ width: '100%' }}
                  >
                    <Select.Option value="OPEN">OPEN</Select.Option>
                    <Select.Option value="HELD">HELD</Select.Option>
                    <Select.Option value="ESCALATED">ESCALATED</Select.Option>
                    <Select.Option value="RESOLVED">RESOLVED</Select.Option>
                    <Select.Option value="REJECTED">REJECTED</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col xs={24} sm={8} md={8}>
                <Form.Item label="Order ID" style={{ width: '100%', marginBottom: 0 }}>
                  <Input
                    allowClear
                    placeholder="Filter by Order UUID"
                    value={filterOrder}
                    onChange={(e) => setFilterOrder(e.target.value)}
                  />
                </Form.Item>
              </Col>
              <Col xs={24} sm={24} md={4} style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
                  Refresh
                </Button>
              </Col>
            </Row>
          </Form>
        </Card>

        <Table
          dataSource={exceptions || []}
          columns={columns}
          rowKey="id"
          loading={isLoading}
          pagination={{ pageSize: 20 }}
        />
      </Card>
    </div>
  );
};
