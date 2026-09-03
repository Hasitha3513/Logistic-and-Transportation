export interface Preferences {
  emailEnabled: boolean;
  smsEnabled: boolean;
  maskedEmail?: string;
  maskedPhone?: string;
  explicitProfile: boolean;
  version?: number | null;
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

const base = '/public/v1/delivery-self-service';

async function request<T>(token: string, path = '', init: {
  method?: 'GET' | 'POST' | 'PUT'; body?: unknown; idempotencyKey?: string;
} = {}): Promise<T> {
  try {
    const response = await publicApi.request<T>({
      url: `${base}${path}`,
      method: init.method ?? 'GET',
      data: init.body,
      headers: {
        Authorization: `DeliveryAccess ${token}`,
        ...(init.idempotencyKey ? { 'Idempotency-Key': init.idempotencyKey } : {}),
      },
    });
    return response.data;
  } catch (reason) {
    if (axios.isAxiosError(reason) && reason.response?.status === 404) {
      throw new Error('This delivery link is invalid or no longer available.');
    }
    if (axios.isAxiosError(reason) && reason.response?.status === 429) {
      throw new Error('Access is temporarily limited. Please wait and try again.');
    }
    throw new Error('We could not complete that request.');
  }
}

export const selfServiceApi = {
  track: (token: string) => request<Projection>(token),
  preferences: (token: string) => request<Preferences>(token, '/notification-preferences'),
  replacePreferences: (token: string, body: Pick<Preferences, 'emailEnabled' | 'smsEnabled' | 'version'>) =>
    request<Preferences>(token, '/notification-preferences', { method: 'PUT', body }),
  issue: (token: string, category: string, description: string) => request<Submission>(token, '/issues', {
    method: 'POST', idempotencyKey: crypto.randomUUID(), body: { category, description },
  }),
  feedback: (token: string, rating: number, comment: string) => request<Submission>(token, '/feedback', {
    method: 'POST', idempotencyKey: crypto.randomUUID(), body: { rating, comment: comment || null },
  }),
  deliveryRequest: (token: string, preferredStartAt?: string, preferredEndAt?: string, notes?: string) =>
    request<Submission>(token, '/redelivery-requests', { method: 'POST', idempotencyKey: crypto.randomUUID(),
      body: { preferredStartAt: preferredStartAt ? new Date(preferredStartAt).toISOString() : null,
        preferredEndAt: preferredEndAt ? new Date(preferredEndAt).toISOString() : null, notes: notes || null } }),
};
import axios from 'axios';
import { publicApi } from '../../../../api/client';
