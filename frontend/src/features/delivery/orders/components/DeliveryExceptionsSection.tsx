import React, { useState } from 'react';
import {
  Card,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Table,
  Tag,
  Space,
  Typography,
  Alert,
  message,
  Upload,
  InputNumber,
  Divider,
} from 'antd';
import {
  WarningOutlined,
  PlusOutlined,
  CheckCircleOutlined,
  StopOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import { isAxiosError } from 'axios';
import type { DeliveryOrder } from '../types/deliveryOrder';
import type {
  DeliveryExceptionCase,
  DeliveryExceptionType,
  DeliveryExceptionSeverity,
  DeliveryExceptionResolutionCode,
  DeliveryFailureDisposition,
} from '../types/deliveryException';
import { useDeliveryExceptions } from '../hooks/useDeliveryExceptions';
import { useAuth } from '../../../../auth/AuthContext';

const { Text, Paragraph } = Typography;

interface DeliveryExceptionsSectionProps {
  delivery: DeliveryOrder;
}

export const DeliveryExceptionsSection: React.FC<DeliveryExceptionsSectionProps> = ({ delivery }) => {
  const { hasPermission } = useAuth();
  const canCreate = hasPermission('DELIVERY_EXCEPTION_CREATE');
  const canView = hasPermission('DELIVERY_EXCEPTION_VIEW') || canCreate;
  const canManage = hasPermission('DELIVERY_EXCEPTION_MANAGE');
  const canResolve = hasPermission('DELIVERY_EXCEPTION_RESOLVE');

  const {
    exceptions,
    isLoading,
    reportException,
    isReporting,
    investigateException,
    isInvestigating,
    resolveException,
    isResolving,
    cancelException,
    isCancelling,
  } = useDeliveryExceptions(delivery.id);

  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [selectedCaseForResolve, setSelectedCaseForResolve] = useState<DeliveryExceptionCase | null>(null);
  const [selectedCaseForCancel, setSelectedCaseForCancel] = useState<DeliveryExceptionCase | null>(null);

  const [reportForm] = Form.useForm();
  const [resolveForm] = Form.useForm();
  const [cancelForm] = Form.useForm();

  const [selectedExceptionType, setSelectedExceptionType] = useState<DeliveryExceptionType>('DAMAGED_DELIVERY');
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  if (!canView) {
    return null;
  }

  const handleReportSubmit = async (values: any) => {
    try {
      const evidenceList: { originalFilename: string; base64Content: string }[] = [];
      for (const file of fileList) {
        if (file.originFileObj) {
          const base64 = await fileToBase64(file.originFileObj);
          evidenceList.push({
            originalFilename: file.name,
            base64Content: base64,
          });
        }
      }

      await reportException({
        exceptionType: values.exceptionType,
        severity: values.severity,
        description: values.description,
        correctedLocationId: values.correctedLocationId,
        otpAttemptReference: values.otpAttemptReference,
        deliveredItemsDescription: values.deliveredItemsDescription,
        undeliveredItemsDescription: values.undeliveredItemsDescription,
        quantityDelivered: values.quantityDelivered,
        quantityUndelivered: values.quantityUndelivered,
        evidenceList: evidenceList.length > 0 ? evidenceList : undefined,
      });

      void message.success('Delivery exception reported successfully');
      setIsReportModalOpen(false);
      reportForm.resetFields();
      setFileList([]);
    } catch (error) {
      if (isAxiosError(error)) {
        void message.error(error.response?.data?.message ?? 'Failed to report exception');
      } else {
        void message.error('Failed to report exception');
      }
    }
  };

  const handleResolveSubmit = async (values: any) => {
    if (!selectedCaseForResolve) return;
    try {
      await resolveException({
        exceptionId: selectedCaseForResolve.id,
        payload: {
          expectedVersion: selectedCaseForResolve.version,
          resolutionCode: values.resolutionCode,
          resolutionNotes: values.resolutionNotes,
          correctedLocationId: values.correctedLocationId,
          followUpDisposition: values.followUpDisposition,
        },
      });

      void message.success('Exception resolved successfully');
      setSelectedCaseForResolve(null);
      resolveForm.resetFields();
    } catch (error) {
      if (isAxiosError(error)) {
        void message.error(error.response?.data?.message ?? 'Failed to resolve exception');
      } else {
        void message.error('Failed to resolve exception');
      }
    }
  };

  const handleCancelSubmit = async (values: any) => {
    if (!selectedCaseForCancel) return;
    try {
      await cancelException({
        exceptionId: selectedCaseForCancel.id,
        payload: {
          expectedVersion: selectedCaseForCancel.version,
          reason: values.reason,
        },
      });

      void message.success('Exception cancelled successfully');
      setSelectedCaseForCancel(null);
      cancelForm.resetFields();
    } catch (error) {
      if (isAxiosError(error)) {
        void message.error(error.response?.data?.message ?? 'Failed to cancel exception');
      } else {
        void message.error('Failed to cancel exception');
      }
    }
  };

  const handleInvestigate = async (record: DeliveryExceptionCase) => {
    try {
      await investigateException({
        exceptionId: record.id,
        expectedVersion: record.version,
      });
      void message.success('Exception case moved to Under Investigation');
    } catch (error) {
      if (isAxiosError(error)) {
        void message.error(error.response?.data?.message ?? 'Failed to update exception');
      } else {
        void message.error('Failed to update exception');
      }
    }
  };

  const fileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        const result = reader.result as string;
        const base64 = result.split(',')[1];
        resolve(base64);
      };
      reader.onerror = (error) => reject(error);
    });
  };

  const columns = [
    {
      title: 'Type',
      dataIndex: 'exceptionType',
      key: 'exceptionType',
      render: (type: DeliveryExceptionType) => (
        <Tag color={type === 'DAMAGED_DELIVERY' ? 'red' : type === 'OTP_MISMATCH' ? 'orange' : 'blue'}>
          {type.replaceAll('_', ' ')}
        </Tag>
      ),
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      render: (sev: DeliveryExceptionSeverity) => (
        <Tag color={sev === 'CRITICAL' ? 'volcano' : sev === 'HIGH' ? 'red' : sev === 'MEDIUM' ? 'gold' : 'green'}>
          {sev}
        </Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'OPEN' ? 'red' : status === 'UNDER_INVESTIGATION' ? 'processing' : status === 'RESOLVED' ? 'success' : 'default'}>
          {status.replaceAll('_', ' ')}
        </Tag>
      ),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: 'Reported',
      key: 'reported',
      render: (_: any, r: DeliveryExceptionCase) => (
        <div>
          <Text style={{ fontSize: '12px' }}>{new Date(r.reportedAt).toLocaleString()}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: '11px' }}>By: {r.reportedBy}</Text>
        </div>
      ),
    },
    {
      title: 'Resolution',
      key: 'resolution',
      render: (_: any, r: DeliveryExceptionCase) => {
        if (r.resolution) {
          return (
            <div>
              <Tag color="green">{r.resolution.resolutionCode.replaceAll('_', ' ')}</Tag>
              <Paragraph ellipsis={{ rows: 2 }} style={{ margin: 0, fontSize: '12px' }}>
                {r.resolution.resolutionNotes}
              </Paragraph>
              <Text type="secondary" style={{ fontSize: '11px' }}>
                Resolved by: {r.resolution.resolvedBy} on {new Date(r.resolution.resolvedAt).toLocaleString()}
              </Text>
            </div>
          );
        }
        return <Text type="secondary">—</Text>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DeliveryExceptionCase) => {
        if (record.status === 'RESOLVED' || record.status === 'CANCELLED') {
          return <Tag>TERMINAL</Tag>;
        }
        return (
          <Space>
            {canManage && record.status === 'OPEN' && (
              <Button
                size="small"
                icon={<SearchOutlined />}
                loading={isInvestigating}
                onClick={() => void handleInvestigate(record)}
              >
                Investigate
              </Button>
            )}
            {canResolve && (
              <Button
                size="small"
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={() => {
                  setSelectedCaseForResolve(record);
                  resolveForm.resetFields();
                }}
              >
                Resolve
              </Button>
            )}
            {canResolve && (
              <Button
                size="small"
                danger
                icon={<StopOutlined />}
                onClick={() => {
                  setSelectedCaseForCancel(record);
                  cancelForm.resetFields();
                }}
              >
                Cancel
              </Button>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <Card
      title={
        <Space>
          <WarningOutlined style={{ color: '#fa8c16' }} />
          <span>Exceptions & Disputes</span>
        </Space>
      }
      extra={
        canCreate && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setIsReportModalOpen(true);
              reportForm.resetFields();
              setSelectedExceptionType('DAMAGED_DELIVERY');
              setFileList([]);
            }}
          >
            Report Exception
          </Button>
        )
      }
      style={{ marginTop: 16 }}
    >
      <Table
        dataSource={exceptions}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={false}
        locale={{ emptyText: 'No delivery exceptions recorded' }}
      />

      {/* Report Exception Modal */}
      <Modal
        title="Report Delivery Exception"
        open={isReportModalOpen}
        onCancel={() => setIsReportModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form
          form={reportForm}
          layout="vertical"
          initialValues={{
            exceptionType: 'DAMAGED_DELIVERY',
            severity: 'MEDIUM',
          }}
          onFinish={handleReportSubmit}
        >
          <Form.Item
            name="exceptionType"
            label="Exception Type"
            rules={[{ required: true, message: 'Please select exception type' }]}
          >
            <Select
              onChange={(value: DeliveryExceptionType) => setSelectedExceptionType(value)}
              options={[
                { value: 'DAMAGED_DELIVERY', label: 'Damaged Delivery (Requires Photo Evidence)' },
                { value: 'WRONG_ADDRESS', label: 'Wrong Address' },
                { value: 'PARTIAL_DELIVERY', label: 'Partial Delivery' },
                { value: 'OTP_MISMATCH', label: 'OTP Mismatch' },
                { value: 'RECIPIENT_REFUSAL', label: 'Recipient Refusal' },
              ]}
            />
          </Form.Item>

          <Form.Item
            name="severity"
            label="Severity"
            rules={[{ required: true, message: 'Please select severity' }]}
          >
            <Select
              options={[
                { value: 'LOW', label: 'Low' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'HIGH', label: 'High' },
                { value: 'CRITICAL', label: 'Critical' },
              ]}
            />
          </Form.Item>

          <Form.Item
            name="description"
            label="Description"
            rules={[
              { required: true, message: 'Please enter a description' },
              { max: 1000, message: 'Maximum 1000 characters' },
            ]}
          >
            <Input.TextArea rows={3} placeholder="Describe the exception condition..." />
          </Form.Item>

          {/* Conditional Fields based on Exception Type */}
          {selectedExceptionType === 'DAMAGED_DELIVERY' && (
            <Form.Item
              label="Photo Evidence (Max 3, JPEG/PNG, up to 10MB each)"
              required
            >
              <Upload
                listType="picture"
                maxCount={3}
                beforeUpload={(file) => {
                  const isImage = file.type === 'image/jpeg' || file.type === 'image/png';
                  if (!isImage) {
                    void message.error('You can only upload JPG/PNG file!');
                    return Upload.LIST_IGNORE;
                  }
                  const isLt10M = file.size / 1024 / 1024 < 10;
                  if (!isLt10M) {
                    void message.error('Image must be smaller than 10MB!');
                    return Upload.LIST_IGNORE;
                  }
                  return false;
                }}
                fileList={fileList}
                onChange={({ fileList }) => setFileList(fileList)}
              >
                {fileList.length < 3 && (
                  <Button icon={<UploadOutlined />}>Select Photo</Button>
                )}
              </Upload>
            </Form.Item>
          )}

          {selectedExceptionType === 'PARTIAL_DELIVERY' && (
            <>
              <Form.Item
                name="deliveredItemsDescription"
                label="Delivered Items Description"
              >
                <Input placeholder="E.g., 2 boxes of parts received" />
              </Form.Item>
              <Form.Item
                name="undeliveredItemsDescription"
                label="Undelivered / Missing Items Description"
              >
                <Input placeholder="E.g., 1 pallet of coolant missing" />
              </Form.Item>
              <Space style={{ width: '100%' }}>
                <Form.Item name="quantityDelivered" label="Qty Delivered">
                  <InputNumber min={0} />
                </Form.Item>
                <Form.Item name="quantityUndelivered" label="Qty Undelivered">
                  <InputNumber min={0} />
                </Form.Item>
              </Space>
            </>
          )}

          {selectedExceptionType === 'OTP_MISMATCH' && (
            <Form.Item
              name="otpAttemptReference"
              label="OTP Verification Reference (Masked / Audit Only)"
            >
              <Input placeholder="E.g. OTP-ATTEMPT-01 (Do not enter actual OTP secret)" />
            </Form.Item>
          )}

          <Divider />
          <Form.Item>
            <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button onClick={() => setIsReportModalOpen(false)}>Cancel</Button>
              <Button type="primary" htmlType="submit" loading={isReporting}>
                Submit Exception
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Resolve Modal */}
      <Modal
        title={`Resolve Exception: ${selectedCaseForResolve?.exceptionType.replaceAll('_', ' ')}`}
        open={Boolean(selectedCaseForResolve)}
        onCancel={() => setSelectedCaseForResolve(null)}
        footer={null}
        destroyOnClose
      >
        <Form form={resolveForm} layout="vertical" onFinish={handleResolveSubmit}>
          <Form.Item
            name="resolutionCode"
            label="Resolution Code"
            rules={[{ required: true, message: 'Please select resolution code' }]}
          >
            <Select
              options={
                selectedCaseForResolve?.exceptionType === 'DAMAGED_DELIVERY'
                  ? [
                      { value: 'RETURN_TO_BASE_APPROVED', label: 'Return to Base Approved' },
                      { value: 'ACCEPTED_AS_IS', label: 'Accepted As-Is by Recipient' },
                      { value: 'REDELIVERY_APPROVED', label: 'Redelivery Approved' },
                    ]
                  : selectedCaseForResolve?.exceptionType === 'WRONG_ADDRESS'
                  ? [
                      { value: 'ADDRESS_CORRECTED', label: 'Address Corrected' },
                      { value: 'RETURN_TO_BASE_APPROVED', label: 'Return to Base Approved' },
                    ]
                  : selectedCaseForResolve?.exceptionType === 'OTP_MISMATCH'
                  ? [
                      { value: 'OTP_OVERRIDDEN_BY_MANAGER', label: 'OTP Overridden by Manager' },
                      { value: 'NEW_OTP_REQUESTED', label: 'New OTP Requested / Redeliver' },
                      { value: 'RETURN_TO_BASE_APPROVED', label: 'Return to Base Approved' },
                    ]
                  : selectedCaseForResolve?.exceptionType === 'PARTIAL_DELIVERY'
                  ? [
                      { value: 'PARTIAL_ACCEPTED_CLOSE', label: 'Partial Accepted / Close Order' },
                      { value: 'REDELIVERY_APPROVED', label: 'Redelivery Remainder Approved' },
                      { value: 'RETURN_TO_BASE_APPROVED', label: 'Return All to Base' },
                    ]
                  : [
                      { value: 'REFUSAL_CONFIRMED_RTO', label: 'Refusal Confirmed -> Return to Base' },
                      { value: 'REDELIVERY_APPROVED', label: 'Dispute Resolved -> Redeliver' },
                    ]
              }
            />
          </Form.Item>

          {selectedCaseForResolve?.exceptionType === 'WRONG_ADDRESS' && (
            <Form.Item
              name="correctedLocationId"
              label="Corrected Destination Location ID (UUID)"
              rules={[{ required: true, message: 'Corrected location is required' }]}
            >
              <Input placeholder="Enter verified Location UUID" />
            </Form.Item>
          )}

          <Form.Item
            name="followUpDisposition"
            label="Follow-Up Disposition"
          >
            <Select
              allowClear
              options={[
                { value: 'REDELIVERY_ELIGIBLE', label: 'Redelivery Eligible' },
                { value: 'RETURN_TO_BASE_REQUIRED', label: 'Return to Base Required' },
                { value: 'ESCALATED', label: 'Escalated' },
              ]}
            />
          </Form.Item>

          <Form.Item
            name="resolutionNotes"
            label="Resolution Justification & Notes"
            rules={[
              { required: true, message: 'Resolution notes are required' },
              { max: 1000, message: 'Maximum 1000 characters' },
            ]}
          >
            <Input.TextArea rows={3} placeholder="Explain resolution justification..." />
          </Form.Item>

          <Divider />
          <Form.Item>
            <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button onClick={() => setSelectedCaseForResolve(null)}>Cancel</Button>
              <Button type="primary" htmlType="submit" loading={isResolving}>
                Apply Resolution
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Cancel Modal */}
      <Modal
        title="Cancel Delivery Exception"
        open={Boolean(selectedCaseForCancel)}
        onCancel={() => setSelectedCaseForCancel(null)}
        footer={null}
        destroyOnClose
      >
        <Form form={cancelForm} layout="vertical" onFinish={handleCancelSubmit}>
          <Form.Item
            name="reason"
            label="Cancellation Reason"
            rules={[{ max: 1000, message: 'Maximum 1000 characters' }]}
          >
            <Input.TextArea rows={3} placeholder="Explain why this exception case is being voided..." />
          </Form.Item>

          <Divider />
          <Form.Item>
            <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button onClick={() => setSelectedCaseForCancel(null)}>Back</Button>
              <Button type="primary" danger htmlType="submit" loading={isCancelling}>
                Confirm Cancellation
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};
