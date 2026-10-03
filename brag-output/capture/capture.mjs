// Captures real SEOPulse screens for the brag video. The UI is the running Vite dev
// server; every API call is answered with the fictional workspace in demo-data.mjs.
// Usage: node brag-output/capture/capture.mjs  (with `npm run dev` on :5173)
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import * as D from './demo-data.mjs';

const require = createRequire(fileURLToPath(new URL('../../seopulse-frontend/package.json', import.meta.url)));
const { chromium } = require('@playwright/test');

const APP = 'http://localhost:5173';
const OUT = fileURLToPath(new URL('../composition/assets/ui/', import.meta.url));
mkdirSync(OUT, { recursive: true });
const only = process.argv.slice(2);

const cors = {
  'access-control-allow-origin': APP,
  'access-control-allow-credentials': 'true',
  'access-control-allow-headers': 'content-type, authorization, x-requested-with, accept',
  'access-control-allow-methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
};
const pageOf = (content) => ({ content, page: 0, size: content.length, totalElements: content.length, totalPages: 1, first: true, last: true });
const unknown = new Set();

function api(method, path, query, state) {
  let m;
  if (path === '/auth/refresh' || path === '/auth/login') return D.user;
  if (path === '/orgs') return [D.org];
  if ((m = path.match(/^\/orgs\/\d+\/billing$/))) return state.billing;
  if (/^\/orgs\/\d+\/members$/.test(path)) return D.members;
  if (/^\/orgs\/\d+\/invitations$/.test(path)) return [];
  if (/^\/orgs\/\d+\/alerts$/.test(path)) return D.alerts;
  if (/^\/orgs\/\d+\/alerts\/deliveries$/.test(path)) return D.deliveries;
  if (/^\/orgs\/\d+\/branding$/.test(path)) return { branding: null, whiteLabelAvailable: false };
  if (path === '/projects') return pageOf([D.project]);
  if (/^\/projects\/\d+$/.test(path)) return D.project;
  if (/^\/projects\/\d+\/summary$/.test(path)) return { projectId: 1, projectName: D.project.name, totalWebsites: 3, totalAudits: D.allAudits.length, completedAudits: D.allAudits.length - 1, failedAudits: 0, activeAudits: 1 };
  if (/^\/projects\/\d+\/onboarding$/.test(path)) return D.onboarding;
  if (/^\/projects\/\d+\/websites$/.test(path)) return pageOf(D.websites);
  if ((m = path.match(/^\/projects\/\d+\/websites\/(\d+)$/))) return D.websites.find((w) => w.id === Number(m[1]));
  if (/^\/projects\/\d+\/websites\/\d+\/trend$/.test(path)) return D.trend;
  if (/^\/projects\/\d+\/websites\/\d+\/schedule$/.test(path)) return { enabled: true, frequency: 'WEEKLY', dayOfWeek: 'MONDAY', hour: 6, timezone: 'Europe/London', nextRunAt: '2026-10-05T05:00:00Z' };
  if (/^\/projects\/\d+\/audits$/.test(path)) {
    const site = Number(query.get('websiteId'));
    return pageOf(site ? D.auditsBySite[site] ?? [] : D.allAudits);
  }
  const audit = (id) => (Number(id) === D.liveAudit.id ? state.live : D.allAudits.find((a) => a.id === Number(id)));
  if ((m = path.match(/^\/projects\/\d+\/audits\/(\d+)$/))) return audit(m[1]);
  if ((m = path.match(/^\/projects\/\d+\/audits\/(\d+)\/summary$/))) return D.summaryFor(audit(m[1]));
  if (/^\/projects\/\d+\/audits\/\d+\/compare$/.test(path)) return D.comparison;
  if (/^\/projects\/\d+\/audits\/\d+\/web-vitals$/.test(path)) return D.webVitals;
  if (/^\/projects\/\d+\/audits\/\d+\/reports$/.test(path)) return D.reports;
  if (/^\/projects\/\d+\/audits\/\d+\/shares$/.test(path)) return D.shares;
  if (/^\/projects\/\d+\/audits\/\d+\/pages$/.test(path)) return pageOf(D.pages);
  if (/^\/projects\/\d+\/audits\/\d+\/issues$/.test(path)) {
    const sev = query.get('severity');
    const rule = query.get('ruleCode');
    return pageOf(D.issues.filter((i) => (!sev || i.severity === sev) && (!rule || i.ruleCode === rule)));
  }
  if (path === '/public/quick-check') return D.quickCheck;
  unknown.add(`${method} ${path}${query.toString() ? `?${query}` : ''}`);
  return null;
}

