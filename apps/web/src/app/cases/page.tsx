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
      setError('Failed to load investigation cases.');
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
    <div className="min-h-full bg-slate-50 py-8">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Investigation Cases
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              Browse and manage your forensic email investigations.
            </p>
          </div>

          <button
            onClick={loadCases}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>

        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by subject, sender, or filename..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-4 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition"
            />
          </div>

          <button className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 transition">
            <Filter className="h-4 w-4" />
            Filter
          </button>
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
                  <th className="px-6 py-4">Case Details</th>
                  <th className="px-6 py-4">Sender</th>
                  <th className="px-6 py-4">Risk Level</th>
                  <th className="px-6 py-4">Analysis Date</th>
                  <th className="px-6 py-4 text-right">Action</th>
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
                ) : filteredCases.length > 0 ? (
                  filteredCases.map((item) => (
                    <tr
                      key={item.id}
                      className="group transition-colors hover:bg-slate-50/50"
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-500 group-hover:bg-indigo-50 group-hover:text-indigo-600 transition">
                            <FileText className="h-5 w-5" />
                          </div>
                          <div className="min-w-0">
                            <p className="font-semibold text-slate-900 truncate max-w-xs">
                              {item.header?.subject || 'No Subject'}
                            </p>
                            <p className="text-[11px] text-slate-500">
                              #{item.id} • {item.fileName}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2 text-slate-600">
                          <User className="h-3.5 w-3.5" />
                          <span className="truncate max-w-[180px]">
                            {item.header?.senderFrom || 'Unknown'}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <RiskBadge score={item.threatScore} />
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2 text-slate-500">
                          <Calendar className="h-3.5 w-3.5" />
                          {formatDate(item.createdAt)}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link
                          href={`/cases/${item.id}`}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-indigo-600"
                        >
                          View Report
                          <ChevronRight className="h-3.5 w-3.5" />
                        </Link>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5}>
                      <div className="py-12">
                        <EmptyState
                          icon={Mail}
                          title="No cases found"
                          description={searchQuery ? "No results matching your search query." : "You haven't analyzed any emails yet."}
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
