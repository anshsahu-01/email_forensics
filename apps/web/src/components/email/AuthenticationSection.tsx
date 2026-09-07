import React from 'react';
import { EmailHeader } from '@/types';
import { ShieldCheck, ShieldAlert, Shield } from 'lucide-react';

interface AuthenticationSectionProps {
  header: EmailHeader | null;
}

export default function AuthenticationSection({ header }: AuthenticationSectionProps) {
  if (!header) return null;

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-sm font-bold text-slate-900 mb-6 flex items-center gap-2">
        <ShieldCheck className="h-4 w-4 text-indigo-500" />
        Authentication Checks
      </h2>

      <div className="grid gap-4 sm:grid-cols-3">
        <AuthBadge label="SPF" status={header.spfStatus} />
        <AuthBadge label="DKIM" status={header.dkimStatus} />
        <AuthBadge label="DMARC" status={header.dmarcStatus} />
      </div>

      <div className="mt-6 rounded-xl bg-slate-50 p-4 border border-slate-100">
        <p className="text-[11px] text-slate-500 leading-relaxed">
          <span className="font-bold text-slate-700">Note:</span> These results are parsed from the <code className="bg-slate-200 px-1 rounded">Authentication-Results</code> or <code className="bg-slate-200 px-1 rounded">Received-SPF</code> headers provided by the receiving mail server. They represent the server&apos;s own verification at the time of delivery.
        </p>
      </div>
    </div>
  );
}

function AuthBadge({ label, status }: { label: string; status: string | null }) {
  const normStatus = (status || 'none').toLowerCase();

  let styles = 'bg-slate-100 text-slate-600 border-slate-200';
  let Icon = Shield;
  const labelText = status || 'NONE';

  if (normStatus === 'pass') {
    styles = 'bg-emerald-50 text-emerald-700 border-emerald-200';
    Icon = ShieldCheck;
  } else if (normStatus === 'fail' || normStatus === 'softfail') {
    styles = 'bg-red-50 text-red-700 border-red-200';
    Icon = ShieldAlert;
  } else if (normStatus !== 'none') {
    styles = 'bg-amber-50 text-amber-700 border-amber-200';
  }

  return (
    <div className={`flex flex-col items-center justify-center rounded-xl border p-4 transition ${styles}`}>
      <Icon className="h-5 w-5 mb-2" />
      <span className="text-[10px] font-bold uppercase tracking-widest opacity-70">{label}</span>
      <span className="mt-1 text-sm font-bold">{labelText.toUpperCase()}</span>
    </div>
  );
}
