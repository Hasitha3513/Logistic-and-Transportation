import React from 'react';
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, EyeOutlined, FileProtectOutlined } from '@ant-design/icons';
import { useAuth } from '../../../../auth/AuthContext';
import { Navigate, useNavigate } from 'react-router-dom';
import { useClaims } from '../hooks/useInsurance';
import { ClaimResponse, ClaimStatus } from '../types/insurance';

const { Title, Text } = Typography;

export const ClaimListPage: React.FC = () => {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const { data: claims, isLoading, isError } = useClaims();

  if (!hasPermission('CARGO_INSURANCE_VIEW')) {
    return <Navigate to="/workspace" replace />;
  }

  const getStatusColor = (status: ClaimStatus) => {
    switch (status) {
      case 'OPEN':
        return 'blue';
      case 'UNDER_REVIEW':
        return 'orange';
      case 'APPROVED':
        return 'cyan';
      case 'REJECTED':
        return 'red';
      case 'DISPUTED':
        return 'purple';
      case 'SETTLED':
        return 'green';
      default:
        return 'default';
    }
  };

  const columns = [
    {
      title: 'Claim Number',
      dataIndex: 'claimNumber',
      key: 'claimNumber',
      render: (text: string, record: ClaimResponse) => (
        <a onClick={() => navigate(`/freight/insurance/claims/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: 'Policy ID',
      dataIndex: 'policyId',
      key: 'policyId',
      render: (text: string) => (
        <a onClick={() => navigate(`/freight/insurance/policies/${text}`)}>
          {text.substring(0, 8)}...
        </a>
      ),
    },
    {
      title: 'Incident Date',
      dataIndex: 'incidentDate',
      key: 'incidentDate',
      render: (text: string) => new Date(text).toLocaleDateString(),
    },
    {
      title: 'Claimed Amount',
      key: 'claimedAmount',
      render: (_: unknown, record: ClaimResponse) => (
        <span>{record.claimedAmount.toLocaleString()} {record.currencyCode}</span>
      ),
    },
    {
      title: 'Assessed Amount',
      key: 'assessedAmount',
      render: (_: unknown, record: ClaimResponse) => (
        <span>
          {record.assessedAmount != null
            ? `${record.assessedAmount.toLocaleString()} ${record.currencyCode}`
            : '—'}
        </span>
      ),
    },
    {
      title: 'Settled Amount',
      key: 'totalSettledAmount',
      render: (_: unknown, record: ClaimResponse) => (
        <Text strong type={record.totalSettledAmount > 0 ? 'success' : undefined}>
          {record.totalSettledAmount.toLocaleString()} {record.currencyCode}
        </Text>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: ClaimStatus) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: ClaimResponse) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/freight/insurance/claims/${record.id}`)}
          >
            View
          </Button>
        </Space>
      ),
    },
  ];

  if (isError) {
    message.error('Failed to load insurance claims');
  }

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>
              <FileProtectOutlined style={{ marginRight: 8, color: '#1677ff' }} />
              Freight Insurance Claims
            </Title>
            <Text type="secondary">Review, assess, approve, dispute, and settle insurance claims</Text>
          </div>
          <Space>
            <Button onClick={() => navigate('/freight/insurance/policies')}>
              View Policies
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/freight/insurance/claims/new')}
            >
              File Claim
            </Button>
          </Space>
        </div>

        <Table
          dataSource={claims || []}
          columns={columns}
          rowKey="id"
          loading={isLoading}
          pagination={{ pageSize: 50 }}
        />
      </Card>
    </div>
  );
};
