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
  Popconfirm,
  Checkbox,
} from 'antd';
import { PlusOutlined, ExperimentOutlined, CheckCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  useDriverDrugTests,
  useScheduleDriverDrugTest,
  useRecordDrugTestResult,
  useClearReturnToDuty,
} from './useDriverDrugTests';
import type {
  DriverDrugTest,
  DrugTestResult,
  DrugTestStatus,
  DrugTestType,
} from './types';
import { useAuth } from '../auth/AuthContext';

const { Text } = Typography;

interface DriverDrugTestSectionProps {
  driverId: string;
}

export const DriverDrugTestSection: React.FC<DriverDrugTestSectionProps> = ({ driverId }) => {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('DRIVER_DRUG_TEST_MANAGE');

  const { data: tests = [], isLoading, error } = useDriverDrugTests(driverId);
  const scheduleMutation = useScheduleDriverDrugTest(driverId);
  const recordResultMutation = useRecordDrugTestResult(driverId);
  const clearRtdMutation = useClearReturnToDuty(driverId);

  const [isScheduleOpen, setIsScheduleOpen] = useState(false);
  const [scheduleForm] = Form.useForm();

  const [resultModalTest, setResultModalTest] = useState<DriverDrugTest | null>(null);
  const [resultForm] = Form.useForm();

  const [rtdModalTest, setRtdModalTest] = useState<DriverDrugTest | null>(null);
  const [rtdForm] = Form.useForm();

  const handleSchedule = async (values: any) => {
    await scheduleMutation.mutateAsync({
      testType: values.testType,
      scheduledDate: values.scheduledDate.format('YYYY-MM-DD'),
      laboratoryOrProvider: values.laboratoryOrProvider,
      referenceNumber: values.referenceNumber,
      remarks: values.remarks,
    });
    setIsScheduleOpen(false);
    scheduleForm.resetFields();
  };

  const handleRecordResult = async (values: any) => {
    if (!resultModalTest) return;
    await recordResultMutation.mutateAsync({
      testId: resultModalTest.id,
      payload: {
        result: values.result,
        resultDate: values.resultDate ? values.resultDate.format('YYYY-MM-DD') : undefined,
        remarks: values.remarks,
        returnToDutyRequired: values.returnToDutyRequired,
      },
    });
    setResultModalTest(null);
    resultForm.resetFields();
  };

  const handleClearRtd = async (values: any) => {
    if (!rtdModalTest) return;
    await clearRtdMutation.mutateAsync({
      testId: rtdModalTest.id,
      remarks: values.remarks,
    });
    setRtdModalTest(null);
    rtdForm.resetFields();
  };

  const getResultTag = (result: DrugTestResult) => {
    switch (result) {
      case 'POSITIVE':
        return <Tag color="error">POSITIVE</Tag>;
      case 'NEGATIVE':
        return <Tag color="success">NEGATIVE</Tag>;
      case 'INCONCLUSIVE':
        return <Tag color="warning">INCONCLUSIVE</Tag>;
      default:
        return <Tag color="default">PENDING</Tag>;
    }
  };

  const getRtdTag = (test: DriverDrugTest) => {
    if (!test.returnToDutyRequired) {
      return <Text type="secondary">Not Required</Text>;
    }
    if (test.returnToDutyClearedAt) {
      return <Tag color="success">CLEARED</Tag>;
    }
    return <Tag color="error">REQUIRED (BLOCKED)</Tag>;
  };

  const columns = [
    {
      title: 'Test Type',
      dataIndex: 'testType',
      key: 'testType',
      render: (type: DrugTestType) => <Tag color="blue">{type.replace(/_/g, ' ')}</Tag>,
    },
    {
      title: 'Scheduled Date',
      dataIndex: 'scheduledDate',
      key: 'scheduledDate',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: DrugTestStatus) => <Tag>{status.replace(/_/g, ' ')}</Tag>,
    },
    {
      title: 'Result',
      dataIndex: 'result',
      key: 'result',
      render: (result: DrugTestResult) => getResultTag(result),
    },
    {
      title: 'Return to Duty',
      key: 'rtd',
      render: (_: any, record: DriverDrugTest) => getRtdTag(record),
    },
    {
      title: 'Lab / Provider',
      dataIndex: 'laboratoryOrProvider',
      key: 'laboratoryOrProvider',
      render: (text?: string) => text || '-',
    },
    {
      title: 'Reference #',
      dataIndex: 'referenceNumber',
      key: 'referenceNumber',
      render: (text?: string) => text || '-',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DriverDrugTest) => (
        <Space size="small">
          {canManage && record.status !== 'COMPLETED' && record.status !== 'CANCELLED' && (
            <Button
              size="small"
              type="primary"
              onClick={() => {
                setResultModalTest(record);
                resultForm.setFieldsValue({
                  result: 'NEGATIVE',
                  resultDate: dayjs(),
                  returnToDutyRequired: false,
                });
              }}
            >
              Record Result
            </Button>
          )}

          {canManage && record.returnToDutyRequired && !record.returnToDutyClearedAt && (
            <Button
              size="small"
              type="dashed"
              icon={<CheckCircleOutlined />}
              onClick={() => {
                setRtdModalTest(record);
                rtdForm.resetFields();
              }}
            >
              Clear RTD
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card
      title={
        <Space>
          <ExperimentOutlined />
          <span>Substance Screening & Drug Tests (US-44)</span>
        </Space>
      }
      extra={
        canManage && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setIsScheduleOpen(true)}
          >
            Schedule Drug Test
          </Button>
        )
      }
      style={{ marginBottom: 16 }}
    >
      {error && (
        <Alert
          type="error"
          message="Failed to load drug tests"
          description={error.message}
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        dataSource={tests}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 5 }}
      />

      {/* Schedule Test Modal */}
      <Modal
        title="Schedule Substance Screening / Drug Test"
        open={isScheduleOpen}
        onCancel={() => setIsScheduleOpen(false)}
        onOk={() => scheduleForm.submit()}
        confirmLoading={scheduleMutation.isPending}
        destroyOnClose
      >
        <Form form={scheduleForm} layout="vertical" onFinish={handleSchedule}>
          <Form.Item
            name="testType"
            label="Test Type"
            rules={[{ required: true, message: 'Test type is required' }]}
            initialValue="RANDOM"
          >
            <Select>
              <Select.Option value="RANDOM">RANDOM</Select.Option>
              <Select.Option value="SCHEDULED">SCHEDULED</Select.Option>
              <Select.Option value="PRE_EMPLOYMENT">PRE EMPLOYMENT</Select.Option>
              <Select.Option value="POST_INCIDENT">POST INCIDENT</Select.Option>
              <Select.Option value="REASONABLE_SUSPICION">REASONABLE SUSPICION</Select.Option>
              <Select.Option value="RETURN_TO_DUTY">RETURN TO DUTY</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="scheduledDate"
            label="Scheduled Date"
            rules={[{ required: true, message: 'Scheduled date is required' }]}
            initialValue={dayjs()}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="laboratoryOrProvider" label="Laboratory / Provider">
            <Input placeholder="e.g. LabCorp / Quest Diagnostics" />
          </Form.Item>

          <Form.Item name="referenceNumber" label="Chain of Custody / Reference #">
            <Input placeholder="e.g. CCF-2026-4455" />
          </Form.Item>

          <Form.Item name="remarks" label="Remarks">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Record Result Modal */}
      <Modal
        title="Record Drug Test Result"
        open={Boolean(resultModalTest)}
        onCancel={() => setResultModalTest(null)}
        onOk={() => resultForm.submit()}
        confirmLoading={recordResultMutation.isPending}
        destroyOnClose
      >
        <Form form={resultForm} layout="vertical" onFinish={handleRecordResult}>
          <Form.Item
            name="result"
            label="Test Result"
            rules={[{ required: true, message: 'Result is required' }]}
          >
            <Select
              onChange={(value) => {
                if (value === 'POSITIVE') {
                  resultForm.setFieldsValue({ returnToDutyRequired: true });
                }
              }}
            >
              <Select.Option value="NEGATIVE">NEGATIVE</Select.Option>
              <Select.Option value="POSITIVE">POSITIVE (Blocks driver)</Select.Option>
              <Select.Option value="INCONCLUSIVE">INCONCLUSIVE</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="resultDate" label="Result Date" initialValue={dayjs()}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="returnToDutyRequired" valuePropName="checked">
            <Checkbox>Require Return-to-Duty Clearance (Blocks driver from assignments)</Checkbox>
          </Form.Item>

          <Form.Item name="remarks" label="Laboratory Remarks">
            <Input.TextArea rows={2} placeholder="e.g. Verified 10-panel test result" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Clear RTD Modal */}
      <Modal
        title="Clear Return to Duty"
        open={Boolean(rtdModalTest)}
        onCancel={() => setRtdModalTest(null)}
        onOk={() => rtdForm.submit()}
        confirmLoading={clearRtdMutation.isPending}
        destroyOnClose
      >
        <Form form={rtdForm} layout="vertical" onFinish={handleClearRtd}>
          <Alert
            type="info"
            message="Return to Duty Clearance"
            description="Recording clearance will remove the drug test assignment block for this driver."
            style={{ marginBottom: 16 }}
          />
          <Form.Item
            name="remarks"
            label="Clearance Documentation / SAP Evaluation Details"
            rules={[{ required: true, message: 'Clearance details are required' }]}
          >
            <Input.TextArea rows={3} placeholder="e.g. Completed SAP rehabilitation program and negative follow-up drug screen" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};
