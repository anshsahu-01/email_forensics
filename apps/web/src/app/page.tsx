'use client';

import { useState, useEffect } from 'react';
import { Upload, ShieldAlert, CheckCircle, FileText, Globe, RefreshCw } from 'lucide-react';

interface EmailHeader {
  subject: string;
  senderFrom: string;
  replyTo: string;
  spfStatus: string;
  dkimStatus: string;
  dmarcStatus: string;
}

interface EmailIndicator {
  type: string;
  value: string;
  isp?: string;
  country?: string;
}

interface EmailCase {
  id: number;
  fileName: string;
  fileHash: string;
  threatScore: number;
  riskLevel: string;
  aiSummary: string;
  createdAt: string;
  header: EmailHeader;
  indicators: EmailIndicator[];
}

export default function ForensicDashboard() {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [currentCase, setCurrentCase] = useState<EmailCase | null>(null);
  const [history, setHistory] = useState<EmailCase[]>([]);
  const [error, setError] = useState<string | null>(null);

  const API_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1';

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      const res = await fetch(`${API_URL}/cases`);
      if (res.ok) {
        const data = await res.json();
        setHistory(data);
      }
    } catch (err) {
      console.error('Failed to load cases:', err);
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;

    setLoading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await fetch(`${API_URL}/emails/analyze`, {
        method: 'POST',
        body: formData,
      });

      if (!res.ok) throw new Error('Analysis failed or server returned an error.');

      const data: EmailCase = await res.json();
      setCurrentCase(data);
      fetchHistory();
    } catch (err: any) {
      setError(err.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    const isPass = status?.toUpperCase() === 'PASS';
    return (
      <span className={`px-2 py-0.5 text-xs font-semibold rounded ${isPass ? 'bg-green-900/60 text-green-300' : 'bg-red-900/60 text-red-300'}`}>
        {status || 'UNKNOWN'}
      </span>
    );
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-8 font-sans">
      <header className="max-w-6xl mx-auto mb-8 border-b border-slate-800 pb-4 flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <ShieldAlert className="text-blue-500" /> Email Forensic Analyzer
          </h1>
          <p className="text-sm text-slate-400">Spring Boot + Next.js Engine Verification</p>
        </div>
        <button onClick={fetchHistory} className="p-2 bg-slate-800 hover:bg-slate-700 rounded-lg text-slate-300">
          <RefreshCw className="w-4 h-4" />
        </button>
      </header>

      <main className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="space-y-6">
          <section className="bg-slate-800 p-6 rounded-xl border border-slate-700">
            <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
              <Upload className="w-5 h-5 text-blue-400" /> Upload .EML File
            </h2>
            <form onSubmit={handleUpload} className="space-y-4">
              <input
                type="file"
                accept=".eml"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
                className="block w-full text-sm text-slate-400 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-600 file:text-white hover:file:bg-blue-700 cursor-pointer"
              />
              <button
                type="submit"
                disabled={!file || loading}
                className="w-full py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 rounded-lg font-medium transition"
              >
                {loading ? 'Analyzing EML...' : 'Analyze Case'}
              </button>
            </form>
            {error && <p className="text-red-400 text-xs mt-3">{error}</p>}
          </section>

          <section className="bg-slate-800 p-6 rounded-xl border border-slate-700 max-h-96 overflow-y-auto">
            <h2 className="text-lg font-semibold mb-3">Case History ({history.length})</h2>
            <div className="space-y-2">
              {history.map((c) => (
                <div
                  key={c.id}
                  onClick={() => setCurrentCase(c)}
                  className={`p-3 rounded-lg border text-sm cursor-pointer transition ${
                    currentCase?.id === c.id ? 'bg-slate-700 border-blue-500' : 'bg-slate-900 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  <p className="font-medium truncate">{c.header?.subject || c.fileName || 'No Subject'}</p>
                  <div className="flex justify-between items-center text-xs text-slate-400 mt-1">
                    <span>ID: #{c.id}</span>
                    <span>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : ''}</span>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        <div className="lg:col-span-2">
          {currentCase ? (
            <div className="space-y-6">
              <section className="bg-slate-800 p-6 rounded-xl border border-slate-700">
                <div className="flex justify-between items-start border-b border-slate-700 pb-4 mb-4">
                  <div>
                    <span className="text-xs font-mono bg-blue-900/50 text-blue-300 px-2 py-1 rounded">
                      Case #{currentCase.id}
                    </span>
                    <h2 className="text-xl font-bold mt-2">{currentCase.header?.subject || 'No Subject Defined'}</h2>
                  </div>
                  <div className="text-right">
                    <span className="text-xs text-slate-400 block">Risk Level</span>
                    <span className="text-sm font-semibold text-amber-400">{currentCase.riskLevel || 'ANALYZED'}</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-slate-400 text-xs">From</p>
                    <p className="font-mono text-slate-200 truncate">{currentCase.header?.senderFrom || 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-slate-400 text-xs">Reply-To</p>
                    <p className="font-mono text-slate-200 truncate">{currentCase.header?.replyTo || 'None'}</p>
                  </div>
                  <div className="col-span-2">
                    <p className="text-slate-400 text-xs">SHA-256 Hash</p>
                    <p className="font-mono text-xs text-slate-300 break-all bg-slate-900 p-2 rounded mt-1">
                      {currentCase.fileHash}
                    </p>
                  </div>
                </div>
              </section>

              <section className="bg-slate-800 p-6 rounded-xl border border-slate-700">
                <h3 className="text-md font-semibold mb-4 flex items-center gap-2">
                  <CheckCircle className="w-4 h-4 text-green-400" /> Authentication Results
                </h3>
                <div className="grid grid-cols-3 gap-4 text-center">
                  <div className="bg-slate-900 p-3 rounded-lg border border-slate-800">
                    <p className="text-xs text-slate-400 mb-1">SPF</p>
                    {getStatusBadge(currentCase.header?.spfStatus)}
                  </div>
                  <div className="bg-slate-900 p-3 rounded-lg border border-slate-800">
                    <p className="text-xs text-slate-400 mb-1">DKIM</p>
                    {getStatusBadge(currentCase.header?.dkimStatus)}
                  </div>
                  <div className="bg-slate-900 p-3 rounded-lg border border-slate-800">
                    <p className="text-xs text-slate-400 mb-1">DMARC</p>
                    {getStatusBadge(currentCase.header?.dmarcStatus)}
                  </div>
                </div>
              </section>

              <section className="bg-slate-800 p-6 rounded-xl border border-slate-700">
                <h3 className="text-md font-semibold mb-4 flex items-center gap-2">
                  <Globe className="w-4 h-4 text-purple-400" /> Indicators of Compromise (IoCs)
                </h3>
                <div className="space-y-2">
                  {currentCase.indicators && currentCase.indicators.length > 0 ? (
                    currentCase.indicators.map((ind, idx) => (
                      <div key={idx} className="flex items-center justify-between bg-slate-900 p-3 rounded-lg border border-slate-800 text-xs font-mono">
                        <span className={`px-2 py-0.5 rounded font-bold ${ind.type === 'IP' ? 'bg-purple-900/50 text-purple-300' : 'bg-amber-900/50 text-amber-300'}`}>
                          {ind.type}
                        </span>
                        <span className="text-slate-300 break-all">{ind.value}</span>
                      </div>
                    ))
                  ) : (
                    <p className="text-xs text-slate-500">No IoCs found in this file.</p>
                  )}
                </div>
              </section>
            </div>
          ) : (
            <div className="bg-slate-800 border border-slate-700 rounded-xl p-12 text-center text-slate-400">
              <FileText className="w-12 h-12 mx-auto mb-3 text-slate-600" />
              <p>Upload a file or click on a case from history to display forensic output.</p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}