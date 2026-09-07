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
    } catch (err) {
      setError('Failed to load intelligence data.');
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

  // Aggregate all indicators from all cases
  const allIndicators = cases.flatMap(c =>
    (c.indicators || []).map(i => ({ ...i, caseId: c.id, caseSubject: c.header?.subject }))
  );

  // Deduplicate indicators by value, keeping the most "malicious" result if multiple exist
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
    <div className="min-h-full bg-slate-50 py-8">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Threat Intelligence
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              Aggregated indicators of compromise (IOCs) across all investigations.
            </p>
          </div>

          <button
            onClick={loadData}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>

        {/* Intelligence Stats */}
        <div className="grid gap-4 mb-8 sm:grid-cols-3">
          <IntelligenceStat label="Total Indicators" value={uniqueIndicators.length} color="text-indigo-600" />
          <IntelligenceStat label="Malicious Flags" value={maliciousCount} color="text-red-500" />
          <IntelligenceStat label="Unique Domains/URLs" value={uniqueIndicators.filter(i => i.type === 'URL').length} color="text-indigo-600" />
        </div>

        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search indicators..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-4 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition"
            />
          </div>
        </div>

        {error && (
          <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-100 bg-red-50 p-4 text-red-800">
            <AlertCircle className="h-5 w-5" />
            <p className="text-sm font-medium">{error}</p>
          </div>
        )}

        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/50 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  <th className="px-6 py-4">Indicator</th>
                  <th className="px-6 py-4">Type</th>
                  <th className="px-6 py-4">Reputation</th>
                  <th className="px-6 py-4">Last Seen In</th>
                  <th className="px-6 py-4 text-right">External</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading ? (
                  [1, 2, 3, 4, 5].map((i) => (
                    <tr key={i} className="animate-pulse">
                      <td colSpan={5} className="px-6 py-6">
                        <div className="h-5 rounded bg-slate-100" />
                      </td>
                    </tr>
                  ))
                ) : filteredIndicators.length > 0 ? (
                  filteredIndicators.map((item) => (
                    <tr key={item.id} className="group hover:bg-slate-50/50 transition-colors">
                      <td className="px-6 py-4">
                        <p className="font-mono text-[13px] font-semibold text-slate-700 truncate max-w-md">
                          {item.value}
                        </p>
                      </td>
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-600 border border-slate-200">
                          {item.type === 'URL' ? <Link2 className="h-3 w-3" /> : <Globe className="h-3 w-3" />}
                          {item.type}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        {item.vtMalicious !== null ? (
                          <div className="flex items-center gap-2">
                            {(item.vtMalicious ?? 0) > 0 ? (
                              <span className="flex items-center gap-1 text-red-600 font-bold">
                                <ShieldAlert className="h-3.5 w-3.5" />
                                {item.vtMalicious} Malicious
                              </span>
                            ) : (
                              <span className="flex items-center gap-1 text-emerald-600 font-bold">
                                <ShieldCheck className="h-3.5 w-3.5" />
                                Clean
                              </span>
                            )}
                          </div>
                        ) : (
                          <span className="text-slate-400 italic text-xs">No data</span>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <Link
                          href={`/cases/${item.caseId}`}
                          className="text-indigo-600 hover:underline font-medium truncate max-w-[200px] block"
                        >
                          {item.caseSubject || `Case #${item.caseId}`}
                        </Link>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <a
                          href={`https://www.virustotal.com/gui/search/${encodeURIComponent(item.value)}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1.5 text-slate-400 hover:text-indigo-600 transition"
                        >
                          <ExternalLink className="h-4 w-4" />
                        </a>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5}>
                      <div className="py-12">
                        <EmptyState
                          icon={BarChart3}
                          title="No indicators found"
                          description={searchQuery ? "No results matching your search query." : "No threat intelligence data available yet."}
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
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">{label}</p>
      <p className={`mt-2 text-3xl font-bold tracking-tight ${color}`}>{value}</p>
    </div>
  );
}
