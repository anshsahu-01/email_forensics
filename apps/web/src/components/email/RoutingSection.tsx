import React from 'react';
import { ReceivedHeader } from '@/types';
import { Server, MapPin, Clock } from 'lucide-react';

interface RoutingSectionProps {
  receivedHeaders: ReceivedHeader[] | null;
}

export default function RoutingSection({ receivedHeaders }: RoutingSectionProps) {
  if (!receivedHeaders || receivedHeaders.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
        <Server className="mx-auto h-8 w-8 text-slate-300" />
        <h3 className="mt-4 text-sm font-semibold text-slate-900">No Routing Information</h3>
        <p className="mt-1 text-sm text-slate-500">Could not extract any hop data from headers.</p>
      </div>
    );
  }

  // Sorting: hop 0 is usually the oldest (origin) if parsed correctly by the backend
  // But standard email headers are top-to-bottom (newest to oldest)
  // Our backend seems to provide them in order of appearance in the email file
  const hops = [...receivedHeaders].sort((a, b) => b.hopIndex - a.hopIndex);

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-sm font-bold text-slate-900 mb-8 flex items-center gap-2">
          <Server className="h-4 w-4 text-indigo-500" />
          Mail Delivery Route (SMTP Hops)
        </h2>

        <div className="relative pl-8">
          {/* Vertical line */}
          <div className="absolute left-[15px] top-2 bottom-2 w-0.5 bg-slate-100" />

          <div className="space-y-10">
            {hops.map((hop, index) => (
              <div key={index} className="relative">
                {/* Node dot */}
                <div className={`absolute -left-[23px] top-1.5 h-4 w-4 rounded-full border-2 bg-white ${
                  index === 0 ? 'border-emerald-500 scale-125' :
                  index === hops.length - 1 ? 'border-indigo-500' : 'border-slate-300'
                }`} />

                <div className="rounded-xl border border-slate-100 bg-slate-50/50 p-4 transition hover:bg-white hover:shadow-md">
                  <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 bg-slate-200/50 px-2 py-0.5 rounded">
                          Hop #{hop.hopIndex}
                        </span>
                        {index === 0 && (
                          <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-100">
                            Origin
                          </span>
                        )}
                        {index === hops.length - 1 && (
                          <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                            Destination
                          </span>
                        )}
                      </div>

                      <div className="grid gap-4 sm:grid-cols-2">
                        <div>
                          <p className="text-[10px] font-bold text-slate-400 uppercase">From</p>
                          <p className="text-sm font-semibold text-slate-700 truncate">{hop.from || 'Unknown'}</p>
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-slate-400 uppercase">By</p>
                          <p className="text-sm font-semibold text-slate-700 truncate">{hop.by || 'Unknown'}</p>
                        </div>
                      </div>

                      <div className="mt-4 flex flex-wrap gap-4 text-[11px] text-slate-500">
                        {hop.ip && (
                          <div className="flex items-center gap-1.5">
                            <MapPin className="h-3 w-3" />
                            IP: <span className="font-mono font-bold text-indigo-600">{hop.ip}</span>
                          </div>
                        )}
                        {hop.date && (
                          <div className="flex items-center gap-1.5">
                            <Clock className="h-3 w-3" />
                            {hop.date}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
