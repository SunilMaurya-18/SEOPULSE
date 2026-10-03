# Brag Plan: SEOPulse

## What is this app?
SEOPulse is a website audit and SEO intelligence SaaS: enter any URL and it crawls every page, scores each issue by severity, compares each audit with the last one, measures Core Web Vitals, alerts you when things break, and hands you a report you can share. Free for 3 websites; Pro and Agency are billed in dollars (Stripe) or rupees (Razorpay, UPI).

## The angle
"When did you last read your own website?" Google reads every page; most owners never do. SEOPulse reads it for you, and the video proves it with one fictional customer, Northwind Coffee, followed through the real product: a free quick check on the landing page, a live crawl, a score that climbs from 52 to 86 across eight weekly audits, the exact fixes, speed, alerts, reports, and pricing in two currencies. Every product shot is a real SEOPulse screen captured from the running app (Vite dev build) with fictional demo data; nothing comes from a real account.

## Hook (first 6 seconds)
Black terminal canvas, one coral pulse line. Three lines type in, one by one, under the narrator: "Hundreds of pages." / "Google reads every one." / "When did you last read yours?" The question is the hook; the answer is the product.

## Key moments (the middle)
- The landing page quick check: "northwind-coffee.com" types into the real field, "Check my site free" is clicked, and the real result card rises with its 74 score ring and six category bars.
- The live crawl: the real audit screen with a page counter climbing and the Queued → Crawling → Analyzing steps lighting up.
- The payoff: the real audit header (score 86, Grade B), then "Issues changed since last audit" with 14 fixed, 2 new, "+7 score".
- Issues ranked by severity with plain fixes; Core Web Vitals rated "Good"; the score trend line drawing from 52 to 86.
- Alerts (email, Slack, signed webhook), PDF report and share link, then pricing flipping from $29 to ₹1,999 with UPI.

## Outro / punchline
"SEOPulse. Know exactly what to fix first." The wordmark (coral slider mark + SEOPulse) over the coral arc glow, then the real CTA "Check my site free".

## User flow worth showing
Landing quick check (type URL → score + top issues) → full audit runs live → score, what changed, ranked fixes → schedule, trend, alerts, reports.

## Tone
- Preset: app-store
- Creative direction: terminal-precise product film; black canvas, coral pulse, real screens doing real work
- Interpretation: feature-forward and confident, one idea per scene, clean slides and wipes, steady light SFX layer; the brand's dark marketing look frames the light app screens like a lit window.

## Format: landscape — 1920x1080
## Duration: 90 seconds (user request; narration sets the pace)

