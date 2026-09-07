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
    HIGH: 'text-red-600 border-red-200 bg-red-50',
    MEDIUM: 'text-amber-600 border-amber-200 bg-amber-50',
    LOW: 'text-emerald-600 border-emerald-200 bg-emerald-50',
    UNKNOWN: 'text-slate-600 border-slate-200 bg-slate-50',
  };

  const currentStyle = styles[riskLevel as keyof typeof styles] || styles.UNKNOWN;

  return (
    <span className={`inline-flex items-center border px-2 py-0.5 text-[10px] font-bold tracking-widest uppercase ${currentStyle} ${className}`}>
      {riskLevel}
    </span>
  );
}
