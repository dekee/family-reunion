import './Skeleton.css';

export function Skeleton({ width, height = '1rem', style }: {
  width?: string;
  height?: string;
  style?: React.CSSProperties;
}) {
  return (
    <div
      className="skeleton"
      style={{ width: width || '100%', height, ...style }}
    />
  );
}

export function SkeletonCard({ lines = 3 }: { lines?: number }) {
  return (
    <div className="skeleton-card">
      <Skeleton width="60%" height="1.25rem" />
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} width={i === lines - 1 ? '40%' : '90%'} />
      ))}
    </div>
  );
}

export function SkeletonTable({ rows = 4, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <div className="skeleton-table">
      <div className="skeleton-table-header">
        {Array.from({ length: cols }).map((_, i) => (
          <Skeleton key={i} height="0.85rem" />
        ))}
      </div>
      {Array.from({ length: rows }).map((_, r) => (
        <div key={r} className="skeleton-table-row">
          {Array.from({ length: cols }).map((_, c) => (
            <Skeleton key={c} width={c === 0 ? '70%' : '50%'} />
          ))}
        </div>
      ))}
    </div>
  );
}
