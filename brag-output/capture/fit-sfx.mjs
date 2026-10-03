// Sets each SFX slot's data-duration to the real media length (ffprobe).
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const DIR = fileURLToPath(new URL('../composition/', import.meta.url));
const file = `${DIR}index.html`;
const lengths = new Map();
const probe = (src) => {
  if (!lengths.has(src)) {
    const out = execFileSync('ffprobe', ['-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', DIR + src]).toString();
    lengths.set(src, Math.max(0.05, Math.floor(parseFloat(out) * 100) / 100));
  }
  return lengths.get(src);
};

let count = 0;
const html = readFileSync(file, 'utf8').replace(
  /(<audio id="sfx-[^"]+" src="(assets\/sfx\/[^"]+)"[^>]*?data-duration=")([\d.]+)/g,
  (_, head, src) => { count++; return head + probe(src); },
);
writeFileSync(file, html);
console.log(`fitted ${count} sfx slots`);
