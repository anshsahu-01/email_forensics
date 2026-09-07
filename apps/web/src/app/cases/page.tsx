'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  FileText,
  Search,
  Filter,
  ChevronRight,
  Calendar,
  User,
  Mail,
  RefreshCw,
  AlertCircle
} from 'lucide-react';
import { fetchCases } from '@/lib/api';
import { EmailCase } from '@/types';
import RiskBadge from '@/components/common/RiskBadge';
import EmptyState from '@/components/common/EmptyState';

function formatDate(value: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

export default function CasesPage() {
  const [cases, setCases] = useState<EmailCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const loadCases = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchCases();
      setCases(data);
    } catch (err) {
      console.error('Failed to load cases:', err);
      setError('Forensic database access failed.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadCases();
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const filteredCases = cases.filter(c => {
    const searchLower = searchQuery.toLowerCase();
    return (
      c.header?.subject?.toLowerCase().includes(searchLower) ||
      c.header?.senderFrom?.toLowerCase().includes(searchLower) ||
      c.fileName?.toLowerCase().includes(searchLower) ||
      c.id.toString().includes(searchLower)
    );
  });

  return (
    <div className="min-h-full bg-white py-12">
      <div className="mx-auto max-w-7xl px-8">
        <div className="mb-12 flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between border-b border-slate-900 pb-8">
          <div>
            <p className="mb-2 text-[10px] font-black uppercase tracking-[0.3em] text-indigo-600">Archival Data</p>
            <h1 className="text-4xl font-black uppercase tracking-tighter text-slate-900 sm:text-5xl">
              Investigation Ledger
            </h1>
            <p className="mt-4 text-sm font-medium text-slate-500 max-w-md">
              Complete historical record of all email forensic investigations conducted in this environment.
            </p>
          </div>

          <button
            onClick={loadCases}
            disabled={loading}
            className="inline-flex items-center justify-center gap-3 border-2 border-slate-900 bg-white px-6 py-3 text-[11px] font-black uppercase tracking-widest text-slate-900 transition hover:bg-slate-900 hover:text-white disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Sync Records
          </button>
        </div>

        <div className="mb-10 flex flex-col gap-6 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Filter by Subject, Sender, or Artifact ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full border-2 border-slate-200 bg-white py-4 pl-12 pr-4 text-[11px] font-bold uppercase tracking-widest focus:border-slate-900 focus:outline-none transition"
            />
          </div>

          <button className="inline-flex items-center gap-3 border-2 border-slate-200 bg-white px-6 py-4 text-[11px] font-black uppercase tracking-widest text-slate-900 hover:border-slate-900 transition">
            <Filter className="h-4 w-4" />
            Parameters
          </button>
        </div>

        {error && (
          <div className="mb-8 flex items-center gap-4 border border-red-200 bg-red-50 p-6 text-red-800">
            <AlertCircle className="h-5 w-5 shrink-0" />
            <div>
              <p className="text-[10px] font-black uppercase tracking-widest">Access Denied</p>
              <p className="text-sm font-medium">{error}</p>
            </div>
          </div>
        )}

        <div className="border border-slate-200 bg-white">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b-2 border-slate-900 bg-slate-50 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
                  <th className="px-8 py-5">Case Identifier</th>
                  <th className="px-8 py-5">Source Node</th>
                  <th className="px-8 py-5">Risk Matrix</th>
                  <th className="px-8 py-5">Timestamp</th>
                  <th className="px-8 py-5 text-right">Report</th>
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
                ) : filteredCases.length > 0 ? (
                  filteredCases.map((item) => (
                    <tr
                      key={item.id}
                      className="group transition-colors hover:bg-slate-50"
                    >
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-4">
                          <div className="flex h-12 w-12 shrink-0 items-center justify-center border border-slate-200 bg-white text-slate-400 group-hover:border-slate-900 group-hover:text-slate-900 transition">
                            <FileText className="h-5 w-5" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-xs font-black uppercase tracking-tight text-slate-900 truncate max-w-xs">
                              {item.header?.subject || 'NO SUBJECT HEADER'}
                            </p>
                            <p className="mt-1 text-[9px] font-bold uppercase tracking-widest text-slate-400">
                              REF-{item.id} • {item.fileName}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-3 text-slate-600">
                          <User className="h-4 w-4 text-slate-300" />
                          <span className="text-[11px] font-bold truncate max-w-[180px]">
                            {item.header?.senderFrom || 'UNSPECIFIED'}
                          </span>
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <RiskBadge score={item.threatScore} />
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-3 text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                          <Calendar className="h-4 w-4 text-slate-300" />
                          {formatDate(item.createdAt)}
                        </div>
                      </td>
                      <td className="px-8 py-6 text-right">
                        <Link
                          href={`/cases/${item.id}`}
                          className="inline-flex items-center gap-2 border border-slate-900 px-4 py-2 text-[10px] font-black uppercase tracking-widest text-slate-900 transition hover:bg-slate-900 hover:text-white"
                        >
                          Access Report
                          <ChevronRight className="h-4 w-4" />
                        </Link>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5}>
                      <div className="py-24">
                        <EmptyState
                          icon={Mail}
                          title="No Records Found"
                          description={searchQuery ? "The filter parameters yielded zero results." : "The investigation database is currently empty."}
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
