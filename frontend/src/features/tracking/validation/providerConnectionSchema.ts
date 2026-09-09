import { z } from 'zod';

const forbiddenKey=/(token|secret|password|credential|authorization|private.?key)/i;
export const safeConfigurationText=z.string().max(8192,'Safe configuration must be 8 KiB or less').superRefine((value,ctx)=>{
  try {
    const parsed:unknown=JSON.parse(value||'{}');
    if (!parsed || Array.isArray(parsed) || typeof parsed!=='object') ctx.addIssue({code:'custom',message:'Safe configuration must be a JSON object'});
    else if (Object.keys(parsed).some(key=>forbiddenKey.test(key))) ctx.addIssue({code:'custom',message:'Secret-like keys are not allowed in safe configuration'});
    else if (Object.values(parsed).some(item=>typeof item!=='string')) ctx.addIssue({code:'custom',message:'Safe configuration values must be strings'});
  } catch { ctx.addIssue({code:'custom',message:'Enter a valid JSON object'}); }
});

const endpoint=z.string().trim().max(500).refine(value=>{
  if (!value) return true;
  try { const uri=new URL(value); return uri.protocol==='https:' && uri.port!=='' && uri.port!=='443' ? false : uri.protocol==='https:' && !['localhost','127.0.0.1','::1'].includes(uri.hostname); } catch { return false; }
},'Use an HTTPS endpoint on the provider host (port 443)');

export const providerConnectionSchema=z.object({
  providerType:z.string().min(1,'Select a provider type'), displayName:z.string().trim().min(1).max(120),
  providerAlias:z.string().trim().min(1).max(80).regex(/^[A-Za-z0-9][A-Za-z0-9_-]*$/,'Use letters, numbers, underscore or hyphen'),
  providerKeyId:z.string().trim().min(1).max(160), endpointUri:endpoint,
  credentialReference:z.string().trim().max(160), pollIntervalSeconds:z.number().int().min(5).max(86400),
  pageSize:z.number().int().min(1).max(500), safeConfigurationText,
});
export type ProviderConnectionFormValues=z.infer<typeof providerConnectionSchema>;
