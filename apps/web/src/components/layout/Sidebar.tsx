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
  Sparkles,
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

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="flex h-screen w-64 shrink-0 flex-col border-r border-slate-200 bg-white">
      {/* Brand */}
      <div className="flex h-16 items-center border-b border-slate-200 px-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 text-white">
          <Sparkles className="h-5 w-5" />
        </div>

        <div className="ml-3">
          <h1 className="text-sm font-bold tracking-tight text-slate-900">
            Email Forensics
          </h1>
          <p className="text-[11px] text-slate-500">
            Investigation Platform
          </p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-5">
        <p className="mb-2 px-3 text-[10px] font-semibold uppercase tracking-wider text-slate-400">
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
                className={`group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                  isActive
                    ? 'bg-indigo-50 text-indigo-700'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`}
              >
                <Icon
                  className={`h-[18px] w-[18px] ${
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
      <div className="border-t border-slate-200 p-3">
        <Link
          href="/settings"
          className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
            pathname.startsWith('/settings')
              ? 'bg-indigo-50 text-indigo-700'
              : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
          }`}
        >
          <Settings
            className={`h-[18px] w-[18px] ${
              pathname.startsWith('/settings')
                ? 'text-indigo-600'
                : 'text-slate-400'
            }`}
          />

          <span>Settings</span>
        </Link>
      </div>
    </aside>
  );
}