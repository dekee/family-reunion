import { describe, it, expect } from 'vitest';
import {
  ALL_SIZES,
  ONESIE_SIZES,
  SIZE_LABELS,
  UNISEX_SIZES,
  YOUTH_SIZES,
  isKnownSize,
  sizeGroupsForAgeGroup,
  sizeLabel,
  suggestedCategoryFor,
} from './tshirtSizes';

describe('T-shirt size constants (mirror of backend TshirtSize enum)', () => {
  it('offers seven unisex, four youth and five onesie sizes', () => {
    expect(UNISEX_SIZES).toEqual(['S', 'M', 'L', 'XL', 'XXL', 'XXXL', 'XXXXL']);
    expect(YOUTH_SIZES).toEqual(['YS', 'YM', 'YL', 'YXL']);
    expect(ONESIE_SIZES).toEqual(['NEWBORN', 'M0_3', 'M3_6', 'M6_9', 'M9_12']);
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

  it('suggests a group by age but always lists all three groups', () => {
    expect(suggestedCategoryFor('ADULT')).toBe('UNISEX');
    expect(suggestedCategoryFor('SPOUSE')).toBe('UNISEX');
    expect(suggestedCategoryFor('CHILD')).toBe('YOUTH');
    expect(suggestedCategoryFor('INFANT')).toBe('ONESIE');

    expect(sizeGroupsForAgeGroup('CHILD')).toEqual(['YOUTH', 'UNISEX', 'ONESIE']);
    expect(sizeGroupsForAgeGroup('INFANT')).toEqual(['ONESIE', 'UNISEX', 'YOUTH']);
    expect(sizeGroupsForAgeGroup('ADULT')).toEqual(['UNISEX', 'YOUTH', 'ONESIE']);
  });

  it('accepts any known size for anyone', () => {
    expect(isKnownSize('YS')).toBe(true);
    expect(isKnownSize('XXXXL')).toBe(true);
    expect(isKnownSize('M9_12')).toBe(true);
    expect(isKnownSize('HUGE')).toBe(false);
  });
});
