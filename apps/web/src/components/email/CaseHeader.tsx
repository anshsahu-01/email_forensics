import React from 'react';
import { EmailCase } from '@/types';
import { Mail, User, Send, Calendar, Hash, FileCode } from 'lucide-react';

interface CaseHeaderProps {
  emailCase: EmailCase;
}

export default function CaseHeader({ emailCase }: CaseHeaderProps) {
  const { header } = emailCase;

  return (
    <div className="border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-8 py-4 bg-slate-50 flex items-center justify-between">
        <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
          <Mail className="h-4 w-4 text-indigo-600" />
          General Metadata
        </h2>
        <span className="text-[9px] font-bold uppercase tracking-widest text-slate-400">
          Extracted: {new Date(emailCase.createdAt).toLocaleDateString()}
        </span>
      </div>

      <div className="p-8 grid gap-10 sm:grid-cols-2">
        <div className="space-y-6">
          <DetailItem
            label="Header Subject"
            value={header?.subject}
            icon={Mail}
            isHighlighted
          />
          <DetailItem
            label="Primary Sender (From)"
            value={header?.senderFrom}
            icon={User}
          />
          <DetailItem
            label="Primary Recipient (To)"
            value={header?.to}
            icon={Send}
          />
        </div>

        <div className="space-y-6">
          <DetailItem
            label="Header Timestamp"
            value={header?.date}
            icon={Calendar}
          />
          <DetailItem
            label="Message Identifier"
            value={header?.messageId}
            icon={Hash}
            isMono
          />
          <DetailItem
            label="Return-Path Header"
            value={header?.returnPath}
            icon={FileCode}
            isMono
          />
        </div>
      </div>

      {header?.cc && (
        <div className="px-8 pb-8 pt-0">
           <div className="border-t border-slate-100 pt-6">
            <DetailItem
              label="CC Recipients"
              value={header.cc}
              icon={Send}
            />
           </div>
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
      <p className="text-[9px] font-black uppercase tracking-[0.2em] text-slate-400 mb-2 flex items-center gap-2">
        <Icon className="h-3 w-3" />
        {label}
      </p>
      <p className={`text-sm break-all ${
        isHighlighted ? 'font-black text-slate-900' : 'font-bold text-slate-700'
      } ${isMono ? 'font-mono text-[12px] bg-slate-50 p-2 border border-slate-100' : ''}`}>
        {value || 'DATA NOT OBSERVED'}
      </p>
    </div>
  );
}
