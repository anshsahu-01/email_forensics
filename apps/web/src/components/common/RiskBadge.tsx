import React from 'react';

type RiskLevel = 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';

interface RiskBadgeProps {
  score?: number | null;
  level?: RiskLevel | string | null;
  className?: string;
}

export function getRiskLevel(score: number | null | undefined): RiskLevel {
  if (score === null || score === undefined) return 'UNKNOWN';
  if (score >= 70) return 'HIGH';
  if (score >= 40) return 'MEDIUM';
  return 'LOW';
}

export default function RiskBadge({ score, level, className = '' }: RiskBadgeProps) {
  const riskLevel = level ? (level.toUpperCase() as RiskLevel) : getRiskLevel(score);

  const styles = {
    HIGH: 'bg-red-50 text-red-700 border-red-200',
    MEDIUM: 'bg-amber-50 text-amber-700 border-amber-200',
    LOW: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    UNKNOWN: 'bg-slate-50 text-slate-700 border-slate-200',
  };

  const currentStyle = styles[riskLevel as keyof typeof styles] || styles.UNKNOWN;

  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-[10px] font-bold tracking-wider ${currentStyle} ${className}`}>
      {riskLevel}
    </span>
  );
}
