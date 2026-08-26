import { vehicleSchema } from './vehicleSchema';

const validVehicle = {
  registrationNumber: 'WP-CAB-1201',
  chassisNumber: '',
  engineNumber: '',
  categoryId: 'category-1',
  typeId: 'type-1',
  manufacturer: 'Isuzu',
  model: 'NPR',
  manufactureYear: 2024,
  ownershipType: 'COMPANY_OWNED' as const,
  operationalStatus: 'AVAILABLE' as const,
  currentOdometerKm: 0,
  engineHours: 0,
  capacityKg: 5000,
  active: true,
};

describe('vehicleSchema', () => {
  it('accepts the current backend Vehicle Master contract', () => {
    expect(vehicleSchema.safeParse(validVehicle).success).toBe(true);
  });

  it('requires registration, category, and vehicle type', () => {
    const result = vehicleSchema.safeParse({ ...validVehicle, registrationNumber: '', categoryId: '', typeId: '' });
    expect(result.success).toBe(false);
    if (!result.success) expect(result.error.issues.map((issue) => issue.path[0])).toEqual(
      expect.arrayContaining(['registrationNumber', 'categoryId', 'typeId']),
    );
  });

  it('rejects unsupported status values and negative readings', () => {
    expect(vehicleSchema.safeParse({ ...validVehicle, operationalStatus: 'RETIRED' }).success).toBe(false);
    expect(vehicleSchema.safeParse({ ...validVehicle, currentOdometerKm: -1 }).success).toBe(false);
  });
});
