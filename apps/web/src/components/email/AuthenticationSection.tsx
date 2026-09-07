import React from 'react';
import { EmailHeader } from '@/types';
import { ShieldCheck, ShieldAlert, Shield } from 'lucide-react';

interface AuthenticationSectionProps {
  header: EmailHeader | null;
}

export default function AuthenticationSection({ header }: AuthenticationSectionProps) {
  if (!header) return null;

  return (
    <div className="border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-8 py-4 bg-slate-50">
        <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-indigo-600" />
          Protocol Verification (SPF/DKIM/DMARC)
        </h2>
      </div>

      <div className="p-8 grid gap-6 sm:grid-cols-3">
        <AuthBadge label="SPF Alignment" status={header.spfStatus} />
        <AuthBadge label="DKIM Signature" status={header.dkimStatus} />
        <AuthBadge label="DMARC Policy" status={header.dmarcStatus} />
      </div>

      <div className="mx-8 mb-8 bg-slate-50 border-l-4 border-slate-900 p-6">
        <p className="text-[10px] font-bold text-slate-500 leading-relaxed uppercase tracking-widest">
          <span className="text-slate-900">Technical Note:</span> Results are parsed from the <code className="bg-slate-200 px-1 font-mono text-slate-700">Authentication-Results</code> header as observed at the point of ingestion.
        </p>
      </div>
    </div>
  );
}

function AuthBadge({ label, status }: { label: string; status: string | null }) {
  const normStatus = (status || 'none').toLowerCase();

  let styles = 'border-slate-200 text-slate-400 bg-slate-50';
  let Icon = Shield;
  const labelText = status || 'NOT OBSERVED';

  if (normStatus === 'pass') {
    styles = 'border-emerald-600 text-emerald-600 bg-emerald-50/30';
    Icon = ShieldCheck;
  } else if (normStatus === 'fail' || normStatus === 'softfail') {
    styles = 'border-red-600 text-red-600 bg-red-50/30';
    Icon = ShieldAlert;
  } else if (normStatus !== 'none') {
    styles = 'border-amber-600 text-amber-600 bg-amber-50/30';
  }

  return (
    <div className={`flex flex-col items-center justify-center border-2 py-6 px-4 transition-all ${styles}`}>
      <Icon className="h-6 w-6 mb-3" />
      <span className="text-[9px] font-black uppercase tracking-[0.3em] opacity-80">{label}</span>
      <span className="mt-2 text-sm font-black uppercase tracking-tighter">{labelText}</span>
    </div>
  );
}