async function mock(context, state) {
  await context.route('http://localhost:8082/**', async (route) => {
    const request = route.request();
    if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers: cors });
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/api\/v1/, '');
    if (/\/audits\/\d+\/events$/.test(path)) {
      const body = `event: audit\ndata: ${JSON.stringify(state.live)}\n\n`;
      return route.fulfill({ status: 200, headers: { ...cors, 'content-type': 'text/event-stream' }, body });
    }
    if (state.anon && path.startsWith('/auth/')) return route.fulfill({ status: 401, headers: { ...cors, 'content-type': 'application/json' }, body: '{}' });
    if (state.delay && path === '/public/quick-check') await new Promise((r) => setTimeout(r, state.delay));
    const json = api(request.method(), path, url.searchParams, state);
    if (json === null) return route.fulfill({ status: 404, headers: { ...cors, 'content-type': 'application/json' }, body: '{"title":"Not found"}' });
    return route.fulfill({ status: 200, headers: { ...cors, 'content-type': 'application/json' }, body: JSON.stringify(json) });
  });
}

const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });

async function open({ theme = 'light', locale = 'en-US', timezone = 'Europe/London', height = 900, state = {} } = {}) {
  const context = await browser.newContext({
    viewport: { width: 1440, height }, deviceScaleFactor: 2, colorScheme: theme, locale, timezoneId: timezone,
  });
  await context.addInitScript((t) => {
    localStorage.setItem('seopulse-theme', t);
    localStorage.setItem('seopulse.theme', t);
  }, theme);
  await mock(context, { billing: D.billing, live: D.liveAudit, ...state });
  const page = await context.newPage();
  page.on('pageerror', (e) => console.log('pageerror:', e.message));
  return { context, page };
}

async function settle(page, ms = 900) {
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(ms);
}

const shots = [];
async function shot(page, name, opts = {}) {
  await page.screenshot({ path: `${OUT}${name}.png`, animations: 'disabled', caret: 'hide', ...opts });
  shots.push(name);
  console.log('shot', name);
}
const want = (name) => !only.length || only.includes(name);

// Screenshots the card (nearest wide, rounded, filled ancestor) that holds an exact text label.
async function card(page, text, name) {
  const handle = await page.evaluateHandle((label) => {
    const hit = [...document.querySelectorAll('h1,h2,h3,h4,p,span,div,label')].find((n) => n.childElementCount === 0 && n.textContent.trim() === label);
    const isCard = (n) => {
      if (!n) return false;
      const style = getComputedStyle(n);
      return n.getBoundingClientRect().width > 480 && style.borderRadius !== '0px' && style.backgroundColor !== 'rgba(0, 0, 0, 0)';
    };
    let node = hit;
    while (node && node.parentElement) {
      if (isCard(node)) return node;
      if (isCard(node.nextElementSibling)) return node.nextElementSibling;
      node = node.parentElement;
    }
    return node;
  }, text);
  const el = handle.asElement();
  if (!el) return console.log('card not found:', text);
  await el.screenshot({ path: `${OUT}${name}.png`, animations: 'disabled' });
  shots.push(name);
  console.log('card', name);
}

const boxes = {};
async function measure(page, name, labels) {
  boxes[name] = await page.evaluate((list) => Object.fromEntries(list.map((label) => {
    const hit = [...document.querySelectorAll('h1,h2,h3,h4,p,span,div,label,button,a')].find((n) => n.childElementCount === 0 && n.textContent.trim() === label);
    if (!hit) return [label, null];
    const b = hit.getBoundingClientRect();
    return [label, { x: Math.round(b.left), y: Math.round(b.top), w: Math.round(b.width), h: Math.round(b.height) }];
  })), labels);
}

