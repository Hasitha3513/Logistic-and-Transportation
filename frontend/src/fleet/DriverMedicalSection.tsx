import React, { useState } from 'react';
import {
  Card,
  Table,
  Button,
  Tag,
  Space,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  Alert,
  Typography,
} from 'antd';
import { PlusOutlined, MedicineBoxOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  useDriverMedicalRecords,
  useCreateDriverMedicalRecord,
} from './useDriverMedicalRecords';
import type {
  DriverMedicalRecord,
  DriverMedicalStatus,
  VisionTestStatus,
} from './types';
import { useAuth } from '../auth/AuthContext';

const { Text } = Typography;

interface DriverMedicalSectionProps {
  driverId: string;
}

export const DriverMedicalSection: React.FC<DriverMedicalSectionProps> = ({ driverId }) => {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('DRIVER_MEDICAL_MANAGE');

  const { data: records = [], isLoading, error } = useDriverMedicalRecords(driverId);
  const createMutation = useCreateDriverMedicalRecord(driverId);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();

  const handleCreate = async (values: any) => {
    await createMutation.mutateAsync({
      assessmentDate: values.assessmentDate.format('YYYY-MM-DD'),
      validFrom: values.validFrom.format('YYYY-MM-DD'),
      validUntil: values.validUntil.format('YYYY-MM-DD'),
      fitnessStatus: values.fitnessStatus,
      visionTestStatus: values.visionTestStatus,
      restrictions: values.restrictions,
      examinerOrProvider: values.examinerOrProvider,
      certificateReference: values.certificateReference,
      remarks: values.remarks,
    });
    setIsModalOpen(false);
    form.resetFields();
  };

  const getFitnessTag = (status: DriverMedicalStatus, validUntil: string) => {
    const isExpired = dayjs().isAfter(dayjs(validUntil));
    if (status === 'UNFIT') {
      return <Tag color="error">UNFIT</Tag>;
    }
    if (status === 'TEMPORARILY_UNFIT') {
      return <Tag color="warning">TEMPORARILY UNFIT</Tag>;
    }
    if (isExpired) {
      return <Tag color="error">EXPIRED ({status})</Tag>;
    }
    if (status === 'FIT_WITH_RESTRICTIONS') {
      return <Tag color="cyan">FIT (WITH RESTRICTIONS)</Tag>;
    }
    return <Tag color="success">FIT</Tag>;
  };

  const columns = [
    {
      title: 'Fitness Status',
      dataIndex: 'fitnessStatus',
      key: 'fitnessStatus',
      render: (status: DriverMedicalStatus, record: DriverMedicalRecord) =>
        getFitnessTag(status, record.validUntil),
    },
    {
      title: 'Assessment Date',
      dataIndex: 'assessmentDate',
      key: 'assessmentDate',
    },
    {
      title: 'Validity Period',
      key: 'validity',
      render: (_: any, record: DriverMedicalRecord) => (
        <span>
          {record.validFrom} to {record.validUntil}
        </span>
      ),
    },
    {
      title: 'Vision Test',
      dataIndex: 'visionTestStatus',
      key: 'visionTestStatus',
      render: (status?: VisionTestStatus) =>
        status ? <Tag>{status.replace(/_/g, ' ')}</Tag> : '-',
    },
    {
      title: 'Restrictions',
      dataIndex: 'restrictions',
      key: 'restrictions',
      render: (text?: string) => text || <Text type="secondary">None</Text>,
    },
    {
      title: 'Certificate Ref',
      dataIndex: 'certificateReference',
      key: 'certificateReference',
      render: (text?: string) => text || '-',
    },
    {
      title: 'Examiner / Clinic',
      dataIndex: 'examinerOrProvider',
      key: 'examinerOrProvider',
      render: (text?: string) => text || '-',
    },
  ];

  return (
    <Card
      title={
        <Space>
          <MedicineBoxOutlined />
          <span>Medical Fitness & Certificates (US-43)</span>
        </Space>
      }
      extra={
        canManage && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setIsModalOpen(true)}
          >
            Record Medical Assessment
          </Button>
        )
      }
      style={{ marginBottom: 16 }}
    >
      {error && (
        <Alert
          type="error"
          message="Failed to load medical records"
          description={error.message}
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        dataSource={records}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 5 }}
      />

      <Modal
        title="Record Driver Medical Assessment"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={createMutation.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            name="assessmentDate"
            label="Assessment Date"
            rules={[{ required: true, message: 'Assessment date is required' }]}
            initialValue={dayjs()}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="validFrom"
            label="Valid From"
            rules={[{ required: true, message: 'Valid from date is required' }]}
            initialValue={dayjs()}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="validUntil"
            label="Valid Until"
            rules={[{ required: true, message: 'Valid until date is required' }]}
            initialValue={dayjs().add(1, 'year')}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="fitnessStatus"
            label="Fitness Status"
            rules={[{ required: true, message: 'Fitness status is required' }]}
            initialValue="FIT"
          >
            <Select>
              <Select.Option value="FIT">FIT</Select.Option>
              <Select.Option value="FIT_WITH_RESTRICTIONS">FIT WITH RESTRICTIONS</Select.Option>
              <Select.Option value="TEMPORARILY_UNFIT">TEMPORARILY UNFIT</Select.Option>
              <Select.Option value="UNFIT">UNFIT</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="visionTestStatus" label="Vision Test Result" initialValue="PASSED">
            <Select allowClear>
              <Select.Option value="PASSED">PASSED</Select.Option>
              <Select.Option value="PASSED_WITH_CORRECTIVE_LENSES">PASSED WITH CORRECTIVE LENSES</Select.Option>
              <Select.Option value="FAILED">FAILED</Select.Option>
              <Select.Option value="NOT_TESTED">NOT TESTED</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="restrictions" label="Medical Restrictions">
            <Input.TextArea placeholder="e.g. Must wear corrective glasses / Daytime driving only" rows={2} />
          </Form.Item>

          <Form.Item name="certificateReference" label="Certificate Reference">
            <Input placeholder="e.g. MED-2026-9876" />
          </Form.Item>

          <Form.Item name="examinerOrProvider" label="Examiner / Healthcare Provider">
            <Input placeholder="e.g. Dr. Jane Smith, Occupational Health Clinic" />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};
