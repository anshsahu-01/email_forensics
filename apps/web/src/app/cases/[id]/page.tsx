'use client';

import { useEffect, useState, use } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Download,
  Share2,
  ShieldAlert,
  Info,
  Hash,
  Globe,
  MapPin,
  Server,
  Link2,
  Loader2
} from 'lucide-react';
import { fetchCaseById } from '@/lib/api';
import { EmailCase } from '@/types';
import RiskBadge from '@/components/common/RiskBadge';

// We'll build these sub-components next
import CaseHeader from '@/components/email/CaseHeader';
import AuthenticationSection from '@/components/email/AuthenticationSection';
import RoutingSection from '@/components/email/RoutingSection';
import IndicatorsSection from '@/components/email/IndicatorsSection';
import RawContentSection from '@/components/email/RawContentSection';

export default function CaseDetailsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [emailCase, setEmailCase] = useState<EmailCase | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'routing' | 'indicators' | 'raw'>('overview');

  useEffect(() => {
    const loadCase = async () => {
      try {
        setLoading(true);
        const data = await fetchCaseById(id);
        setEmailCase(data);
      } catch (err) {
        console.error('Failed to load case:', err);
        setError('Failed to load investigation details. The case may not exist.');
      } finally {
        setLoading(false);
      }
    };
    void loadCase();
  }, [id]);

  if (loading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50">
        <Loader2 className="h-10 w-10 animate-spin text-indigo-600" />
        <p className="mt-4 text-sm font-medium text-slate-600">Loading forensic report...</p>
      </div>
    );
  }

  if (error || !emailCase) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-6 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-red-500">
          <ShieldAlert className="h-8 w-8" />
        </div>
        <h1 className="mt-6 text-2xl font-bold text-slate-900">Investigation Not Found</h1>
        <p className="mt-2 text-slate-500 max-w-md">{error || 'The requested forensic report could not be retrieved.'}</p>
        <Link
          href="/cases"
          className="mt-8 rounded-xl bg-indigo-600 px-6 py-2.5 text-sm font-semibold text-white shadow-md hover:bg-indigo-700 transition"
        >
          Back to Cases
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-full bg-slate-50 pb-20">
      {/* Sticky Header */}
      <div className="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur-md">
        <div className="mx-auto max-w-7xl px-6 py-4 lg:px-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-4">
              <Link
                href="/cases"
                className="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 hover:text-indigo-600 transition"
              >
                <ArrowLeft className="h-5 w-5" />
              </Link>
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-xl font-bold text-slate-900 truncate max-w-md">
                    Case #{emailCase.id}
                  </h1>
                  <RiskBadge score={emailCase.threatScore} />
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  Investigation started on {new Date(emailCase.createdAt).toLocaleString()}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition">
                <Download className="h-3.5 w-3.5" />
                Export PDF
              </button>
              <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition">
                <Share2 className="h-3.5 w-3.5" />
                Share
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-6 py-8 lg:px-8">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">

          {/* Main Content Area */}
          <div className="lg:col-span-2 space-y-6">

            {/* Tabs */}
            <div className="flex gap-1 rounded-xl bg-slate-200/50 p-1">
              {(['overview', 'routing', 'indicators', 'raw'] as const).map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`flex-1 rounded-lg px-4 py-2 text-xs font-bold uppercase tracking-wider transition ${
                    activeTab === tab
                      ? 'bg-white text-indigo-600 shadow-sm'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>

            {/* Tab Panels */}
            {activeTab === 'overview' && (
              <>
                <CaseHeader emailCase={emailCase} />
                <AuthenticationSection header={emailCase.header} />

                {/* Quick Info Grid */}
                <div className="grid gap-6 sm:grid-cols-2">
                  <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                    <h3 className="flex items-center gap-2 text-sm font-bold text-slate-900 mb-4">
                      <Globe className="h-4 w-4 text-indigo-500" />
                      Originating Infrastructure
                    </h3>
                    <div className="space-y-4">
                      <div>
                        <p className="text-[10px] font-bold uppercase text-slate-400">IP Address</p>
                        <p className="mt-1 font-mono text-sm font-semibold text-slate-700">
                          {emailCase.originatingIp || 'Not detected'}
                        </p>
                      </div>
                      <div className="flex gap-6">
                        <div>
                          <p className="text-[10px] font-bold uppercase text-slate-400">Location</p>
                          <p className="mt-1 flex items-center gap-1 text-sm text-slate-600">
                            <MapPin className="h-3 w-3" />
                            Global (Cloud)
                          </p>
                        </div>
                        <div>
                          <p className="text-[10px] font-bold uppercase text-slate-400">ASN</p>
                          <p className="mt-1 text-sm text-slate-600">Unknown</p>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                    <h3 className="flex items-center gap-2 text-sm font-bold text-slate-900 mb-4">
                      <Hash className="h-4 w-4 text-indigo-500" />
                      Forensic Artifacts
                    </h3>
                    <div className="space-y-4">
                      <div>
                        <p className="text-[10px] font-bold uppercase text-slate-400">SHA-256 Hash</p>
                        <p className="mt-1 font-mono text-[11px] font-medium text-slate-500 break-all bg-slate-50 p-2 rounded-lg">
                          {emailCase.fileHash || 'N/A'}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </>
            )}

            {activeTab === 'routing' && (
              <RoutingSection receivedHeaders={emailCase.receivedHeaders} />
            )}

            {activeTab === 'indicators' && (
              <IndicatorsSection indicators={emailCase.indicators} />
            )}

            {activeTab === 'raw' && (
              <RawContentSection content={emailCase.rawBody} />
            )}
          </div>

          {/* Sidebar Info */}
          <div className="space-y-6">
            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <h3 className="text-sm font-bold text-slate-900 mb-4">Threat Summary</h3>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-slate-500">Threat Score</span>
                  <span className="text-sm font-bold text-slate-900">{emailCase.threatScore ?? 0}/100</span>
                </div>
                <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full ${
                      (emailCase.threatScore ?? 0) > 70 ? 'bg-red-500' : (emailCase.threatScore ?? 0) > 40 ? 'bg-amber-500' : 'bg-emerald-500'
                    }`}
                    style={{ width: `${emailCase.threatScore ?? 0}%` }}
                  />
                </div>
                <div className="rounded-xl bg-slate-50 p-4 border border-slate-100">
                  <p className="text-xs text-slate-600 leading-relaxed italic">
                    {(emailCase.threatScore ?? 0) > 70
                      ? "High probability of malicious intent. Multiple indicators of compromise detected in headers or body."
                      : (emailCase.threatScore ?? 0) > 40
                      ? "Suspicious activity detected. Headers may be spoofed or contain unusual routing patterns."
                      : "No major threats detected. Email appears to follow standard security protocols."
                    }
                  </p>
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <h3 className="text-sm font-bold text-slate-900 mb-4">Indicator Breakdown</h3>
              <div className="space-y-3">
                <IndicatorStat label="Malicious URLs" count={emailCase.indicators?.filter(i => i.type === 'URL' && (i.vtMalicious ?? 0) > 0).length ?? 0} icon={Link2} color="text-red-500" />
                <IndicatorStat label="Suspicious IPs" count={0} icon={Server} color="text-amber-500" />
                <IndicatorStat label="Auth Failures" count={[emailCase.header?.spfStatus, emailCase.header?.dkimStatus, emailCase.header?.dmarcStatus].filter(s => s === 'fail').length} icon={ShieldAlert} color="text-red-500" />
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-indigo-600 p-6 shadow-lg shadow-indigo-100 text-white">
              <Info className="h-8 w-8 mb-4 opacity-80" />
              <h3 className="text-sm font-bold mb-2">Automated Analysis</h3>
              <p className="text-xs text-indigo-100 leading-relaxed">
                Our forensic engine has completed the automated triage. Review the routing and indicator tabs for a deeper technical investigation.
              </p>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

function IndicatorStat({ label, count, icon: Icon, color }: { label: string, count: number, icon: React.ComponentType<{ className?: string }>, color: string }) {
  return (
    <div className="flex items-center justify-between py-1">
      <div className="flex items-center gap-2">
        <Icon className={`h-3.5 w-3.5 ${color}`} />
        <span className="text-xs text-slate-600">{label}</span>
      </div>
      <span className="text-xs font-bold text-slate-900">{count}</span>
    </div>
  );
}
