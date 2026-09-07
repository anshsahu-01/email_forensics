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
    <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
      <div className="bg-slate-50 px-6 py-4 border-b border-slate-200 flex items-center justify-between">
        <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
          <FileCode className="h-4 w-4 text-indigo-500" />
          Raw Message Content
        </h2>
        <button
          onClick={handleCopy}
          disabled={!content}
          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
        >
          {copied ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
          {copied ? 'Copied' : 'Copy Raw'}
        </button>
      </div>
      <div className="p-0">
        <pre className="p-6 text-[12px] font-mono text-slate-600 bg-slate-900 overflow-x-auto min-h-[400px] leading-relaxed selection:bg-indigo-500/30">
          <code>{content || 'No message content available.'}</code>
        </pre>
      </div>
    </div>
  );
}
