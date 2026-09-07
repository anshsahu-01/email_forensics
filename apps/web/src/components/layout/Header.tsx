'use client';

import { Bell, Search, UserCircle } from 'lucide-react';

export default function Header() {
  return (
    <header className="h-16 border-b border-slate-200 bg-white px-6 flex items-center justify-between">
      {/* Left */}
      <div>
        <h1 className="text-lg font-semibold text-slate-900">
          Email Forensics
        </h1>
        <p className="text-xs text-slate-500">
          Analyze and investigate suspicious emails
        </p>
      </div>

      {/* Right */}
      <div className="flex items-center gap-2">
        <button
          type="button"
          className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition"
          aria-label="Search"
        >
          <Search className="h-4 w-4" />
        </button>

        <button
          type="button"
          className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition"
          aria-label="Notifications"
        >
          <Bell className="h-4 w-4" />
        </button>

        <div className="ml-2 h-8 w-px bg-slate-200" />

        <button
          type="button"
          className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-slate-100 transition"
        >
          <UserCircle className="h-7 w-7 text-slate-400" />

          <div className="hidden sm:block text-left">
            <p className="text-sm font-medium text-slate-800">
              Investigator
            </p>
            <p className="text-[11px] text-slate-500">
              Forensic Analyst
            </p>
          </div>
        </button>
      </div>
    </header>
  );
}