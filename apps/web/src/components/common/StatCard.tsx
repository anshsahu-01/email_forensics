import React from 'react';
import { LucideIcon } from 'lucide-react';

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
  iconClassName = 'text-slate-900',
}: StatCardProps) {
  return (
    <div className="border border-slate-200 bg-white p-6 transition-colors hover:border-slate-300">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-widest text-slate-500">{label}</p>
          <p className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
            {value}
          </p>
        </div>

        <div className="flex h-8 w-8 items-center justify-center text-slate-400">
          <Icon className={`h-5 w-5 ${iconClassName}`} />
        </div>
      </div>

      {description && (
        <p className="mt-4 text-xs font-medium text-slate-400">
          {description}
        </p>
      )}
    </div>
  );
}

