import type { AgeGroup } from '../types';

interface AgeGroupConfig {
  label: string;
  shortLabel: string;
  ageRange: string;
  fee: number;
}

export const AGE_GROUPS: Record<AgeGroup, AgeGroupConfig> = {
  ADULT:  { label: 'Adult',   shortLabel: 'Adult',   ageRange: '18+',  fee: 100 },
  SPOUSE: { label: 'Spouse',  shortLabel: 'Spouse',  ageRange: '18+',  fee: 100 },
  CHILD:  { label: 'Child',   shortLabel: 'Child',   ageRange: '6–17', fee: 50 },
  INFANT: { label: 'Under 5', shortLabel: 'Under 5', ageRange: '0–5',  fee: 0 },
};

export function ageLabel(ageGroup: string): string {
  return AGE_GROUPS[ageGroup as AgeGroup]?.label ?? ageGroup;
}

export function ageLabelWithRange(ageGroup: string): string {
  const config = AGE_GROUPS[ageGroup as AgeGroup];
  if (!config) return ageGroup;
  return `${config.label} (${config.ageRange})`;
}

export function ageLabelWithFee(ageGroup: string): string {
  const config = AGE_GROUPS[ageGroup as AgeGroup];
  if (!config) return ageGroup;
  if (config.fee === 0) return `${config.label} (Free)`;
  return `${config.label} ${config.ageRange} ($${config.fee})`;
}

export function feeForAge(ageGroup: string): number {
  return AGE_GROUPS[ageGroup as AgeGroup]?.fee ?? 0;
}

export const ADULT_FEE = AGE_GROUPS.ADULT.fee;
export const CHILD_FEE = AGE_GROUPS.CHILD.fee;
export const INFANT_FEE = AGE_GROUPS.INFANT.fee;
