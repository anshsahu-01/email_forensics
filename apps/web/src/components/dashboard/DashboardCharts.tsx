'use client';

import { EmailCase } from '@/types';
import { getRiskLevel } from '@/components/common/RiskBadge';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';

export function TopSuspiciousLocations({ history }: { history: EmailCase[] }) {
  const suspiciousCases = history.filter(item => {
    const risk = getRiskLevel(item.threatScore);
    return risk === 'HIGH' || risk === 'MEDIUM';
  });

  const locationCounts: Record<string, number> = {};
  suspiciousCases.forEach(c => {
    if (c.geoCountry) {
      const loc = c.geoCity ? `${c.geoCity}, ${c.geoCountry}` : c.geoCountry;
      locationCounts[loc] = (locationCounts[loc] || 0) + 1;
    }
  });

  const data = Object.entries(locationCounts)
    .map(([location, count]) => ({ location, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);

  if (data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-center text-slate-400">
        <p className="text-[10px] font-black uppercase tracking-widest">Insufficient geographic data</p>
      </div>
    );
  }

  const maxCount = data[0]?.count || 1;

  return (
    <div className="flex flex-col gap-6 py-2">
      {data.map((item, i) => (
        <div key={item.location} className="flex flex-col gap-2">
          <div className="flex justify-between items-end">
            <div className="flex gap-4 items-baseline">
              <span className="text-[10px] font-black text-slate-300 w-4 tracking-widest">{(i + 1).toString().padStart(2, '0')}</span>
              <div className="flex flex-col">
                <span className="text-[11px] font-black uppercase tracking-widest text-slate-900">{item.location}</span>
                <span className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mt-0.5">Network Infrastructure Location</span>
              </div>
            </div>
            <span className="text-[11px] font-black text-slate-900">{item.count}</span>
          </div>
          <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden flex">
            <div 
              className="h-full bg-indigo-600 rounded-full transition-all duration-1000 ease-out" 
              style={{ width: `${(item.count / maxCount) * 100}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

export function AuthenticationOverview({ history }: { history: EmailCase[] }) {
  const spfCounts = { PASS: 0, FAIL: 0, OTHER: 0 };
  const dkimCounts = { PASS: 0, FAIL: 0, OTHER: 0 };
  const dmarcCounts = { PASS: 0, FAIL: 0, OTHER: 0 };

  history.forEach(c => {
    if (!c.header) return;
    const spf = (c.header.spfStatus || 'NONE').toUpperCase();
    if (spf.includes('PASS')) spfCounts.PASS++;
    else if (spf.includes('FAIL') || spf.includes('SOFTFAIL') || spf.includes('PERMERROR')) spfCounts.FAIL++;
    else spfCounts.OTHER++;

    const dkim = (c.header.dkimStatus || 'NONE').toUpperCase();
    if (dkim.includes('PASS')) dkimCounts.PASS++;
    else if (dkim.includes('FAIL') || dkim.includes('SOFTFAIL') || dkim.includes('PERMERROR')) dkimCounts.FAIL++;
    else dkimCounts.OTHER++;

    const dmarc = (c.header.dmarcStatus || 'NONE').toUpperCase();
    if (dmarc.includes('PASS')) dmarcCounts.PASS++;
    else if (dmarc.includes('FAIL') || dmarc.includes('SOFTFAIL') || dmarc.includes('PERMERROR')) dmarcCounts.FAIL++;
    else dmarcCounts.OTHER++;
  });

  const renderAuthWidget = (counts: { PASS: number, FAIL: number, OTHER: number }, title: string) => {
    const total = counts.PASS + counts.FAIL + counts.OTHER;
    const passPercent = total > 0 ? Math.round((counts.PASS / total) * 100) : 0;
    
    const data = [
      { name: 'PASS', value: counts.PASS, color: '#10b981' },
      { name: 'FAIL', value: counts.FAIL, color: '#ef4444' },
      { name: 'OTHER', value: counts.OTHER, color: '#64748b' }
    ].filter(d => d.value > 0);

    return (
      <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6 bg-slate-50 border border-slate-100 p-6">
        <div className="relative h-28 w-28 shrink-0">
          {total > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={data} cx="50%" cy="50%" innerRadius={32} outerRadius={46} stroke="none" dataKey="value" cornerRadius={2}>
                  {data.map((entry, index) => <Cell key={index} fill={entry.color} />)}
                </Pie>
                <RechartsTooltip 
                  contentStyle={{ fontSize: '10px', padding: '6px 10px', border: '1px solid #e2e8f0', borderRadius: '2px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)', fontWeight: 900, textTransform: 'uppercase', letterSpacing: '0.1em' }} 
                  itemStyle={{ color: '#0f172a' }} 
                />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="w-full h-full rounded-full border-4 border-slate-200" />
          )}
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
            <span className="text-[11px] font-black text-slate-900">{total > 0 ? `${passPercent}%` : '-'}</span>
          </div>
        </div>
        
        <div className="flex-1 flex flex-col justify-center w-full sm:w-auto">
          <div className="mb-4 text-center sm:text-left">
            <h3 className="text-[12px] font-black uppercase tracking-[0.2em] text-slate-900">{title}</h3>
            <p className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mt-1">{counts.PASS} / {total} PASS</p>
          </div>
          <div className="flex flex-col gap-2 w-full">
            <div className="flex justify-between items-center text-[9px] font-black tracking-widest uppercase">
              <span className="text-emerald-600">PASS</span>
              <span className="text-slate-900">{counts.PASS}</span>
            </div>
            <div className="flex justify-between items-center text-[9px] font-black tracking-widest uppercase">
              <span className="text-red-600">FAIL</span>
              <span className="text-slate-900">{counts.FAIL}</span>
            </div>
            <div className="flex justify-between items-center text-[9px] font-black tracking-widest uppercase">
              <span className="text-slate-500">OTHER</span>
              <span className="text-slate-900">{counts.OTHER}</span>
            </div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {renderAuthWidget(spfCounts, 'SPF')}
      {renderAuthWidget(dkimCounts, 'DKIM')}
      {renderAuthWidget(dmarcCounts, 'DMARC')}
    </div>
  );
}
