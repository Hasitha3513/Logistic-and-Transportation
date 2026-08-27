
import { request, APIRequestContext } from '@playwright/test';

export class ApiClient {
  private baseURL: string;

  constructor(baseURL = process.env.BASE_URL || 'http://localhost:5173') {
    this.baseURL = baseURL;
  }

  async getContext(token?: string): Promise<APIRequestContext> {
    return request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    });
  }

  async get<T = unknown>(endpoint: string, token?: string, params?: Record<string, string | number | boolean>): Promise<T> {
    const context = await this.getContext(token);
    const response = await context.get(endpoint, { params });
    if (!response.ok()) throw new Error(`GET ${endpoint} failed with ${response.status()}`);
    return response.json();
  }

  async post<T = unknown>(endpoint: string, body: unknown, token?: string): Promise<T> {
    const context = await this.getContext(token);
    const response = await context.post(endpoint, { data: body });
    if (!response.ok()) throw new Error(`POST ${endpoint} failed with ${response.status()}`);
    return response.json();
  }

  async put<T = unknown>(endpoint: string, body: unknown, token?: string): Promise<T> {
    const context = await this.getContext(token);
    const response = await context.put(endpoint, { data: body });
    if (!response.ok()) throw new Error(`PUT ${endpoint} failed with ${response.status()}`);
    return response.json();
  }

  async delete(endpoint: string, token?: string): Promise<boolean> {
    const context = await this.getContext(token);
    const response = await context.delete(endpoint);
    return response.ok();
  }
}

export const api = new ApiClient();
