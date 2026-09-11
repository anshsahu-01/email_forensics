'use client';

import { use, useEffect, useState, type ReactNode } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Download,
  Share2,
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  Info,
  Hash,
  Globe,
  MapPin,
  Loader2,
  Brain,
  Link2,
  Database,
  Crosshair,
  FileSearch,
} from 'lucide-react';

import { fetchCaseById } from '@/lib/api';
import { EmailCase } from '@/types';
import RiskBadge from '@/components/common/RiskBadge';

import CaseHeader from '@/components/email/CaseHeader';
import AuthenticationSection from '@/components/email/AuthenticationSection';
import RoutingSection from '@/components/email/RoutingSection';
import IndicatorsSection from '@/components/email/IndicatorsSection';
import RawContentSection from '@/components/email/RawContentSection';

type JsonValue =
  | string
  | number
  | boolean
  | null
  | JsonValue[]
  | { [key: string]: JsonValue };

function parseJson(value: string | null | undefined): JsonValue | null {
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value) as JsonValue;
  } catch {
    return value;
  }
}

function prettyJson(value: string | null | undefined): string {
  const parsed = parseJson(value);

  if (parsed === null) {
    return 'NO DATA AVAILABLE';
  }

  if (typeof parsed === 'string') {
    return parsed;
  }

  return JSON.stringify(parsed, null, 2);
}

function countJsonItems(value: string | null | undefined): number {
  const parsed = parseJson(value);

  if (Array.isArray(parsed)) {
    return parsed.length;
  }

  if (parsed && typeof parsed === 'object') {
    return Object.keys(parsed).length;
  }

  return parsed ? 1 : 0;
}

function getVerdictClasses(verdict: string | null | undefined) {
  const value = (verdict || '').toUpperCase();

  if (value === 'MALICIOUS') {
    return 'border-red-200 bg-red-50 text-red-800';
  }

  if (value === 'SUSPICIOUS') {
    return 'border-amber-200 bg-amber-50 text-amber-800';
  }

  return 'border-emerald-200 bg-emerald-50 text-emerald-800';
}

function AICodeBlock({
  title,
  icon,
  value,
}: {
  title: string;
  icon: ReactNode;
  value: string | null | undefined;
}) {
  return (
    <section className="border border-slate-200 bg-white">
      <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-6 py-4">
        <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
          {icon}
          {title}
        </h3>
      </div>

      <pre className="max-h-[420px] overflow-auto whitespace-pre-wrap break-words p-6 font-mono text-[10px] leading-relaxed text-slate-700">
        {prettyJson(value)}
      </pre>
    </section>
  );
}

function MetricCard({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="border border-slate-200 bg-white p-6">
      <div className="mb-5 text-slate-500">{icon}</div>

      <p className="text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
        {label}
      </p>

      <p className="mt-2 break-words text-xl font-black uppercase tracking-tight text-slate-900">
        {value}
      </p>
    </div>
  );
}

function MiniMetric({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="border border-slate-100 bg-slate-50 p-4">
      <p className="text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
        {label}
      </p>

      <p className="mt-1 break-words text-[10px] font-black uppercase text-slate-800">
        {value}
      </p>
    </div>
  );
}

