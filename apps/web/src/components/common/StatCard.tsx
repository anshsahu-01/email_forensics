import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  label: string;
  value: string | number;
  description?: string;
  icon: LucideIcon;
  iconClassName?: string;
  trend?: {
    value: number;
    isUp: boolean;
  };
}

export default function StatCard({
  label,
  value,
  description,
  icon: Icon,
  iconClassName = 'text-indigo-600',
}: StatCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:shadow-md">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-bold tracking-tight text-slate-900">
            {value}
          </p>
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-50">
          <Icon className={`h-5 w-5 ${iconClassName}`} />
        </div>
      </div>

      {description && (
        <p className="mt-3 text-[11px] text-slate-400">
          {description}
        </p>
      )}
    </div>
  );
}
