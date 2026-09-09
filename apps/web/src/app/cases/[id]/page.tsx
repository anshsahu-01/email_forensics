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
  Loader2
} from 'lucide-react';
import { fetchCaseById } from '@/lib/api';
import { EmailCase } from '@/types';
import RiskBadge from '@/components/common/RiskBadge';

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
        setError('Case record could not be retrieved from secure storage.');
      } finally {
        setLoading(false);
      }
    };

    void loadCase();
  }, [id]);

  if (loading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white">
        <Loader2 className="h-10 w-10 animate-spin text-slate-900" />
        <p className="mt-6 text-[10px] font-black uppercase tracking-[0.3em] text-slate-400">
          Deciphering artifact...
        </p>
      </div>
    );
  }

  if (error || !emailCase) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white px-8 text-center">
        <div className="flex h-20 w-20 items-center justify-center border-2 border-red-600 text-red-600">
          <ShieldAlert className="h-10 w-10" />
        </div>

        <h1 className="mt-8 text-2xl font-black uppercase tracking-widest text-slate-900">
          Access Denied
        </h1>

        <p className="mt-4 max-w-md text-sm font-medium uppercase tracking-tight text-slate-500">
          {error || 'The requested forensic report could not be retrieved.'}
        </p>

        <Link
          href="/cases"
          className="mt-12 border-2 border-slate-900 bg-slate-900 px-8 py-3 text-[11px] font-black uppercase tracking-widest text-white transition hover:bg-slate-800"
        >
          Return to Ledger
        </Link>
      </div>
    );
  }

  /*
   * RDAP / ASN data is stored on the IP indicator.
   * Find it once and reuse it throughout the case overview.
   */
  const ipIndicator = emailCase.indicators?.find(
    (indicator) => indicator.type?.toUpperCase() === 'IP'
  );

  return (
    <div className="min-h-full bg-white pb-24">

      {/* Sticky Header */}
      <div className="sticky top-0 z-10 border-b-2 border-slate-900 bg-white/95 backdrop-blur-sm">
        <div className="mx-auto max-w-7xl px-8 py-6">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">

            <div className="flex items-center gap-6">
              <Link
                href="/cases"
                className="flex h-12 w-12 items-center justify-center border-2 border-slate-200 bg-white text-slate-400 transition-all hover:border-slate-900 hover:text-slate-900"
              >
                <ArrowLeft className="h-6 w-6" />
              </Link>

              <div>
                <div className="flex items-center gap-4">
                  <h1 className="text-2xl font-black uppercase tracking-tighter text-slate-900">
                    REPORT-{emailCase.id}
                  </h1>

                  <RiskBadge score={emailCase.threatScore} />
                </div>

                <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">
                  Investigation log active since{' '}
                  {new Date(emailCase.createdAt).toLocaleString()}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <button className="inline-flex items-center gap-2 border-2 border-slate-200 bg-white px-5 py-2.5 text-[10px] font-black uppercase tracking-widest text-slate-900 transition-all hover:border-slate-900">
                <Download className="h-4 w-4" />
                Export
              </button>

              <button className="inline-flex items-center gap-2 border-2 border-slate-200 bg-white px-5 py-2.5 text-[10px] font-black uppercase tracking-widest text-slate-900 transition-all hover:border-slate-900">
                <Share2 className="h-4 w-4" />
                Transmit
              </button>
            </div>

          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-8 py-12">
        <div className="grid grid-cols-1 gap-12 lg:grid-cols-3">

          {/* Main Content Area */}
          <div className="space-y-10 lg:col-span-2">

            {/* Tabs */}
            <div className="flex gap-px border border-slate-200 bg-slate-200">
              {(['overview', 'routing', 'indicators', 'raw'] as const).map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`flex-1 px-6 py-4 text-[10px] font-black uppercase tracking-[0.2em] transition-all ${
                    activeTab === tab
                      ? 'bg-slate-900 text-white'
                      : 'bg-white text-slate-400 hover:bg-slate-50 hover:text-slate-900'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>

            {/* Overview */}
            {activeTab === 'overview' && (
              <>
                <CaseHeader emailCase={emailCase} />

                <AuthenticationSection header={emailCase.header} />

                {/* Quick Info Grid */}
                <div className="grid gap-8 sm:grid-cols-2">

                  {/* Infrastructure Intelligence */}
                  <div className="border border-slate-200 bg-white">
                    <div className="border-b border-slate-200 bg-slate-50 px-8 py-4">
                      <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
                        <Globe className="h-4 w-4 text-indigo-600" />
                        Infrastructure Intelligence
                      </h3>
                    </div>

                    <div className="space-y-8 p-8">

                      {/* Network Origin IP */}
                      <div>
                        <p className="mb-2 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                          Network Origin IP
                        </p>

                        <p className="border border-slate-100 bg-slate-50 p-3 font-mono text-sm font-black text-slate-900">
                          {emailCase.originatingIp || 'DATA NOT DETECTED'}
                        </p>
                      </div>

                      {/* Geo Location + ASN */}
                      <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">

                        {/* Geo Location */}
                        <div>
                          <p className="mb-3 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                            Geo-Location
                          </p>

                          <div className="space-y-2">
                            <p className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-widest text-slate-900">
                              <MapPin className="h-3.5 w-3.5 text-slate-300" />
                              {emailCase.geoCountry || 'UNKNOWN COUNTRY'}
                            </p>

                            <p className="text-[10px] font-bold uppercase tracking-widest text-slate-500">
                              {emailCase.geoCity || 'UNKNOWN CITY'}
                            </p>

                            <p className="font-mono text-[10px] text-slate-500">
                              {emailCase.geoTimezone || 'TIMEZONE UNAVAILABLE'}
                            </p>

                            {(emailCase.geoLatitude !== null &&
                              emailCase.geoLatitude !== undefined &&
                              emailCase.geoLongitude !== null &&
                              emailCase.geoLongitude !== undefined) && (
                              <p className="font-mono text-[9px] text-slate-400">
                                {emailCase.geoLatitude.toFixed(4)},{' '}
                                {emailCase.geoLongitude.toFixed(4)}
                              </p>
                            )}
                          </div>
                        </div>

                        {/* ASN / Network */}
                        <div>
                          <p className="mb-3 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                            ASN / Network
                          </p>

                          <div className="space-y-2">
                            <p className="font-mono text-[11px] font-black text-slate-900">
                              {ipIndicator?.asnNumber || 'ASN UNDEFINED'}
                            </p>

                            <p className="break-words text-[10px] font-bold uppercase tracking-widest text-slate-500">
                              {ipIndicator?.asnOrg || 'NETWORK ORGANIZATION UNDEFINED'}
                            </p>
                          </div>
                        </div>

                      </div>

                      {/* RDAP / Registry Intelligence */}
                      <div className="border-t border-slate-200 pt-6">

                        <div className="mb-5 flex items-center justify-between">
                          <p className="text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                            RDAP / Registry Intelligence
                          </p>

                          <span className="text-[8px] font-black uppercase tracking-[0.2em] text-indigo-600">
                            IP REGISTRY
                          </span>
                        </div>

                        <div className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">

                          {/* Registry */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              Registry
                            </p>

                            <p className="text-[10px] font-black uppercase tracking-widest text-slate-900">
                              {ipIndicator?.rdapRegistry || 'NOT AVAILABLE'}
                            </p>
                          </div>

                          {/* RDAP Server */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              RDAP Server
                            </p>

                            <p className="break-all font-mono text-[10px] font-bold text-slate-700">
                              {ipIndicator?.rdapServer || 'NOT AVAILABLE'}
                            </p>
                          </div>

                          {/* Registry Handle */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              Registry Handle
                            </p>

                            <p className="break-all font-mono text-[10px] font-bold text-slate-700">
                              {ipIndicator?.rdapHandle || 'NOT AVAILABLE'}
                            </p>
                          </div>

                          {/* Network Name */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              Network Name
                            </p>

                            <p className="break-words text-[10px] font-black uppercase tracking-widest text-slate-900">
                              {ipIndicator?.rdapName || 'NOT AVAILABLE'}
                            </p>
                          </div>

                          {/* Organization */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              Organization
                            </p>

                            <p className="break-words text-[10px] font-bold uppercase tracking-widest text-slate-700">
                              {ipIndicator?.rdapOrganization || 'NOT AVAILABLE'}
                            </p>
                          </div>

                          {/* Country */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              Registry Country
                            </p>

                            <p className="text-[10px] font-black uppercase tracking-widest text-slate-900">
                              {ipIndicator?.rdapCountry || 'NOT AVAILABLE'}
                            </p>
                          </div>

                          {/* CIDR */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              CIDR
                            </p>

                            <p className="font-mono text-[10px] font-black text-slate-900">
                              {ipIndicator?.rdapCidr || 'NOT AVAILABLE'}
                            </p>
                          </div>

                          {/* Address Range */}
                          <div>
                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              Address Range
                            </p>

                            <p className="break-all font-mono text-[10px] font-bold text-slate-700">
                              {ipIndicator?.rdapStartAddress &&
                              ipIndicator?.rdapEndAddress
                                ? `${ipIndicator.rdapStartAddress} — ${ipIndicator.rdapEndAddress}`
                                : 'NOT AVAILABLE'}
                            </p>
                          </div>

                        </div>
                      </div>

                    </div>
                  </div>

                  {/* Integrity Artifacts */}
                  <div className="border border-slate-200 bg-white">
                    <div className="border-b border-slate-200 bg-slate-50 px-8 py-4">
                      <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
                        <Hash className="h-4 w-4 text-indigo-600" />
                        Integrity Artifacts
                      </h3>
                    </div>

                    <div className="space-y-6 p-8">
                      <div>
                        <p className="mb-2 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                          SHA-256 Digest
                        </p>

                        <p className="break-all border border-slate-100 bg-slate-50 p-4 font-mono text-[11px] font-bold leading-relaxed text-slate-600">
                          {emailCase.fileHash || 'CHECKSUM PENDING'}
                        </p>
                      </div>
                    </div>
                  </div>

                </div>
              </>
            )}

            {/* Routing */}
            {activeTab === 'routing' && (
              <RoutingSection receivedHeaders={emailCase.receivedHeaders} />
            )}

            {/* Indicators */}
            {activeTab === 'indicators' && (
              <IndicatorsSection indicators={emailCase.indicators} />
            )}

            {/* Raw */}
            {activeTab === 'raw' && (
              <RawContentSection content={emailCase.rawBody} />
            )}

          </div>

          {/* Sidebar Info */}
          <div className="space-y-10">

            {/* Risk Assessment */}
            <div className="border border-slate-200 bg-white">
              <div className="border-b border-slate-200 bg-slate-50 px-8 py-4">
                <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
                  Risk Assessment
                </h3>
              </div>

              <div className="space-y-8 p-8">

                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-black uppercase tracking-widest text-slate-500">
                    Threat Matrix Score
                  </span>

                  <span className="text-lg font-black text-slate-900">
                    {emailCase.threatScore ?? 0}
                    <span className="ml-1 text-xs text-slate-300">/100</span>
                  </span>
                </div>

                <div className="h-1.5 w-full overflow-hidden bg-slate-100">
                  <div
                    className={`h-full transition-all duration-1000 ${
                      (emailCase.threatScore ?? 0) > 70
                        ? 'bg-red-600'
                        : (emailCase.threatScore ?? 0) > 40
                          ? 'bg-amber-600'
                          : 'bg-emerald-600'
                    }`}
                    style={{ width: `${emailCase.threatScore ?? 0}%` }}
                  />
                </div>

                <div className="border-l-4 border-slate-900 bg-slate-50 p-6">
                  <p className="text-[10px] font-bold uppercase tracking-[0.15em] leading-relaxed text-slate-600">
                    {(emailCase.threatScore ?? 0) > 70
                      ? 'CRITICAL: Artifact exhibits high-confidence indicators of malicious intent or unauthorized spoofing.'
                      : (emailCase.threatScore ?? 0) > 40
                        ? 'WARNING: Artifact contains anomalies requiring secondary analyst verification.'
                        : 'VERIFIED: No significant deviations from security specifications identified.'}
                  </p>
                </div>

              </div>
            </div>

            {/* Indicator Matrix */}
            <div className="border border-slate-200 bg-white">
              <div className="border-b border-slate-200 bg-slate-50 px-8 py-4">
                <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
                  Indicator Matrix
                </h3>
              </div>

              <div className="space-y-6 p-8">
                <IndicatorStat
                  label="MALICIOUS ENDPOINTS"
                  count={
                    emailCase.indicators?.filter(
                      (i) => i.type === 'URL' && (i.vtMalicious ?? 0) > 0
                    ).length ?? 0
                  }
                  color="text-red-600"
                />

                <IndicatorStat
                  label="SUSPICIOUS INFRASTRUCTURE"
                  count={0}
                  color="text-amber-600"
                />

                <IndicatorStat
                  label="PROTOCOL FAILURES"
                  count={
                    [
                      emailCase.header?.spfStatus,
                      emailCase.header?.dkimStatus,
                      emailCase.header?.dmarcStatus
                    ].filter((s) => s === 'fail').length
                  }
                  color="text-red-600"
                />
              </div>
            </div>

            {/* Forensic Triage */}
            <div className="bg-slate-900 p-8 text-white">
              <Info className="mb-6 h-8 w-8 text-indigo-400" />

              <h3 className="mb-4 text-xs font-black uppercase tracking-[0.3em]">
                Forensic Triage Complete
              </h3>

              <p className="text-[10px] font-bold uppercase tracking-[0.2em] leading-relaxed text-slate-400">
                Automated analysis sequence finished. Peer review of routing and indicator metrics is mandatory for final classification.
              </p>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

function IndicatorStat({
  label,
  count,
  color
}: {
  label: string;
  count: number;
  color: string;
}) {
  return (
    <div className="flex items-center justify-between py-1">
      <span className="text-[9px] font-black uppercase tracking-[0.2em] text-slate-500">
        {label}
      </span>

      <span className={`text-sm font-black ${color}`}>
        {count}
      </span>
    </div>
  );
}