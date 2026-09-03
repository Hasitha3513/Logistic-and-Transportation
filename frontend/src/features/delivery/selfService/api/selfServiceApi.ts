export interface Preferences {
  emailEnabled: boolean;
  smsEnabled: boolean;
  maskedEmail?: string;
  maskedPhone?: string;
  explicitProfile: boolean;
  version?: number;
}

export interface Submission { reference: string; type: string; status: string; createdAt: string }
export interface Projection {
  deliveryNumber: string;
  status: string;
  explanation: string;
  scheduledStart: string;
  scheduledEnd: string;
  timeZone: string;
  estimatedArrivalAt?: string;
  etaCalculatedAt?: string;
  etaFreshness: 'CURRENT' | 'STALE' | 'UNAVAILABLE';
  availableActions: string[];
  destination: string;
  podAvailability: 'AVAILABLE' | 'NOT_AVAILABLE';
  completedAt?: string;
  notificationPreferences: Preferences;
  submissions: Submission[];
  requestState?: string;
}

const base = `${import.meta.env.VITE_API_BASE_URL ?? '/api'}/public/v1/delivery-self-service`;

async function request<T>(token: string, path = '', init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${base}${path}`, {
    ...init,
    cache: 'no-store',
    referrerPolicy: 'no-referrer',
    headers: { 'Content-Type': 'application/json', Authorization: `DeliveryAccess ${token}`, ...init.headers },
  });
  if (!response.ok) throw new Error(response.status === 404 ? 'This delivery link is invalid or no longer available.' : 'We could not complete that request.');
  return response.json() as Promise<T>;
}

export const selfServiceApi = {
  track: (token: string) => request<Projection>(token),
  preferences: (token: string) => request<Preferences>(token, '/notification-preferences'),
  replacePreferences: (token: string, body: Pick<Preferences, 'emailEnabled' | 'smsEnabled' | 'version'>) =>
    request<Preferences>(token, '/notification-preferences', { method: 'PUT', body: JSON.stringify(body) }),
  issue: (token: string, category: string, description: string) => request<Submission>(token, '/issues', {
    method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ category, description }),
  }),
  feedback: (token: string, rating: number, comment: string) => request<Submission>(token, '/feedback', {
    method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ rating, comment: comment || null }),
  }),
  deliveryRequest: (token: string, preferredStartAt?: string, preferredEndAt?: string, notes?: string) =>
    request<Submission>(token, '/redelivery-requests', { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({ preferredStartAt: preferredStartAt ? new Date(preferredStartAt).toISOString() : null,
        preferredEndAt: preferredEndAt ? new Date(preferredEndAt).toISOString() : null, notes: notes || null }) }),
};