export default function CaseDetailsPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  const [emailCase, setEmailCase] = useState<EmailCase | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [downloadingReport, setDownloadingReport] = useState(false);

  const [activeTab, setActiveTab] = useState<
    'overview' | 'ai' | 'routing' | 'indicators' | 'raw'
  >('overview');

  useEffect(() => {
    const loadCase = async () => {
      try {
        setLoading(true);
        setError(null);

        const data = await fetchCaseById(id);

        setEmailCase(data);
      } catch (err) {
        console.error('Failed to load case:', err);

        setError(
          'Case record could not be retrieved from secure storage.'
        );
      } finally {
        setLoading(false);
      }
    };

    void loadCase();
  }, [id]);

  /**
   * Download the forensic PDF report for the current case.
   *
   * Flow:
   * Frontend
   *   -> Spring Boot
   *   -> ReportController
   *   -> ReportService
   *   -> PDF bytes
   *   -> Browser download
   */
  const handleDownloadReport = async () => {
    try {
      setDownloadingReport(true);

      const response = await fetch(
        `http://localhost:5000/api/v1/cases/${id}/report`,
        {
          method: 'GET',
        }
      );

      if (!response.ok) {
        throw new Error(
          `Report generation failed with status ${response.status}`
        );
      }

      const blob = await response.blob();

      if (blob.size === 0) {
        throw new Error('Generated PDF is empty.');
      }

      const downloadUrl = window.URL.createObjectURL(blob);

      const link = document.createElement('a');

      link.href = downloadUrl;
      link.download = `email-forensic-report-${id}.pdf`;

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(downloadUrl);
    } catch (err) {
      console.error(
        'Failed to download forensic report:',
        err
      );

      window.alert(
        'Forensic PDF could not be generated. Please check the backend logs.'
      );
    } finally {
      setDownloadingReport(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white">
        <Loader2 className="h-10 w-10 animate-spin text-slate-900" />

        <p className="mt-6 text-[10px] font-black uppercase tracking-[0.3em] text-slate-400">
          Loading forensic report...
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
          {error ||
            'The requested forensic report could not be retrieved.'}
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

  const score = emailCase.threatScore ?? 0;

  const verdict =
    emailCase.aiVerdict || 'NOT CLASSIFIED';

  const riskLevel =
    emailCase.aiRiskLevel || 'NOT AVAILABLE';

  const confidence =
    emailCase.aiConfidence;

  const ipIndicator = emailCase.indicators?.find(
    (indicator) =>
      indicator.type?.toUpperCase() === 'IP'
  );

  return (
    <div className="min-h-full bg-white pb-24">

      {/* =====================================================
          HEADER
      ===================================================== */}

      <div className="sticky top-0 z-20 border-b-2 border-slate-900 bg-white/95 backdrop-blur-sm">
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

                <div className="flex flex-wrap items-center gap-4">

                  <h1 className="text-2xl font-black uppercase tracking-tighter text-slate-900">
                    REPORT-{emailCase.id}
                  </h1>

                  <RiskBadge score={score} />

                </div>

                <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">
                  Investigation log active since{' '}
                  {new Date(
                    emailCase.createdAt
                  ).toLocaleString()}
                </p>

              </div>

            </div>

            <div className="flex items-center gap-4">

              {/* PDF EXPORT */}

              <button
                type="button"
                onClick={handleDownloadReport}
                disabled={downloadingReport}
                className="inline-flex items-center gap-2 border-2 border-slate-200 bg-white px-5 py-2.5 text-[10px] font-black uppercase tracking-widest text-slate-900 transition-all hover:border-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {downloadingReport ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Download className="h-4 w-4" />
                )}

                {downloadingReport
                  ? 'Generating...'
                  : 'Export'}
              </button>

              <button
                type="button"
                className="inline-flex items-center gap-2 border-2 border-slate-200 bg-white px-5 py-2.5 text-[10px] font-black uppercase tracking-widest text-slate-900 transition-all hover:border-slate-900"
              >
                <Share2 className="h-4 w-4" />
                Transmit
              </button>

            </div>

          </div>

        </div>
      </div>


      <div className="mx-auto max-w-7xl px-8 py-12">

        {/* =====================================================
            AI VERDICT
        ===================================================== */}

        <section
          className={`mb-10 border p-6 ${getVerdictClasses(
            emailCase.aiVerdict
          )}`}
        >

          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">

            <div className="flex items-start gap-4">

              <Brain className="mt-1 h-7 w-7 shrink-0" />

              <div>

                <p className="text-[9px] font-black uppercase tracking-[0.25em]">
                  AI Forensic Assessment
                </p>

                <div className="mt-2 flex flex-wrap items-center gap-3">

                  <h2 className="text-2xl font-black uppercase tracking-tight">
                    {verdict}
                  </h2>

                  <span className="border border-current px-2 py-1 text-[9px] font-black uppercase tracking-widest">
                    {riskLevel}
                  </span>

                </div>

                <p className="mt-3 max-w-3xl text-sm font-medium leading-relaxed">
                  {emailCase.aiSummary ||
                    'No AI summary was stored for this case.'}
                </p>

              </div>

            </div>

            <div className="min-w-[170px] border border-current/20 bg-white/50 p-5">

              <p className="text-[9px] font-black uppercase tracking-widest opacity-70">
                AI Confidence
              </p>

              <p className="mt-1 text-3xl font-black">
                {confidence !== null &&
                confidence !== undefined
                  ? `${confidence}%`
                  : '—'}
              </p>

            </div>

          </div>

        </section>


        {/* =====================================================
            TABS
        ===================================================== */}

        <div className="mb-10 flex flex-wrap gap-px border border-slate-200 bg-slate-200">

          {(
            [
              'overview',
              'ai',
              'routing',
              'indicators',
              'raw',
            ] as const
          ).map((tab) => (

            <button
              key={tab}
              type="button"
              onClick={() => setActiveTab(tab)}
              className={`min-w-[120px] flex-1 px-5 py-4 text-[10px] font-black uppercase tracking-[0.2em] transition-all ${
                activeTab === tab
                  ? 'bg-slate-900 text-white'
                  : 'bg-white text-slate-400 hover:bg-slate-50 hover:text-slate-900'
              }`}
            >
              {tab === 'ai'
                ? 'AI Forensics'
                : tab}
            </button>

          ))}

        </div>


        {/* =====================================================
            OVERVIEW
        ===================================================== */}

        {activeTab === 'overview' && (

          <div className="grid grid-cols-1 gap-12 lg:grid-cols-3">

            <div className="space-y-10 lg:col-span-2">

              <CaseHeader
                emailCase={emailCase}
              />

              <AuthenticationSection
                header={emailCase.header}
              />


              {/* Infrastructure */}

              <div className="grid gap-8 sm:grid-cols-2">

                <div className="border border-slate-200 bg-white">

                  <div className="border-b border-slate-200 bg-slate-50 px-8 py-4">

                    <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">

                      <Globe className="h-4 w-4 text-indigo-600" />

                      Infrastructure Intelligence

                    </h3>

                  </div>


                  <div className="space-y-8 p-8">

                    <div>

                      <p className="mb-2 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                        Network Origin IP
                      </p>

                      <p className="border border-slate-100 bg-slate-50 p-3 font-mono text-sm font-black text-slate-900">
                        {emailCase.originatingIp ||
                          'DATA NOT DETECTED'}
                      </p>

                    </div>


                    <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">

                      <div>

                        <p className="mb-3 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                          Geo-Location
                        </p>

                        <div className="space-y-2">

                          <p className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-widest text-slate-900">

                            <MapPin className="h-3.5 w-3.5 text-slate-300" />

                            {emailCase.geoCountry ||
                              'UNKNOWN COUNTRY'}

                          </p>

                          <p className="text-[10px] font-bold uppercase tracking-widest text-slate-500">
                            {emailCase.geoCity ||
                              'UNKNOWN CITY'}
                          </p>

                          <p className="font-mono text-[10px] text-slate-500">
                            {emailCase.geoTimezone ||
                              'TIMEZONE UNAVAILABLE'}
                          </p>

                        </div>

                      </div>


                      <div>

                        <p className="mb-3 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                          ASN / Network
                        </p>

                        <div className="space-y-2">

                          <p className="font-mono text-[11px] font-black text-slate-900">
                            {ipIndicator?.asnNumber ||
                              'ASN UNDEFINED'}
                          </p>

                          <p className="break-words text-[10px] font-bold uppercase tracking-widest text-slate-500">
                            {ipIndicator?.asnOrg ||
                              'NETWORK ORGANIZATION UNDEFINED'}
                          </p>

                        </div>

                      </div>

                    </div>


                    {/* RDAP */}

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

                        {[
                          [
                            'Registry',
                            ipIndicator?.rdapRegistry,
                          ],
                          [
                            'RDAP Server',
                            ipIndicator?.rdapServer,
                          ],
                          [
                            'Registry Handle',
                            ipIndicator?.rdapHandle,
                          ],
                          [
                            'Network Name',
                            ipIndicator?.rdapName,
                          ],
                          [
                            'Organization',
                            ipIndicator?.rdapOrganization,
                          ],
                          [
                            'Registry Country',
                            ipIndicator?.rdapCountry,
                          ],
                          [
                            'CIDR',
                            ipIndicator?.rdapCidr,
                          ],
                          [
                            'Address Range',
                            ipIndicator?.rdapStartAddress &&
                            ipIndicator?.rdapEndAddress
                              ? `${ipIndicator.rdapStartAddress} — ${ipIndicator.rdapEndAddress}`
                              : null,
                          ],
                        ].map(([label, value]) => (

                          <div key={String(label)}>

                            <p className="mb-1 text-[8px] font-black uppercase tracking-[0.15em] text-slate-400">
                              {label}
                            </p>

                            <p className="break-all font-mono text-[10px] font-bold text-slate-700">
                              {value ||
                                'NOT AVAILABLE'}
                            </p>

                          </div>

                        ))}

                      </div>

                    </div>

                  </div>

                </div>


                {/* Integrity */}

                <div className="border border-slate-200 bg-white">

                  <div className="border-b border-slate-200 bg-slate-50 px-8 py-4">

                    <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">

                      <Hash className="h-4 w-4 text-indigo-600" />

                      Integrity Artifacts

                    </h3>

                  </div>


                  <div className="space-y-8 p-8">

                    <div>

                      <p className="mb-2 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                        SHA-256 Digest
                      </p>

                      <p className="break-all border border-slate-100 bg-slate-50 p-4 font-mono text-[11px] font-bold leading-relaxed text-slate-600">
                        {emailCase.fileHash ||
                          'CHECKSUM PENDING'}
                      </p>

                    </div>


                    <div>

                      <p className="mb-2 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                        Analysis Status
                      </p>

                      <p className="border border-slate-100 bg-slate-50 p-4 text-[10px] font-black uppercase tracking-widest text-slate-700">
                        {emailCase.analysisStatus}
                      </p>

                    </div>


                    <div>

                      <p className="mb-2 text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">
                        Sender
                      </p>

                      <p className="break-all border border-slate-100 bg-slate-50 p-4 text-[10px] font-bold text-slate-700">
                        {emailCase.header?.senderFrom ||
                          'NOT AVAILABLE'}
                      </p>

                    </div>

                  </div>

                </div>

              </div>

            </div>


            {/* Sidebar */}

            <aside className="space-y-10">

              <div className="border border-slate-200 bg-white">

                <div className="border-b border-slate-200 bg-slate-50 px-8 py-4">

                  <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">
                    AI Risk Assessment
                  </h3>

                </div>


                <div className="space-y-7 p-8">

                  <div className="flex items-center justify-between">

                    <span className="text-[10px] font-black uppercase tracking-widest text-slate-500">
                      Threat Score
                    </span>

                    <span className="text-lg font-black text-slate-900">
                      {score}
                      <span className="ml-1 text-xs text-slate-300">
                        /100
                      </span>
                    </span>

                  </div>


                  <div className="h-2 w-full overflow-hidden bg-slate-100">

                    <div
                      className={`h-full ${
                        score > 70
                          ? 'bg-red-600'
                          : score > 40
                            ? 'bg-amber-600'
                            : 'bg-emerald-600'
                      }`}
                      style={{
                        width: `${Math.max(
                          0,
                          Math.min(100, score)
                        )}%`,
                      }}
                    />

                  </div>


                  <div className="grid grid-cols-2 gap-3">

                    <MiniMetric
                      label="VERDICT"
                      value={verdict}
                    />

                    <MiniMetric
                      label="RISK"
                      value={riskLevel}
                    />

                    <MiniMetric
                      label="CONFIDENCE"
                      value={
                        confidence !== null &&
                        confidence !== undefined
                          ? `${confidence}%`
                          : '—'
                      }
                    />

                    <MiniMetric
                      label="AI INDICATORS"
                      value={String(
                        countJsonItems(
                          emailCase.aiIndicators
                        )
                      )}
                    />

                  </div>

                </div>

              </div>


              <div className="bg-slate-900 p-8 text-white">

                <Brain className="mb-6 h-8 w-8 text-indigo-400" />

                <h3 className="mb-4 text-xs font-black uppercase tracking-[0.3em]">
                  AI Investigation Available
                </h3>

                <p className="text-[10px] font-bold uppercase tracking-[0.2em] leading-relaxed text-slate-400">
                  Open the AI Forensics tab to inspect
                  the structured AI assessment, indicators,
                  attack techniques, IOCs, origin intelligence,
                  URL analysis, WHOIS intelligence and CISA
                  threat intelligence.
                </p>

              </div>

            </aside>

          </div>

        )}


        {/* =====================================================
            AI FORENSICS
        ===================================================== */}

        {activeTab === 'ai' && (

          <div className="space-y-8">

            {/* AI metrics */}

            <div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-4">

              <MetricCard
                icon={
                  <ShieldAlert className="h-5 w-5" />
                }
                label="Threat Score"
                value={`${score}/100`}
              />

              <MetricCard
                icon={
                  <AlertTriangle className="h-5 w-5" />
                }
                label="Risk Level"
                value={riskLevel}
              />

              <MetricCard
                icon={
                  <ShieldCheck className="h-5 w-5" />
                }
                label="Verdict"
                value={verdict}
              />

              <MetricCard
                icon={
                  <Brain className="h-5 w-5" />
                }
                label="Confidence"
                value={
                  confidence !== null &&
                  confidence !== undefined
                    ? `${confidence}%`
                    : '—'
                }
              />

            </div>


            {/* Summary */}

            <section className="border border-slate-200 bg-white">

              <div className="border-b border-slate-200 bg-slate-50 px-6 py-4">

                <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">

                  <Brain className="h-4 w-4 text-indigo-600" />

                  Llama Forensic Summary

                </h3>

              </div>

              <div className="p-6">

                <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
                  {emailCase.aiSummary ||
                    'NO AI SUMMARY AVAILABLE'}
                </p>

              </div>

            </section>


            {/* AI structured data */}

            <div className="grid gap-8 lg:grid-cols-2">

              <AICodeBlock
                title="AI Indicators"
                icon={
                  <Crosshair className="h-4 w-4 text-red-600" />
                }
                value={emailCase.aiIndicators}
              />

              <AICodeBlock
                title="Attack Techniques / MITRE"
                icon={
                  <ShieldAlert className="h-4 w-4 text-amber-600" />
                }
                value={emailCase.aiAttackTechniques}
              />

              <AICodeBlock
                title="IOCs"
                icon={
                  <Hash className="h-4 w-4 text-indigo-600" />
                }
                value={emailCase.aiIocs}
              />

              <AICodeBlock
                title="Origin Analysis"
                icon={
                  <MapPin className="h-4 w-4 text-indigo-600" />
                }
                value={emailCase.aiOriginAnalysis}
              />

              <AICodeBlock
                title="URL Analysis"
                icon={
                  <Link2 className="h-4 w-4 text-blue-600" />
                }
                value={emailCase.aiUrlAnalysis}
              />

              <AICodeBlock
                title="WHOIS / Domain Intelligence"
                icon={
                  <Globe className="h-4 w-4 text-emerald-600" />
                }
                value={emailCase.aiWhoisAnalysis}
              />

              <AICodeBlock
                title="CISA / RAG Intelligence"
                icon={
                  <Database className="h-4 w-4 text-purple-600" />
                }
                value={emailCase.aiRagInsights}
              />

              <AICodeBlock
                title="Graph Data"
                icon={
                  <FileSearch className="h-4 w-4 text-slate-600" />
                }
                value={emailCase.aiGraphs}
              />

            </div>


            {/* Reasoning */}

            <section className="border border-slate-200 bg-white">

              <div className="border-b border-slate-200 bg-slate-50 px-6 py-4">

                <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-slate-900">

                  <Info className="h-4 w-4 text-indigo-600" />

                  AI Reasoning

                </h3>

              </div>

              <pre className="max-h-[420px] overflow-auto whitespace-pre-wrap break-words p-6 font-mono text-[10px] leading-relaxed text-slate-700">
                {prettyJson(emailCase.aiReasoning)}
              </pre>

            </section>


            {/* AI error */}

            {emailCase.aiError && (

              <section className="border border-red-200 bg-red-50 p-6 text-red-800">

                <p className="text-[9px] font-black uppercase tracking-[0.2em]">
                  AI Service Error
                </p>

                <p className="mt-2 whitespace-pre-wrap text-sm">
                  {emailCase.aiError}
                </p>

              </section>

            )}

          </div>

        )}


        {/* =====================================================
            ROUTING
        ===================================================== */}

        {activeTab === 'routing' && (
          <RoutingSection
            receivedHeaders={
              emailCase.receivedHeaders
            }
          />
        )}


        {/* =====================================================
            INDICATORS
        ===================================================== */}

        {activeTab === 'indicators' && (
          <IndicatorsSection
            indicators={emailCase.indicators}
          />
        )}


        {/* =====================================================
            RAW EMAIL
        ===================================================== */}

        {activeTab === 'raw' && (
          <RawContentSection
            content={emailCase.rawBody}
          />
        )}

      </div>
    </div>
  );
}