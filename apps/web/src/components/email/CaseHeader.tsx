import React from 'react';
import { EmailCase } from '@/types';
import { Mail, User, Send, Calendar, Hash, FileCode } from 'lucide-react';

interface CaseHeaderProps {
  emailCase: EmailCase;
}

export default function CaseHeader({ emailCase }: CaseHeaderProps) {
  const { header } = emailCase;

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-sm font-bold text-slate-900 mb-6 flex items-center gap-2">
        <Mail className="h-4 w-4 text-indigo-500" />
        Email Metadata
      </h2>

      <div className="grid gap-6 sm:grid-cols-2">
        <div className="space-y-4">
          <DetailItem
            label="Subject"
            value={header?.subject}
            icon={Mail}
            isHighlighted
          />
          <DetailItem
            label="Sender (From)"
            value={header?.senderFrom}
            icon={User}
          />
          <DetailItem
            label="Recipient (To)"
            value={header?.to}
            icon={Send}
          />
        </div>

        <div className="space-y-4">
          <DetailItem
            label="Date Sent"
            value={header?.date}
            icon={Calendar}
          />
          <DetailItem
            label="Message-ID"
            value={header?.messageId}
            icon={Hash}
            isMono
          />
          <DetailItem
            label="Return-Path"
            value={header?.returnPath}
            icon={FileCode}
            isMono
          />
        </div>
      </div>

      {header?.cc && (
        <div className="mt-4 pt-4 border-t border-slate-50">
           <DetailItem
            label="CC"
            value={header.cc}
            icon={Send}
          />
        </div>
      )}
    </div>
  );
}

function DetailItem({
  label,
  value,
  icon: Icon,
  isHighlighted = false,
  isMono = false
}: {
  label: string,
  value: string | null | undefined,
  icon: React.ComponentType<{ className?: string }>,
  isHighlighted?: boolean,
  isMono?: boolean
}) {
  return (
    <div>
      <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1 flex items-center gap-1.5">
        <Icon className="h-3 w-3" />
        {label}
      </p>
      <p className={`text-sm break-all ${
        isHighlighted ? 'font-bold text-slate-900' : 'text-slate-700'
      } ${isMono ? 'font-mono text-[13px]' : ''}`}>
        {value || 'Not available'}
      </p>
    </div>
  );
}
