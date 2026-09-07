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
    <div className="min-h-full bg-white">
      <div className="mx-auto max-w-7xl px-8 py-12">
        {/* Page heading */}
        <div className="mb-12 flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between border-b border-slate-900 pb-8">
          <div>
            <p className="mb-2 text-[10px] font-black uppercase tracking-[0.3em] text-indigo-600">Forensic Workspace</p>
            <h1 className="text-4xl font-black uppercase tracking-tighter text-slate-900 sm:text-5xl">Dashboard</h1>
            <p className="mt-4 text-sm font-medium text-slate-500 max-w-md">Real-time monitoring of email threat vectors and active forensic investigations.</p>
          </div>

          <button
            type="button"
            onClick={loadCases}
            disabled={loading}
            className="inline-flex items-center justify-center gap-3 border-2 border-slate-900 bg-white px-6 py-3 text-[11px] font-black uppercase tracking-widest text-slate-900 transition hover:bg-slate-900 hover:text-white disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh Data
          </button>
        </div>

        {/* Error */}
        {error && (
          <div className="mb-8 flex items-start gap-4 border border-red-200 bg-red-50 p-6">
            <XCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
            <div>
              <p className="text-xs font-black uppercase tracking-widest text-red-800">System Error</p>
              <p className="mt-1 text-sm font-medium text-red-700">{error}</p>
            </div>
          </div>
        )}

        {/* Stats */}
        <div className="grid gap-px bg-slate-200 sm:grid-cols-2 xl:grid-cols-4 border border-slate-200">
          <StatCard label="Total Investigations" value={totalCases} icon={FileText} description="Total artifacts processed" />
          <StatCard label="Critical Threats" value={highRiskCases} icon={ShieldAlert} description="Immediate action required" iconClassName="text-red-600" />
          <StatCard label="Suspicious" value={mediumRiskCases} icon={AlertTriangle} description="Awaiting detailed review" iconClassName="text-amber-600" />
          <StatCard label="Verified Safe" value={lowRiskCases} icon={ShieldCheck} description="No malicious indicators" iconClassName="text-emerald-600" />
        </div>

        {/* Main content */}
        <div className="mt-12 grid gap-12 xl:grid-cols-[1.2fr_0.8fr]">
          {/* Analyze */}
          <section className="border border-slate-200 bg-white">
            <div className="border-b border-slate-200 px-8 py-6 flex items-center justify-between bg-slate-50">
              <div>
                <h2 className="text-xs font-black uppercase tracking-[0.2em] text-slate-900">New Investigation</h2>
                <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">Intake .eml for analysis</p>
              </div>
              <FileSearch className="h-5 w-5 text-slate-300" />
            </div>

            <div className="p-8">
              <label
                htmlFor="email-upload"
                className={`group flex min-h-[300px] cursor-pointer flex-col items-center justify-center border-2 border-dashed transition-all ${
                  selectedFile ? 'border-indigo-600 bg-indigo-50/20' : 'border-slate-200 bg-slate-50 hover:border-slate-400 hover:bg-white'
                }`}
              >
                <div className="mb-6 flex h-16 w-16 items-center justify-center border-2 border-slate-200 bg-white transition-all group-hover:border-slate-900">
                  <Upload className="h-6 w-6 text-slate-900" />
                </div>

                {selectedFile ? (
                  <div className="text-center">
                    <p className="max-w-xs truncate text-sm font-black uppercase tracking-tight text-slate-900">{selectedFile.name}</p>
                    <p className="mt-2 text-[10px] font-bold text-slate-500">READY FOR PROCESSING • {(selectedFile.size / 1024).toFixed(1)} KB</p>
                  </div>
                ) : (
                  <div className="text-center">
                    <p className="text-[11px] font-black uppercase tracking-[0.2em] text-slate-900">Drop Forensic Artifact</p>
                    <p className="mt-2 text-[10px] font-bold uppercase tracking-widest text-slate-400">Supported format: .eml only</p>
                  </div>
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

              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <button
                  type="button"
                  onClick={handleAnalyze}
                  disabled={!selectedFile || uploading}
                  className="flex-1 border-2 border-slate-900 bg-slate-900 px-6 py-4 text-[11px] font-black uppercase tracking-widest text-white transition hover:bg-slate-800 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {uploading ? 'Processing artifact...' : 'Initiate Deep Analysis'}
                </button>
                <Link
                  href="/analyze"
                  className="inline-flex items-center justify-center gap-3 border-2 border-slate-200 bg-white px-6 py-4 text-[11px] font-black uppercase tracking-widest text-slate-900 transition hover:border-slate-900"
                >
                  Bulk Upload
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </div>
            </div>
          </section>

          {/* Recent cases */}
          <section className="border border-slate-200 bg-white flex flex-col">
            <div className="border-b border-slate-200 px-8 py-6 flex items-center justify-between bg-slate-50">
              <div>
                <h2 className="text-xs font-black uppercase tracking-[0.2em] text-slate-900">Active Ledger</h2>
                <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">Latest investigation results</p>
              </div>
              <Link href="/cases" className="text-[10px] font-black uppercase tracking-widest text-indigo-600 hover:text-indigo-800">Full Access</Link>
            </div>

            <div className="flex-1 divide-y divide-slate-100">
              {loading ? (
                <div className="space-y-4 p-8">
                  {[1, 2, 3].map((item) => (
                    <div key={item} className="h-16 animate-pulse bg-slate-50 border border-slate-100" />
                  ))}
                </div>
              ) : recentCases.length > 0 ? (
                recentCases.map((item) => (
                  <Link
                    key={item.id}
                    href={`/cases/${item.id}`}
                    className="flex items-center gap-4 px-8 py-5 transition hover:bg-slate-50"
                  >
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center border border-slate-200 bg-white text-slate-400">
                      <FileText className="h-4 w-4" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-xs font-black uppercase tracking-tight text-slate-900">
                        {item.header?.subject || item.fileName || 'Unnamed Artifact'}
                      </p>
                      <div className="mt-1 flex items-center gap-3 text-[9px] font-bold uppercase tracking-widest text-slate-400">
                        <span>ID-{item.id}</span>
                        <span>•</span>
                        <span>{formatDate(item.createdAt)}</span>
                      </div>
                    </div>
                    <RiskBadge score={item.threatScore} className="hidden sm:inline-flex" />
                  </Link>
                ))
              ) : (
                <div className="p-12">
                  <EmptyState
                    icon={FileText}
                    title="Ledger Empty"
                    description="No forensic investigations have been initiated in this workspace."
                  />
                </div>
              )}
            </div>
          </section>
        </div>

        {/* Investigation overview */}
        <section className="mt-12 border border-slate-200 bg-white">
          <div className="border-b border-slate-200 px-8 py-6 bg-slate-50">
            <h2 className="text-xs font-black uppercase tracking-[0.2em] text-slate-900">Risk Distribution</h2>
            <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">Workspace threat intelligence metrics</p>
          </div>
          <div className="grid gap-12 p-8 md:grid-cols-3">
            <RiskOverview label="High Risk / Critical" value={highRiskCases} total={totalCases} icon={ShieldAlert} className="text-red-600" barClassName="bg-red-600" />
            <RiskOverview label="Suspicious / Warning" value={mediumRiskCases} total={totalCases} icon={AlertTriangle} className="text-amber-600" barClassName="bg-amber-600" />
            <RiskOverview label="Verified / Harmless" value={lowRiskCases} total={totalCases} icon={CheckCircle2} className="text-emerald-600" barClassName="bg-emerald-600" />
          </div>
        </section>
      </div>
    </div>
  );
}
