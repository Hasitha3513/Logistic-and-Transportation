import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Card, Checkbox, Flex, Form, Input, InputNumber, Select, Spin, Tag, Typography } from 'antd';
import { selfServiceApi, type Projection } from '../api/selfServiceApi';

function consumeFragment(): string | undefined {
  const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
  const token = params.get('access_token') ?? undefined;
  if (window.location.hash) window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}`);
  return token;
}

export default function DeliverySelfServicePage() {
  const [token, setToken] = useState(consumeFragment);
  const [delivery, setDelivery] = useState<Projection>();
  const [error, setError] = useState<string>();
  const [busy, setBusy] = useState(Boolean(token));
  const [message, setMessage] = useState<string>();

  const load = useCallback(async () => {
    if (!token) return;
    try { setDelivery(await selfServiceApi.track(token)); setError(undefined); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'This delivery link is unavailable.'); }
    finally { setBusy(false); }
  }, [token]);
  useEffect(() => {
    if (!token) return;
    let current = true;
    selfServiceApi.track(token)
      .then(value => { if (current) { setDelivery(value); setError(undefined); } })
      .catch(reason => { if (current) setError(reason instanceof Error ? reason.message : 'This delivery link is unavailable.'); })
      .finally(() => { if (current) setBusy(false); });
    return () => { current = false; };
  }, [token]);
  useEffect(() => {
    const reopen = () => {
      const next = consumeFragment();
      if (next) { setBusy(true); setDelivery(undefined); setError(undefined); setToken(next); }
    };
    window.addEventListener('hashchange', reopen);
    return () => window.removeEventListener('hashchange', reopen);
  }, []);

  if (!token) return <PublicShell><Alert type="warning" showIcon message="Open your original delivery link" description="For your security, access is not retained after a reload." /></PublicShell>;
  if (busy && !delivery) return <PublicShell><Spin size="large" aria-label="Loading delivery" /></PublicShell>;
  if (!delivery) return <PublicShell><Alert type="error" showIcon message="Delivery access unavailable" description={error} /></PublicShell>;

  const act = async (work: () => Promise<unknown>, success: string) => {
    setBusy(true); setMessage(undefined);
    try { await work(); setMessage(success); await load(); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Request failed.'); }
    finally { setBusy(false); }
  };

  return <PublicShell>
    <Flex vertical gap={18}>
      {message && <Alert type="success" showIcon message={message} />}
      {error && <Alert type="error" closable onClose={() => setError(undefined)} message={error} />}
      <Card>
        <Flex justify="space-between" align="start" wrap="wrap" gap={12}>
          <div><Typography.Text type="secondary">Delivery</Typography.Text><Typography.Title level={2}>{delivery.deliveryNumber}</Typography.Title></div>
          <Tag color="cyan">{delivery.status}</Tag>
        </Flex>
        <Typography.Paragraph>{delivery.explanation}</Typography.Paragraph>
        <dl className="track-facts">
          <div><dt>Destination</dt><dd>{delivery.destination}</dd></div>
          <div><dt>Scheduled window</dt><dd>{new Date(delivery.scheduledStart).toLocaleString()} – {new Date(delivery.scheduledEnd).toLocaleString()} ({delivery.timeZone})</dd></div>
          <div><dt>Estimated arrival</dt><dd>{delivery.estimatedArrivalAt ? new Date(delivery.estimatedArrivalAt).toLocaleString() : 'Not available'} <Tag>{delivery.etaFreshness}</Tag></dd></div>
          <div><dt>Proof of delivery</dt><dd>{delivery.podAvailability === 'AVAILABLE' ? 'Recorded' : 'Not yet available'}</dd></div>
        </dl>
      </Card>
      <Card title="Notification preferences">
        <Form layout="vertical" initialValues={delivery.notificationPreferences} onFinish={(values) => act(() => selfServiceApi.replacePreferences(token, { ...values, version: delivery.notificationPreferences.version }), 'Preferences updated.')}>
          <Form.Item name="emailEnabled" valuePropName="checked"><Checkbox>Email updates {delivery.notificationPreferences.maskedEmail && `(${delivery.notificationPreferences.maskedEmail})`}</Checkbox></Form.Item>
          <Form.Item name="smsEnabled" valuePropName="checked"><Checkbox>SMS updates {delivery.notificationPreferences.maskedPhone && `(${delivery.notificationPreferences.maskedPhone})`}</Checkbox></Form.Item>
          <Button htmlType="submit" type="primary" loading={busy}>Save preferences</Button>
        </Form>
      </Card>
      <Card title="Report a delivery issue">
        <Form layout="vertical" onFinish={(values) => act(() => selfServiceApi.issue(token, values.category, values.description), 'Your issue was submitted.')}>
          <Form.Item name="category" label="Issue category" rules={[{ required: true }]}><Select options={['DELIVERY_TIMING','ACCESS_OR_ADDRESS_CLARIFICATION','DELIVERY_CONDITION','DELIVERY_SERVICE','OTHER'].map(value => ({ value, label: value.replaceAll('_', ' ') }))} /></Form.Item>
          <Form.Item name="description" label="Description" rules={[{ required: true, min: 10, max: 1000 }]}><Input.TextArea rows={4} /></Form.Item>
          <Button htmlType="submit" loading={busy}>Submit issue</Button>
        </Form>
      </Card>
      {delivery.availableActions.includes('DELIVERY_REQUEST') && <Card title="Delivery request">
        <Form layout="vertical" onFinish={(values) => act(() => selfServiceApi.deliveryRequest(token, values.start, values.end, values.notes), 'Your non-binding delivery request was submitted.')}>
          <Flex gap={12} wrap="wrap"><Form.Item name="start" label="Preferred start"><Input type="datetime-local" /></Form.Item><Form.Item name="end" label="Preferred end"><Input type="datetime-local" /></Form.Item></Flex>
          <Form.Item name="notes" label="Notes"><Input.TextArea maxLength={1000} /></Form.Item>
          <Button htmlType="submit" loading={busy}>Submit request</Button>
        </Form>
      </Card>}
      {delivery.availableActions.includes('FEEDBACK') && <Card title="Delivery feedback">
        <Form layout="vertical" onFinish={(values) => act(() => selfServiceApi.feedback(token, values.rating, values.comment), 'Thank you for your feedback.')}>
          <Form.Item name="rating" label="Rating" rules={[{ required: true }]}><InputNumber min={1} max={5} /></Form.Item>
          <Form.Item name="comment" label="Comment"><Input.TextArea maxLength={1000} /></Form.Item>
          <Button htmlType="submit" loading={busy}>Submit feedback</Button>
        </Form>
      </Card>}
    </Flex>
  </PublicShell>;
}

function PublicShell({ children }: { children: React.ReactNode }) {
  return <main className="track-page"><header className="track-header"><div className="track-brand">TL</div><div><strong>Transport Logistics</strong><span>Delivery self-service</span></div></header><section className="track-content">{children}</section><footer>Your secure link provides access only to this delivery.</footer></main>;
}

export { consumeFragment };
