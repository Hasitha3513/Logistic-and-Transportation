import React from 'react';
import { Button, Card, Col, DatePicker, Form, Input, InputNumber, Row, Select, Typography, message } from 'antd';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { isAxiosError } from 'axios';
import type { Dayjs } from 'dayjs';
import { useAuth } from '../../../../auth/AuthContext';
import { Navigate, useNavigate } from 'react-router-dom';
import { useCreatePolicy } from '../hooks/useInsurance';
import { CreatePolicyPayload } from '../types/insurance';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface PolicyCreateFormValues {
  freightOrderId: string;
  insuranceProvider: string;
  policyType: string;
  coverageAmount: number;
  premiumAmount: number;
  deductibleAmount: number;
  currencyCode: string;
  validity: [Dayjs, Dayjs];
  termsAndConditions?: string;
}

export const PolicyCreatePage: React.FC = () => {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [form] = Form.useForm<PolicyCreateFormValues>();
  const createPolicyMutation = useCreatePolicy();

  if (!hasPermission('CARGO_INSURANCE_MANAGE')) {
    return <Navigate to="/freight/insurance/policies" replace />;
  }

  const handleSubmit = async (values: PolicyCreateFormValues) => {
    try {
      const payload: CreatePolicyPayload = {
        freightOrderId: values.freightOrderId,
        insuranceProvider: values.insuranceProvider,
        policyType: values.policyType,
        coverageAmount: values.coverageAmount,
        premiumAmount: values.premiumAmount || 0,
        deductibleAmount: values.deductibleAmount || 0,
        currencyCode: values.currencyCode || 'USD',
        validFrom: values.validity[0].toISOString(),
        validUntil: values.validity[1].toISOString(),
        termsAndConditions: values.termsAndConditions,
      };

      const result = await createPolicyMutation.mutateAsync(payload);
      message.success('Insurance policy created successfully');
      navigate(`/freight/insurance/policies/${result.id}`);
    } catch (err: unknown) {
      const msg = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message ?? 'Failed to create policy'
        : 'Failed to create policy';
      message.error(msg);
    }
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 24 }}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/freight/insurance/policies')}
            style={{ marginRight: 16 }}
          />
          <div>
            <Title level={3} style={{ margin: 0 }}>Create Freight Insurance Policy</Title>
            <Text type="secondary">Issue coverage for a freight order</Text>
          </div>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            currencyCode: 'USD',
            policyType: 'ALL_RISK',
            premiumAmount: 0,
            deductibleAmount: 0,
          }}
        >
          <Row gutter={24}>
            <Col xs={24} md={12}>
              <Form.Item
                name="freightOrderId"
                label="Freight Order ID"
                rules={[
                  { required: true, message: 'Please input freight order UUID' },
                  { pattern: /^[0-9a-fA-F-]{36}$/, message: 'Must be a valid UUID' },
                ]}
              >
                <Input placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="insuranceProvider"
                label="Insurance Provider"
                rules={[{ required: true, message: 'Please input insurance provider name' }]}
              >
                <Input placeholder="e.g. Zurich Freight Mutual" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col xs={24} md={8}>
              <Form.Item
                name="policyType"
                label="Policy Type"
                rules={[{ required: true, message: 'Please select policy type' }]}
              >
                <Select
                  options={[
                    { value: 'ALL_RISK', label: 'All Risk Cargo' },
                    { value: 'NAMED_PERILS', label: 'Named Perils' },
                    { value: 'TOTAL_LOSS_ONLY', label: 'Total Loss Only' },
                    { value: 'LIABILITY_CARRIER', label: 'Carrier Liability' },
                  ]}
                />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item
                name="currencyCode"
                label="Currency"
                rules={[{ required: true, len: 3, message: '3-letter currency code' }]}
              >
                <Input placeholder="USD" maxLength={3} />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item
                name="validity"
                label="Validity Range"
                rules={[{ required: true, message: 'Please select validity period' }]}
              >
                <DatePicker.RangePicker showTime style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col xs={24} md={8}>
              <Form.Item
                name="coverageAmount"
                label="Coverage Limit"
                rules={[{ required: true, message: 'Please input coverage amount' }]}
              >
                <InputNumber min={0.01} step={100} style={{ width: '100%' }} placeholder="50000.00" />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item
                name="premiumAmount"
                label="Premium Amount"
                rules={[{ required: true, message: 'Please input premium amount' }]}
              >
                <InputNumber min={0} step={10} style={{ width: '100%' }} placeholder="500.00" />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item
                name="deductibleAmount"
                label="Deductible Amount"
                rules={[{ required: true, message: 'Please input deductible amount' }]}
              >
                <InputNumber min={0} step={50} style={{ width: '100%' }} placeholder="250.00" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="termsAndConditions" label="Terms & Conditions">
            <TextArea rows={4} placeholder="Specific conditions, exclusions, clauses..." />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              icon={<SaveOutlined />}
              loading={createPolicyMutation.isPending}
            >
              Create Policy
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};
