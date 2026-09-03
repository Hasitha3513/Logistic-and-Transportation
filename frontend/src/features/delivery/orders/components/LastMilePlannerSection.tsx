import { CompassOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Flex, Space, Statistic, Tag, Typography } from 'antd';
import { isAxiosError } from 'axios';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../../../auth/AuthContext';
import { deliveryOrderApi } from '../api/deliveryOrderApi';
import { useNavigate } from 'react-router-dom';

const targetForAction: Record<string, string> = {
  RECORD_FAILED_ATTEMPT: 'failed-delivery',
  REVIEW_WRONG_ADDRESS: 'delivery-exceptions',
  REVIEW_SPECIALIZED_EXCEPTION: 'delivery-exceptions',
  ESCALATE: 'failed-delivery',
  SCHEDULE_REDELIVERY: 'redelivery',
  REASSIGN_RIDER: 'rider-assignment',
  REVIEW_BATCH: 'batch-context',
  RECALCULATE_ETA: 'delivery-eta',
};

const labelForAction: Record<string, string> = {
  RECORD_FAILED_ATTEMPT: 'Record actual failed attempt',
  REVIEW_WRONG_ADDRESS: 'Use address investigation',
  REVIEW_SPECIALIZED_EXCEPTION: 'Review active exception',
  ESCALATE: 'Escalate delivery',
  SCHEDULE_REDELIVERY: 'Schedule redelivery',
  REASSIGN_RIDER: 'Reassign Rider',
  REVIEW_BATCH: 'Review batch context',
  RECALCULATE_ETA: 'Recalculate ETA',
};

export function LastMilePlannerSection({ deliveryId }: { deliveryId: string }) {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const canView = hasPermission('DELIVERY_FAIL_VIEW') || hasPermission('DELIVERY_EXCEPTION_VIEW');
  const context = useQuery({
    queryKey: ['last-mile-planner', deliveryId],
    queryFn: () => deliveryOrderApi.getLastMilePlannerContext(deliveryId),
    enabled: canView,
  });

  if (!canView) return null;
  if (context.isLoading) return <Card title="Last-Mile Planner"><Typography.Text>Loading planner context…</Typography.Text></Card>;
  if (context.isError) {
    const message = isAxiosError<{ message?: string }>(context.error) ? context.error.response?.data?.message : undefined;
    return <Alert type="warning" showIcon message="Last-mile planner context unavailable" description={message ?? 'Refresh the delivery or check your access.'} />;
  }
  if (!context.data) return null;

  const goTo = (action: string) => {
    if (action === 'REASSIGN_RIDER') return navigate('/deliveries/riders');
    if (action === 'REVIEW_BATCH') return navigate('/deliveries/batches');
    document.getElementById(targetForAction[action])?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };
  return <Card id="last-mile-planner" title={<Space><CompassOutlined />Last-Mile Planner</Space>}>
    <Typography.Paragraph type="secondary">Classify the field situation, then use the owning Delivery workflow. This planner does not create a separate exception record or change status by itself.</Typography.Paragraph>
    <Flex gap={18} wrap="wrap"><Statistic title="Failed attempts" value={context.data.failedAttemptCount} /><Statistic title="Active exceptions" value={context.data.activeExceptionCount} /><Statistic title="Open escalations" value={context.data.openEscalationCount} /></Flex>
    <Space wrap style={{ marginTop: 16 }}>{context.data.availableActions.map((action) => <Button key={action} onClick={() => goTo(action)}>{labelForAction[action] ?? action}</Button>)}</Space>
    {context.data.availableActions.length === 0 && <Tag color="default" style={{ marginTop: 16 }}>No last-mile action is available for this terminal delivery state.</Tag>}
  </Card>;
}
