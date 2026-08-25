import { z } from 'zod';

export const loadPlanItemPlacementSchema = z.object({
  manifestItemId: z.string().min(1, 'Manifest item is required'),
  placementOrder: z.number().int().min(0, 'Placement order must be >= 0'),
  zoneReference: z.string().max(120, 'Max 120 characters').optional().nullable(),
  stackGroup: z.string().max(120, 'Max 120 characters').optional().nullable(),
  containerReference: z.string().max(200, 'Max 200 characters').optional().nullable(),
  loadingSequence: z.number().int().min(0, 'Loading sequence must be >= 0'),
  specialHandlingNotes: z.string().max(500, 'Max 500 characters').optional().nullable(),
});

export const loadPlanSchema = z.object({
  cargoManifestId: z.string().min(1, 'Cargo manifest is required'),
  vehicleId: z.string().min(1, 'Vehicle is required'),
  notes: z.string().max(2000, 'Max 2000 characters').optional().nullable(),
  placements: z.array(loadPlanItemPlacementSchema).default([]),
});

export type LoadPlanFormValues = z.infer<typeof loadPlanSchema>;
export type LoadPlanItemPlacementFormValues = z.infer<typeof loadPlanItemPlacementSchema>;
