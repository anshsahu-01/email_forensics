import React from 'react';
import { ArrowRight, LucideIcon } from 'lucide-react';

interface StatCardProps {
  label: string;
  value: string | number;
  description?: string;
  icon: LucideIcon;
  iconClassName?: string;
  iconBgClassName?: string;
}

export default function StatCard({
  label,
  value,
  description,
  icon: Icon,
  iconClassName = 'text-blue-600',
  iconBgClassName = 'bg-blue-50',
}: StatCardProps) {
  return (
    <div className="relative overflow-hidden rounded-xl border border-slate-200 bg-white px-5 py-4 transition-all hover:border-slate-300 hover:shadow-sm">
      
      {/* Top Row */}
      <div className="flex items-center justify-between">
        
        {/* Icon */}
        <div className={`flex h-10 w-10 items-center justify-center rounded-full ${iconBgClassName}`}>
          <Icon className={`h-5 w-5 ${iconClassName}`} />
        </div>

        {/* Arrow */}
        <div className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-50 text-slate-400">
          <ArrowRight className="h-4 w-4" />
        </div>
      </div>

      {/* Content */}
      <div className="mt-3">
        <p className="text-[11px] font-bold uppercase tracking-wide text-slate-500">
          {label}
        </p>

        <p className="mt-1 text-3xl font-bold tracking-tight text-slate-900">
          {value}
        </p>

        {description && (
          <p className="mt-1 text-[11px] font-medium text-slate-400">
            {description}
          </p>
        )}
      </div>

    </div>
  );
}