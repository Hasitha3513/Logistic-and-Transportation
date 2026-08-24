import { api } from '../../../../api/client';
import type {
  Vehicle,
  VehicleCategoryReference,
  VehicleDocument,
  VehicleInput,
  VehicleTypeReference,
} from '../types/vehicle';

export const vehicleApi = {
  async list(): Promise<Vehicle[]> {
    return (await api.get<Vehicle[]>('/vehicles')).data;
  },

  async get(id: string): Promise<Vehicle> {
    return (await api.get<Vehicle>(`/vehicles/${id}`)).data;
  },

  async create(input: VehicleInput): Promise<Vehicle> {
    return (await api.post<Vehicle>('/vehicles', input)).data;
  },

  async update(id: string, input: VehicleInput): Promise<Vehicle> {
    return (await api.put<Vehicle>(`/vehicles/${id}`, input)).data;
  },

  async deactivate(id: string): Promise<void> {
    await api.delete(`/vehicles/${id}`);
  },

  async listCategories(): Promise<VehicleCategoryReference[]> {
    return (await api.get<VehicleCategoryReference[]>('/vehicle-categories')).data;
  },

  async listTypes(): Promise<VehicleTypeReference[]> {
    return (await api.get<VehicleTypeReference[]>('/vehicle-types')).data;
  },

  async listDocuments(vehicleId: string): Promise<VehicleDocument[]> {
    return (await api.get<VehicleDocument[]>(`/vehicles/${vehicleId}/documents`)).data;
  },

  async deleteDocument(vehicleId: string, documentId: string): Promise<void> {
    await api.delete(`/vehicles/${vehicleId}/documents/${documentId}`);
  },
};
