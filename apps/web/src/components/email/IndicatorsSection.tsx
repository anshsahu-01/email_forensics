import React from 'react';
import { EmailIndicator } from '@/types';
import { Link2, ShieldAlert, ShieldCheck, Globe, ExternalLink } from 'lucide-react';

interface IndicatorsSectionProps {
  indicators: EmailIndicator[] | null;
}

export default function IndicatorsSection({ indicators }: IndicatorsSectionProps) {
  if (!indicators || indicators.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
        <Link2 className="mx-auto h-8 w-8 text-slate-300" />
        <h3 className="mt-4 text-sm font-semibold text-slate-900">No Indicators Extracted</h3>
        <p className="mt-1 text-sm text-slate-500">No URLs or IOCs were found in this email.</p>
      </div>
    );
  }

  const urls = indicators.filter(i => i.type === 'URL');
  const others = indicators.filter(i => i.type !== 'URL');

  return (
    <div className="space-y-6">
      {urls.length > 0 && (
        <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
          <div className="bg-slate-50 px-6 py-4 border-b border-slate-200">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Link2 className="h-4 w-4 text-indigo-500" />
              URL Indicators ({urls.length})
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
        <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
          <div className="bg-slate-50 px-6 py-4 border-b border-slate-200">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Globe className="h-4 w-4 text-indigo-500" />
              Other Indicators ({others.length})
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
    <div className="p-6 transition-colors hover:bg-slate-50/50">
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-6">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 mb-2">
             <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
              {indicator.type}
            </span>
            {isMalicious && (
              <span className="text-[10px] font-bold uppercase tracking-wider text-red-600 bg-red-50 px-2 py-0.5 rounded border border-red-100 flex items-center gap-1">
                <ShieldAlert className="h-3 w-3" /> Malicious
              </span>
            )}
             {isClean && (
              <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-100 flex items-center gap-1">
                <ShieldCheck className="h-3 w-3" /> Clean
              </span>
            )}
          </div>
          <p className="text-sm font-mono text-slate-700 break-all font-semibold">
            {indicator.value}
          </p>
        </div>

        {indicator.vtReputation !== null && (
          <div className="shrink-0 flex flex-col items-end gap-2">
            <div className="flex items-center gap-3">
              <VTStat label="Malicious" count={indicator.vtMalicious} color="text-red-500" />
              <VTStat label="Undetected" count={indicator.vtUndetected} color="text-slate-400" />
            </div>
            <a
              href={`https://www.virustotal.com/gui/search/${encodeURIComponent(indicator.value)}`}
              target="_blank"
              rel="noopener noreferrer"
              className="text-[10px] font-bold text-indigo-600 hover:text-indigo-700 flex items-center gap-1 uppercase tracking-wider"
            >
              View on VirusTotal <ExternalLink className="h-2.5 w-2.5" />
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
    <div className="text-center">
      <p className={`text-lg font-bold leading-none ${color}`}>{count}</p>
      <p className="text-[9px] font-bold text-slate-400 uppercase tracking-tighter mt-1">{label}</p>
    </div>
  );
}
