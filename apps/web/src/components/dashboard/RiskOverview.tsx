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
          <Icon className={`h-3.5 w-3.5 ${className}`} />
          <span className="text-[10px] font-bold uppercase tracking-widest text-slate-500">
            {label}
          </span>
        </div>

        <span className="text-sm font-bold text-slate-900">
          {value}
        </span>
      </div>

      <div className="h-1 overflow-hidden bg-slate-100">
        <div
          className={`h-full transition-all ${barClassName}`}
          style={{ width: `${percentage}%` }}
        />
      </div>

      <p className="mt-3 text-[10px] font-medium text-slate-400">
        {percentage}% of investigations
      </p>
    </div>
  );
}

