// Copies GSAP, the chosen SFX and local font files into the composition.
import { copyFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const C = fileURLToPath(new URL('../composition/assets/', import.meta.url));
const SKILL = 'D:/brag/skills/brag/assets/';
const copy = (from, to) => { mkdirSync(dirname(C + to), { recursive: true }); copyFileSync(from, C + to); };

copy('D:/brag-output/composition/assets/vendor/gsap.min.js', 'vendor/gsap.min.js');

const sfx = [
  'impact/impactSoft_medium_000.ogg', 'impact/impactSoft_medium_001.ogg', 'impact/impactSoft_medium_003.ogg',
  'impact/impactBell_heavy_000.ogg', 'impact/impactBell_heavy_004.ogg',
  'interface/click_002.ogg', 'interface/click_003.ogg', 'interface/drop_001.ogg', 'interface/drop_002.ogg', 'interface/drop_003.ogg',
  'interface/bong_001.ogg', 'interface/switch_002.ogg', 'ui/click2.ogg', 'ui/rollover2.ogg', 'casino/card-slide-1.ogg',
  ...Array.from({ length: 12 }, (_, i) => `keyboard/keypress-${String(i + 1).padStart(3, '0')}.wav`),
];
for (const f of sfx) copy(`${SKILL}sfx/${f}`, `sfx/${f}`);

// Google Fonts serves latin woff2 subsets to a modern browser UA.
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36';
const families = { geist: 'Geist:wght@400;500;600;700', plex: 'IBM+Plex+Mono:wght@400;500;600' };
mkdirSync(`${C}fonts`, { recursive: true });
const faces = [];
for (const [key, spec] of Object.entries(families)) {
  const css = await (await fetch(`https://fonts.googleapis.com/css2?family=${spec}&display=swap`, { headers: { 'user-agent': UA } })).text();
  const blocks = css.split('@font-face').slice(1).filter((b) => b.includes('/* latin */') || !b.includes('/*')) ;
  const latin = css.split(/\/\* ([\w-]+) \*\//).reduce((acc, part, i, arr) => (arr[i - 1] === 'latin' ? [...acc, part] : acc), []);
  for (const block of latin.length ? latin : blocks) {
    const url = /url\((https:[^)]+)\)/.exec(block)?.[1];
    const weight = /font-weight: (\d+)/.exec(block)?.[1];
    const family = /font-family: '([^']+)'/.exec(block)?.[1];
    if (!url) continue;
    const file = `${key}-${weight}.woff2`;
    writeFileSync(`${C}fonts/${file}`, Buffer.from(await (await fetch(url)).arrayBuffer()));
    faces.push({ family, weight, file });
  }
}
console.log(faces);
