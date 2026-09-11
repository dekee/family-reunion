import type { AgeGroup, TshirtSize } from '../types';

/**
 * T-shirt sizes offered to paid attendees. Size values are the enum names the backend
 * stores and validates (see TshirtSize.kt) — keep both files in sync.
 *
 * Anyone may pick any size. Age group only decides which group is listed first in the
 * dropdown (a big kid may need an adult shirt, a small adult a youth one).
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
  UNISEX: 'Adult (Unisex)',
  YOUTH: 'Youth',
  ONESIE: 'Onesie',
};

/** Pseudo line item the backend creates for angel donations — never has a size. */
export const ANGEL_LINE_ITEM_NAME = 'Angel Contribution';

/** The size group most likely to fit; used only to order the dropdown. */
export function suggestedCategoryFor(ageGroup: string): SizeCategory {
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

/** All size groups, with the suggested group for this age first. */
export function sizeGroupsForAgeGroup(ageGroup: string): SizeCategory[] {
  const suggested = suggestedCategoryFor(ageGroup);
  const rest: SizeCategory[] = (['UNISEX', 'YOUTH', 'ONESIE'] as SizeCategory[]).filter(c => c !== suggested);
  return [suggested, ...rest];
}

export function sizeLabel(size: string | null | undefined): string {
  if (!size) return '';
  return SIZE_LABELS[size as TshirtSize] ?? size;
}

export function isKnownSize(size: string): size is TshirtSize {
  return (ALL_SIZES as string[]).includes(size);
}
