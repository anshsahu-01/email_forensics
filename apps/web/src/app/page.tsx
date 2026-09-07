'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  FileSearch,
  FileText,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Upload,
  XCircle,
} from 'lucide-react';
import { fetchCases, analyzeEmail } from '@/lib/api';
import { EmailCase } from '@/types';
import StatCard from '@/components/common/StatCard';
import RiskBadge, { getRiskLevel } from '@/components/common/RiskBadge';
import EmptyState from '@/components/common/EmptyState';
import RiskOverview from '@/components/dashboard/RiskOverview';

function formatDate(value: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

export default function DashboardPage() {
  const router = useRouter();
  const [history, setHistory] = useState<EmailCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadCases = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchCases();
      setHistory(data);
    } catch (err) {
      console.error('Failed to load cases:', err);
      setError(err instanceof Error ? err.message : 'Unable to connect to the forensic backend.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadCases();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadCases]);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selected = event.target.files?.[0] ?? null;
    if (!selected) {
      setSelectedFile(null);
      return;
    }
    if (!selected.name.toLowerCase().endsWith('.eml')) {
      setError('Only .eml files are supported.');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }
    setError(null);
    setSelectedFile(selected);
  };

  const handleAnalyze = async () => {
    if (!selectedFile || uploading) return;
    try {
      setUploading(true);
      setError(null);
      const analyzedCase = await analyzeEmail(selectedFile);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      router.push(`/cases/${analyzedCase.id}`);
    } catch (err) {
      console.error('Analysis failed:', err);
      setError(err instanceof Error ? err.message : 'Something went wrong while analyzing the email.');
    } finally {
      setUploading(false);
    }
  };

  const totalCases = history.length;
  const highRiskCases = history.filter(item => getRiskLevel(item.threatScore) === 'HIGH').length;
  const mediumRiskCases = history.filter(item => getRiskLevel(item.threatScore) === 'MEDIUM').length;
  const lowRiskCases = history.filter(item => getRiskLevel(item.threatScore) === 'LOW').length;

  const recentCases = [...history]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  return (
    <div className="min-h-full">
      <div className="mx-auto max-w-7xl px-6 py-8 lg:px-8">
        {/* Page heading */}
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="mb-2 text-sm font-medium text-indigo-600">Investigation workspace</p>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Dashboard</h1>
            <p className="mt-1 text-sm text-slate-500">Monitor email investigations and threat activity.</p>
          </div>

          <button
            type="button"
            onClick={loadCases}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>

        {/* Error */}
        {error && (
          <div className="mb-6 flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4">
            <XCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-500" />
            <div>
              <p className="text-sm font-semibold text-red-800">Unable to load dashboard</p>
              <p className="mt-1 text-sm text-red-700">{error}</p>
            </div>
          </div>
        )}

        {/* Stats */}
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard label="Total Cases" value={totalCases} icon={FileText} description="All analyzed emails" />
          <StatCard label="High Risk" value={highRiskCases} icon={ShieldAlert} description="Requires investigation" iconClassName="text-red-500" />
          <StatCard label="Medium Risk" value={mediumRiskCases} icon={AlertTriangle} description="Needs attention" iconClassName="text-amber-500" />
          <StatCard label="Low Risk" value={lowRiskCases} icon={ShieldCheck} description="No major indicators" iconClassName="text-emerald-500" />
        </div>

        {/* Main content */}
        <div className="mt-6 grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
          {/* Analyze */}
          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-100 px-6 py-5">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50">
                  <FileSearch className="h-5 w-5 text-indigo-600" />
                </div>
                <div>
                  <h2 className="font-semibold text-slate-900">Analyze an Email</h2>
                  <p className="text-xs text-slate-500">Upload an EML file for forensic analysis</p>
                </div>
              </div>
            </div>

            <div className="p-6">
              <label
                htmlFor="email-upload"
                className={`group flex min-h-48 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 text-center transition ${
                  selectedFile ? 'border-indigo-300 bg-indigo-50/50' : 'border-slate-200 bg-slate-50 hover:border-indigo-300 hover:bg-indigo-50/30'
                }`}
              >
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-white shadow-sm ring-1 ring-slate-200">
                  <Upload className="h-5 w-5 text-indigo-600" />
                </div>

                {selectedFile ? (
                  <>
                    <p className="max-w-full truncate text-sm font-semibold text-slate-800">{selectedFile.name}</p>
                    <p className="mt-1 text-xs text-slate-500">{(selectedFile.size / 1024).toFixed(1)} KB</p>
                  </>
                ) : (
                  <>
                    <p className="text-sm font-semibold text-slate-700">Choose an EML file</p>
                    <p className="mt-1 text-xs text-slate-500">Email files only • .eml format</p>
                  </>
                )}

                <input
                  ref={fileInputRef}
                  id="email-upload"
                  type="file"
                  accept=".eml,message/rfc822"
                  onChange={handleFileChange}
                  className="hidden"
                />
              </label>

              <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                <button
                  type="button"
                  onClick={handleAnalyze}
                  disabled={!selectedFile || uploading}
                  className="flex-1 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
                >
                  {uploading ? 'Analyzing email...' : 'Analyze Email'}
                </button>
                <Link
                  href="/analyze"
                  className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                >
                  Advanced Analysis
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </div>
            </div>
          </section>

          {/* Recent cases */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-slate-100 px-6 py-5">
              <div>
                <h2 className="font-semibold text-slate-900">Recent Cases</h2>
                <p className="mt-0.5 text-xs text-slate-500">Latest forensic investigations</p>
              </div>
              <Link href="/cases" className="text-xs font-semibold text-indigo-600 hover:text-indigo-700">View all</Link>
            </div>

            <div className="divide-y divide-slate-100">
              {loading ? (
                <div className="space-y-3 p-6">
                  {[1, 2, 3].map((item) => (
                    <div key={item} className="h-14 animate-pulse rounded-lg bg-slate-100" />
                  ))}
                </div>
              ) : recentCases.length > 0 ? (
                recentCases.map((item) => (
                  <Link
                    key={item.id}
                    href={`/cases/${item.id}`}
                    className="flex items-center gap-3 px-6 py-4 transition hover:bg-slate-50"
                  >
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-slate-100">
                      <FileText className="h-4 w-4 text-slate-500" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-slate-800">
                        {item.header?.subject || item.fileName || 'No subject'}
                      </p>
                      <div className="mt-1 flex items-center gap-2 text-[11px] text-slate-400">
                        <span>Case #{item.id}</span>
                        <span>•</span>
                        <span>{formatDate(item.createdAt)}</span>
                      </div>
                    </div>
                    <RiskBadge score={item.threatScore} className="hidden sm:inline-flex" />
                  </Link>
                ))
              ) : (
                <div className="p-6">
                  <EmptyState
                    icon={FileText}
                    title="No cases yet"
                    description="Upload an EML file to start an investigation."
                  />
                </div>
              )}
            </div>
          </section>
        </div>

        {/* Investigation overview */}
        <section className="mt-6 rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-6 py-5">
            <h2 className="font-semibold text-slate-900">Investigation Overview</h2>
            <p className="mt-0.5 text-xs text-slate-500">Current distribution of analyzed cases</p>
          </div>
          <div className="grid gap-6 p-6 md:grid-cols-3">
            <RiskOverview label="High Risk" value={highRiskCases} total={totalCases} icon={ShieldAlert} className="text-red-500" barClassName="bg-red-500" />
            <RiskOverview label="Medium Risk" value={mediumRiskCases} total={totalCases} icon={AlertTriangle} className="text-amber-500" barClassName="bg-amber-500" />
            <RiskOverview label="Low Risk" value={lowRiskCases} total={totalCases} icon={CheckCircle2} className="text-emerald-500" barClassName="bg-emerald-500" />
          </div>
        </section>
      </div>
    </div>
  );
}
