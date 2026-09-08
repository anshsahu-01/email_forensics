'use client';

import { useEffect, useRef, useState } from 'react';

interface Point {
  lat: number;
  lng: number;
  label: string;
  detail?: string;
}

interface GoogleMapProps {
  points: Point[];
  className?: string;
}

export default function GoogleMap({ points, className = "h-96 w-full bg-slate-100" }: GoogleMapProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const apiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY;
    if (!apiKey) {
      setTimeout(() => setError('Google Maps API key is missing. Geographic intelligence disabled.'), 0);
      return;
    }

    if (window.google?.maps) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setLoaded(true);
      return;
    }

    const scriptId = 'google-maps-script';
    let script = document.getElementById(scriptId) as HTMLScriptElement;

    if (!script) {
      script = document.createElement('script');
      script.id = scriptId;
      script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places`;
      script.async = true;
      script.defer = true;
      script.onload = () => setLoaded(true);
      script.onerror = () => setError('Failed to load Google Maps API.');
      document.head.appendChild(script);
    } else {
      script.addEventListener('load', () => setLoaded(true));
    }
  }, []);

  useEffect(() => {
    if (!loaded || !mapRef.current || !window.google?.maps) return;

    if (points.length === 0) return;

    const bounds = new window.google.maps.LatLngBounds();
    points.forEach(p => {
        if (!isNaN(p.lat) && !isNaN(p.lng)) {
            bounds.extend(new window.google.maps.LatLng(p.lat, p.lng));
        }
    });

    const map = new window.google.maps.Map(mapRef.current, {
      center: bounds.getCenter(),
      zoom: points.length === 1 ? 10 : 2,
      mapTypeId: 'roadmap',
      disableDefaultUI: false,
      styles: [
        { featureType: 'all', elementType: 'labels.text.fill', stylers: [{ color: '#334155' }] },
        { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#e2e8f0' }] },
        { featureType: 'landscape', elementType: 'geometry', stylers: [{ color: '#f8fafc' }] },
        { featureType: 'poi', stylers: [{ visibility: 'off' }] }
      ]
    });

    const infoWindow = new window.google.maps.InfoWindow();

    points.forEach(point => {
      if (isNaN(point.lat) || isNaN(point.lng)) return;
      const marker = new window.google.maps.Marker({
        position: { lat: point.lat, lng: point.lng },
        map,
        title: point.label,
      });

      marker.addListener('click', () => {
        infoWindow.setContent(`
          <div style="padding: 4px; font-family: sans-serif; color: #0f172a;">
            <strong style="display: block; font-size: 12px; margin-bottom: 2px;">${point.label}</strong>
            ${point.detail ? `<span style="font-size: 11px; color: #475569;">${point.detail}</span>` : ''}
          </div>
        `);
        infoWindow.open(map, marker);
      });
    });

    if (points.length > 1) {
      map.fitBounds(bounds);
    }

  }, [loaded, points]);

  if (error) {
    return (
      <div className={`flex flex-col items-center justify-center border-2 border-dashed border-slate-200 bg-slate-50 text-slate-500 p-8 text-center ${className}`}>
        <p className="text-[10px] font-black uppercase tracking-widest">{error}</p>
      </div>
    );
  }

  if (points.length === 0) {
    return (
      <div className={`flex flex-col items-center justify-center border-2 border-dashed border-slate-200 bg-slate-50 text-slate-400 p-8 text-center ${className}`}>
        <p className="text-[10px] font-black uppercase tracking-widest">No Geographic Intelligence Available</p>
      </div>
    );
  }

  return (
    <div ref={mapRef} className={`border border-slate-200 ${className}`} />
  );
}
