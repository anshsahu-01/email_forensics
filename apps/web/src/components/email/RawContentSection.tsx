import React from 'react';
import { FileCode, Copy, Check } from 'lucide-react';

interface RawContentSectionProps {
  content: string | null;
}

export default function RawContentSection({ content }: RawContentSectionProps) {
  const [copied, setCopied] = React.useState(false);

  const handleCopy = () => {
    if (!content) return;
    navigator.clipboard.writeText(content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="border border-slate-200 bg-white overflow-hidden">
      <div className="bg-slate-50 px-8 py-4 border-b border-slate-200 flex items-center justify-between">
        <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
          <FileCode className="h-4 w-4 text-indigo-600" />
          Raw Forensic Payload
        </h2>
        <button
          onClick={handleCopy}
          disabled={!content}
          className="inline-flex items-center gap-2 border-2 border-slate-900 px-4 py-2 text-[9px] font-black uppercase tracking-widest text-slate-900 hover:bg-slate-900 hover:text-white transition-all"
        >
          {copied ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
          {copied ? 'PAYLOAD COPIED' : 'COPY PAYLOAD'}
        </button>
      </div>
      <div className="p-0">
        <pre className="p-8 text-[12px] font-mono text-white bg-slate-900 overflow-x-auto min-h-[500px] leading-relaxed selection:bg-indigo-500/50">
          <code>{content || 'NO PAYLOAD DATA OBSERVED.'}</code>
        </pre>
      </div>
    </div>
  );
}
