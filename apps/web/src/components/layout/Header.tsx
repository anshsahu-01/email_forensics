'use client';

import {
  Bell,
  Menu,
  Search,
  UserCircle,
} from 'lucide-react';

interface HeaderProps {
  onMenuClick: () => void;
}

export default function Header({
  onMenuClick,
}: HeaderProps) {
  return (
    <header className="fixed left-0 right-0 top-0 z-30 flex h-20 items-center justify-between border-b border-slate-100 bg-white px-4 sm:px-6 lg:left-64 lg:px-8">
      {/* Left */}
      <div className="flex min-w-0 items-center gap-3">
        {/* Mobile Menu */}
        <button
          type="button"
          onClick={onMenuClick}
          aria-label="Open navigation"
          className="p-1.5 text-slate-400 transition hover:text-slate-900 lg:hidden"
        >
          <Menu className="h-5 w-5" />
        </button>

        <h2 className="truncate text-[10px] font-black uppercase tracking-[0.25em] text-slate-900 sm:text-xs sm:tracking-[0.3em]">
          Investigation Portal
        </h2>
      </div>

      {/* Right */}
      <div className="flex shrink-0 items-center gap-4 sm:gap-6">
        {/* Search */}
        <button
          type="button"
          className="text-slate-400 transition hover:text-slate-900"
          aria-label="Search"
        >
          <Search className="h-4 w-4" />
        </button>

        {/* Notifications */}
        <button
          type="button"
          className="text-slate-400 transition hover:text-slate-900"
          aria-label="Notifications"
        >
          <Bell className="h-4 w-4" />
        </button>

        {/* User */}
        <div className="flex items-center gap-3 border-l border-slate-100 pl-3 sm:pl-4">
          {/* Hide text on small screens */}
          <div className="hidden text-right sm:block">
            <p className="text-[10px] font-black uppercase tracking-widest text-slate-900">
              Analyst
            </p>

            <p className="text-[9px] font-bold uppercase tracking-widest text-slate-400">
              Level 3 Access
            </p>
          </div>

          <UserCircle className="h-6 w-6 text-slate-300" />
        </div>
      </div>
    </header>
  );
}