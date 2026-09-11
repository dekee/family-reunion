import { describe, it, expect } from 'vitest';
import {
  ALL_SIZES,
  ONESIE_SIZES,
  SIZE_LABELS,
  UNISEX_SIZES,
  YOUTH_SIZES,
  isValidSizeFor,
  sizeLabel,
  sizesForAgeGroup,
} from './tshirtSizes';

describe('T-shirt size constants (mirror of backend TshirtSize enum)', () => {
  it('adults and spouses get the seven unisex sizes', () => {
    expect(sizesForAgeGroup('ADULT')).toEqual(['S', 'M', 'L', 'XL', 'XXL', 'XXXL', 'XXXXL']);
    expect(sizesForAgeGroup('SPOUSE')).toEqual(sizesForAgeGroup('ADULT'));
  });

  it('children get youth sizes', () => {
    expect(sizesForAgeGroup('CHILD')).toEqual(['YS', 'YM', 'YL', 'YXL']);
  });

  it('infants get onesie sizes only', () => {
    expect(sizesForAgeGroup('INFANT')).toEqual(['NEWBORN', 'M0_3', 'M3_6', 'M6_9', 'M9_12']);
  });

  it('every size appears in exactly one category', () => {
    expect(UNISEX_SIZES.length + YOUTH_SIZES.length + ONESIE_SIZES.length).toBe(ALL_SIZES.length);
    expect(new Set(ALL_SIZES).size).toBe(ALL_SIZES.length);
  });

  it('every size has a human label', () => {
    for (const s of ALL_SIZES) {
      expect(SIZE_LABELS[s]).toBeTruthy();
    }
    expect(sizeLabel('XXL')).toBe('2XL');
    expect(sizeLabel('M0_3')).toBe('0-3 mths');
    expect(sizeLabel(null)).toBe('');
  });

  it('validates sizes against the age group', () => {
    expect(isValidSizeFor('YS', 'ADULT')).toBe(false);
    expect(isValidSizeFor('L', 'INFANT')).toBe(false);
    expect(isValidSizeFor('NEWBORN', 'CHILD')).toBe(false);
    expect(isValidSizeFor('XXXXL', 'SPOUSE')).toBe(true);
    expect(isValidSizeFor('M9_12', 'INFANT')).toBe(true);
  });
});
