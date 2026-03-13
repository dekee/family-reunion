import type { AgeGroup } from '../types';
import type { FeeSchedule } from '../api';

interface AgeGroupConfig {
  label: string;
  shortLabel: string;
  ageRange: string;
  fee: number;
}

// Default fees (in dollars) — overridden by server via /api/payments/fees
const DEFAULT_FEES: FeeSchedule = {
  ADULT: 10000,
  SPOUSE: 10000,
  CHILD: 5000,
  INFANT: 1500,
};

let currentFees: FeeSchedule = DEFAULT_FEES;

export function setFees(fees: FeeSchedule) {
  currentFees = fees;
}

export function getFees(): FeeSchedule {
  return currentFees;
}

function buildAgeGroups(): Record<AgeGroup, AgeGroupConfig> {
  return {
    ADULT:  { label: 'Adult',   shortLabel: 'Adult',   ageRange: '18+',  fee: currentFees.ADULT / 100 },
    SPOUSE: { label: 'Spouse',  shortLabel: 'Spouse',  ageRange: '18+',  fee: currentFees.SPOUSE / 100 },
    CHILD:  { label: 'Child',   shortLabel: 'Child',   ageRange: '6–17', fee: currentFees.CHILD / 100 },
    INFANT: { label: 'Under 5', shortLabel: 'Under 5', ageRange: '0–5',  fee: currentFees.INFANT / 100 },
  };
}

export function getAgeGroups(): Record<AgeGroup, AgeGroupConfig> {
  return buildAgeGroups();
}

// Legacy exports — use getAgeGroups() for dynamic fees
export const AGE_GROUPS = buildAgeGroups();

export function ageLabel(ageGroup: string): string {
  return getAgeGroups()[ageGroup as AgeGroup]?.label ?? ageGroup;
}

export function ageLabelWithRange(ageGroup: string): string {
  const config = getAgeGroups()[ageGroup as AgeGroup];
  if (!config) return ageGroup;
  return `${config.label} (${config.ageRange})`;
}

export function ageLabelWithFee(ageGroup: string): string {
  const config = getAgeGroups()[ageGroup as AgeGroup];
  if (!config) return ageGroup;
  return `${config.label} ${config.ageRange} ($${config.fee})`;
}

export function feeForAge(ageGroup: string): number {
  return getAgeGroups()[ageGroup as AgeGroup]?.fee ?? 0;
}

export const ADULT_FEE = DEFAULT_FEES.ADULT / 100;
export const CHILD_FEE = DEFAULT_FEES.CHILD / 100;
export const INFANT_FEE = DEFAULT_FEES.INFANT / 100;
