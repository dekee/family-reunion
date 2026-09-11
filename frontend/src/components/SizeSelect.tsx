import type { TshirtSize } from '../types';
import { SIZES_BY_CATEGORY, SIZE_LABELS, SIZE_CATEGORY_LABELS, sizeGroupsForAgeGroup } from '../constants/tshirtSizes';

interface SizeSelectProps {
  /** Only affects ordering: the group most likely to fit this age is listed first. */
  ageGroup: string;
  value: TshirtSize | '' | null | undefined;
  onChange: (size: TshirtSize) => void;
  disabled?: boolean;
  className?: string;
  placeholder?: string;
  ariaLabel?: string;
}

/** Dropdown of every T-shirt size, grouped Adult / Youth / Onesie, likeliest group first. */
export default function SizeSelect({
  ageGroup,
  value,
  onChange,
  disabled = false,
  className = '',
  placeholder = 'Size…',
  ariaLabel = 'T-shirt size',
}: SizeSelectProps) {
  const missing = !value;

  return (
    <select
      className={`size-select ${missing ? 'size-missing' : ''} ${className}`.trim()}
      value={value ?? ''}
      onChange={e => onChange(e.target.value as TshirtSize)}
      onClick={e => e.stopPropagation()}
      disabled={disabled}
      aria-label={ariaLabel}
    >
      <option value="" disabled>{placeholder}</option>
      {sizeGroupsForAgeGroup(ageGroup).map(cat => (
        <optgroup key={cat} label={SIZE_CATEGORY_LABELS[cat]}>
          {SIZES_BY_CATEGORY[cat].map(s => (
            <option key={s} value={s}>{SIZE_LABELS[s]}</option>
          ))}
        </optgroup>
      ))}
    </select>
  );
}
