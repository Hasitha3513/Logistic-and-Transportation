import React from 'react';
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, EyeOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useAuth } from '../../../../auth/AuthContext';
import { Navigate, useNavigate } from 'react-router-dom';
import { usePolicies } from '../hooks/useInsurance';
import { PolicyResponse, PolicyStatus } from '../types/insurance';

const { Title, Text } = Typography;

export const PolicyListPage: React.FC = () => {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const { data: policies, isLoading, isError } = usePolicies();

  if (!hasPermission('CARGO_INSURANCE_VIEW')) {
    return <Navigate to="/workspace" replace />;
  }

  const getStatusColor = (status: PolicyStatus) => {
    switch (status) {
      case 'ACTIVE':
        return 'green';
      case 'EXPIRED':
        return 'orange';
      case 'CANCELLED':
        return 'red';
      default:
        return 'default';
    }
  };

  const columns = [
    {
      title: 'Policy Number',
      dataIndex: 'policyNumber',
      key: 'policyNumber',
      render: (text: string, record: PolicyResponse) => (
        <a onClick={() => navigate(`/freight/insurance/policies/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: 'Provider',
      dataIndex: 'insuranceProvider',
      key: 'insuranceProvider',
    },
    {
      title: 'Policy Type',
      dataIndex: 'policyType',
      key: 'policyType',
    },
    {
      title: 'Coverage',
      key: 'coverage',
      render: (_: unknown, record: PolicyResponse) => (
        <Text strong>
          {record.coverageAmount.toLocaleString()} {record.currencyCode}
        </Text>
      ),
    },
    {
      title: 'Deductible',
      key: 'deductible',
      render: (_: unknown, record: PolicyResponse) => (
        <span>
          {record.deductibleAmount.toLocaleString()} {record.currencyCode}
        </span>
      ),
    },
    {
      title: 'Validity',
      key: 'validity',
      render: (_: unknown, record: PolicyResponse) => (
        <span>
          {new Date(record.validFrom).toLocaleDateString()} - {new Date(record.validUntil).toLocaleDateString()}
        </span>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: PolicyStatus) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: PolicyResponse) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/freight/insurance/policies/${record.id}`)}
          >
            View
          </Button>
        </Space>
      ),
    },
  ];

  if (isError) {
    message.error('Failed to load insurance policies');
  }

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>
              <SafetyCertificateOutlined style={{ marginRight: 8, color: '#1677ff' }} />
              Freight Insurance Policies
            </Title>
            <Text type="secondary">Manage cargo insurance coverage policies and terms</Text>
          </div>
          <Space>
            <Button
              onClick={() => navigate('/freight/insurance/claims')}
            >
              View Claims
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/freight/insurance/policies/new')}
            >
              New Policy
            </Button>
          </Space>
        </div>

        <Table
          dataSource={policies || []}
          columns={columns}
          rowKey="id"
          loading={isLoading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
};
