export const ADULT_FEE = 100;
export const CHILD_FEE = 50;
export const INFANT_FEE = 0;

export function feeForAge(ageGroup: string): number {
  if (ageGroup === 'ADULT' || ageGroup === 'SPOUSE') return ADULT_FEE;
  if (ageGroup === 'CHILD') return CHILD_FEE;
  return INFANT_FEE;
}
