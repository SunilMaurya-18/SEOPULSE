"use client";

"use client";

type OrbitRing = {
  diameter: number;
  positionY: number;
};

const DESKTOP_RINGS: OrbitRing[] = [
  { diameter: 318, positionY: 371 },
  { diameter: 355, positionY: 372 },
  { diameter: 393, positionY: 373 },
  { diameter: 432, positionY: 374 },
  { diameter: 434, positionY: 375 },
];

const MOBILE_RINGS: OrbitRing[] = [
  { diameter: 297, positionY: -3 },
  { diameter: 327, positionY: 0 },
  { diameter: 358, positionY: 0 },
  { diameter: 381, positionY: 0 },
  { diameter: 416, positionY: 0 },
];

const ORBIT_LINE_RINGS = 2;

const ORBIT_LINE_DURATIONS = ["12s", "18s"] as const;

const ORBIT_LINE_WIDTH = 2.5;

const OrbitLine = ({ duration }: { duration: string }) => {
  const ringMask = `radial-gradient(farthest-side, transparent calc(100% - ${ORBIT_LINE_WIDTH}px), #000 calc(100% - ${ORBIT_LINE_WIDTH}px))`;

  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 overflow-visible"
      style={{
        maskImage:
          "radial-gradient(ellipse 72% 58% at 50% 38%, #000 0%, #000 42%, rgba(0,0,0,0.35) 68%, transparent 100%)",
        WebkitMaskImage:
          "radial-gradient(ellipse 72% 58% at 50% 38%, #000 0%, #000 42%, rgba(0,0,0,0.35) 68%, transparent 100%)",
      }}
    >
      <div
        className="absolute inset-0 rounded-full will-change-transform animate-globe-orbit motion-reduce:animate-none"
        style={{
          animationDuration: duration,

          background:
            "conic-gradient(from 0deg, transparent 0deg, transparent 250deg, rgba(255,255,255,0.02) 280deg, rgba(255,255,255,0.08) 310deg, rgba(255,255,255,0.22) 335deg, rgba(255,255,255,0.55) 350deg, rgba(255,255,255,0.9) 357deg, #fff 360deg)",
          maskImage: ringMask,
          WebkitMaskImage: ringMask,
        }}
      />
    </div>
  );
};

const OrbitLayer = ({
  rings,
  className,
  positionClass,
}: {
  rings: OrbitRing[];
  className: string;
  positionClass: string;
}) => {
  return (
    <div
      aria-hidden="true"
      className={`absolute inset-0 mask-[linear-gradient(to_right,transparent,black_14%,black_86%,transparent)] ${className}`}
    >
      {rings.map((ring, index) => (
        <div
          key={ring.diameter}
          style={{
            width: `${ring.diameter}px`,
            height: `${ring.diameter}px`,
            marginTop: `${ring.positionY}px`,
          }}
          className={`absolute left-1/2 -translate-x-1/2 -translate-y-1/2 ${positionClass}`}
        >
          <div
            className="absolute inset-0 rounded-full border border-dashed border-white"
            style={{ opacity: 0.18 - index * 0.0375 }}
          />

          {index < ORBIT_LINE_RINGS ? (
            <OrbitLine duration={ORBIT_LINE_DURATIONS[index]} />
          ) : null}
        </div>
      ))}
    </div>
  );
};

const OrbitControls = () => {
  return (
    <>
      <OrbitLayer
        rings={DESKTOP_RINGS}
        className="hidden sm:block"
        positionClass="top-92.375"
      />
      <OrbitLayer
        rings={MOBILE_RINGS}
        className="sm:hidden"
        positionClass="top-67"
      />
    </>
  );
};

export default OrbitControls;