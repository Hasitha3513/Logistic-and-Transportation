import React, { useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Divider,
  Modal,
  Form,
  InputNumber,
  DatePicker,
  Select,
  Input,
  Space,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import {
  ArrowLeftOutlined,
  EditOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { isAxiosError } from 'axios';
import type { Dayjs } from 'dayjs';
import { useAuth } from '../../../../auth/AuthContext';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import {
  useClaimsByPolicy,
  usePolicy,
  useUpdatePolicy
} from '../hooks/useInsurance';
import { ClaimResponse, ClaimStatus, PolicyStatus, UpdatePolicyPayload } from '../types/insurance';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface PolicyUpdateFormValues {
  coverageAmount: number;
  premiumAmount: number;
  deductibleAmount: number;
  validity?: [Dayjs, Dayjs];
  status: PolicyStatus;
  termsAndConditions?: string;
}

export const PolicyDetailsPage: React.FC = () => {
  const { hasPermission } = useAuth();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [form] = Form.useForm<PolicyUpdateFormValues>();

  const { data: policy, isLoading, isError } = usePolicy(id || '');
  const { data: claims, isLoading: isClaimsLoading } = useClaimsByPolicy(id || '');
  const updatePolicyMutation = useUpdatePolicy(id || '');

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

  const getClaimStatusColor = (status: ClaimStatus) => {
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

  const handleOpenEdit = () => {
    if (!policy) return;
    form.setFieldsValue({
      coverageAmount: policy.coverageAmount,
      premiumAmount: policy.premiumAmount,
      deductibleAmount: policy.deductibleAmount,
      status: policy.status,
      termsAndConditions: policy.termsAndConditions,
    });
    setIsEditModalOpen(true);
  };

  const handleUpdate = async (values: PolicyUpdateFormValues) => {
    if (!policy) return;
    try {
      const payload: UpdatePolicyPayload = {
        coverageAmount: values.coverageAmount,
        premiumAmount: values.premiumAmount,
        deductibleAmount: values.deductibleAmount,
        validFrom: values.validity ? values.validity[0].toISOString() : policy.validFrom,
        validUntil: values.validity ? values.validity[1].toISOString() : policy.validUntil,
        status: values.status,
        termsAndConditions: values.termsAndConditions,
        version: policy.version,
      };
      await updatePolicyMutation.mutateAsync(payload);
      message.success('Policy updated successfully');
      setIsEditModalOpen(false);
    } catch (err: unknown) {
      const msg = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message ?? 'Failed to update policy'
        : 'Failed to update policy';
      message.error(msg);
    }
  };

  const claimColumns = [
    {
      title: 'Claim Number',
      dataIndex: 'claimNumber',
      key: 'claimNumber',
      render: (text: string, record: ClaimResponse) => (
        <a onClick={() => navigate(`/freight/insurance/claims/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: 'Incident Date',
      dataIndex: 'incidentDate',
      key: 'incidentDate',
      render: (text: string) => new Date(text).toLocaleDateString(),
    },
    {
      title: 'Claimed',
      key: 'claimedAmount',
      render: (_: unknown, record: ClaimResponse) => (
        <span>
          {record.claimedAmount.toLocaleString()} {record.currencyCode}
        </span>
      ),
    },
    {
      title: 'Assessed',
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
      title: 'Settled',
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
        <Tag color={getClaimStatusColor(status)}>{status}</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: ClaimResponse) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/freight/insurance/claims/${record.id}`)}
        >
          View
        </Button>
      ),
    },
  ];

  if (isLoading) {
    return <Card loading style={{ margin: 24 }} />;
  }

  if (isError || !policy) {
    return (
      <Card style={{ margin: 24 }}>
        <Text type="danger">Failed to load policy details</Text>
      </Card>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <Space>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/freight/insurance/policies')}
            />
            <div>
              <Title level={3} style={{ margin: 0 }}>
                <SafetyCertificateOutlined style={{ marginRight: 8, color: '#1677ff' }} />
                Policy: {policy.policyNumber}
              </Title>
              <Text type="secondary">ID: {policy.id}</Text>
            </div>
          </Space>
          <Space>
            <Button icon={<EditOutlined />} onClick={handleOpenEdit}>
              Edit Policy
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate(`/freight/insurance/claims/new?policyId=${policy.id}`)}
              disabled={policy.status !== 'ACTIVE'}
            >
              File Claim
            </Button>
          </Space>
        </div>

        <Descriptions bordered column={{ xxl: 3, xl: 3, lg: 2, md: 2, sm: 1, xs: 1 }}>
          <Descriptions.Item label="Policy Number">{policy.policyNumber}</Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color={getStatusColor(policy.status)}>{policy.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Provider">{policy.insuranceProvider}</Descriptions.Item>

          <Descriptions.Item label="Freight Order ID">
            <a onClick={() => navigate(`/freight/orders/${policy.freightOrderId}`)}>
              {policy.freightOrderId}
            </a>
          </Descriptions.Item>
          <Descriptions.Item label="Policy Type">{policy.policyType}</Descriptions.Item>
          <Descriptions.Item label="Currency">{policy.currencyCode}</Descriptions.Item>

          <Descriptions.Item label="Coverage Limit">
            <Text strong>{policy.coverageAmount.toLocaleString()} {policy.currencyCode}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Premium">
            {policy.premiumAmount.toLocaleString()} {policy.currencyCode}
          </Descriptions.Item>
          <Descriptions.Item label="Deductible">
            {policy.deductibleAmount != null ? policy.deductibleAmount.toLocaleString() : '-'} {policy.currency || policy.currencyCode || ''}
          </Descriptions.Item>

          <Descriptions.Item label="Valid From">
            {new Date(policy.validFrom).toLocaleString()}
          </Descriptions.Item>
          <Descriptions.Item label="Valid Until">
            {new Date(policy.validUntil).toLocaleString()}
          </Descriptions.Item>
          <Descriptions.Item label="Version">v{policy.version}</Descriptions.Item>

          <Descriptions.Item label="Terms & Conditions" span={3}>
            {policy.termsAndConditions || 'None specified'}
          </Descriptions.Item>
        </Descriptions>

        <Divider />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Title level={4} style={{ margin: 0 }}>Associated Claims</Title>
        </div>

        <Table
          dataSource={claims || []}
          columns={claimColumns}
          rowKey="id"
          loading={isClaimsLoading}
          pagination={false}
        />
      </Card>

      <Modal
        title="Edit Policy"
        open={isEditModalOpen}
        onCancel={() => setIsEditModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleUpdate}>
          <Form.Item
            name="coverageAmount"
            label="Coverage Amount"
            rules={[{ required: true, message: 'Please input coverage amount' }]}
          >
            <InputNumber min={0.01} step={100} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="premiumAmount"
            label="Premium Amount"
            rules={[{ required: true, message: 'Please input premium amount' }]}
          >
            <InputNumber min={0} step={10} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="deductibleAmount"
            label="Deductible Amount"
            rules={[{ required: true, message: 'Please input deductible amount' }]}
          >
            <InputNumber min={0} step={10} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="status"
            label="Status"
            rules={[{ required: true, message: 'Please select status' }]}
          >
            <Select
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'EXPIRED', label: 'EXPIRED' },
                { value: 'CANCELLED', label: 'CANCELLED' },
              ]}
            />
          </Form.Item>

          <Form.Item name="validity" label="Update Validity Range (optional)">
            <DatePicker.RangePicker showTime style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="termsAndConditions" label="Terms & Conditions">
            <TextArea rows={3} />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setIsEditModalOpen(false)}>Cancel</Button>
              <Button type="primary" htmlType="submit" loading={updatePolicyMutation.isPending}>
                Save Changes
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
