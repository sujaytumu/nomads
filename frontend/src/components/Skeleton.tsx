// Shimmer placeholders shaped like the real content, so pages feel instant and nothing jumps when data arrives.
// They reuse the global `.animate-pulse` shimmer (works in light and dark theme).

export function Bone({ className = "" }: { className?: string }) {
  return <div aria-hidden="true" className={`animate-pulse rounded-lg ${className}`} />;
}

/** Same shape as <PackageCard>: cover, title, two text lines, two buttons. */
export function PackageCardSkeleton() {
  return (
    <div className="card overflow-hidden" aria-hidden="true">
      <Bone className="h-48 rounded-none" />
      <div className="space-y-3 p-4">
        <Bone className="h-5 w-3/4" />
        <Bone className="h-3.5 w-full" />
        <Bone className="h-3.5 w-2/3" />
        <div className="flex gap-2 pt-1">
          <Bone className="h-8 flex-1 rounded-full" />
          <Bone className="h-8 flex-1 rounded-full" />
        </div>
      </div>
    </div>
  );
}

export function PackageGridSkeleton({ count = 3, className = "grid gap-4 sm:grid-cols-2 lg:grid-cols-3" }: { count?: number; className?: string }) {
  return (
    <div className={className} aria-busy="true" aria-label="Loading packages">
      {Array.from({ length: count }, (_, i) => <PackageCardSkeleton key={i} />)}
    </div>
  );
}

/** A trip row on My Trips / Payment: title, date line, progress dots, action buttons. */
export function TripCardSkeleton() {
  return (
    <div className="card space-y-4 p-5" aria-hidden="true">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1 space-y-2">
          <Bone className="h-5 w-1/2" />
          <Bone className="h-3.5 w-1/3" />
        </div>
        <Bone className="h-6 w-20 rounded-full" />
      </div>
      <div className="flex items-center gap-2">
        {Array.from({ length: 6 }, (_, i) => <Bone key={i} className="h-7 w-7 rounded-full" />)}
      </div>
      <div className="flex gap-2">
        <Bone className="h-8 w-24 rounded-full" />
        <Bone className="h-8 w-24 rounded-full" />
      </div>
    </div>
  );
}

export function TripListSkeleton({ count = 2, label = "Loading your trips" }: { count?: number; label?: string }) {
  return (
    <div className="space-y-3" aria-busy="true" aria-label={label}>
      {Array.from({ length: count }, (_, i) => <TripCardSkeleton key={i} />)}
    </div>
  );
}

/** Place details page: big image, title, paragraph, side card. */
export function PlaceSkeleton() {
  return (
    <div className="section py-8" aria-busy="true" aria-label="Loading place">
      <div className="grid gap-6 md:grid-cols-3">
        <div className="space-y-3 md:col-span-2">
          <Bone className="h-72 w-full rounded-2xl" />
          <Bone className="h-7 w-2/3" />
          <Bone className="h-4 w-full" />
          <Bone className="h-4 w-5/6" />
        </div>
        <div className="card space-y-4 p-4">
          <Bone className="h-4 w-1/3" />
          <Bone className="h-3.5 w-full" />
          <Bone className="h-4 w-1/3" />
          <Bone className="h-3.5 w-2/3" />
          <Bone className="h-9 w-32 rounded-full" />
        </div>
      </div>
    </div>
  );
}

/** Generic stack of text rows inside a card (itinerary, profile, forms). */
export function CardSkeleton({ rows = 4, label = "Loading" }: { rows?: number; label?: string }) {
  return (
    <div className="card space-y-3 p-5" aria-busy="true" aria-label={label}>
      <Bone className="h-5 w-1/3" />
      {Array.from({ length: rows }, (_, i) => <Bone key={i} className={`h-4 ${i % 3 === 2 ? "w-2/3" : "w-full"}`} />)}
    </div>
  );
}

/** Itinerary stops: numbered circle + title + time. */
export function StopListSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div className="space-y-2" aria-busy="true" aria-label="Loading stops">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="flex items-center gap-3 rounded-2xl border border-slate-800 bg-slate-900/60 px-3 py-3">
          <Bone className="h-6 w-6 shrink-0 rounded-full" />
          <Bone className="h-3.5 w-20 shrink-0" />
          <Bone className="h-4 flex-1" />
        </div>
      ))}
    </div>
  );
}

export function MapSkeleton({ height = 420 }: { height?: number }) {
  return <div aria-busy="true" aria-label="Loading map" style={{ height }} className="animate-pulse w-full rounded-2xl" />;
}
