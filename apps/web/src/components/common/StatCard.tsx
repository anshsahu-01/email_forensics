import React from 'react';
import { ArrowRight, LucideIcon } from 'lucide-react';

interface StatCardProps {
  label: string;
  value: string | number;
  description?: string;
  icon: LucideIcon;
  iconClassName?: string;
}

export default function StatCard({
  label,
  value,
  description,
  icon: Icon,
  iconClassName = 'text-blue-600',
}: StatCardProps) {
  return (
    <div className="relative overflow-hidden rounded-xl border border-slate-200 bg-white px-5 py-4 transition-all hover:border-slate-300 hover:shadow-sm">
      
      {/* Top Row */}
      <div className="flex items-center justify-between">
        
        {/* Icon */}
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-50">
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

      {/* Sparkline */}
      <svg
        className="absolute bottom-3 right-4 h-12 w-24"
        viewBox="0 0 100 45"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M2 38
             C10 34, 15 27, 23 29
             C31 31, 36 25, 43 26
             C51 27, 55 28, 62 23
             C69 18, 73 10, 80 15
             C87 20, 91 5, 98 8"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          className="text-blue-300"
        />
      </svg>
    </div>
  );
}