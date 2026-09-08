'use client';

import { EmailCase, EmailIndicator } from '@/types';
import { getRiskLevel } from '@/components/common/RiskBadge';
import { BarChart, Bar, XAxis, YAxis, Tooltip as RechartsTooltip, ResponsiveContainer, Cell, LabelList } from 'recharts';

export function RiskDistributionChart({ cases }: { cases: EmailCase[] }) {
  const counts = { CRITICAL: 0, SUSPICIOUS: 0, SAFE: 0, UNKNOWN: 0 };
  
  cases.forEach(c => {
    const risk = getRiskLevel(c.threatScore);
    if (risk === 'HIGH') counts.CRITICAL++;
    else if (risk === 'MEDIUM') counts.SUSPICIOUS++;
    else if (risk === 'LOW') counts.SAFE++;
    else counts.UNKNOWN++;
  });

  const data = [
    { name: 'CRITICAL', value: counts.CRITICAL, color: '#ef4444' },
    { name: 'SUSPICIOUS', value: counts.SUSPICIOUS, color: '#f59e0b' },
    { name: 'SAFE', value: counts.SAFE, color: '#10b981' },
    { name: 'UNKNOWN', value: counts.UNKNOWN, color: '#64748b' }
  ].filter(d => d.value > 0);

  if (data.length === 0) {
    return <div className="flex justify-center p-12 text-[10px] font-black uppercase tracking-widest text-slate-400">No Data</div>;
  }

  return (
    <div className="w-full py-4" style={{ height: Math.max(220, data.length * 50) }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="vertical" margin={{ top: 0, right: 30, left: 10, bottom: 0 }}>
          <XAxis type="number" hide />
          <YAxis 
            dataKey="name" 
            type="category" 
            width={100} 
            axisLine={false} 
            tickLine={false} 
            tick={{ fontSize: 10, fill: '#475569', fontWeight: 900 }} 
          />
          <RechartsTooltip 
            cursor={{ fill: '#f8fafc' }} 
            contentStyle={{ fontSize: '11px', fontWeight: 'bold', borderRadius: '2px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)', textTransform: 'uppercase', letterSpacing: '0.1em' }} 
            itemStyle={{ color: '#0f172a' }}
          />
          <Bar dataKey="value" radius={[0, 2, 2, 0]} barSize={24}>
            <LabelList dataKey="value" position="right" style={{ fontSize: 11, fill: '#0f172a', fontWeight: 900 }} />
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function IndicatorDistributionChart({ indicators }: { indicators: EmailIndicator[] }) {
  const typeCounts: Record<string, number> = {};
  
  indicators.forEach(i => {
    const t = i.type.toUpperCase();
    typeCounts[t] = (typeCounts[t] || 0) + 1;
  });

  const data = Object.entries(typeCounts)
    .map(([type, count]) => ({ type, count }))
    .sort((a, b) => b.count - a.count);

  if (data.length === 0) {
    return <div className="flex justify-center p-12 text-[10px] font-black uppercase tracking-widest text-slate-400">No Indicators</div>;
  }

  return (
    <div className="w-full py-4" style={{ height: Math.max(220, data.length * 50) }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="vertical" margin={{ top: 0, right: 30, left: 10, bottom: 0 }}>
          <XAxis type="number" hide />
          <YAxis 
            dataKey="type" 
            type="category" 
            width={80} 
            axisLine={false} 
            tickLine={false} 
            tick={{ fontSize: 10, fill: '#475569', fontWeight: 900, fontFamily: 'monospace' }} 
          />
          <RechartsTooltip 
            cursor={{ fill: '#f8fafc' }} 
            contentStyle={{ fontSize: '11px', fontWeight: 'bold', borderRadius: '2px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)', textTransform: 'uppercase', letterSpacing: '0.1em' }} 
            itemStyle={{ color: '#0f172a' }}
          />
          <Bar dataKey="count" fill="#4f46e5" radius={[0, 2, 2, 0]} barSize={24}>
            <LabelList dataKey="count" position="right" style={{ fontSize: 11, fill: '#0f172a', fontWeight: 900 }} />
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill="#4f46e5" />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
