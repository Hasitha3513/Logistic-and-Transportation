import React, { useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DollarOutlined,
  ExclamationCircleOutlined,
  FileProtectOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons';
import { useAuth } from '../../../../auth/AuthContext';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import {
  useApproveClaim,
  useAssessClaim,
  useClaim,
  useDisputeClaim,
  useRecordSettlement,
  useRejectClaim
} from '../hooks/useInsurance';
import { ClaimResponse, ClaimSettlementResponse, ClaimStatus } from '../types/insurance';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

export const ClaimDetailsPage: React.FC = () => {
  const { hasPermission } = useAuth();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // Modal states
  const [activeModal, setActiveModal] = useState<
    'ASSESS' | 'APPROVE' | 'REJECT' | 'DISPUTE' | 'SETTLE' | null
  >(null);

  const [form] = Form.useForm();

  const { data: claim, isLoading, isError } = useClaim(id || '');

  const assessMutation = useAssessClaim(id || '');
  const approveMutation = useApproveClaim(id || '');
  const rejectMutation = useRejectClaim(id || '');
  const disputeMutation = useDisputeClaim(id || '');
  const settleMutation = useRecordSettlement(id || '');

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

  const handleOpenModal = (modalType: 'ASSESS' | 'APPROVE' | 'REJECT' | 'DISPUTE' | 'SETTLE') => {
    form.resetFields();
    if (modalType === 'ASSESS' && claim) {
      form.setFieldsValue({
        assessedAmount: claim.assessedAmount || claim.claimedAmount,
        assessmentNotes: claim.assessmentNotes || '',
      });
    } else if (modalType === 'SETTLE' && claim) {
      const remaining = (claim.assessedAmount || 0) - claim.totalSettledAmount;
      form.setFieldsValue({
        amount: remaining > 0 ? remaining : 0,
      });
    }
    setActiveModal(modalType);
  };

  const handleModalSubmit = async (values: any) => {
    if (!claim) return;
    try {
      if (activeModal === 'ASSESS') {
        await assessMutation.mutateAsync({
          assessedAmount: values.assessedAmount,
          assessmentNotes: values.assessmentNotes,
          version: claim.version,
        });
        message.success('Claim assessment recorded');
      } else if (activeModal === 'APPROVE') {
        await approveMutation.mutateAsync({
          notes: values.notes,
          version: claim.version,
        });
        message.success('Claim approved for settlement');
      } else if (activeModal === 'REJECT') {
        await rejectMutation.mutateAsync({
          reason: values.reason,
          version: claim.version,
        });
        message.success('Claim rejected');
      } else if (activeModal === 'DISPUTE') {
        await disputeMutation.mutateAsync({
          reason: values.reason,
          version: claim.version,
        });
        message.success('Claim marked as disputed');
      } else if (activeModal === 'SETTLE') {
        await settleMutation.mutateAsync({
          amount: values.amount,
          notes: values.notes,
          version: claim.version,
        });
        message.success('Settlement recorded successfully');
      }
      setActiveModal(null);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Operation failed';
      message.error(msg);
    }
  };

  const settlementColumns = [
    {
      title: 'Reference',
      dataIndex: 'settlementReference',
      key: 'settlementReference',
    },
    {
      title: 'Settled Amount',
      dataIndex: 'settledAmount',
      key: 'settledAmount',
      render: (val: number, record: ClaimSettlementResponse) => (
        <Text strong type="success">
          {val.toLocaleString()} {record.currencyCode}
        </Text>
      ),
    },
    {
      title: 'Settled By',
      dataIndex: 'settledBy',
      key: 'settledBy',
    },
    {
      title: 'Settled At',
      dataIndex: 'settledAt',
      key: 'settledAt',
      render: (val: string) => new Date(val).toLocaleString(),
    },
    {
      title: 'Notes',
      dataIndex: 'settlementNotes',
      key: 'settlementNotes',
      render: (val?: string) => val || '—',
    },
  ];

  if (isLoading) {
    return <Card loading style={{ margin: 24 }} />;
  }

  if (isError || !claim) {
    return (
      <Card style={{ margin: 24 }}>
        <Text type="danger">Failed to load claim details</Text>
      </Card>
    );
  }

  const remainingToSettle = (claim.assessedAmount || 0) - claim.totalSettledAmount;
  const isSettled = claim.status === 'SETTLED';
  const canAssess = ['OPEN', 'UNDER_REVIEW', 'DISPUTED'].includes(claim.status);
  const canApprove = (claim.status === 'UNDER_REVIEW' || claim.status === 'OPEN') && (claim.assessedAmount != null && claim.assessedAmount > 0);
  const canReject = ['OPEN', 'UNDER_REVIEW'].includes(claim.status);
  const canDispute = ['REJECTED', 'UNDER_REVIEW'].includes(claim.status);
  const canSettle = claim.status === 'APPROVED' && remainingToSettle > 0;

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <Space>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/freight/insurance/claims')}
            />
            <div>
              <Title level={3} style={{ margin: 0 }}>
                <FileProtectOutlined style={{ marginRight: 8, color: '#1677ff' }} />
                Claim: {claim.claimNumber}
              </Title>
              <Text type="secondary">ID: {claim.id}</Text>
            </div>
          </Space>

          <Space wrap>
            {canAssess && (
              <Button onClick={() => handleOpenModal('ASSESS')}>
                Assess Claim
              </Button>
            )}
            {canApprove && (
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={() => handleOpenModal('APPROVE')}
              >
                Approve Claim
              </Button>
            )}
            {canReject && (
              <Button
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => handleOpenModal('REJECT')}
              >
                Reject Claim
              </Button>
            )}
            {canDispute && (
              <Button
                icon={<ExclamationCircleOutlined />}
                onClick={() => handleOpenModal('DISPUTE')}
              >
                Dispute Claim
              </Button>
            )}
            {canSettle && (
              <Button
                type="primary"
                style={{ background: '#52c41a', borderColor: '#52c41a' }}
                icon={<DollarOutlined />}
                onClick={() => handleOpenModal('SETTLE')}
              >
                Record Settlement
              </Button>
            )}
          </Space>
        </div>

        <Descriptions bordered column={{ xxl: 3, xl: 3, lg: 2, md: 2, sm: 1, xs: 1 }}>
          <Descriptions.Item label="Claim Number">{claim.claimNumber}</Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color={getStatusColor(claim.status)}>{claim.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Incident Date">
            {new Date(claim.incidentDate).toLocaleString()}
          </Descriptions.Item>

          <Descriptions.Item label="Policy">
            <a onClick={() => navigate(`/freight/insurance/policies/${claim.policyId}`)}>
              View Policy ({claim.policyId.substring(0, 8)}...)
            </a>
          </Descriptions.Item>
          <Descriptions.Item label="Currency">{claim.currencyCode}</Descriptions.Item>
          <Descriptions.Item label="Version">v{claim.version}</Descriptions.Item>

          <Descriptions.Item label="Claimed Amount">
            <Text strong>{claim.claimedAmount.toLocaleString()} {claim.currencyCode}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Assessed Amount">
            <Text strong>
              {claim.assessedAmount != null
                ? `${claim.assessedAmount.toLocaleString()} ${claim.currencyCode}`
                : 'Pending Assessment'}
            </Text>
          </Descriptions.Item>
          <Descriptions.Item label="Total Settled">
            <Text strong type={claim.totalSettledAmount > 0 ? 'success' : undefined}>
              {claim.totalSettledAmount.toLocaleString()} {claim.currencyCode}
            </Text>
          </Descriptions.Item>

          {claim.assessedBy && (
            <Descriptions.Item label="Assessed By">
              {claim.assessedBy} ({new Date(claim.assessedAt || '').toLocaleString()})
            </Descriptions.Item>
          )}

          {claim.resolutionReason && (
            <Descriptions.Item label="Resolution / Reason" span={2}>
              {claim.resolutionReason}
            </Descriptions.Item>
          )}

          <Descriptions.Item label="Loss Description" span={3}>
            {claim.description}
          </Descriptions.Item>

          {claim.assessmentNotes && (
            <Descriptions.Item label="Assessment Notes" span={3}>
              {claim.assessmentNotes}
            </Descriptions.Item>
          )}
        </Descriptions>

        <Divider />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div>
            <Title level={4} style={{ margin: 0 }}>Settlement History</Title>
            <Text type="secondary">Audit trail of financial settlements for this claim</Text>
          </div>
          {claim.assessedAmount != null && !isSettled && (
            <Tag color="processing">
              Remaining to Settle: {remainingToSettle.toLocaleString()} {claim.currencyCode}
            </Tag>
          )}
        </div>

        <Table
          dataSource={claim.settlements || []}
          columns={settlementColumns}
          rowKey="id"
          pagination={false}
          locale={{ emptyText: 'No settlements recorded yet' }}
        />
      </Card>

      {/* Dynamic Action Modals */}
      <Modal
        title={
          activeModal === 'ASSESS'
            ? 'Assess Claim Amount'
            : activeModal === 'APPROVE'
            ? 'Approve Claim for Settlement'
            : activeModal === 'REJECT'
            ? 'Reject Claim'
            : activeModal === 'DISPUTE'
            ? 'Mark Claim as Disputed'
            : 'Record Settlement Payment'
        }
        open={activeModal !== null}
        onCancel={() => setActiveModal(null)}
        footer={null}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleModalSubmit}>
          {activeModal === 'ASSESS' && (
            <>
              <Form.Item
                name="assessedAmount"
                label="Assessed Amount"
                rules={[{ required: true, message: 'Please input assessed amount' }]}
              >
                <InputNumber min={0.01} step={100} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name="assessmentNotes"
                label="Assessment Notes / Evidence"
                rules={[{ required: true, message: 'Please provide assessment rationale' }]}
              >
                <TextArea rows={3} placeholder="Evaluator rationale, surveyor reports..." />
              </Form.Item>
            </>
          )}

          {activeModal === 'APPROVE' && (
            <>
              <Paragraph>
                Are you sure you want to approve this claim? The assessed amount of{' '}
                <Text strong>{claim.assessedAmount?.toLocaleString()} {claim.currencyCode}</Text> will be unlocked for settlement.
              </Paragraph>
              <Form.Item name="notes" label="Approval Notes (optional)">
                <TextArea rows={2} placeholder="Authorized by manager..." />
              </Form.Item>
            </>
          )}

          {activeModal === 'REJECT' && (
            <Form.Item
              name="reason"
              label="Rejection Reason"
              rules={[{ required: true, message: 'Please provide reason for rejection' }]}
            >
              <TextArea rows={3} placeholder="Exclusion clause, lack of proof..." />
            </Form.Item>
          )}

          {activeModal === 'DISPUTE' && (
            <Form.Item
              name="reason"
              label="Dispute Reason"
              rules={[{ required: true, message: 'Please provide dispute details' }]}
            >
              <TextArea rows={3} placeholder="Insured disagrees with assessment/rejection..." />
            </Form.Item>
          )}

          {activeModal === 'SETTLE' && (
            <>
              <Form.Item
                name="amount"
                label={`Settlement Amount (Max: ${remainingToSettle.toLocaleString()} ${claim.currencyCode})`}
                rules={[
                  { required: true, message: 'Please input settlement amount' },
                  {
                    type: 'number',
                    max: remainingToSettle,
                    message: `Amount cannot exceed remaining assessed amount (${remainingToSettle})`,
                  },
                ]}
              >
                <InputNumber min={0.01} max={remainingToSettle} step={50} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item name="notes" label="Settlement Notes / Transaction Ref (optional)">
                <TextArea rows={2} placeholder="Wire transfer ref, check #..." />
              </Form.Item>
            </>
          )}

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setActiveModal(null)}>Cancel</Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={
                  assessMutation.isPending ||
                  approveMutation.isPending ||
                  rejectMutation.isPending ||
                  disputeMutation.isPending ||
                  settleMutation.isPending
                }
              >
                Submit
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
