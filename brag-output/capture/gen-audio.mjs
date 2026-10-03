// Per-frame music energy (RMS and bass, 0-1, 30 fps) for the composition's audio-reactive glow.
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const DIR = fileURLToPath(new URL('../composition/', import.meta.url));
const MUSIC = `${DIR}assets/music/happy-beats-business-moves-vol-1-by-ende-dot-app.mp3`;
const FPS = 30;
const RATE = 22050;
const DURATION = 90;

function decode(filter) {
  const raw = execFileSync('ffmpeg', ['-v', 'error', '-i', MUSIC, '-t', String(DURATION), ...(filter ? ['-af', filter] : []),
    '-ac', '1', '-ar', String(RATE), '-f', 'f32le', '-'], { maxBuffer: 1 << 30 });
  return new Float32Array(raw.buffer, raw.byteOffset, raw.byteLength / 4);
}

function envelope(samples) {
  const per = RATE / FPS;
  const out = Array.from({ length: DURATION * FPS }, (_, f) => {
    let sum = 0;
    let n = 0;
    for (let i = Math.floor(f * per); i < Math.min(samples.length, Math.floor((f + 1) * per)); i++, n++) sum += samples[i] ** 2;
    return n ? Math.sqrt(sum / n) : 0;
  });
  const top = [...out].sort((a, b) => a - b)[Math.floor(out.length * 0.98)] || 1;
  return out.map((v) => Math.round(Math.min(1, v / top) * 1000) / 1000);
}

mkdirSync(`${DIR}assets/data`, { recursive: true });
const rms = envelope(decode(''));
const bass = envelope(decode('lowpass=f=150,lowpass=f=150'));
writeFileSync(`${DIR}assets/data/audio.js`, `window.SP_AUDIO=${JSON.stringify({ fps: FPS, rms, bass })};\n`);
console.log(`audio.js: ${rms.length} frames`);
