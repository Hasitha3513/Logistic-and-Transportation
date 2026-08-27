import { request } from '@playwright/test';

const baseURL = process.env.BASE_URL || 'http://localhost:5173';

export async function apiCreate(path: string, body: unknown, token?: string) {
  const context = await request.newContext({ baseURL });
  const response = await context.post(path.startsWith('/api') ? path : `/api${path.startsWith('/') ? path : `/${path}`}`, {
    data: body,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return response.json();
}

export async function apiDelete(path: string, token?: string) {
  const context = await request.newContext({ baseURL });
  const response = await context.delete(path.startsWith('/api') ? path : `/api${path.startsWith('/') ? path : `/${path}`}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return response.ok();
}

export async function apiGet(path: string, token?: string) {
  const context = await request.newContext({ baseURL });
  const response = await context.get(path.startsWith('/api') ? path : `/api${path.startsWith('/') ? path : `/${path}`}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return response.json();
}