if (want('landing')) {
  const { context, page } = await open({ theme: 'dark', state: { delay: 600, anon: true } });
  await page.goto(`${APP}/`);
  await settle(page, 3000);
  await shot(page, 'landing-hero');
  await measure(page, 'landing-hero', ['Check my site free', 'Use data to get a 360-degree view of your site.']);
  const input = page.locator('#quick-check-url');
  boxes['landing-input'] = await input.boundingBox();
  await input.fill('northwind-coffee.com');
  await input.blur();
  await shot(page, 'landing-typed');
  await page.getByRole('button', { name: 'Check my site free' }).click();
  await page.getByRole('region', { name: /SEO check for/ }).waitFor();
  await settle(page, 900);
  const report = page.getByRole('region', { name: /SEO check for/ });
  await report.screenshot({ path: `${OUT}quick-check-card.png`, animations: 'disabled' });
  shots.push('quick-check-card');
  await context.close();
}

if (want('dashboard')) {
  const { context, page } = await open({ height: 2400 });
  await page.goto(`${APP}/dashboard`);
  await settle(page, 2000);
  await shot(page, 'dashboard-tall');
  await context.close();
}

if (want('live')) {
  const { context, page } = await open();
  await page.goto(`${APP}/audits/${D.liveAudit.id}`);
  await settle(page, 1500);
  await shot(page, 'audit-live');
  await measure(page, 'audit-live', ['Queued', 'Crawling', 'Analyzing', 'Ready', 'Pages', '146', '32%', 'Live crawl']);
  await context.close();
}

if (want('audit')) {
  const { context, page } = await open({ height: 3400 });
  await page.goto(`${APP}/audits/${D.latest.id}`);
  await settle(page, 2500);
  await shot(page, 'audit-tall');
  await measure(page, 'audit-tall', ['86', 'Issues changed since last audit', 'Fixed', 'New', 'Still open', '+7 score', 'Score by category', 'Core Web Vitals', 'Score trend']);
  await card(page, 'Core Web Vitals', 'card-web-vitals');
  await card(page, 'Score trend', 'card-trend');
  await card(page, 'Issues changed since last audit', 'card-changes');
  await context.close();
}

if (want('issues')) {
  const { context, page } = await open({ height: 1800 });
  await page.goto(`${APP}/audits/${D.latest.id}/issues`);
  await settle(page, 1500);
  await shot(page, 'issues-tall');
  await measure(page, 'issues-tall', ['Link to an importer page returns 404', '3 redirects before the final page', 'Returned 410 Gone', '4 images have no alt text']);
  await context.close();
}

if (want('settings')) {
  const { context, page } = await open({ height: 4200, timezone: 'Asia/Kolkata', locale: 'en-IN', state: { billing: D.billingFree } });
  await page.goto(`${APP}/settings`);
  await settle(page, 2000);
  await shot(page, 'settings-free-tall');
  await card(page, 'Pay in', 'card-plan-free');
  await context.close();
}

if (want('alerts')) {
  const { context, page } = await open({ height: 4200 });
  await page.goto(`${APP}/settings`);
  await settle(page, 2000);
  await shot(page, 'settings-pro-tall');
  await card(page, 'Alerts', 'card-alerts');
  await context.close();
}

if (want('pricing')) {
  const { context, page } = await open({ theme: 'dark', locale: 'en-IN', timezone: 'Asia/Kolkata' });
  await page.goto(`${APP}/pricing`);
  await settle(page, 1200);
  await shot(page, 'pricing-inr');
  await measure(page, 'pricing-inr', ['Pro', 'Free', 'Agency']);
  await context.close();
  const usd = await open({ theme: 'dark' });
  await usd.page.goto(`${APP}/pricing`);
  await settle(usd.page, 1200);
  await shot(usd.page, 'pricing-usd');
  await usd.context.close();
}

await browser.close();
let previous = { shots: [], boxes: {} };
try { previous = JSON.parse(readFileSync(`${OUT}shots.json`, 'utf8')); } catch { /* first run */ }
writeFileSync(`${OUT}shots.json`, `${JSON.stringify({ shots: [...new Set([...previous.shots, ...shots])], boxes: { ...previous.boxes, ...boxes } }, null, 2)}\n`);
if (unknown.size) console.log('UNMOCKED:\n  ' + [...unknown].join('\n  '));
console.log(`screens written to ${OUT}`);
