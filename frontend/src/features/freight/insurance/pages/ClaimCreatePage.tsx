import React from 'react';
import { Button, Card, Col, DatePicker, Form, Input, InputNumber, Row, Typography, message } from 'antd';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCreateClaim } from '../hooks/useInsurance';
import { CreateClaimPayload } from '../types/insurance';

const { Title, Text } = Typography;
const { TextArea } = Input;

export const ClaimCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [form] = Form.useForm();
  const createClaimMutation = useCreateClaim();

  const defaultPolicyId = searchParams.get('policyId') || '';

  const handleSubmit = async (values: any) => {
    try {
      const payload: CreateClaimPayload = {
        policyId: values.policyId,
        incidentDate: values.incidentDate.toISOString(),
        description: values.description,
        claimedAmount: values.claimedAmount,
        currencyCode: values.currencyCode || 'USD',
      };

      const result = await createClaimMutation.mutateAsync(payload);
      message.success('Insurance claim filed successfully');
      navigate(`/freight/insurance/claims/${result.id}`);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to file claim';
      message.error(msg);
    }
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 24 }}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/freight/insurance/claims')}
            style={{ marginRight: 16 }}
          />
          <div>
            <Title level={3} style={{ margin: 0 }}>File Insurance Claim</Title>
            <Text type="secondary">Submit a new cargo loss/damage claim against an active policy</Text>
          </div>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            policyId: defaultPolicyId,
            currencyCode: 'USD',
          }}
        >
          <Row gutter={24}>
            <Col xs={24} md={12}>
              <Form.Item
                name="policyId"
                label="Insurance Policy ID"
                rules={[
                  { required: true, message: 'Please input insurance policy UUID' },
                  { pattern: /^[0-9a-fA-F-]{36}$/, message: 'Must be a valid UUID' },
                ]}
              >
                <Input placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="incidentDate"
                label="Incident Date & Time"
                rules={[{ required: true, message: 'Please select incident timestamp' }]}
              >
                <DatePicker showTime style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col xs={24} md={12}>
              <Form.Item
                name="claimedAmount"
                label="Claimed Amount"
                rules={[{ required: true, message: 'Please input claimed amount' }]}
              >
                <InputNumber min={0.01} step={100} style={{ width: '100%' }} placeholder="12500.00" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="currencyCode"
                label="Currency Code"
                rules={[{ required: true, len: 3, message: '3-letter currency code' }]}
              >
                <Input placeholder="USD" maxLength={3} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="description"
            label="Incident Description & Loss Details"
            rules={[{ required: true, message: 'Please describe the cargo loss or damage' }]}
          >
            <TextArea rows={4} placeholder="Describe cargo condition, incident location, damage details..." />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              icon={<SaveOutlined />}
              loading={createClaimMutation.isPending}
            >
              Submit Claim
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};
