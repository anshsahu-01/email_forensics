'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  AlertTriangle,
  ArrowRight,
  FileSearch,
  FileText,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Upload,
  XCircle,
  Eye,
  Mail,
  Zap,
  Shield,
  ShieldHalf,
  FileCode2,
  FileIcon
} from 'lucide-react';
import { fetchCases, analyzeEmail } from '@/lib/api';
import { EmailCase } from '@/types';
import StatCard from '@/components/common/StatCard';
import { getRiskLevel } from '@/components/common/RiskBadge';
import { TopSuspiciousLocations, AuthenticationOverview } from '@/components/dashboard/DashboardCharts';
import GoogleMap from '@/components/common/GoogleMap';
import { RiskDistributionChart } from '@/components/intelligence/IntelligenceCharts';

function formatDate(value: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  
  // Format to match screenshot: YYYY-MM-DD HH:MM
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
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

  const mapPoints = history
    .filter(c => c.geoLatitude != null && c.geoLongitude != null)
    .map(c => ({
      lat: c.geoLatitude!,
      lng: c.geoLongitude!,
      label: c.geoCity ? `${c.geoCity}, ${c.geoCountry}` : (c.geoCountry || 'Unknown Infrastructure Location'),
      detail: c.header?.subject || `ID-${c.id}`
    }));

  const recentCases = [...history]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  return (
    <div className="min-h-full bg-[#f8fafc] p-6 lg:p-8">
      <div className="mx-auto max-w-[1400px]">
        
        {/* Banner Section */}
        <div className="relative mb-6 flex flex-col justify-between overflow-hidden rounded-2xl bg-gradient-to-r from-blue-50 to-indigo-50/30 p-8 shadow-sm border border-blue-100 sm:flex-row sm:items-center">
          <div className="relative z-10">
            <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">Forensic Workspace</p>
            <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 sm:text-5xl">DASHBOARD</h1>
            <p className="mt-2 max-w-lg text-sm text-slate-500">Real-time monitoring of email threat vectors and active forensic investigations.</p>
          </div>
          
          <div className="relative z-10 mt-6 sm:mt-0">
            <button
              type="button"
              onClick={loadCases}
              disabled={loading}
              className="inline-flex items-center justify-center gap-2 rounded-lg border-2 border-blue-600 bg-white px-5 py-2.5 text-xs font-bold uppercase tracking-wide text-blue-600 shadow-sm transition hover:bg-blue-50 focus:outline-none disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh Data
            </button>
          </div>

          {/* Decorative Background Shield */}
          <div className="absolute -right-8 top-1/2 hidden -translate-y-1/2 transform sm:block opacity-20 pointer-events-none">
             <ShieldHalf className="h-48 w-48 text-blue-500" />
          </div>
        </div>

        {/* Error */}
        {error && (
          <div className="mb-6 flex items-start gap-4 rounded-xl border border-red-200 bg-red-50 p-5 shadow-sm">
            <XCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-red-800">System Error</p>
              <p className="mt-1 text-sm text-red-700">{error}</p>
            </div>
          </div>
        )}

        {/* Stats Row */}
        <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard 
            label="Total Investigations" 
            value={totalCases} 
            icon={FileText} 
            description="Total artifacts processed" 
            iconClassName="text-blue-600"
            iconBgClassName="bg-blue-100"
          />
          <StatCard 
            label="Critical Threats" 
            value={highRiskCases} 
            icon={ShieldAlert} 
            description="Immediate action required" 
            iconClassName="text-red-600"
            iconBgClassName="bg-red-100"
          />
          <StatCard 
            label="Suspicious" 
            value={mediumRiskCases} 
            icon={AlertTriangle} 
            description="Awaiting detailed review" 
            iconClassName="text-amber-600"
            iconBgClassName="bg-amber-100"
          />
          <StatCard 
            label="Verified Safe" 
            value={lowRiskCases} 
            icon={ShieldCheck} 
            description="No malicious indicators" 
            iconClassName="text-emerald-600"
            iconBgClassName="bg-emerald-100"
          />
        </div>

        {/* Middle Section: New Investigation & Active Ledger */}
        <div className="mb-6 grid gap-6 xl:grid-cols-[1fr_1.5fr]">
          
          {/* New Investigation */}
          <section className="flex flex-col rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="border-b border-slate-100 px-6 py-5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-50">
                   <Upload className="h-4 w-4 text-indigo-600" />
                </div>
                <div>
                  <h2 className="text-sm font-extrabold uppercase tracking-wide text-slate-900">New Investigation</h2>
                  <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Intake .eml for analysis</p>
                </div>
              </div>
              <FileSearch className="h-5 w-5 text-slate-300" />
            </div>

            <div className="p-6 flex-1 flex flex-col">
              <label
                htmlFor="email-upload"
                className={`group flex flex-1 min-h-[160px] cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed transition-all ${
                  selectedFile ? 'border-indigo-400 bg-indigo-50/50' : 'border-blue-200 bg-blue-50/30 hover:border-blue-400 hover:bg-blue-50'
                }`}
              >
                <div className="mb-4">
                  <Upload className={`h-8 w-8 ${selectedFile ? 'text-indigo-600' : 'text-blue-500'}`} />
                </div>
                {selectedFile ? (
                  <div className="text-center px-4">
                    <p className="max-w-[200px] truncate text-sm font-bold text-slate-900">{selectedFile.name}</p>
                    <p className="mt-1 text-xs text-indigo-600 font-medium">Ready for processing</p>
                  </div>
                ) : (
                  <div className="text-center">
                    <p className="text-sm font-bold text-slate-700">Drag & Drop Files Here</p>
                    <p className="mt-1 text-xs text-blue-600">or click to browse</p>
                    <p className="mt-3 text-[10px] text-slate-400">Supports .eml (max 10MB)</p>
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

              {/* Action Buttons */}
              <div className="mt-4 grid grid-cols-3 gap-2">
                <button
                  type="button"
                  onClick={handleAnalyze}
                  disabled={!selectedFile || uploading}
                  className="flex flex-col items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white p-3 text-center transition hover:border-indigo-300 hover:bg-indigo-50 focus:outline-none disabled:opacity-50 disabled:pointer-events-none"
                >
                  <Mail className="h-4 w-4 text-blue-600" />
                  <div>
                     <p className="text-[10px] font-bold text-slate-700">Email Analysis</p>
                     <p className="text-[9px] text-slate-400 hidden sm:block">Extract headers, links</p>
                  </div>
                </button>
                <button
                  type="button"
                  disabled
                  className="flex flex-col items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white p-3 text-center transition opacity-60 cursor-not-allowed"
                >
                  <Zap className="h-4 w-4 text-amber-500" />
                  <div>
                     <p className="text-[10px] font-bold text-slate-700">Auto Triage</p>
                     <p className="text-[9px] text-slate-400 hidden sm:block">Detect threats</p>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={handleAnalyze}
                  disabled={!selectedFile || uploading}
                  className="flex flex-col items-center justify-center gap-2 rounded-lg border border-indigo-200 bg-indigo-50 p-3 text-center transition hover:bg-indigo-100 focus:outline-none disabled:opacity-50 disabled:pointer-events-none"
                >
                  <Shield className="h-4 w-4 text-indigo-600" />
                  <div>
                     <p className="text-[10px] font-bold text-indigo-900">Start Investigation</p>
                     <p className="text-[9px] text-indigo-600 hidden sm:block">Add to case directly</p>
                  </div>
                </button>
              </div>
            </div>
          </section>

          {/* Active Ledger */}
          <section className="flex flex-col rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="border-b border-slate-100 px-6 py-5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-50">
                   <FileCode2 className="h-4 w-4 text-blue-600" />
                </div>
                <div>
                  <h2 className="text-sm font-extrabold uppercase tracking-wide text-slate-900">Active Ledger</h2>
                  <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Latest investigation results</p>
                </div>
              </div>
              <Link href="/cases" className="text-[11px] font-bold uppercase tracking-wide text-blue-600 hover:text-blue-800 flex items-center gap-1">
                Full Access <ArrowRight className="h-3 w-3" />
              </Link>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    <th className="px-6 py-4 font-medium">Date & Time</th>
                    <th className="px-6 py-4 font-medium">Source Artifact</th>
                    <th className="px-6 py-4 font-medium">Status</th>
                    <th className="px-6 py-4 text-right font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {loading ? (
                    <tr>
                       <td colSpan={4} className="p-6">
                         <div className="flex flex-col gap-3">
                           {[1, 2, 3, 4].map(i => <div key={i} className="h-10 w-full bg-slate-50 animate-pulse rounded-md" />)}
                         </div>
                       </td>
                    </tr>
                  ) : recentCases.length > 0 ? (
                    recentCases.map((item) => {
                      const riskLevel = getRiskLevel(item.threatScore);
                      let statusBadge = null;
                      
                      if (riskLevel === 'HIGH') {
                        statusBadge = <span className="inline-flex items-center rounded-full bg-red-100 px-2.5 py-1 text-[10px] font-bold text-red-700">Threat Found</span>;
                      } else if (riskLevel === 'MEDIUM') {
                        statusBadge = <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-1 text-[10px] font-bold text-amber-700">Suspicious</span>;
                      } else {
                        statusBadge = <span className="inline-flex items-center rounded-full bg-emerald-100 px-2.5 py-1 text-[10px] font-bold text-emerald-700">Completed</span>;
                      }

                      return (
                        <tr key={item.id} className="transition hover:bg-slate-50/50">
                          <td className="whitespace-nowrap px-6 py-4 text-[11px] font-medium text-slate-500">
                            {formatDate(item.createdAt)}
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-2">
                               <FileIcon className="h-3 w-3 text-slate-400 shrink-0" />
                               <span className="truncate max-w-[150px] sm:max-w-[200px] text-[12px] font-medium text-slate-700">
                                 {item.header?.subject || item.fileName || 'Unnamed Artifact'}
                               </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                             {statusBadge}
                          </td>
                          <td className="px-6 py-4 text-right whitespace-nowrap">
                            <Link href={`/cases/${item.id}`} className="inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-400 hover:bg-slate-100 hover:text-slate-900 transition-colors">
                              <Eye className="h-4 w-4" />
                            </Link>
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan={4} className="px-6 py-12 text-center text-sm text-slate-500">
                        No recent investigations found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        {/* Intelligence Grid */}
        <div className="grid gap-6 lg:grid-cols-3 xl:grid-cols-3">
          
          {/* Threat Distribution */}
          <section className="flex flex-col rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden lg:col-span-1">
            <div className="border-b border-slate-100 px-6 py-5">
              <h2 className="text-sm font-extrabold uppercase tracking-wide text-slate-900">Threat Distribution</h2>
            </div>
            <div className="p-6 flex-1 flex flex-col justify-center">
              <RiskDistributionChart cases={history} />
            </div>
          </section>

          {/* Authentication Overview */}
          <section className="flex flex-col rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden lg:col-span-2">
            <div className="border-b border-slate-100 px-6 py-5">
              <h2 className="text-sm font-extrabold uppercase tracking-wide text-slate-900">Authentication Overview</h2>
            </div>
            <div className="p-6">
              <AuthenticationOverview history={history} />
            </div>
          </section>
          
          {/* Top Suspicious Locations */}
          <section className="flex flex-col rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden lg:col-span-1">
            <div className="border-b border-slate-100 px-6 py-5">
              <h2 className="text-sm font-extrabold uppercase tracking-wide text-slate-900">Suspicious Locations</h2>
            </div>
            <div className="p-6">
              <TopSuspiciousLocations history={history} />
            </div>
          </section>

          {/* Map */}
          <section className="flex flex-col rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden lg:col-span-2">
            <div className="border-b border-slate-100 px-6 py-5">
              <h2 className="text-sm font-extrabold uppercase tracking-wide text-slate-900">Geographic Intelligence</h2>
            </div>
            <div className="p-6 bg-slate-50/50">
              <GoogleMap points={mapPoints} className="h-[260px] w-full rounded-xl border border-slate-200 bg-slate-100" />
            </div>
          </section>

        </div>
      </div>
    </div>
  );
}
