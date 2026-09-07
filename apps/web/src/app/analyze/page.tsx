'use client';

import { useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import {
  FileSearch,
  Upload,
  X,
  AlertCircle,
  ArrowLeft,
  Loader2,
  Server,
  Globe,
  Database
} from 'lucide-react';
import Link from 'next/link';
import { analyzeEmail } from '@/lib/api';

export default function AnalyzePage() {
  const router = useRouter();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.name.toLowerCase().endsWith('.eml')) {
      setError('Unsupported file format. Artifact must be in .eml specification.');
      return;
    }

    setSelectedFile(file);
    setError(null);
  };

  const removeFile = () => {
    setSelectedFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setUploading(true);
      setError(null);
      const analyzedCase = await analyzeEmail(selectedFile);
      router.push(`/cases/${analyzedCase.id}`);
    } catch (err) {
      console.error('Analysis failed:', err);
      setError(err instanceof Error ? err.message : 'Analysis failed. Forensic engine returned an error.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="min-h-full bg-white py-12">
      <div className="mx-auto max-w-4xl px-8">
        <Link
          href="/"
          className="mb-10 inline-flex items-center gap-3 text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 hover:text-indigo-600 transition"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Terminal
        </Link>

        <div className="mb-12 border-b border-slate-900 pb-8">
          <h1 className="text-4xl font-black uppercase tracking-tighter text-slate-900 sm:text-5xl">
            Artifact Intake
          </h1>
          <p className="mt-4 text-sm font-medium text-slate-500 max-w-2xl">
            Upload raw email data for deep-packet header inspection, infrastructure route mapping, and external threat intelligence enrichment.
          </p>
        </div>

        <div className="border border-slate-200 bg-white">
          {!selectedFile ? (
            <div
              onClick={() => fileInputRef.current?.click()}
              className="group flex min-h-[400px] cursor-pointer flex-col items-center justify-center border-2 border-dashed border-slate-200 bg-slate-50 transition-all hover:border-slate-900 hover:bg-white"
            >
              <div className="mb-8 flex h-20 w-20 items-center justify-center border-2 border-slate-200 bg-white transition-all group-hover:border-slate-900">
                <Upload className="h-8 w-8 text-slate-900" />
              </div>
              <h3 className="text-[11px] font-black uppercase tracking-[0.3em] text-slate-900">Initiate Upload</h3>
              <p className="mt-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 max-w-xs text-center">
                RFC-822 / .EML Specification Only
              </p>

              <div className="mt-10 flex flex-wrap justify-center gap-6">
                <FeatureItem icon={Server} label="SMTP Routing" />
                <FeatureItem icon={Globe} label="Geo-Intel" />
                <FeatureItem icon={Database} label="IOC Extraction" />
              </div>
            </div>
          ) : (
            <div className="p-10">
              <div className="flex items-start justify-between border border-indigo-100 bg-indigo-50/20 p-8">
                <div className="flex items-center gap-6">
                  <div className="flex h-16 w-16 items-center justify-center border-2 border-slate-900 bg-slate-900 text-white">
                    <FileSearch className="h-8 w-8" />
                  </div>
                  <div>
                    <h3 className="text-sm font-black uppercase tracking-tight text-slate-900 truncate max-w-md">
                      {selectedFile.name}
                    </h3>
                    <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">
                      {(selectedFile.size / 1024).toFixed(1)} KB • Verified Specification
                    </p>
                  </div>
                </div>
                <button
                  onClick={removeFile}
                  className="text-slate-400 hover:text-red-600 transition"
                >
                  <X className="h-6 w-6" />
                </button>
              </div>

              <div className="mt-10 flex flex-col gap-4">
                <button
                  onClick={handleUpload}
                  disabled={uploading}
                  className="flex w-full items-center justify-center gap-3 bg-slate-900 px-8 py-5 text-[11px] font-black uppercase tracking-[0.2em] text-white transition hover:bg-slate-800 disabled:opacity-50"
                >
                  {uploading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Executing Forensic Triage...
                    </>
                  ) : (
                    'Initiate Deep Forensic Analysis'
                  )}
                </button>
                <button
                  onClick={removeFile}
                  disabled={uploading}
                  className="w-full border-2 border-slate-200 bg-white px-8 py-4 text-[11px] font-black uppercase tracking-[0.2em] text-slate-900 transition hover:border-slate-900"
                >
                  Abort Intake
                </button>
              </div>
            </div>
          )}

          <input
            ref={fileInputRef}
            type="file"
            accept=".eml,message/rfc822"
            onChange={handleFileChange}
            className="hidden"
          />
        </div>

        {error && (
          <div className="mt-8 flex items-start gap-4 border border-red-200 bg-red-50 p-6">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
            <div>
              <p className="text-[10px] font-black uppercase tracking-widest text-red-800">Process Halted</p>
              <p className="mt-1 text-sm font-medium text-red-700">{error}</p>
            </div>
          </div>
        )}

        <div className="mt-16 grid gap-12 sm:grid-cols-2">
          <section>
            <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 mb-6">Forensic Scope</h4>
            <ul className="space-y-4">
              <ScopeItem text="Verification of SMTP hop-chain integrity" />
              <ScopeItem text="Authentication alignment (SPF, DKIM, DMARC)" />
              <ScopeItem text="Recursive extraction of indicators (URL/IP)" />
              <ScopeItem text="Reputation scoring via multi-source intelligence" />
            </ul>
          </section>
          <section>
            <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 mb-6">Security Protocol</h4>
            <p className="text-xs font-medium text-slate-500 leading-relaxed">
              All processed data is confined to the secure workspace. Intelligence enrichment utilizes encrypted API channels. Raw artifacts are stored in encrypted volumes for chain-of-custody preservation.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}

function FeatureItem({ icon: Icon, label }: { icon: React.ComponentType<{ className?: string }>, label: string }) {
  return (
    <div className="flex items-center gap-2 text-[9px] font-black uppercase tracking-widest text-slate-500">
      <Icon className="h-3 w-3" /> {label}
    </div>
  );
}

function ScopeItem({ text }: { text: string }) {
  return (
    <li className="flex items-start gap-3 text-[11px] font-bold text-slate-500">
      <div className="mt-1.5 h-1 w-1 shrink-0 bg-indigo-600" />
      {text}
    </li>
  );
}
