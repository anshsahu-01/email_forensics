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
    <div className="min-h-full bg-slate-50 py-8">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Forensic Reporting
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              Summarized investigation reports and activity analytics.
            </p>
          </div>

          <button
            onClick={loadData}
            className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 transition"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>

        {/* Stats Grid */}
        <div className="grid gap-6 mb-8 sm:grid-cols-2 lg:grid-cols-4">
          <ReportStatCard label="Total Reports" value={cases.length} icon={FileText} />
          <ReportStatCard label="Critical Threats" value={highRisk} icon={AlertTriangle} color="text-red-600" />
          <ReportStatCard label="Suspicious Activity" value={mediumRisk} icon={TrendingUp} color="text-amber-600" />
          <ReportStatCard label="Clean / Safe" value={lowRisk} icon={CheckCircle2} color="text-emerald-600" />
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/30">
            <h2 className="font-bold text-slate-900">Recent Investigation Reports</h2>
            <button className="text-xs font-bold text-indigo-600 hover:text-indigo-700">Export All (JSON)</button>
          </div>

          <div className="divide-y divide-slate-100">
            {loading ? (
              [1, 2, 3].map(i => <div key={i} className="p-8 animate-pulse bg-slate-50/50 my-2 mx-6 rounded-xl" />)
            ) : cases.length > 0 ? (
              cases.map((c) => (
                <div key={c.id} className="p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-6 hover:bg-slate-50 transition-colors">
                  <div className="flex items-start gap-4">
                    <div className={`mt-1 p-2 rounded-lg bg-white border ${
                      getRiskLevel(c.threatScore) === 'HIGH' ? 'border-red-200 text-red-500' :
                      getRiskLevel(c.threatScore) === 'MEDIUM' ? 'border-amber-200 text-amber-500' : 'border-emerald-200 text-emerald-500'
                    }`}>
                      <FileText className="h-5 w-5" />
                    </div>
                    <div>
                      <h3 className="font-bold text-slate-900">{c.header?.subject || 'Unnamed Investigation'}</h3>
                      <div className="mt-1 flex items-center gap-3 text-xs text-slate-500 font-medium">
                        <span className="flex items-center gap-1"><Calendar className="h-3 w-3" /> {new Date(c.createdAt).toLocaleDateString()}</span>
                        <span>•</span>
                        <span>Case #{c.id}</span>
                        <span>•</span>
                        <RiskBadge score={c.threatScore} />
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <button className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 hover:border-indigo-200 hover:text-indigo-600 transition">
                      <Download className="h-3.5 w-3.5" />
                      PDF
                    </button>
                    <a
                      href={`/cases/${c.id}`}
                      className="flex items-center gap-2 rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-bold text-white shadow-sm hover:bg-indigo-700 transition"
                    >
                      View Full Report
                    </a>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-20">
                <EmptyState icon={ShieldCheck} title="No reports generated" description="Analysis reports will appear here once you process email files." />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function ReportStatCard({ label, value, icon: Icon, color = "text-slate-600" }: { label: string, value: number, icon: React.ComponentType<{ className?: string }>, color?: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center gap-3 mb-2">
        <Icon className={`h-4 w-4 ${color}`} />
        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">{label}</span>
      </div>
      <p className="text-3xl font-bold tracking-tight text-slate-900">{value}</p>
    </div>
  );
}