## Visual identity (from the project)
- Background: #05070a (marketing near-black), pure #000 in hero; app screens light #f5f5f7
- Accent: #f5504a (coral), hover #ff6d66
- Text: #f2f5ea (warm off-white), muted #8b93a1
- Display font: Geist (marketing headings); labels in IBM Plex Mono with wide tracking (the site's "SEO ANALYTICS PLATFORM" kicker style)
- Body font: Geist
- Strongest visual element: the coral arc glow behind the hero and the score ring; the floating command-bar nav in the app

## Share copy (draft)
Google reads every page of your site. SEOPulse does too, and tells you exactly what to fix first. Free for 3 websites.

## Audio direction
- Role: warm upbeat bed under a single narrator, with a light, consistent UI SFX layer
- Music: happy-beats-business-moves-vol-1 (120 BPM, most energetic bundled track; one track covers 90 s)
- Music treatment: fade in over the hook, duck to ~0.13 under narration, lift to ~0.32 in the gaps between scenes, swell under the final wordmark and fade out by 90 s
- Music cue guidance: full-track analysis in `composition/assets/music/cues/vol-1.music-cues.json` (tempo 120.19 BPM, beats on x.02 / x.52). Strong cues to target: 17.02 s (quick-check card rises), 54.52 s (Core Web Vitals reveal), 77.01 s (pricing). Beat-grid windows: 30.0–31.5 s (crawl steps), 48.0–51.0 s (issue cards, every other beat), 69.0–72.0 s (alert rows, every other beat)
- Audio-reactive treatment: subtle; music RMS/bass makes the coral arc glow and the screen frame's shadow breathe. No waveform or equalizer graphics.
- SFX posture: moderate, app-store; key ticks for typed text, click on simulated taps, drop/card sounds on card reveals, one bell on the outro
- Audio-coupled moments: hook typing, URL typing + click, score ring landing, crawl step ticks, issue cards, alert rows, currency switch, wordmark
- Restraint rule: no SFX over a narrated number; nothing louder than the narrator

## Storyboard

### Scene 1 — Hook — 0–8 s
Black canvas, coral pulse line drawing across. Lines type in: "Hundreds of pages." (1.1 s), "Google reads every one." (3.5 s), "When did you last read yours?" (5.3 s, coral).
Sequential/interaction: yes — three lines typed one by one, each held until scene end
Audio intent: curiosity; music fades in under the voice
Audio-coupled idea: soft key ticks on the typing
Transition mood: soft → Scene 2

### Scene 2 — Reveal — 8–13.5 s
SEOPulse wordmark lands (8.52 s, beat) over the coral arc glow; kicker "SEO ANALYTICS PLATFORM"; line "A full SEO audit, in about a minute."
Sequential/interaction: none
Audio intent: arrival; one soft impact on the logo
Transition mood: clean slide → Scene 3

### Scene 3 — Quick check — 13.5–25.5 s
Real landing page in a browser frame. Cursor types "northwind-coffee.com" into the real field; clicks "Check my site free"; the real quick-check card rises (17.02 s strong cue): score 74, Grade C, six category bars, three top issues. Side labels: "No sign-up", "5 pages, on the spot".
Sequential/interaction: yes — typing, click, card reveal, issue rows highlighted one by one
Audio intent: hands-on; keys, click, card landing
Transition mood: clean wipe → Scene 4

### Scene 4 — Live audit — 25.5–36.5 s
Real audit screen for shop.northwind-coffee.com (light app). A page counter climbs; step chips Queued → Crawling → Analyzing light in turn (30.0 / 30.5 / 31.5 s). Side note: "Honours robots.txt".
Sequential/interaction: yes — counter ticks, three step chips light one by one
Audio intent: momentum; light ticks
Transition mood: slide → Scene 5

### Scene 5 — The payoff — 36.5–46.5 s
Real audit header for northwind-coffee.com: score 86, Grade B; six category scores. Then the real "Issues changed since last audit" card: Fixed 14 (42.3 s), New 2 (43.3 s), "+7 score" (44.4 s).
Sequential/interaction: yes — three numbers highlighted one by one
Audio intent: satisfaction; one positive accent on +7
Transition mood: slide → Scene 6

### Scene 6 — Ranked fixes — 46.5–54 s
Real issues page; the first three issue cards (Error) slide into focus, each with its recommendation highlighted (48.0 / 49.0 / 50.0 s).
Sequential/interaction: yes — three cards, every other beat, held
Audio intent: clarity; card sounds
Transition mood: slide → Scene 7

### Scene 7 — Speed — 54–60.5 s
Real Core Web Vitals card zooms in (54.52 s strong cue): performance 91, every metric "Good" (2.1 s LCP, 160 ms INP, 0.05 CLS).
Sequential/interaction: yes — metric tiles highlight left to right
Audio intent: lift
Transition mood: clean → Scene 8

### Scene 8 — The climb — 60.5–68 s
Real score trend card; the coral line draws left to right (61.52 s); counter "52 → 86" in big type; label "8 weekly audits".
Sequential/interaction: yes — line draws, count-up
Audio intent: rising
Transition mood: slide → Scene 9

### Scene 9 — Alerts and reports — 68–77 s
Real alerts card: four rules arrive (email, Slack, webhook). Then report chips: "PDF report", "Share link · no account needed", "White-label on Agency".
Sequential/interaction: yes — four alert rows, then three chips
Audio intent: dependable; soft drops
Transition mood: slide → Scene 10

### Scene 10 — Pricing — 77–83.5 s
Real pricing page (77.01 s strong cue). Pro price flips $29 → ₹1,999 (81.0 s) as the INR pricing page replaces the USD one; chip "UPI · RuPay · cards via Razorpay"; "Free for 3 websites".
Sequential/interaction: yes — currency switch
Audio intent: light, friendly; switch sound
Transition mood: soft → Scene 11

### Scene 11 — Outro — 83.5–90 s
Black canvas, coral arc glow. Wordmark (84.02 s), tagline "Know exactly what to fix first." (85.6 s), CTA button "Check my site free" (86.6 s). Hold, fade at 89.4 s.
Sequential/interaction: none
Audio intent: resolve; bell on wordmark, music swell then fade
Transition mood: end

**Music mood for this video:** upbeat
**Audio summary:** a quiet curious open, the bed lifts with the logo, steady narrated tour with light UI sounds, and a bright swell under the final wordmark.

## Voiceover script
Narrator: Kokoro `af_heart`. One clip per scene (`composition/assets/vo/vo-NN.wav`), placed at the scene's VO start.

| Clip | Starts | Length | Line |
|---|---:|---:|---|
| vo-01 | 1.0 s | 5.89 s | Your website has hundreds of pages. Google reads every one of them. When did you last read yours? |
| vo-02 | 8.6 s | 3.82 s | Meet SEOPulse. A full SEO audit, in about a minute. |
| vo-03 | 14.0 s | 9.77 s | Type any address on the homepage. No sign-up. SEOPulse checks five pages on the spot: a score, six category grades, and the first things to fix. |
| vo-04 | 26.0 s | 9.47 s | Sign up free, and the full audit crawls every page, live. Queued, crawling, analyzing. And it plays by the rules of robots dot T X T. |
| vo-05 | 37.0 s | 8.28 s | Then, one health score across six categories. Every audit is compared with the last: fourteen issues fixed, two new, seven points up. |
| vo-06 | 47.0 s | 6.38 s | Each issue is ranked by severity, with the exact page and a plain fix. No guessing what comes first. |
| vo-07 | 54.5 s | 5.14 s | Speed counts too. Core Web Vitals from Google, in the lab and from real visitors. |
| vo-08 | 61.0 s | 6.02 s | Schedule audits weekly or daily, and watch the trend climb. Fifty two to eighty six, in eight weeks. |
| vo-09 | 68.5 s | 8.06 s | When something breaks, alerts reach email, Slack or a signed webhook. Reports go out as a PDF, or a link anyone can open. |
| vo-10 | 77.5 s | 5.10 s | Start free with three websites. Upgrade in dollars, or in rupees with UPI. |
| vo-11 | 84.3 s | 2.99 s | SEOPulse. Know exactly what to fix first. |

## Privacy note
All names, domains, scores and alert targets are fictional (Northwind Coffee is a classic sample company). The UI was served demo data by `capture/capture.mjs`; no real account, email or customer data appears.
