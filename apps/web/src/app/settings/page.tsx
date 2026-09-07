'use client';

import {
  Settings,
  Shield,
  Bell,
  Key,
  User
} from 'lucide-react';

export default function SettingsPage() {
  return (
    <div className="min-h-full bg-white py-12">
      <div className="mx-auto max-w-4xl px-8">
        <div className="mb-12 border-b border-slate-900 pb-8">
          <h1 className="text-4xl font-black uppercase tracking-tighter text-slate-900 sm:text-5xl">
            Platform Configuration
          </h1>
          <p className="mt-4 text-sm font-medium text-slate-500 max-w-md">
            Manage forensic workspace parameters and external intelligence node integrations.
          </p>
        </div>

        <div className="space-y-12">
          {/* General Section */}
          <section className="border border-slate-200 bg-white overflow-hidden">
            <div className="px-8 py-4 border-b border-slate-200 bg-slate-50">
              <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
                <User className="h-4 w-4 text-indigo-600" />
                Workspace Identification
              </h2>
            </div>
            <div className="p-8 space-y-8">
              <div className="grid gap-3">
                <label className="text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">Node Designation</label>
                <input
                  type="text"
                  defaultValue="PRIMARY FORENSIC UNIT - INDIA"
                  className="border-2 border-slate-200 px-4 py-3 text-[11px] font-bold uppercase tracking-widest text-slate-900 focus:border-slate-900 focus:outline-none transition-all"
                />
              </div>
              <div className="grid gap-3">
                <label className="text-[9px] font-black uppercase tracking-[0.2em] text-slate-400">Administrative Contact</label>
                <input
                  type="email"
                  defaultValue="admin@forensics.india.gov"
                  className="border-2 border-slate-200 px-4 py-3 text-[11px] font-bold uppercase tracking-widest text-slate-900 focus:border-slate-900 focus:outline-none transition-all"
                />
              </div>
            </div>
          </section>

          {/* Intelligence Section */}
          <section className="border border-slate-200 bg-white overflow-hidden">
            <div className="px-8 py-4 border-b border-slate-200 bg-slate-50">
              <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
                <Shield className="h-4 w-4 text-indigo-600" />
                Intelligence Node Status
              </h2>
            </div>
            <div className="divide-y divide-slate-100">
              <div className="p-8 flex items-center justify-between transition-colors hover:bg-slate-50">
                <div>
                  <h3 className="text-xs font-black uppercase tracking-tight text-slate-900">VirusTotal V3 Core</h3>
                  <p className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mt-1">Automated URL/Hash reputation synchronization.</p>
                </div>
                <div className="flex items-center gap-6">
                   <span className="inline-flex items-center gap-2 border border-emerald-600 bg-emerald-50 px-3 py-1 text-[9px] font-black text-emerald-600 uppercase tracking-widest">
                    SYNCHRONIZED
                  </span>
                  <button className="text-slate-300 hover:text-slate-900 transition-all">
                    <Settings className="h-4 w-4" />
                  </button>
                </div>
              </div>
              <div className="p-8 flex items-center justify-between transition-colors hover:bg-slate-50">
                <div>
                  <h3 className="text-xs font-black uppercase tracking-tight text-slate-900">AbuseIPDB Integration</h3>
                  <p className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mt-1">Infrastructure abuse tracking and reporting.</p>
                </div>
                <div className="flex items-center gap-6">
                   <span className="inline-flex items-center gap-2 border border-slate-200 bg-slate-50 px-3 py-1 text-[9px] font-black text-slate-400 uppercase tracking-widest">
                    NODE DISCONNECTED
                  </span>
                  <button className="text-slate-300 hover:text-slate-900 transition-all">
                    <Settings className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          </section>

          {/* Notifications Section */}
          <section className="border border-slate-200 bg-white overflow-hidden">
            <div className="px-8 py-4 border-b border-slate-200 bg-slate-50">
              <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
                <Bell className="h-4 w-4 text-indigo-600" />
                Operational Alerts
              </h2>
            </div>
            <div className="p-8 space-y-8">
              <div className="flex items-center justify-between group">
                <div>
                  <h3 className="text-xs font-black uppercase tracking-tight text-slate-900">Status Reports</h3>
                  <p className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mt-1">Transmit summary reports upon investigation completion.</p>
                </div>
                <input type="checkbox" className="h-5 w-5 border-2 border-slate-900 text-slate-900 focus:ring-0" />
              </div>
              <div className="flex items-center justify-between group">
                <div>
                  <h3 className="text-xs font-black uppercase tracking-tight text-slate-900">Critical Threat Escalation</h3>
                  <p className="text-[9px] font-bold uppercase tracking-widest text-slate-400 mt-1">Immediate notification for indices exceeding 70% threat score.</p>
                </div>
                <input type="checkbox" defaultChecked className="h-5 w-5 border-2 border-slate-900 text-slate-900 focus:ring-0" />
              </div>
            </div>
          </section>

           {/* Security Section */}
           <section className="border border-slate-200 bg-white overflow-hidden">
            <div className="px-8 py-4 border-b border-slate-200 bg-slate-50">
              <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
                <Key className="h-4 w-4 text-indigo-600" />
                Access Control
              </h2>
            </div>
            <div className="p-8">
              <div className="border-l-4 border-indigo-600 bg-indigo-50/20 p-6 mb-10">
                <p className="text-[10px] font-bold uppercase tracking-widest text-indigo-900 leading-relaxed">
                  Security credentials are managed via Environment variable injection. Modification requires administrative override.
                </p>
              </div>
              <button className="w-full border-2 border-slate-900 bg-slate-900 px-8 py-4 text-[11px] font-black uppercase tracking-widest text-white transition-all hover:bg-slate-800">
                Revoke Current Session Tokens
              </button>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
