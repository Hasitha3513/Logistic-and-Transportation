import React from 'react';
import { Button, Card, Col, Form, Input, Row, Select, Typography, message } from 'antd';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { isAxiosError } from 'axios';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../../../auth/AuthContext';
import { useRecordException } from '../hooks/useCargoExceptions';
import type { CreateCargoExceptionPayload, ExceptionSeverity, ExceptionType } from '../types';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface ExceptionFormValues {
  exceptionType: ExceptionType;
  severity: ExceptionSeverity;
  freightOrderId: string;
  manifestId?: string;
  manifestItemId?: string;
  description: string;
  impact?: string;
  restriction?: string;
  correctiveAction?: string;
}

export const CargoExceptionCreatePage: React.FC = () => {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [form] = Form.useForm<ExceptionFormValues>();
  const recordMutation = useRecordException();

  if (!hasPermission('CARGO_EXCEPTION_MANAGE')) {
    return (
      <div style={{ padding: '24px' }}>
        <Card>
          <Text type="danger">You do not have permission to record cargo exceptions.</Text>
        </Card>
      </div>
    );
  }

  const defaultOrderId = searchParams.get('freightOrderId') || '';
  const defaultManifestId = searchParams.get('manifestId') || '';

  const handleSubmit = async (values: ExceptionFormValues) => {
    try {
      const payload: CreateCargoExceptionPayload = {
        exceptionType: values.exceptionType,
        severity: values.severity,
        freightOrderId: values.freightOrderId.trim(),
        manifestId: values.manifestId?.trim() || undefined,
        manifestItemId: values.manifestItemId?.trim() || undefined,
        description: values.description.trim(),
        impact: values.impact?.trim() || undefined,
        restriction: values.restriction?.trim() || undefined,
        correctiveAction: values.correctiveAction?.trim() || undefined,
      };

      const result = await recordMutation.mutateAsync(payload);
      message.success('Cargo exception recorded successfully');
      navigate(`/freight/exceptions/${result.id}`);
    } catch (err: unknown) {
      const msg = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message ?? 'Failed to record exception'
        : 'Failed to record exception';
      message.error(msg);
    }
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 24 }}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/freight/exceptions')}
            style={{ marginRight: 16 }}
          />
          <div>
            <Title level={3} style={{ margin: 0 }}>Record Cargo Exception</Title>
            <Text type="secondary">
              Report cargo damage, partial shipment, weight discrepancy, hazardous materials, unmanifested items or seal tampering
            </Text>
          </div>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            freightOrderId: defaultOrderId,
            manifestId: defaultManifestId,
            severity: 'MEDIUM',
            exceptionType: 'DAMAGE',
          }}
        >
          <Row gutter={24}>
            <Col xs={24} md={12}>
              <Form.Item
                name="exceptionType"
                label="Exception Type"
                rules={[{ required: true, message: 'Please select exception type' }]}
              >
                <Select placeholder="Select Exception Type">
                  <Select.Option value="DAMAGE">Damage</Select.Option>
                  <Select.Option value="PARTIAL_SHIPMENT">Partial Shipment</Select.Option>
                  <Select.Option value="WEIGHT_DISCREPANCY">Weight Discrepancy</Select.Option>
                  <Select.Option value="HAZARDOUS_MATERIAL">Hazardous Material</Select.Option>
                  <Select.Option value="UNMANIFESTED_CARGO">Unmanifested Cargo</Select.Option>
                  <Select.Option value="SEAL_TAMPERING">Seal Tampering</Select.Option>
                </Select>
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="severity"
                label="Severity Level"
                rules={[{ required: true, message: 'Please select severity' }]}
              >
                <Select placeholder="Select Severity">
                  <Select.Option value="LOW">LOW</Select.Option>
                  <Select.Option value="MEDIUM">MEDIUM</Select.Option>
                  <Select.Option value="HIGH">HIGH</Select.Option>
                  <Select.Option value="CRITICAL">CRITICAL</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col xs={24} md={12}>
              <Form.Item
                name="freightOrderId"
                label="Freight Order UUID"
                rules={[
                  { required: true, message: 'Please input freight order UUID' },
                  { pattern: /^[0-9a-fA-F-]{36}$/, message: 'Must be a valid UUID' },
                ]}
              >
                <Input placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                name="manifestId"
                label="Cargo Manifest UUID (Optional)"
                rules={[
                  {
                    validator: async (_, value) => {
                      if (value && !/^[0-9a-fA-F-]{36}$/.test(value)) {
                        throw new Error('Must be a valid UUID if specified');
                      }
                    },
                  },
                ]}
              >
                <Input placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="description"
            label="Exception Description & Incident Summary"
            rules={[{ required: true, message: 'Please provide incident description' }]}
          >
            <TextArea
              rows={4}
              placeholder="Describe what occurred, observations at receipt/handling, damage details..."
            />
          </Form.Item>

          <Row gutter={24}>
            <Col xs={24} md={12}>
              <Form.Item name="impact" label="Operational / Safety Impact (Optional)">
                <TextArea rows={2} placeholder="Impact on vehicle capacity, schedule, safety..." />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item name="restriction" label="Movement / Dispatch Restriction (Optional)">
                <TextArea rows={2} placeholder="e.g. Quarantine at loading bay, dispatch blocked..." />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="correctiveAction" label="Initial Corrective Action (Optional)">
            <TextArea rows={2} placeholder="Initial steps taken to isolate, photograph or secure..." />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              icon={<SaveOutlined />}
              loading={recordMutation.isPending}
            >
              Record Exception
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};
