import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, Button, Card, Checkbox, Flex, Input, InputNumber, Select, Spin, Tag, Typography } from 'antd';
import { selfServiceApi, type Preferences, type Projection } from '../api/selfServiceApi';
import { useCustomerSelfService } from '../hooks/useCustomerSelfService';
import {
  deliveryRequestSchema,
  feedbackSchema,
  issueSchema,
  preferenceSchema,
  type DeliveryRequestValues,
  type FeedbackValues,
  type IssueValues,
  type PreferenceValues,
} from '../validation/selfServiceSchemas';

function consumeFragment(): string | undefined {
  const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
  const token = params.get('access_token') ?? undefined;
  if (window.location.hash) window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}`);
  return token;
}

export default function DeliverySelfServicePage() {
  const [access, setAccess] = useState(() => ({ token: consumeFragment(), revision: 0 }));
  const [error, setError] = useState<string>();
  const [message, setMessage] = useState<string>();
  const delivery = useCustomerSelfService(access.token, access.revision);
  const action = useMutation({
    mutationFn: async (command: { run: () => Promise<unknown>; success: string }) => {
      await command.run();
      return command.success;
    },
    onSuccess: async success => {
      setError(undefined);
      setMessage(success);
      await delivery.refetch();
    },
    onError: reason => setError(reason instanceof Error ? reason.message : 'Request failed.'),
  });

  useEffect(() => {
    const reopen = () => {
      const token = consumeFragment();
      if (token) {
        setError(undefined);
        setMessage(undefined);
        setAccess(previous => ({ token, revision: previous.revision + 1 }));
      }
    };
    window.addEventListener('hashchange', reopen);
    return () => window.removeEventListener('hashchange', reopen);
  }, []);

  if (!access.token) {
    return <PublicShell><Alert type="warning" showIcon message="Open your original delivery link"
      description="For your security, access is not retained after a reload." /></PublicShell>;
  }
  if (delivery.isPending) return <PublicShell><Spin size="large" aria-label="Loading delivery" /></PublicShell>;
  if (!delivery.data) {
    return <PublicShell><Alert type="error" showIcon message="Delivery access unavailable"
      description={delivery.error instanceof Error ? delivery.error.message : 'This delivery link is unavailable.'} /></PublicShell>;
  }

  const token = access.token;
  const perform = (run: () => Promise<unknown>, success: string) => {
    setMessage(undefined);
    action.mutate({ run, success });
  };

  return <PublicShell>
    <Flex vertical gap={18}>
      {message && <Alert type="success" showIcon message={message} />}
      {error && <Alert type="error" closable onClose={() => setError(undefined)} message={error} />}
      <DeliverySummary delivery={delivery.data} />
      {delivery.data.availableActions.includes('PREFERENCES') && <PreferenceForm
        value={delivery.data.notificationPreferences} busy={action.isPending}
        submit={value => perform(() => selfServiceApi.replacePreferences(token, value), 'Preferences updated.')} />}
      {delivery.data.availableActions.includes('REPORT_ISSUE') && <IssueForm busy={action.isPending}
        submit={value => perform(() => selfServiceApi.issue(token, value.category, value.description),
          'Your issue was submitted.')} />}
      {delivery.data.availableActions.includes('DELIVERY_REQUEST') && <DeliveryRequestForm busy={action.isPending}
        submit={value => perform(() => selfServiceApi.deliveryRequest(token, value.start, value.end, value.notes),
          'Your non-binding delivery request was submitted.')} />}
      {delivery.data.availableActions.includes('FEEDBACK') && <FeedbackForm busy={action.isPending}
        submit={value => perform(() => selfServiceApi.feedback(token, value.rating, value.comment),
          'Thank you for your feedback.')} />}
    </Flex>
  </PublicShell>;
}

function DeliverySummary({ delivery }: { delivery: Projection }) {
  return <Card>
    <Flex justify="space-between" align="start" wrap="wrap" gap={12}>
      <div><Typography.Text type="secondary">Delivery</Typography.Text>
        <Typography.Title level={2}>{delivery.deliveryNumber}</Typography.Title></div>
      <Tag color="cyan">{delivery.status}</Tag>
    </Flex>
    <Typography.Paragraph>{delivery.explanation}</Typography.Paragraph>
    <dl className="track-facts">
      <div><dt>Destination</dt><dd>{delivery.destination}</dd></div>
      <div><dt>Scheduled window</dt><dd>{new Date(delivery.scheduledStart).toLocaleString()} – {new Date(delivery.scheduledEnd).toLocaleString()} ({delivery.timeZone})</dd></div>
      <div><dt>Estimated arrival</dt><dd>{delivery.estimatedArrivalAt ? new Date(delivery.estimatedArrivalAt).toLocaleString() : 'Not available'} <Tag>{delivery.etaFreshness}</Tag></dd></div>
      <div><dt>Proof of delivery</dt><dd>{delivery.podAvailability === 'AVAILABLE' ? 'Recorded' : 'Not yet available'}</dd></div>
    </dl>
  </Card>;
}

function PreferenceForm({ value, busy, submit }: {
  value: Preferences; busy: boolean; submit: (value: PreferenceValues) => void;
}) {
  const { control, handleSubmit, reset } = useForm<PreferenceValues>({
    resolver: zodResolver(preferenceSchema),
    defaultValues: { emailEnabled: value.emailEnabled, smsEnabled: value.smsEnabled, version: value.version },
  });
  useEffect(() => reset({ emailEnabled: value.emailEnabled, smsEnabled: value.smsEnabled, version: value.version }),
    [reset, value]);
  return <Card title="Notification preferences"><form onSubmit={handleSubmit(submit)}>
    <Flex vertical gap={12}>
      <Controller name="emailEnabled" control={control} render={({ field }) => <Checkbox
        checked={field.value} onChange={event => field.onChange(event.target.checked)}>
        Email updates {value.maskedEmail && `(${value.maskedEmail})`}</Checkbox>} />
      <Controller name="smsEnabled" control={control} render={({ field }) => <Checkbox
        checked={field.value} onChange={event => field.onChange(event.target.checked)}>
        SMS updates {value.maskedPhone && `(${value.maskedPhone})`}</Checkbox>} />
      <Button htmlType="submit" type="primary" loading={busy}>Save preferences</Button>
    </Flex>
  </form></Card>;
}

function IssueForm({ busy, submit }: { busy: boolean; submit: (value: IssueValues) => void }) {
  const { control, handleSubmit, formState: { errors } } = useForm<IssueValues>({
    resolver: zodResolver(issueSchema), defaultValues: { description: '' },
  });
  return <Card title="Report a delivery issue"><form onSubmit={handleSubmit(submit)}>
    <Flex vertical gap={12}>
      <label htmlFor="issue-category">Issue category</label>
      <Controller name="category" control={control} render={({ field }) => <Select id="issue-category"
        value={field.value} onChange={field.onChange}
        options={['DELIVERY_TIMING', 'ACCESS_OR_ADDRESS_CLARIFICATION', 'DELIVERY_CONDITION', 'DELIVERY_SERVICE', 'OTHER']
          .map(option => ({ value: option, label: option.replaceAll('_', ' ') }))} />} />
      {errors.category && <Typography.Text type="danger" role="alert">Select an issue category.</Typography.Text>}
      <label htmlFor="issue-description">Description</label>
      <Controller name="description" control={control} render={({ field }) => <Input.TextArea
        id="issue-description" rows={4} value={field.value} onChange={field.onChange} onBlur={field.onBlur} />} />
      {errors.description && <Typography.Text type="danger" role="alert">Enter 10 to 1,000 characters.</Typography.Text>}
      <Button htmlType="submit" loading={busy}>Submit issue</Button>
    </Flex>
  </form></Card>;
}

function DeliveryRequestForm({ busy, submit }: {
  busy: boolean; submit: (value: DeliveryRequestValues) => void;
}) {
  const { control, handleSubmit, formState: { errors } } = useForm<DeliveryRequestValues>({
    resolver: zodResolver(deliveryRequestSchema), defaultValues: { start: '', end: '', notes: '' },
  });
  return <Card title="Delivery request"><form onSubmit={handleSubmit(submit)}>
    <Flex vertical gap={12}>
      <Flex gap={12} wrap="wrap">
        <div><label htmlFor="preferred-start">Preferred start</label><Controller name="start" control={control}
          render={({ field }) => <Input id="preferred-start" type="datetime-local" value={field.value}
            onChange={field.onChange} onBlur={field.onBlur} />} /></div>
        <div><label htmlFor="preferred-end">Preferred end</label><Controller name="end" control={control}
          render={({ field }) => <Input id="preferred-end" type="datetime-local" value={field.value}
            onChange={field.onChange} onBlur={field.onBlur} />} /></div>
      </Flex>
      {errors.end && <Typography.Text type="danger" role="alert">{errors.end.message}</Typography.Text>}
      <label htmlFor="request-notes">Notes</label><Controller name="notes" control={control}
        render={({ field }) => <Input.TextArea id="request-notes" maxLength={1000} value={field.value}
          onChange={field.onChange} onBlur={field.onBlur} />} />
      <Button htmlType="submit" loading={busy}>Submit request</Button>
    </Flex>
  </form></Card>;
}

function FeedbackForm({ busy, submit }: { busy: boolean; submit: (value: FeedbackValues) => void }) {
  const { control, handleSubmit, formState: { errors } } = useForm<FeedbackValues>({
    resolver: zodResolver(feedbackSchema), defaultValues: { comment: '' },
  });
  return <Card title="Delivery feedback"><form onSubmit={handleSubmit(submit)}>
    <Flex vertical gap={12}>
      <label htmlFor="feedback-rating">Rating</label>
      <Controller name="rating" control={control} render={({ field }) => <InputNumber id="feedback-rating"
        min={1} max={5} value={field.value} onChange={field.onChange} />} />
      {errors.rating && <Typography.Text type="danger" role="alert">Choose a rating from 1 to 5.</Typography.Text>}
      <label htmlFor="feedback-comment">Comment</label><Controller name="comment" control={control}
        render={({ field }) => <Input.TextArea id="feedback-comment" maxLength={1000} value={field.value}
          onChange={field.onChange} onBlur={field.onBlur} />} />
      <Button htmlType="submit" loading={busy}>Submit feedback</Button>
    </Flex>
  </form></Card>;
}

function PublicShell({ children }: { children: React.ReactNode }) {
  return <main className="track-page"><header className="track-header"><div className="track-brand">TL</div>
    <div><strong>Transport Logistics</strong><span>Delivery self-service</span></div></header>
    <section className="track-content">{children}</section>
    <footer>Your secure link provides access only to this delivery.</footer></main>;
}

export { consumeFragment };
