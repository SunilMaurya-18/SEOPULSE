"use client";

import "./presence-01.css";
import Globe from "@/components/originkit/ui/features-02/globe";
import OrbitControls from "@/components/originkit/ui/features-02/orbit-controls";

const A = "/originkit/features-02";

export interface Features02Metric {
  value: string;
  label: string;
}

export interface Features02Props {
  id?: string;
  eyebrow: string;
  title: string;
  description: string;
  metrics: readonly Features02Metric[];
}

const Features02 = ({ id, eyebrow, title, description, metrics }: Features02Props) => {
  return (
    <div className="overflow-hidden bg-black px-4 text-white sm:px-6">
      <section
        id={id}
        aria-labelledby="features-02-heading"
        className="relative mx-auto flex w-full max-w-295.5 scroll-mt-24 flex-col items-center py-10 sm:min-h-197.5 sm:py-0"
      >
        <div
          aria-hidden="true"
          className="relative h-106 w-full shrink-0 overflow-hidden sm:absolute sm:inset-x-0 sm:top-0 sm:h-126"
        >
          <div className="absolute inset-0 mask-[linear-gradient(to_bottom,black_0%,black_48%,rgba(0,0,0,0.35)_52%,transparent_100%)] [-webkit-mask-image:linear-gradient(to_bottom,black_0%,black_48%,rgba(0,0,0,0.35)_72%,transparent_100%)]">
            <div className="pointer-events-none absolute left-1/2 top-67 h-80 w-80 -translate-x-1/2 -translate-y-1/2 rounded-full bg-[radial-gradient(circle,rgba(255,255,255,0.10),transparent_70%)] sm:top-92.375 sm:h-82.75 sm:w-82.75" />

            <OrbitControls />

            <div className="pointer-events-auto absolute left-1/2 top-28 size-78 -translate-x-1/2 cursor-grab touch-none active:cursor-grabbing sm:top-51 sm:size-82.75">
              <div className="relative size-78 sm:size-82.75">
                <Globe
                  direction="right"
                  dots={{
                    color: "#737373",
                    size: 10,
                    density: 4,
                    allDots: false,
                  }}
                  speed={1}
                  smoothing={0}
                  stopOnHover={false}
                  dragSpeed={5}
                  showOutline={false}
                  showGrid={false}
                  oceanColor="#050505"
                  scale={9}
                  initialLatitude={23}
                  initialLongitude={-23}
                />
              </div>
            </div>
          </div>

          <div className="pointer-events-none absolute inset-x-0 bottom-0 h-40 bg-linear-to-b from-transparent to-black sm:h-28" />
        </div>

        <div className="pointer-events-none relative z-10 flex w-full flex-1 flex-col items-center pt-4 sm:min-h-197.5 sm:pt-104">
          <header className="flex w-full max-w-220.5 flex-col items-center gap-4 px-2 text-center -mt-36 sm:mt-0 md:-mt-2">
            <p className="flex items-center gap-1 text-[15px] font-medium leading-normal text-[#adb1b8] sm:text-[17px]">
              <img
                alt=""
                aria-hidden="true"
                src={`${A}/globe-icon.svg`}
                width={22}
                height={22}
                className="size-4.5 sm:size-5.5"
              />
              {eyebrow}
            </p>

            <div className="flex flex-col items-center gap-4 sm:gap-5">
              <h2
                id="features-02-heading"
                className="font-tight text-[clamp(2.125rem,8vw,2.75rem)] font-bold leading-[1.2] tracking-[-0.01em] text-balance text-white [text-shadow:0_8px_30px_rgba(255,255,255,0.25),0_4px_8px_rgba(255,255,255,0.05)] ipad-landscape:text-[44px] desktop-sm:text-[clamp(2.125rem,8vw,3.625rem)]"
              >
                {title}
              </h2>

              <p className="max-w-2xl font-medium leading-normal text-balance text-[#9297a0] text-[18px]">
                {description}
              </p>
            </div>
          </header>

          <div
            aria-hidden="true"
            className="hidden min-h-10 flex-1 ipad-landscape:block"
          />

          <ul className="pointer-events-auto mt-8 mb-16 grid w-full max-w-140 shrink-0 sm:mt-10 sm:mb-16 desktop-sm:mt-0 ipad-landscape:max-w-none ipad-landscape:grid-cols-3 ipad-landscape:my-16">
            {metrics.map((metric, index) => (
              <li
                key={metric.value}
                className="relative flex w-full max-w-81.75 mx-auto flex-col self-center items-center gap-6 py-8 text-center ipad-landscape:max-w-none ipad-landscape:self-auto ipad-landscape:gap-6 ipad-landscape:px-6 ipad-landscape:py-0"
              >
                {index > 0 ? (
                  <span
                    aria-hidden="true"
                    className="absolute inset-x-0 top-0 mx-auto h-px w-full bg-[#18191b] ipad-landscape:inset-x-auto ipad-landscape:left-0 ipad-landscape:top-1/2 ipad-landscape:h-12 ipad-landscape:w-px ipad-landscape:-translate-y-1/2"
                  />
                ) : null}

                <span className="bg-linear-to-br from-white to-white/50 bg-clip-text font-tight text-[34px] font-semibold tabular-nums leading-[1.2] tracking-[-0.04em] text-transparent [text-shadow:0_8px_30px_rgba(255,255,255,0.25),0_4px_8px_rgba(255,255,255,0.05)] ipad-landscape:text-[44px] desktop-sm:text-[58px]">
                  {metric.value}
                </span>

                <span className="text-[18px] font-medium leading-normal text-[#6b707a] ipad-landscape:max-w-[200px] desktop-sm:max-w-none">
                  {metric.label}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </div>
  );
};

export default Features02;