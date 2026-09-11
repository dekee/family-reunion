import type { AgeGroup, TshirtSize } from '../types';

/**
 * T-shirt sizes offered per age group. Size values are the enum names the backend
 * stores and validates (see TshirtSize.kt) — keep both files in sync.
 */
export type SizeCategory = 'UNISEX' | 'YOUTH' | 'ONESIE';

export const UNISEX_SIZES: TshirtSize[] = ['S', 'M', 'L', 'XL', 'XXL', 'XXXL', 'XXXXL'];
export const YOUTH_SIZES: TshirtSize[] = ['YS', 'YM', 'YL', 'YXL'];
export const ONESIE_SIZES: TshirtSize[] = ['NEWBORN', 'M0_3', 'M3_6', 'M6_9', 'M9_12'];

export const SIZES_BY_CATEGORY: Record<SizeCategory, TshirtSize[]> = {
  UNISEX: UNISEX_SIZES,
  YOUTH: YOUTH_SIZES,
  ONESIE: ONESIE_SIZES,
};

export const ALL_SIZES: TshirtSize[] = [...UNISEX_SIZES, ...YOUTH_SIZES, ...ONESIE_SIZES];

export const SIZE_LABELS: Record<TshirtSize, string> = {
  S: 'S',
  M: 'M',
  L: 'L',
  XL: 'XL',
  XXL: '2XL',
  XXXL: '3XL',
  XXXXL: '4XL',
  YS: 'Youth S',
  YM: 'Youth M',
  YL: 'Youth L',
  YXL: 'Youth XL',
  NEWBORN: 'Newborn',
  M0_3: '0-3 mths',
  M3_6: '3-6 mths',
  M6_9: '6-9 mths',
  M9_12: '9-12 mths',
};

export const SIZE_CATEGORY_LABELS: Record<SizeCategory, string> = {
  UNISEX: 'Unisex',
  YOUTH: 'Youth',
  ONESIE: 'Onesie',
};

/** Pseudo line item the backend creates for angel donations — never has a size. */
export const ANGEL_LINE_ITEM_NAME = 'Angel Contribution';

export function sizeCategoryFor(ageGroup: string): SizeCategory {
  switch (ageGroup as AgeGroup) {
    case 'CHILD':
      return 'YOUTH';
    case 'INFANT':
      return 'ONESIE';
    case 'ADULT':
    case 'SPOUSE':
    default:
      return 'UNISEX';
  }
}

export function sizesForAgeGroup(ageGroup: string): TshirtSize[] {
  return SIZES_BY_CATEGORY[sizeCategoryFor(ageGroup)];
}

export function sizeLabel(size: string | null | undefined): string {
  if (!size) return '';
  return SIZE_LABELS[size as TshirtSize] ?? size;
}

export function isValidSizeFor(size: string, ageGroup: string): boolean {
  return (sizesForAgeGroup(ageGroup) as string[]).includes(size);
}
