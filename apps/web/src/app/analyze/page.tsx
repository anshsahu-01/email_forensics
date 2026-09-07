'use client';

import { useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import {
  FileSearch,
  Upload,
  X,
  ShieldCheck,
  AlertCircle,
  ArrowLeft,
  Loader2
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
      setError('Invalid file format. Please upload an .eml file.');
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
      setError(err instanceof Error ? err.message : 'Analysis failed. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="min-h-full bg-slate-50 py-8">
      <div className="mx-auto max-w-3xl px-6 lg:px-8">
        <Link
          href="/"
          className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-indigo-600 transition"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Dashboard
        </Link>

        <div className="mb-8">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Forensic Email Analysis
          </h1>
          <p className="mt-2 text-slate-600">
            Upload an email file to perform deep header analysis, route tracking, and threat intelligence enrichment.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
          {!selectedFile ? (
            <div
              onClick={() => fileInputRef.current?.click()}
              className="group flex min-h-[300px] cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 transition-all hover:border-indigo-400 hover:bg-indigo-50/30"
            >
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 group-hover:ring-indigo-200 transition-all">
                <Upload className="h-8 w-8 text-indigo-600" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900">Click or drag to upload</h3>
              <p className="mt-2 text-sm text-slate-500 max-w-xs text-center">
                Standard EML files (.eml) containing full headers and message body.
              </p>
              <div className="mt-6 flex flex-wrap justify-center gap-3">
                <div className="flex items-center gap-1.5 rounded-full bg-slate-200/50 px-3 py-1 text-[11px] font-medium text-slate-600">
                  <ShieldCheck className="h-3 w-3" /> Header Analysis
                </div>
                <div className="flex items-center gap-1.5 rounded-full bg-slate-200/50 px-3 py-1 text-[11px] font-medium text-slate-600">
                  <ShieldCheck className="h-3 w-3" /> Route Mapping
                </div>
                <div className="flex items-center gap-1.5 rounded-full bg-slate-200/50 px-3 py-1 text-[11px] font-medium text-slate-600">
                  <ShieldCheck className="h-3 w-3" /> Threat Intel
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-xl border border-indigo-100 bg-indigo-50/30 p-6">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-md">
                    <FileSearch className="h-6 w-6" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-slate-900 truncate max-w-md">
                      {selectedFile.name}
                    </h3>
                    <p className="text-sm text-slate-500">
                      {(selectedFile.size / 1024).toFixed(1)} KB • Email File
                    </p>
                  </div>
                </div>
                <button
                  onClick={removeFile}
                  className="rounded-full p-1 text-slate-400 hover:bg-white hover:text-slate-600 transition"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <div className="mt-8 flex flex-col gap-3">
                <button
                  onClick={handleUpload}
                  disabled={uploading}
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-6 py-3.5 text-sm font-semibold text-white shadow-lg shadow-indigo-200 transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {uploading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Performing Analysis...
                    </>
                  ) : (
                    'Start Deep Forensic Analysis'
                  )}
                </button>
                <button
                  onClick={removeFile}
                  disabled={uploading}
                  className="w-full rounded-xl border border-slate-200 bg-white px-6 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
                >
                  Cancel
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

          {error && (
            <div className="mt-6 flex items-start gap-3 rounded-xl bg-red-50 p-4 border border-red-100">
              <AlertCircle className="h-5 w-5 shrink-0 text-red-500" />
              <p className="text-sm font-medium text-red-800">{error}</p>
            </div>
          )}
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-2">
          <div className="rounded-xl border border-slate-200 p-5">
            <h4 className="font-semibold text-slate-900">What is analyzed?</h4>
            <ul className="mt-3 space-y-2 text-sm text-slate-500">
              <li className="flex items-center gap-2">• SMTP hop chain and IP routing</li>
              <li className="flex items-center gap-2">• SPF, DKIM, and DMARC alignment</li>
              <li className="flex items-center gap-2">• URL and Attachment indicators</li>
              <li className="flex items-center gap-2">• Sender reputation and Geo-location</li>
            </ul>
          </div>
          <div className="rounded-xl border border-slate-200 p-5">
            <h4 className="font-semibold text-slate-900">Data Privacy</h4>
            <p className="mt-3 text-sm text-slate-500">
              Emails are processed securely. Extracted indicators are enriched using external services like VirusTotal. No content is shared with third parties.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
