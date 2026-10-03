// Generates one Kokoro clip per voiceover line, then prints each clip's speech extents.
// Usage: node brag-output/capture/gen-vo.mjs [id ...]
import { spawnSync } from 'node:child_process';
import { mkdirSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const OUT = fileURLToPath(new URL('../composition/assets/vo/', import.meta.url));
mkdirSync(OUT, { recursive: true });
const lines = JSON.parse(readFileSync(new URL('./voiceover.json', import.meta.url), 'utf8'));
const only = process.argv.slice(2);
const npx = process.platform === 'win32' ? 'npx.cmd' : 'npx';

for (const line of lines) {
  if (only.length && !only.includes(line.id)) continue;
  const q = (s) => `"${s.replace(/"/g, '')}"`;
  const run = spawnSync(npx, ['hyperframes', 'tts', q(line.text), '--voice', line.voice ?? 'af_heart', '--speed', String(line.speed ?? 1), '--output', q(`${OUT}${line.id}.wav`)], { encoding: 'utf8', shell: true });
  if (run.status !== 0) throw new Error(`${line.id}: ${run.stderr || run.stdout}`);
  console.log('generated', line.id);
}

for (const line of lines) {
  const { stderr } = spawnSync('ffmpeg', ['-hide_banner', '-i', `${OUT}${line.id}.wav`, '-af', 'silencedetect=n=-40dB:d=0.25', '-f', 'null', '-'], { encoding: 'utf8' });
  const dur = /Duration: (\d+):(\d+):([\d.]+)/.exec(stderr).slice(1).reduce((a, v) => a * 60 + Number(v), 0);
  const starts = [...stderr.matchAll(/silence_start: ([\d.]+)/g)].map((m) => Number(m[1]));
  const ends = [...stderr.matchAll(/silence_end: ([\d.]+)/g)].map((m) => Number(m[1]));
  const gaps = starts.map((s, i) => `${s.toFixed(2)}-${(ends[i] ?? dur).toFixed(2)}`);
  console.log(`${line.id} dur ${dur.toFixed(2)}  pauses ${gaps.join(' ')}`);
}
