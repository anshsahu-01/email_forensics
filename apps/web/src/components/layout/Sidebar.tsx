'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  BarChart3,
  FileSearch,
  FolderOpen,
  LayoutDashboard,
  Settings,
  ShieldCheck,
  X,
} from 'lucide-react';

const navigation = [
  {
    name: 'Dashboard',
    href: '/',
    icon: LayoutDashboard,
  },
  {
    name: 'Analyze Email',
    href: '/analyze',
    icon: FileSearch,
  },
  {
    name: 'Cases',
    href: '/cases',
    icon: FolderOpen,
  },
  {
    name: 'Intelligence',
    href: '/intelligence',
    icon: BarChart3,
  },
  {
    name: 'Reports',
    href: '/reports',
    icon: ShieldCheck,
  },
];

interface SidebarProps {
  mobileOpen: boolean;
  onClose: () => void;
}

export default function Sidebar({
  mobileOpen,
  onClose,
}: SidebarProps) {
  const pathname = usePathname();

  return (
    <>
      {/* Mobile Overlay */}
      {mobileOpen && (
        <button
          type="button"
          aria-label="Close navigation"
          onClick={onClose}
          className="fixed inset-0 z-40 bg-black/20 lg:hidden"
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
          fixed left-0 top-0 z-50 flex h-screen w-64 shrink-0
          flex-col border-r border-slate-200 bg-white
          transition-transform duration-200 ease-out
          ${mobileOpen ? 'translate-x-0' : '-translate-x-full'}
          lg:translate-x-0
        `}
      >
        {/* Brand */}
        <div className="flex h-20 items-center justify-between px-6">
          <div className="flex items-center gap-3">
            <div className="h-6 w-1 bg-indigo-600" />

            <div>
              <h1 className="text-xs font-black uppercase tracking-[0.2em] text-slate-900">
                Forensics
              </h1>
            </div>
          </div>

          {/* Mobile Close Button */}
          <button
            type="button"
            onClick={onClose}
            aria-label="Close navigation"
            className="p-1 text-slate-400 transition hover:text-slate-900 lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-4 py-8">
          <p className="mb-6 px-4 text-[9px] font-bold uppercase tracking-[0.2em] text-slate-400">
            Workspace
          </p>

          <div className="space-y-1">
            {navigation.map((item) => {
              const Icon = item.icon;

              const isActive =
                item.href === '/'
                  ? pathname === '/'
                  : pathname.startsWith(item.href);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={onClose}
                  className={`group flex items-center gap-4 px-4 py-3 text-[11px] font-bold uppercase tracking-widest transition-all ${
                    isActive
                      ? 'border-r-2 border-indigo-600 text-indigo-600'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                >
                  <Icon
                    className={`h-4 w-4 ${
                      isActive
                        ? 'text-indigo-600'
                        : 'text-slate-400 group-hover:text-slate-600'
                    }`}
                  />

                  <span>{item.name}</span>
                </Link>
              );
            })}
          </div>
        </nav>

        {/* Bottom */}
        <div className="border-t border-slate-100 p-4">
          <Link
            href="/settings"
            onClick={onClose}
            className={`flex items-center gap-4 px-4 py-3 text-[11px] font-bold uppercase tracking-widest transition-all ${
              pathname.startsWith('/settings')
                ? 'border-r-2 border-indigo-600 text-indigo-600'
                : 'text-slate-500 hover:text-slate-900'
            }`}
          >
            <Settings
              className={`h-4 w-4 ${
                pathname.startsWith('/settings')
                  ? 'text-indigo-600'
                  : 'text-slate-400'
              }`}
            />

            <span>Settings</span>
          </Link>
        </div>
      </aside>
    </>
  );
}