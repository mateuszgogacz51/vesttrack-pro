interface AllocationRingProps {
  corePercentage: number;
  satellitePercentage: number;
  size?: number;
}

/**
 * Sygnaturowy element wizualny aplikacji: pierscien alokacji Core & Satellite.
 * Rysowany recznie jako SVG (nie generyczny wykres z biblioteki), zeby oddac
 * charakter konkretnej funkcji biznesowej tej platformy.
 */
export function AllocationRing({ corePercentage, satellitePercentage, size = 180 }: AllocationRingProps) {
  const radius = size / 2 - 14;
  const circumference = 2 * Math.PI * radius;
  const coreLength = (corePercentage / 100) * circumference;
  const center = size / 2;

  return (
    <div className="relative inline-flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={center} cy={center} r={radius} fill="none" stroke="#EEF2F6" strokeWidth={16} />
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="#CFA58C"
          strokeWidth={16}
          strokeDasharray={circumference}
          strokeDashoffset={0}
        />
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="#36597A"
          strokeWidth={16}
          strokeDasharray={`${coreLength} ${circumference - coreLength}`}
          strokeDashoffset={0}
          strokeLinecap="butt"
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="font-display text-2xl text-ink">{corePercentage.toFixed(0)}%</span>
        <span className="text-xs text-slate">Core</span>
      </div>
    </div>
  );
}

export function AllocationLegend({ corePercentage, satellitePercentage }: AllocationRingProps) {
  return (
    <div className="flex gap-5 mt-4 justify-center text-sm">
      <div className="flex items-center gap-2">
        <span className="w-3 h-3 rounded-full bg-brand inline-block" />
        <span className="text-slate">Core</span>
        <span className="num text-ink font-medium">{corePercentage.toFixed(1)}%</span>
      </div>
      <div className="flex items-center gap-2">
        <span className="w-3 h-3 rounded-full bg-clay inline-block" />
        <span className="text-slate">Satellite</span>
        <span className="num text-ink font-medium">{satellitePercentage.toFixed(1)}%</span>
      </div>
    </div>
  );
}
