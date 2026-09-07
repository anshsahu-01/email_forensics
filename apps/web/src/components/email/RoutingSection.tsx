import React from 'react';
import { ReceivedHeader } from '@/types';
import { Server, MapPin, Clock } from 'lucide-react';

interface RoutingSectionProps {
  receivedHeaders: ReceivedHeader[] | null;
}

export default function RoutingSection({ receivedHeaders }: RoutingSectionProps) {
  if (!receivedHeaders || receivedHeaders.length === 0) {
    return (
      <div className="border border-slate-200 bg-white p-20 text-center">
        <Server className="mx-auto h-10 w-10 text-slate-200" />
        <h3 className="mt-6 text-[10px] font-black uppercase tracking-[0.3em] text-slate-900">Routing Data Deficient</h3>
        <p className="mt-2 text-xs font-bold text-slate-400 uppercase tracking-widest">No hop metadata identified in headers.</p>
      </div>
    );
  }

  const hops = [...receivedHeaders].sort((a, b) => b.hopIndex - a.hopIndex);

  return (
    <div className="border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-8 py-4 bg-slate-50">
        <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-900 flex items-center gap-2">
          <Server className="h-4 w-4 text-indigo-600" />
          SMTP Propagation Chain
        </h2>
      </div>

      <div className="p-10">
        <div className="relative border-l-2 border-slate-900 ml-4 pl-10 space-y-12">
          {hops.map((hop, index) => (
            <div key={index} className="relative">
              {/* Dot */}
              <div className={`absolute -left-[49px] top-0 h-4 w-4 border-4 bg-white ${
                index === 0 ? 'border-emerald-600' :
                index === hops.length - 1 ? 'border-indigo-600' : 'border-slate-900'
              }`} />

              <div className="border border-slate-200 bg-white p-6 transition-all hover:border-slate-900">
                <div className="flex flex-col gap-6">
                  <div className="flex items-center gap-3">
                    <span className="text-[9px] font-black uppercase tracking-[0.3em] text-slate-900 bg-slate-100 px-3 py-1">
                      NODE HOP #{hop.hopIndex}
                    </span>
                    {index === 0 && (
                      <span className="text-[9px] font-black uppercase tracking-[0.3em] text-white bg-emerald-600 px-3 py-1">
                        ORIGIN
                      </span>
                    )}
                    {index === hops.length - 1 && (
                      <span className="text-[9px] font-black uppercase tracking-[0.3em] text-white bg-indigo-600 px-3 py-1">
                        DESTINATION
                      </span>
                    )}
                  </div>

                  <div className="grid gap-8 sm:grid-cols-2">
                    <div>
                      <p className="text-[9px] font-black text-slate-400 uppercase tracking-[0.2em] mb-2">Transmitted From</p>
                      <p className="text-sm font-bold text-slate-900 break-all">{hop.from || 'UNDISCLOSED'}</p>
                    </div>
                    <div>
                      <p className="text-[9px] font-black text-slate-400 uppercase tracking-[0.2em] mb-2">Processed By</p>
                      <p className="text-sm font-bold text-slate-900 break-all">{hop.by || 'UNDISCLOSED'}</p>
                    </div>
                  </div>

                  <div className="pt-4 mt-2 border-t border-slate-50 flex flex-wrap gap-6 text-[10px] font-black uppercase tracking-widest text-slate-500">
                    {hop.ip && (
                      <div className="flex items-center gap-2">
                        <MapPin className="h-3.5 w-3.5 text-indigo-600" />
                        Network IP: <span className="font-mono text-slate-900">{hop.ip}</span>
                      </div>
                    )}
                    {hop.date && (
                      <div className="flex items-center gap-2">
                        <Clock className="h-3.5 w-3.5 text-slate-300" />
                        Log Entry: {hop.date}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
