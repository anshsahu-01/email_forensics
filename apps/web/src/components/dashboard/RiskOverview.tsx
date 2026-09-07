import React from 'react';

interface RiskOverviewProps {
  label: string;
  value: number;
  total: number;
  icon: React.ComponentType<{ className?: string }>;
  className: string;
  barClassName: string;
}

export default function RiskOverview({
  label,
  value,
  total,
  icon: Icon,
  className,
  barClassName,
}: RiskOverviewProps) {
  const percentage = total > 0 ? Math.round((value / total) * 100) : 0;

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Icon className={`h-4 w-4 ${className}`} />
          <span className="text-sm font-medium text-slate-700">
            {label}
          </span>
        </div>

        <span className="text-sm font-semibold text-slate-900">
          {value}
        </span>
      </div>

      <div className="h-2 overflow-hidden rounded-full bg-slate-100">
        <div
          className={`h-full rounded-full transition-all ${barClassName}`}
          style={{ width: `${percentage}%` }}
        />
      </div>

      <p className="mt-2 text-[11px] text-slate-400">
        {percentage}% of analyzed cases
      </p>
    </div>
  );
}
