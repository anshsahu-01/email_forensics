'use client';

import {
  AlertTriangle,
  Brain,
  CheckCircle2,
  FileText,
  ShieldAlert,
} from 'lucide-react';

export default function AITestPage() {
  return (
    <div className="min-h-[calc(100vh-80px)] bg-slate-100 px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-5xl">
        {/* Development Header */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <Brain className="h-4 w-4 text-indigo-600" />

              <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-500">
                AI Investigation
              </span>
            </div>

            <h1 className="mt-2 text-xl font-semibold text-slate-950">
              Investigation Report Preview
            </h1>
          </div>

          <div className="border border-slate-300 bg-white px-3 py-2">
            <span className="text-[9px] font-bold uppercase tracking-widest text-slate-500">
              PDF Preview
            </span>
          </div>
        </div>

        {/* PDF Paper */}
        <article className="bg-white px-8 py-10 shadow-sm sm:px-12 sm:py-12 lg:px-16">
          {/* Report Header */}
          <header className="border-b-2 border-slate-950 pb-6">
            <div className="flex items-start justify-between gap-6">
              <div>
                <p className="text-[9px] font-bold uppercase tracking-[0.25em] text-slate-500">
                  Digital Forensics Investigation
                </p>

                <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950">
                  AI Email Threat Analysis
                </h2>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
                  Automated forensic interpretation of the analyzed
                  email evidence and associated threat intelligence.
                </p>
              </div>

              <FileText className="hidden h-8 w-8 text-slate-300 sm:block" />
            </div>

            {/* Report Metadata */}
            <div className="mt-6 grid grid-cols-2 gap-x-8 gap-y-4 border-t border-slate-200 pt-5 sm:grid-cols-4">
              <Meta label="Case ID" value="#CASE-00051" />
              <Meta label="Analysis Date" value="08 Sep 2026" />
              <Meta label="Risk Level" value="HIGH" />
              <Meta label="AI Status" value="COMPLETED" />
            </div>
          </header>

          {/* Executive Summary */}
          <ReportSection
            number="01"
            title="Executive Summary"
          >
            <p>
              The analyzed email exhibits multiple indicators
              associated with potentially malicious or suspicious
              activity. Authentication results, sender information,
              routing infrastructure and external threat intelligence
              were evaluated by the forensic analysis pipeline.
            </p>

            <div className="mt-5 border-l-4 border-amber-500 bg-amber-50 px-5 py-4">
              <div className="flex items-start gap-3">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />

                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-amber-800">
                    AI Assessment
                  </p>

                  <p className="mt-1 text-sm leading-6 text-amber-900">
                    The email should be treated as suspicious and
                    investigated further before any user interaction
                    with links or attachments.
                  </p>
                </div>
              </div>
            </div>
          </ReportSection>

          {/* Threat Assessment */}
          <ReportSection
            number="02"
            title="Threat Assessment"
          >
            <div className="grid gap-4 sm:grid-cols-3">
              <AssessmentCard
                label="Overall Risk"
                value="HIGH"
                icon={<ShieldAlert className="h-4 w-4" />}
              />

              <AssessmentCard
                label="Confidence"
                value="87%"
                icon={<Brain className="h-4 w-4" />}
              />

              <AssessmentCard
                label="Threat Indicators"
                value="04"
                icon={<AlertTriangle className="h-4 w-4" />}
              />
            </div>

            <p className="mt-5">
              Based on the available forensic evidence, the AI
              assessment indicates that the case contains sufficient
              suspicious characteristics to warrant additional
              investigation.
            </p>
          </ReportSection>

          {/* Evidence Analysis */}
          <ReportSection
            number="03"
            title="Forensic Evidence Analysis"
          >
            <EvidenceRow
              label="Sender Authentication"
              status="SUSPICIOUS"
              description="Authentication results contain one or more failed or inconsistent checks."
            />

            <EvidenceRow
              label="Email Routing"
              status="REVIEW"
              description="Routing infrastructure was evaluated to identify the apparent originating and connecting systems."
            />

            <EvidenceRow
              label="Threat Intelligence"
              status="FLAGGED"
              description="External intelligence sources returned indicators requiring investigation."
            />

            <EvidenceRow
              label="Sender Spoofing"
              status="DETECTED"
              description="The analyzed sender identity presents characteristics consistent with possible spoofing."
            />
          </ReportSection>

          {/* Network Intelligence */}
          <ReportSection
            number="04"
            title="Network & Infrastructure Intelligence"
          >
            <div className="overflow-hidden border border-slate-200">
              <InfoRow label="Originating IP" value="209.85.220.41" />
              <InfoRow label="Network" value="Google LLC / AS15169" />
              <InfoRow label="Country" value="United States" />
              <InfoRow label="City" value="Approximate / Unresolved" />
              <InfoRow label="Timezone" value="America/Chicago" />
            </div>

            <p className="mt-4 text-xs leading-5 text-slate-500">
              Geographic and network information represents
              infrastructure attribution and should not be interpreted
              as the physical location of the email sender.
            </p>
          </ReportSection>

          {/* AI Findings */}
          <ReportSection
            number="05"
            title="AI Findings"
          >
            <div className="space-y-4">
              <Finding
                title="Authentication Analysis"
                text="The authentication evidence should be considered alongside sender identity and routing information rather than evaluated in isolation."
              />

              <Finding
                title="Infrastructure Analysis"
                text="The observed network infrastructure appears to correspond to email delivery infrastructure. This does not independently establish the physical location of the sender."
              />

              <Finding
                title="Threat Analysis"
                text="The combined evidence increases the likelihood that the message requires additional security review."
              />
            </div>
          </ReportSection>

          {/* Conclusion */}
          <ReportSection
            number="06"
            title="Conclusion & Recommendation"
          >
            <div className="border border-slate-300 p-5">
              <div className="flex items-start gap-3">
                <CheckCircle2 className="mt-0.5 h-5 w-5 text-slate-700" />

                <div>
                  <p className="text-sm font-semibold text-slate-900">
                    Recommended Action
                  </p>

                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    Preserve the original email as evidence and
                    perform additional investigation of the identified
                    indicators before considering the message safe.
                  </p>
                </div>
              </div>
            </div>
          </ReportSection>

          {/* Disclaimer */}
          <footer className="mt-10 border-t border-slate-200 pt-5">
            <p className="text-[9px] leading-5 text-slate-400">
              This report is generated from automated forensic analysis
              and AI-assisted interpretation. Findings should be
              validated against the original evidence and relevant
              investigative context.
            </p>

            <div className="mt-5 flex justify-between text-[9px] font-bold uppercase tracking-widest text-slate-400">
              <span>Email Forensics Platform</span>
              <span>AI Investigation Report</span>
            </div>
          </footer>
        </article>
      </div>
    </div>
  );
}

/* ---------- Components ---------- */

function Meta({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div>
      <p className="text-[9px] font-bold uppercase tracking-widest text-slate-400">
        {label}
      </p>

      <p className="mt-1 text-xs font-semibold text-slate-900">
        {value}
      </p>
    </div>
  );
}

function ReportSection({
  number,
  title,
  children,
}: {
  number: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="mt-10">
      <div className="mb-4 flex items-baseline gap-3 border-b border-slate-200 pb-3">
        <span className="text-[9px] font-bold tracking-widest text-indigo-600">
          {number}
        </span>

        <h3 className="text-sm font-bold uppercase tracking-wider text-slate-950">
          {title}
        </h3>
      </div>

      <div className="text-sm leading-7 text-slate-600">
        {children}
      </div>
    </section>
  );
}

function AssessmentCard({
  label,
  value,
  icon,
}: {
  label: string;
  value: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="border border-slate-200 p-4">
      <div className="flex items-center gap-2 text-slate-400">
        {icon}

        <span className="text-[9px] font-bold uppercase tracking-widest">
          {label}
        </span>
      </div>

      <p className="mt-3 text-xl font-bold text-slate-950">
        {value}
      </p>
    </div>
  );
}

function EvidenceRow({
  label,
  status,
  description,
}: {
  label: string;
  status: string;
  description: string;
}) {
  return (
    <div className="border-b border-slate-200 py-4 last:border-b-0">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-xs font-bold text-slate-900">
            {label}
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-500">
            {description}
          </p>
        </div>

        <span className="shrink-0 text-[9px] font-bold uppercase tracking-widest text-slate-600">
          {status}
        </span>
      </div>
    </div>
  );
}

function InfoRow({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="grid grid-cols-1 border-b border-slate-200 px-4 py-3 last:border-b-0 sm:grid-cols-3">
      <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
        {label}
      </span>

      <span className="mt-1 text-xs font-medium text-slate-800 sm:col-span-2 sm:mt-0">
        {value}
      </span>
    </div>
  );
}

function Finding({
  title,
  text,
}: {
  title: string;
  text: string;
}) {
  return (
    <div className="border-l-2 border-slate-300 pl-4">
      <p className="text-xs font-bold text-slate-900">
        {title}
      </p>

      <p className="mt-1 text-xs leading-6 text-slate-600">
        {text}
      </p>
    </div>
  );
}