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
    <div className="min-h-full bg-slate-50 py-8">
      <div className="mx-auto max-w-4xl px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Platform Settings
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Manage your forensic investigation workspace and service integrations.
          </p>
        </div>

        <div className="space-y-6">
          {/* General Section */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <User className="h-4 w-4 text-indigo-500" />
                Workspace Profile
              </h2>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid gap-2">
                <label className="text-xs font-bold text-slate-500 uppercase">Workspace Name</label>
                <input
                  type="text"
                  defaultValue="Default Investigation Unit"
                  className="rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              <div className="grid gap-2">
                <label className="text-xs font-bold text-slate-500 uppercase">Investigator Email</label>
                <input
                  type="email"
                  defaultValue="admin@forensics.local"
                  className="rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            </div>
          </section>

          {/* Intelligence Section */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Shield className="h-4 w-4 text-indigo-500" />
                Intelligence Providers
              </h2>
            </div>
            <div className="divide-y divide-slate-100">
              <div className="p-6 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">VirusTotal API</h3>
                  <p className="text-xs text-slate-500 mt-0.5">Automated URL and file reputation checks.</p>
                </div>
                <div className="flex items-center gap-2">
                   <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-100 uppercase tracking-wider">
                    Connected
                  </span>
                  <button className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
                    <Settings className="h-4 w-4" />
                  </button>
                </div>
              </div>
              <div className="p-6 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">AbuseIPDB</h3>
                  <p className="text-xs text-slate-500 mt-0.5">IP reputation and historical abuse tracking.</p>
                </div>
                <div className="flex items-center gap-2">
                   <span className="inline-flex items-center gap-1 rounded-full bg-slate-50 px-2 py-0.5 text-[10px] font-bold text-slate-400 border border-slate-200 uppercase tracking-wider">
                    Not Configured
                  </span>
                  <button className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
                    <Settings className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          </section>

          {/* Notifications Section */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Bell className="h-4 w-4 text-indigo-500" />
                Analysis Alerts
              </h2>
            </div>
            <div className="p-6 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Email Notifications</h3>
                  <p className="text-xs text-slate-500 mt-0.5">Send summary reports when analysis completes.</p>
                </div>
                <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-600" />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">High Risk Alerts</h3>
                  <p className="text-xs text-slate-500 mt-0.5">Immediate notification for critical threat scores.</p>
                </div>
                <input type="checkbox" defaultChecked className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-600" />
              </div>
            </div>
          </section>

           {/* Security Section */}
           <section className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Key className="h-4 w-4 text-indigo-500" />
                API Keys & Security
              </h2>
            </div>
            <div className="p-6">
              <div className="rounded-xl border border-indigo-50 bg-indigo-50/30 p-4 mb-6">
                <p className="text-xs text-indigo-700 leading-relaxed">
                  API keys are managed via environment variables for security. Contact your system administrator to update platform-wide credentials.
                </p>
              </div>
              <button className="w-full rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-slate-800 transition">
                Manage Session Tokens
              </button>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
