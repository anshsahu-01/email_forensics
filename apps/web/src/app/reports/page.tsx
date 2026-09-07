'use client';

import { useEffect, useState } from 'react';
import {
  ShieldCheck,
  FileText,
  Download,
  TrendingUp,
  AlertTriangle,
  CheckCircle2,
  Calendar,
  RefreshCw
} from 'lucide-react';
import { fetchCases } from '@/lib/api';
import { EmailCase } from '@/types';
import RiskBadge, { getRiskLevel } from '@/components/common/RiskBadge';
import EmptyState from '@/components/common/EmptyState';

export default function ReportsPage() {
  const [cases, setCases] = useState<EmailCase[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      setLoading(true);
      const data = await fetchCases();
      setCases(data);
    } catch (err) {
      console.error(err);
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

  const highRisk = cases.filter(c => getRiskLevel(c.threatScore) === 'HIGH').length;
  const mediumRisk = cases.filter(c => getRiskLevel(c.threatScore) === 'MEDIUM').length;
  const lowRisk = cases.filter(c => getRiskLevel(c.threatScore) === 'LOW').length;

  return (
    <div className="min-h-full bg-white py-12">
      <div className="mx-auto max-w-7xl px-8">
        <div className="mb-12 flex items-center justify-between border-b border-slate-900 pb-8">
          <div>
            <p className="mb-2 text-[10px] font-black uppercase tracking-[0.3em] text-indigo-600">Executive Summary</p>
            <h1 className="text-4xl font-black uppercase tracking-tighter text-slate-900 sm:text-5xl">
              Forensic Reporting
            </h1>
            <p className="mt-4 text-sm font-medium text-slate-500 max-w-md">
              Aggregated investigation summaries and operational threat metrics for executive review.
            </p>
          </div>

          <button
            onClick={loadData}
            className="inline-flex items-center justify-center gap-3 border-2 border-slate-900 bg-white px-6 py-3 text-[11px] font-black uppercase tracking-widest text-slate-900 transition hover:bg-slate-900 hover:text-white"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Sync Dashboard
          </button>
        </div>

        {/* Stats Grid */}
        <div className="grid gap-px bg-slate-200 border border-slate-200 mb-12 sm:grid-cols-2 lg:grid-cols-4">
          <ReportStatCard label="Total Summaries" value={cases.length} icon={FileText} />
          <ReportStatCard label="Critical Vectors" value={highRisk} icon={AlertTriangle} color="text-red-600" />
          <ReportStatCard label="Anomalous Activity" value={mediumRisk} icon={TrendingUp} color="text-amber-600" />
          <ReportStatCard label="Verified Baseline" value={lowRisk} icon={CheckCircle2} color="text-emerald-600" />
        </div>

        <div className="border border-slate-200 bg-white">
          <div className="px-8 py-6 border-b-2 border-slate-900 flex items-center justify-between bg-slate-50">
            <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">Recent Investigation Summaries</h2>
            <button className="text-[9px] font-black uppercase tracking-widest text-indigo-600 hover:text-slate-900 transition-all">Export Workspace (JSON)</button>
          </div>

          <div className="divide-y divide-slate-100">
            {loading ? (
              [1, 2, 3].map(i => <div key={i} className="p-10 animate-pulse bg-slate-50/50 my-4 mx-8 border border-slate-100" />)
            ) : cases.length > 0 ? (
              cases.map((c) => (
                <div key={c.id} className="p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-10 hover:bg-slate-50 transition-colors">
                  <div className="flex items-start gap-6">
                    <div className={`mt-1 flex h-12 w-12 items-center justify-center border-2 ${
                      getRiskLevel(c.threatScore) === 'HIGH' ? 'border-red-600 text-red-600 bg-red-50' :
                      getRiskLevel(c.threatScore) === 'MEDIUM' ? 'border-amber-600 text-amber-600 bg-amber-50' : 'border-emerald-600 text-emerald-600 bg-emerald-50'
                    }`}>
                      <FileText className="h-6 w-6" />
                    </div>
                    <div>
                      <h3 className="text-xs font-black uppercase tracking-tight text-slate-900">{c.header?.subject || 'UNLABELED INVESTIGATION'}</h3>
                      <div className="mt-2 flex items-center gap-4 text-[9px] font-black uppercase tracking-[0.15em] text-slate-400">
                        <span className="flex items-center gap-1.5"><Calendar className="h-3 w-3 text-slate-300" /> {new Date(c.createdAt).toLocaleDateString()}</span>
                        <span>•</span>
                        <span>REF-{c.id}</span>
                        <span>•</span>
                        <RiskBadge score={c.threatScore} />
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <button className="flex items-center gap-2 border border-slate-900 bg-white px-4 py-2 text-[10px] font-black uppercase tracking-widest text-slate-900 hover:bg-slate-900 hover:text-white transition-all">
                      <Download className="h-3.5 w-3.5" />
                      PDF Report
                    </button>
                    <a
                      href={`/cases/${c.id}`}
                      className="flex items-center gap-2 border-2 border-slate-900 bg-slate-900 px-4 py-2 text-[10px] font-black uppercase tracking-widest text-white transition hover:bg-slate-800"
                    >
                      Access Ledger
                    </a>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-24">
                <EmptyState icon={ShieldCheck} title="No Summary Data" description="Investigation reports will be generated upon successful artifact processing." />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function ReportStatCard({ label, value, icon: Icon, color = "text-slate-900" }: { label: string, value: number, icon: React.ComponentType<{ className?: string }>, color?: string }) {
  return (
    <div className="bg-white p-8">
      <div className="flex items-center gap-3 mb-4">
        <Icon className={`h-4 w-4 ${color}`} />
        <span className="text-[9px] font-black uppercase tracking-[0.3em] text-slate-400">{label}</span>
      </div>
      <p className="text-4xl font-black tracking-tighter text-slate-900">{value}</p>
    </div>
  );
}
