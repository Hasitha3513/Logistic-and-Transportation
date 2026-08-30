import { z } from 'zod';

export const proofDraftSchema = z.object({
  signerName: z.string().trim().max(200).optional(), signerRelationship: z.string().trim().max(100).optional(),
  latitude: z.number().min(-90).max(90).optional(), longitude: z.number().min(-180).max(180).optional(), accuracyMeters: z.number().positive().optional(),
}).refine(v => (v.latitude == null) === (v.longitude == null), { message: 'Latitude and longitude must be supplied together', path: ['latitude'] });
export type ProofDraftValues = z.infer<typeof proofDraftSchema>;

export function validateEvidenceFile(type: 'SIGNATURE' | 'PHOTO', file: File) {
  if (!['image/png', 'image/jpeg'].includes(file.type)) return 'Only PNG or JPEG files are allowed';
  const limit = type === 'SIGNATURE' ? 2 * 1024 * 1024 : 10 * 1024 * 1024;
  return file.size > limit ? `${type === 'SIGNATURE' ? 'Signature' : 'Photo'} exceeds the file-size limit` : null;
}

export async function validateImageDimensions(
  type: 'SIGNATURE' | 'PHOTO',
  file: File,
): Promise<string | null> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      const minW = type === 'SIGNATURE' ? 100 : 320;
      const minH = type === 'SIGNATURE' ? 50 : 240;
      if (img.naturalWidth < minW || img.naturalHeight < minH) {
        resolve(`${type === 'SIGNATURE' ? 'Signature' : 'Photo'} resolution is too low (minimum ${minW}x${minH})`);
      } else {
        resolve(null);
      }
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      resolve('Image file could not be decoded');
    };
    img.src = url;
  });
}

export function validatePodConsent(consentGiven: boolean, hasSignatureOrPhoto: boolean): string | null {
  if (hasSignatureOrPhoto && !consentGiven) {
    return 'Customer consent is required before capturing signature or photo evidence';
  }
  return null;
}
