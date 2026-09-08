export function SkeletonRow({ lines = 3 }: { lines?: number }) {
  return (
    <div className="skeleton-card">
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} className="skeleton-line" style={{ width: `${92 - i * 14}%` }} />
      ))}
    </div>
  );
}

export function SkeletonBars({ count = 4 }: { count?: number }) {
  return (
    <div className="kpi-grid skeleton-bars">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="skeleton-kpi" />
      ))}
    </div>
  );
}