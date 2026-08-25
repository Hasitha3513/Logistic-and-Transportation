import { api } from '../../../api/client';
import type {
  CreateDisruptionInput,
  Route,
  RouteDisruption,
  RouteOptimizationResult,
  RoutePerformanceAnalytics,
  RouteRevision,
} from '../types/route';

export const routeApi = {
  async list(): Promise<Route[]> {
    return (await api.get<Route[]>('/routes')).data;
  },

  async get(id: string): Promise<Route> {
    return (await api.get<Route>(`/routes/${id}`)).data;
  },

  async listRevisions(routeId: string): Promise<RouteRevision[]> {
    return (await api.get<RouteRevision[]>(`/routes/${routeId}/revisions`)).data;
  },

  async getRevision(routeId: string, revisionNumber: number): Promise<RouteRevision> {
    return (await api.get<RouteRevision>(`/routes/${routeId}/revisions/${revisionNumber}`)).data;
  },

  async listDisruptions(routeId: string): Promise<RouteDisruption[]> {
    return (await api.get<RouteDisruption[]>(`/routes/${routeId}/disruptions`)).data;
  },

  async listActiveDisruptions(): Promise<RouteDisruption[]> {
    return (await api.get<RouteDisruption[]>('/routes/disruptions/active')).data;
  },

  async createDisruption(routeId: string, input: CreateDisruptionInput): Promise<RouteDisruption> {
    return (await api.post<RouteDisruption>(`/routes/${routeId}/disruptions`, input)).data;
  },

  async resolveDisruption(routeId: string, disruptionId: string): Promise<RouteDisruption> {
    return (await api.post<RouteDisruption>(`/routes/${routeId}/disruptions/${disruptionId}/resolve`)).data;
  },

  async optimize(routeId: string): Promise<RouteOptimizationResult> {
    return (await api.post<RouteOptimizationResult>(`/routes/${routeId}/optimize`)).data;
  },

  async applyOptimization(routeId: string, optimizedStopLocationIds: string[]): Promise<Route> {
    return (await api.post<Route>(`/routes/${routeId}/apply-optimization`, { optimizedStopLocationIds })).data;
  },

  async getPerformance(routeId: string, from?: string, to?: string): Promise<RoutePerformanceAnalytics> {
    return (await api.get<RoutePerformanceAnalytics>(`/routes/${routeId}/performance`, {
      params: { from, to },
    })).data;
  },
};
