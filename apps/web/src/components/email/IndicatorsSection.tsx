import React from 'react';
import { EmailIndicator } from '@/types';
import { Link2, ShieldAlert, ShieldCheck, Globe, ExternalLink } from 'lucide-react';

interface IndicatorsSectionProps {
  indicators: EmailIndicator[] | null;
}

export default function IndicatorsSection({ indicators }: IndicatorsSectionProps) {
  if (!indicators || indicators.length === 0) {
    return (
      <div className="border border-slate-200 bg-white p-20 text-center">
        <Link2 className="mx-auto h-10 w-10 text-slate-200" />
        <h3 className="mt-6 text-[10px] font-black uppercase tracking-[0.3em] text-slate-900">Indicator Deficit</h3>
        <p className="mt-2 text-xs font-bold text-slate-400 uppercase tracking-widest">No URLs or IOCs identified in artifact.</p>
      </div>
    );
  }

  const urls = indicators.filter(i => i.type === 'URL');
  const others = indicators.filter(i => i.type !== 'URL');

  return (
    <div className="space-y-10">
      {urls.length > 0 && (
        <div className="border border-slate-200 bg-white overflow-hidden">
          <div className="bg-slate-50 px-8 py-4 border-b border-slate-200 flex items-center justify-between">
            <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
              <Link2 className="h-4 w-4 text-indigo-600" />
              URL Artifacts ({urls.length})
            </h2>
          </div>
          <div className="divide-y divide-slate-100">
            {urls.map((url) => (
              <IndicatorCard key={url.id} indicator={url} />
            ))}
          </div>
        </div>
      )}

      {others.length > 0 && (
        <div className="border border-slate-200 bg-white overflow-hidden">
          <div className="bg-slate-50 px-8 py-4 border-b border-slate-200 flex items-center justify-between">
            <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
              <Globe className="h-4 w-4 text-indigo-600" />
              Global Indicators ({others.length})
            </h2>
          </div>
          <div className="divide-y divide-slate-100">
            {others.map((indicator) => (
              <IndicatorCard key={indicator.id} indicator={indicator} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function IndicatorCard({ indicator }: { indicator: EmailIndicator }) {
  const isMalicious = (indicator.vtMalicious ?? 0) > 0;
  const isClean = indicator.vtHarmless !== null && (indicator.vtMalicious ?? 0) === 0;

  return (
    <div className="p-8 transition-colors hover:bg-slate-50">
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-10">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-4 mb-4">
             <span className="text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 bg-slate-100 px-3 py-1">
              TYPE: {indicator.type}
            </span>
            {isMalicious && (
              <span className="text-[9px] font-black uppercase tracking-[0.3em] text-red-600 border border-red-600 px-3 py-1 flex items-center gap-2">
                <ShieldAlert className="h-3 w-3" /> VERIFIED MALICIOUS
              </span>
            )}
             {isClean && (
              <span className="text-[9px] font-black uppercase tracking-[0.3em] text-emerald-600 border border-emerald-600 px-3 py-1 flex items-center gap-2">
                <ShieldCheck className="h-3 w-3" /> VERIFIED CLEAN
              </span>
            )}
          </div>
          <p className="text-sm font-mono text-slate-900 break-all font-black bg-slate-50 p-4 border border-slate-200">
            {indicator.value}
          </p>
        </div>

        {indicator.vtReputation !== null && (
          <div className="shrink-0 flex flex-col items-end gap-6">
            <div className="flex items-center gap-8">
              <VTStat label="Malicious" count={indicator.vtMalicious} color="text-red-600" />
              <VTStat label="Clean" count={indicator.vtHarmless} color="text-emerald-600" />
              <VTStat label="Undetected" count={indicator.vtUndetected} color="text-slate-400" />
            </div>
            <a
              href={`https://www.virustotal.com/gui/search/${encodeURIComponent(indicator.value)}`}
              target="_blank"
              rel="noopener noreferrer"
              className="text-[9px] font-black text-indigo-600 hover:text-slate-900 flex items-center gap-2 uppercase tracking-[0.2em] border-b-2 border-indigo-100 hover:border-slate-900 transition-all pb-1"
            >
              Intelligence Node <ExternalLink className="h-3 w-3" />
            </a>
          </div>
        )}
      </div>
    </div>
  );
}

function VTStat({ label, count, color }: { label: string, count: number | null, color: string }) {
  if (count === null) return null;
  return (
    <div className="text-right">
      <p className={`text-2xl font-black tracking-tighter leading-none ${color}`}>{count}</p>
      <p className="text-[8px] font-black text-slate-400 uppercase tracking-[0.3em] mt-2">{label}</p>
    </div>
  );
}
