import type { TshirtSize } from '../types';
import { sizesForAgeGroup, SIZE_LABELS, SIZE_CATEGORY_LABELS, sizeCategoryFor } from '../constants/tshirtSizes';

interface SizeSelectProps {
  ageGroup: string;
  value: TshirtSize | '' | null | undefined;
  onChange: (size: TshirtSize) => void;
  disabled?: boolean;
  className?: string;
  placeholder?: string;
  ariaLabel?: string;
}

/** Dropdown of T-shirt sizes appropriate for an attendee's age group. */
export default function SizeSelect({
  ageGroup,
  value,
  onChange,
  disabled = false,
  className = '',
  placeholder,
  ariaLabel,
}: SizeSelectProps) {
  const sizes = sizesForAgeGroup(ageGroup);
  const categoryLabel = SIZE_CATEGORY_LABELS[sizeCategoryFor(ageGroup)];
  const missing = !value;

  return (
    <select
      className={`size-select ${missing ? 'size-missing' : ''} ${className}`.trim()}
      value={value ?? ''}
      onChange={e => onChange(e.target.value as TshirtSize)}
      onClick={e => e.stopPropagation()}
      disabled={disabled}
      aria-label={ariaLabel ?? `${categoryLabel} T-shirt size`}
    >
      <option value="" disabled>
        {placeholder ?? `${categoryLabel} size…`}
      </option>
      {sizes.map(s => (
        <option key={s} value={s}>
          {SIZE_LABELS[s]}
        </option>
      ))}
    </select>
  );
}
