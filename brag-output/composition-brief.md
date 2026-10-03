# Hyperframes Composition Brief: SEOPulse

## Objective
Create a 90-second narrated launch-style brag video for SEOPulse (user asked for 90 s with audio, overriding the 15–25 s default).

## Output
- Composition directory: `brag-output/composition/`
- Rendered video: `brag-output/brag.mp4`
- Format: landscape — 1920x1080, 30 fps
- Duration: 90 seconds

## Source Material
- Project root: `SEOPULSE/` (React 19 + Vite frontend in `seopulse-frontend/`, Spring Boot API in `seopulse-backend/`)
- Primary files read: `seopulse-frontend/src/index.css`, `index.html`, landing components (`HeroSection`, `QuickCheck`, `SolutionsSection`, `VisionSection`, `FinalCtaSection`), `PricingPage`, `Logo`, app pages (dashboard, audit detail, issues, settings), `README.md`
- Product name: SEOPulse
- Tagline / strongest claim: "Use data to get a 360-degree view of your site." / "know exactly what to fix first"
- Key UI to show: real screenshots in `composition/assets/ui/` (landing hero, quick-check card, live audit, audit page, changes card, issues, Core Web Vitals card, trend card, alerts card, plan card, pricing USD/INR)
- Copy that must appear verbatim:
  - Check my site free
  - SEO ANALYTICS PLATFORM
  - Know exactly what to fix first.

## Creative Direction
- Tone preset: app-store
- Creative direction: terminal-precise product film; black canvas, coral pulse, real screens doing real work
- Interpretation: one idea per scene, clean slides, light consistent SFX, confident narration
- Angle: "When did you last read your own website?" — follow fictional Northwind Coffee through the real product
- Hook: three typed lines ending in "When did you last read yours?"
- Outro / punchline: "SEOPulse. Know exactly what to fix first." + "Check my site free"
- Avoid: generic SaaS language, abstract filler visuals, redesigning the product UI

## Visual Identity
- Background: #05070a (scenes), app screenshots are light (#f5f5f7)
- Text: #f2f5ea, muted #8b93a1
- Accent: #f5504a
- Display/body font: Geist (local woff2); labels: IBM Plex Mono (local woff2), uppercase, tracking 0.22em
- Visual references: coral arc glow, score ring, floating command bar, rounded white cards

## Storyboard
Use `brag-output/brag-plan.md` as the creative contract. Scenes: Hook 0–8, Reveal 8–13.5, Quick check 13.5–25.5, Live audit 25.5–36.5, Payoff 36.5–46.5, Ranked fixes 46.5–54, Speed 54–60.5, Climb 60.5–68, Alerts and reports 68–77, Pricing 77–83.5, Outro 83.5–90.

## Audio
- Audio role: warm bed under one narrator plus a light UI SFX layer
- Audio arc: quiet open → lift on logo → ducked tour with gap lifts → swell and fade on the wordmark
- Music: `assets/music/happy-beats-business-moves-vol-1-by-ende-dot-app.mp3`
- Music treatment: 0.3 open, duck to 0.13 under each VO clip, 0.3 in gaps, 0.34 under the outro, fade out 88.4–90
- Music cue guidance: `assets/music/cues/vol-1.music-cues.json` (120.19 BPM); lock 17.02, 54.52, 77.01; beat-grid 30–31.5, 48–51, 69–72
- Audio-reactive treatment: subtle; precomputed RMS/bass (`assets/data/audio.js`) drives arc glow opacity and frame shadow
- Voiceover: Kokoro `af_heart`, 11 clips in `assets/vo/`, timings in the plan
- SFX selection guidance: `D:\brag\skills\brag\assets\sfx\sfx-analysis.md`; low high-frequency-risk picks for repeated moments; volumes 0.3–0.6
- Audio files are copied into `composition/assets/`

## Hyperframes Instructions
Follow hyperframes-core: standalone root, one paused GSAP timeline at `window.__timelines["seopulse-brag"]`, `class="clip"` scenes with `data-start`/`data-duration`/`data-track-index`, every `<audio>` with an id, local fonts via `@font-face`, no CSS transform + GSAP tween conflicts. Run `npx hyperframes check` before render.
