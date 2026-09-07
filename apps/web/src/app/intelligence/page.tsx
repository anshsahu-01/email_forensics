'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  BarChart3,
  Search,
  ExternalLink,
  ShieldAlert,
  ShieldCheck,
  Link2,
  Globe,
  RefreshCw,
  AlertCircle
} from 'lucide-react';
import { fetchCases } from '@/lib/api';
import { EmailCase } from '@/types';
import EmptyState from '@/components/common/EmptyState';

export default function IntelligencePage() {
  const [cases, setCases] = useState<EmailCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      const data = await fetchCases();
      setCases(data);
    } catch {
      setError('Intelligence node synchronization failed.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData();
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const allIndicators = cases.flatMap(c =>
    (c.indicators || []).map(i => ({ ...i, caseId: c.id, caseSubject: c.header?.subject }))
  );

  const uniqueIndicators = Object.values(
    allIndicators.reduce((acc, curr) => {
      const existing = acc[curr.value];
      if (!existing || (curr.vtMalicious ?? 0) > (existing.vtMalicious ?? 0)) {
        acc[curr.value] = curr;
      }
      return acc;
    }, {} as Record<string, typeof allIndicators[0]>)
  );

  const filteredIndicators = uniqueIndicators.filter(i =>
    i.value.toLowerCase().includes(searchQuery.toLowerCase()) ||
    i.type.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const maliciousCount = uniqueIndicators.filter(i => (i.vtMalicious ?? 0) > 0).length;

  return (
    <div className="min-h-full bg-white py-12">
      <div className="mx-auto max-w-7xl px-8">
        <div className="mb-12 flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between border-b border-slate-900 pb-8">
          <div>
            <p className="mb-2 text-[10px] font-black uppercase tracking-[0.3em] text-indigo-600">Global Indicators</p>
            <h1 className="text-4xl font-black uppercase tracking-tighter text-slate-900 sm:text-5xl">
              Threat Intelligence
            </h1>
            <p className="mt-4 text-sm font-medium text-slate-500 max-w-lg">
              Aggregated repository of Indicators of Compromise (IOCs) identified across the active forensic workspace.
            </p>
          </div>

          <button
            onClick={loadData}
            disabled={loading}
            className="inline-flex items-center justify-center gap-3 border-2 border-slate-900 bg-white px-6 py-3 text-[11px] font-black uppercase tracking-widest text-slate-900 transition hover:bg-slate-900 hover:text-white disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Sync Intelligence
          </button>
        </div>

        {/* Intelligence Stats */}
        <div className="grid gap-px bg-slate-200 border border-slate-200 mb-12 sm:grid-cols-3">
          <IntelligenceStat label="Unique Artifacts" value={uniqueIndicators.length} color="text-slate-900" />
          <IntelligenceStat label="Confirmed Malicious" value={maliciousCount} color="text-red-600" />
          <IntelligenceStat label="Monitored Endpoints" value={uniqueIndicators.filter(i => i.type === 'URL').length} color="text-slate-900" />
        </div>

        <div className="mb-10 flex flex-col gap-6 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Query indicator value or specification..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full border-2 border-slate-200 bg-white py-4 pl-12 pr-4 text-[11px] font-bold uppercase tracking-widest focus:border-slate-900 focus:outline-none transition"
            />
          </div>
        </div>

        {error && (
          <div className="mb-8 flex items-center gap-4 border border-red-200 bg-red-50 p-6 text-red-800">
            <AlertCircle className="h-5 w-5 shrink-0" />
            <div>
              <p className="text-[10px] font-black uppercase tracking-widest">Synchronization Failure</p>
              <p className="text-sm font-medium">{error}</p>
            </div>
          </div>
        )}

        <div className="border border-slate-200 bg-white">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b-2 border-slate-900 bg-slate-50 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
                  <th className="px-8 py-5">Value / Payload</th>
                  <th className="px-8 py-5">Classification</th>
                  <th className="px-8 py-5">Reputation</th>
                  <th className="px-8 py-5">Association</th>
                  <th className="px-8 py-5 text-right">External</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading ? (
                  [1, 2, 3, 4, 5].map((i) => (
                    <tr key={i} className="animate-pulse">
                      <td colSpan={5} className="px-8 py-8">
                        <div className="h-6 bg-slate-50 border border-slate-100" />
                      </td>
                    </tr>
                  ))
                ) : filteredIndicators.length > 0 ? (
                  filteredIndicators.map((item) => (
                    <tr key={item.id} className="group hover:bg-slate-50 transition-colors">
                      <td className="px-8 py-6">
                        <p className="font-mono text-[12px] font-black text-slate-900 truncate max-w-md bg-slate-50 p-2 border border-slate-100">
                          {item.value}
                        </p>
                      </td>
                      <td className="px-8 py-6">
                        <span className="inline-flex items-center gap-2 border border-slate-200 bg-white px-3 py-1 text-[9px] font-black uppercase tracking-[0.2em] text-slate-600 group-hover:border-slate-900 group-hover:text-slate-900 transition-all">
                          {item.type === 'URL' ? <Link2 className="h-3 w-3" /> : <Globe className="h-3 w-3" />}
                          {item.type}
                        </span>
                      </td>
                      <td className="px-8 py-6">
                        {item.vtMalicious !== null ? (
                          <div className="flex items-center gap-2">
                            {(item.vtMalicious ?? 0) > 0 ? (
                              <span className="flex items-center gap-2 text-red-600 font-black text-[10px] uppercase tracking-widest">
                                <ShieldAlert className="h-4 w-4" />
                                {item.vtMalicious} DETECTIONS
                              </span>
                            ) : (
                              <span className="flex items-center gap-2 text-emerald-600 font-black text-[10px] uppercase tracking-widest">
                                <ShieldCheck className="h-4 w-4" />
                                VERIFIED CLEAN
                              </span>
                            )}
                          </div>
                        ) : (
                          <span className="text-slate-400 font-bold uppercase text-[9px] tracking-widest">UNRATED</span>
                        )}
                      </td>
                      <td className="px-8 py-6">
                        <Link
                          href={`/cases/${item.caseId}`}
                          className="text-indigo-600 hover:text-slate-900 font-black text-[10px] uppercase tracking-widest truncate max-w-[200px] block transition-all"
                        >
                          {item.caseSubject || `REF-${item.caseId}`}
                        </Link>
                      </td>
                      <td className="px-8 py-6 text-right">
                        <a
                          href={`https://www.virustotal.com/gui/search/${encodeURIComponent(item.value)}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-2 text-slate-400 hover:text-slate-900 transition-all"
                        >
                          <ExternalLink className="h-4 w-4" />
                        </a>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5}>
                      <div className="py-24">
                        <EmptyState
                          icon={BarChart3}
                          title="No Intelligence Found"
                          description={searchQuery ? "The query parameters matched no indexed indicators." : "No threat intelligence data has been indexed yet."}
                        />
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

function IntelligenceStat({ label, value, color }: { label: string, value: number, color: string }) {
  return (
    <div className="bg-white p-8">
      <p className="text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 mb-3">{label}</p>
      <p className={`text-4xl font-black tracking-tighter ${color}`}>{value}</p>
    </div>
  );
}
