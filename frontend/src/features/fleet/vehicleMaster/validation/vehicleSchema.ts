import { z } from 'zod';
import { vehicleOperationalStatuses, vehicleOwnershipTypes } from '../types/vehicle';

const optionalText = z.string().trim().optional().or(z.literal(''));
const optionalNonNegative = z.number().nonnegative('Value cannot be negative').optional().nullable();

export const vehicleSchema = z.object({
  registrationNumber: z.string().trim().min(1, 'Registration number is required'),
  chassisNumber: optionalText,
  engineNumber: optionalText,
  categoryId: z.string().trim().min(1, 'Category is required'),
  typeId: z.string().trim().min(1, 'Vehicle type is required'),
  manufacturer: optionalText,
  model: optionalText,
  manufactureYear: z.number().int().min(1900, 'Manufacture year must be at least 1900').optional().nullable(),
  ownershipType: z.enum(vehicleOwnershipTypes, { message: 'Ownership is required' }),
  operationalStatus: z.enum(vehicleOperationalStatuses, { message: 'Operational status is required' }),
  currentOdometerKm: optionalNonNegative,
  engineHours: optionalNonNegative,
  capacityKg: optionalNonNegative,
  tareWeightKg: optionalNonNegative,
  grossVehicleWeightKg: optionalNonNegative,
  cargoVolumeCapacityM3: optionalNonNegative,
  axleCount: z.number().int().min(1, 'Axle count must be at least 1').optional().nullable(),
  maxAxleLoadKg: optionalNonNegative,
  active: z.boolean(),
}).refine((data) => {
  if (data.grossVehicleWeightKg != null && data.tareWeightKg != null) {
    return data.grossVehicleWeightKg >= data.tareWeightKg;
  }
  return true;
}, {
  message: 'Gross vehicle weight must be greater than or equal to tare weight',
  path: ['grossVehicleWeightKg'],
});

export type VehicleFormValues = z.infer<typeof vehicleSchema>;
