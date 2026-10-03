// Fictional demo workspace for the brag video. Nothing here comes from a real account.
const iso = (daysAgo, hour = 9) => new Date(Date.UTC(2026, 9, 3 - daysAgo, hour, 12)).toISOString();

export const user = {
  accessToken: 'demo-token',
  tokenType: 'Bearer',
  expiresIn: 900,
  userId: 1,
  name: 'Maya Chen',
  email: 'maya@northwind-coffee.com',
  role: 'USER',
  emailVerified: true,
  emailVerificationRequired: false,
};

export const org = { id: 1, name: 'Northwind Coffee', slug: 'northwind-coffee', role: 'OWNER' };
export const project = { id: 1, name: 'Northwind Coffee', description: null, createdAt: iso(60), updatedAt: iso(1) };

export const websites = [
  { id: 11, name: 'northwind-coffee.com', url: 'https://northwind-coffee.com', projectId: 1, createdAt: iso(58) },
  { id: 12, name: 'shop.northwind-coffee.com', url: 'https://shop.northwind-coffee.com', projectId: 1, createdAt: iso(40) },
  { id: 13, name: 'northwind-roasters.co.uk', url: 'https://northwind-roasters.co.uk', projectId: 1, createdAt: iso(21) },
];

const scoresMain = [52, 58, 61, 66, 68, 74, 79, 86];
const auditsMain = scoresMain.map((score, i) => {
  const id = 100 + i * 4;
  const days = (scoresMain.length - 1 - i) * 7;
  return {
    id, websiteId: 11, websiteUrl: 'https://northwind-coffee.com', status: 'COMPLETED', score,
    pagesCrawled: 380 + i * 4, pagesAnalyzed: 380 + i * 4, startedAt: iso(days, 6), completedAt: new Date(Date.parse(iso(days, 6)) + 47_000).toISOString(),
    errorMessage: null, createdAt: iso(days, 6), triggeredBy: i === scoresMain.length - 1 ? 'MANUAL' : 'SCHEDULED',
    issueCount: Math.round(140 - score * 1.2), errorCount: Math.max(3, Math.round((100 - score) / 3)), scoreVersion: 2,
  };
}).reverse();

export const latest = auditsMain[0];
export const previous = auditsMain[1];

export const liveAudit = {
  id: 140, websiteId: 12, websiteUrl: 'https://shop.northwind-coffee.com', status: 'CRAWLING', score: null,
  pagesCrawled: 146, pagesAnalyzed: 0, startedAt: iso(0, 9), completedAt: null, errorMessage: null, createdAt: iso(0, 9),
  triggeredBy: 'MANUAL', issueCount: null, errorCount: null, scoreVersion: null,
};

const auditsShop = [
  liveAudit,
  { ...liveAudit, id: 133, status: 'COMPLETED', score: 71, pagesCrawled: 1180, pagesAnalyzed: 1180, completedAt: iso(7), createdAt: iso(7), startedAt: iso(7), triggeredBy: 'SCHEDULED', issueCount: 64, errorCount: 9, scoreVersion: 2 },
];
const auditsUk = [
  { ...liveAudit, id: 131, websiteId: 13, websiteUrl: 'https://northwind-roasters.co.uk', status: 'COMPLETED', score: 64, pagesCrawled: 96, pagesAnalyzed: 96, completedAt: iso(2), createdAt: iso(2), startedAt: iso(2), triggeredBy: 'SCHEDULED', issueCount: 41, errorCount: 7, scoreVersion: 2 },
];

export const auditsBySite = { 11: auditsMain, 12: auditsShop, 13: auditsUk };
export const allAudits = [...auditsMain, ...auditsShop, ...auditsUk];

export const categoryScores = { CONTENT: 88, TECHNICAL: 91, LINKS: 79, SOCIAL: 72, PERFORMANCE: 83, SECURITY: 95 };

export function summaryFor(audit) {
  const done = audit.status === 'COMPLETED';
  return {
    auditId: audit.id, websiteId: audit.websiteId, status: audit.status, score: audit.score,
    pagesCrawled: audit.pagesCrawled, pagesAnalyzed: audit.pagesAnalyzed,
    totalIssues: done ? 37 : 0, errorCount: done ? 3 : 0, warningCount: done ? 19 : 0, infoCount: done ? 15 : 0,
    categoryScores: done ? (audit.id === latest.id ? categoryScores : { CONTENT: 70, TECHNICAL: 68, LINKS: 66, SOCIAL: 60, PERFORMANCE: 64, SECURITY: 90 }) : null,
    scoreVersion: 2, detailsPurged: false,
  };
}

export const trend = auditsMain.slice().reverse().map((a, i) => ({
  auditId: a.id, completedAt: a.completedAt, score: a.score, scoreVersion: 2, issueCount: a.issueCount,
  errorCount: a.errorCount, warningCount: 30 - i * 2, infoCount: 18, pagesCrawled: a.pagesCrawled,
  categoryScores: i === auditsMain.length - 1 ? categoryScores : null, triggeredBy: a.triggeredBy,
}));

const page = (path) => `https://northwind-coffee.com${path}`;

const change = (ruleCode, ruleTitle, severity, category, path, message) => ({
  fingerprint: `${ruleCode}:${path}`, ruleCode, ruleTitle, severity, category, url: page(path), message,
});

export const newIssues = [
  change('IMAGE_MISSING_ALT', 'Images without alt text', 'WARNING', 'CONTENT', '/blog/cold-brew-guide', '4 images have no alt text'),
  change('META_DESCRIPTION_TOO_LONG', 'Meta description too long', 'INFO', 'CONTENT', '/menu/seasonal', 'Meta description is 182 characters'),
];
export const fixedIssues = [
  change('BROKEN_INTERNAL_LINK', 'Broken internal link', 'ERROR', 'LINKS', '/locations', 'Link to /locations/portland returns 404'),
  change('MISSING_TITLE', 'Missing title tag', 'ERROR', 'CONTENT', '/wholesale', 'Page has no <title>'),
  change('DUPLICATE_TITLE', 'Duplicate title', 'WARNING', 'CONTENT', '/menu/espresso', 'Title is shared with 3 other pages'),
  change('MISSING_CANONICAL', 'Missing canonical', 'WARNING', 'TECHNICAL', '/shop/beans', 'No canonical URL'),
  change('SLOW_RESPONSE', 'Slow server response', 'WARNING', 'PERFORMANCE', '/', 'First byte took 1.9 s'),
  change('MISSING_H1', 'Missing H1', 'WARNING', 'CONTENT', '/about', 'Page has no H1 heading'),
];

export const comparison = {
  auditId: latest.id, baselineAuditId: previous.id, baselineCompletedAt: previous.completedAt,
  score: latest.score, baselineScore: previous.score, scoreDelta: latest.score - previous.score,
  pagesCrawled: latest.pagesCrawled, baselinePagesCrawled: previous.pagesCrawled, pagesDelta: latest.pagesCrawled - previous.pagesCrawled,
  detailsAvailable: true, newCount: 2, fixedCount: 14, persistingCount: 35,
  newBySeverity: { WARNING: 1, INFO: 1 }, fixedBySeverity: { ERROR: 4, WARNING: 8, INFO: 2 },
  newIssues, fixedIssues, newFingerprints: newIssues.map((i) => i.fingerprint),
};

const issue = (id, ruleCode, ruleTitle, severity, category, path, message, recommendation) => ({
  id, auditId: latest.id, auditPageId: id, url: page(path), ruleCode, severity, message, recommendation,
  createdAt: latest.completedAt, category, fingerprint: `${ruleCode}:${path}`, ruleTitle, helpUrl: null,
});

export const issues = [
  issue(1, 'BROKEN_EXTERNAL_LINK', 'Broken external link', 'ERROR', 'LINKS', '/blog/origin-trip-ethiopia', 'Link to an importer page returns 404', 'Update or remove the link so visitors and crawlers do not hit a dead end.'),
  issue(2, 'REDIRECT_CHAIN', 'Redirect chain', 'ERROR', 'TECHNICAL', '/shop/gift-cards', '3 redirects before the final page', 'Point links straight at the final URL to save a round trip per hop.'),
  issue(3, 'HTTP_4XX', 'Page returns 4xx', 'ERROR', 'TECHNICAL', '/events/latte-art-2025', 'Returned 410 Gone', 'Remove internal links to this page or redirect it to a live one.'),
  issue(4, 'IMAGE_MISSING_ALT', 'Images without alt text', 'WARNING', 'CONTENT', '/blog/cold-brew-guide', '4 images have no alt text', 'Describe each image in a short alt attribute.'),
  issue(5, 'THIN_CONTENT', 'Thin content', 'WARNING', 'CONTENT', '/menu/tea', 'Only 86 words on the page', 'Add useful copy: origin, tasting notes, brewing tips.'),
  issue(6, 'MISSING_OG_IMAGE', 'Missing Open Graph image', 'WARNING', 'SOCIAL', '/menu/seasonal', 'No og:image tag', 'Add an og:image so shared links show a preview.'),
  issue(7, 'TITLE_TOO_LONG', 'Title too long', 'WARNING', 'CONTENT', '/blog/how-we-roast', 'Title is 74 characters', 'Keep titles under 60 characters so they are not cut off in results.'),
  issue(8, 'MISSING_STRUCTURED_DATA', 'No structured data', 'INFO', 'SOCIAL', '/locations', 'No LocalBusiness schema found', 'Add LocalBusiness structured data with hours and address.'),
  issue(9, 'META_DESCRIPTION_TOO_LONG', 'Meta description too long', 'INFO', 'CONTENT', '/menu/seasonal', 'Meta description is 182 characters', 'Trim it to about 155 characters.'),
  issue(10, 'LARGE_IMAGE', 'Large image', 'INFO', 'PERFORMANCE', '/', 'Hero image is 1.8 MB', 'Serve a compressed WebP or AVIF version.'),
];

export const pages = [
  ['/', 'Northwind Coffee · Small-batch roasters', 1240, 0], ['/menu/espresso', 'Espresso drinks · Northwind Coffee', 610, 1],
  ['/menu/seasonal', 'Seasonal menu · Northwind Coffee', 540, 1], ['/shop/beans', 'Whole bean coffee · Northwind Shop', 880, 1],
  ['/blog/cold-brew-guide', 'The cold brew guide', 2140, 2], ['/blog/how-we-roast', 'How we roast: from green bean to your cup', 1890, 2],
  ['/locations', 'Find a café · Northwind Coffee', 420, 1], ['/about', 'About us · Northwind Coffee', 760, 1],
  ['/wholesale', 'Wholesale coffee for cafés', 690, 1], ['/menu/tea', 'Tea · Northwind Coffee', 86, 2],
].map(([path, title, wordCount, depth], i) => ({
  id: i + 1, auditId: latest.id, url: page(path), status: 'CRAWLED', statusCode: 200, contentType: 'text/html',
  title, metaDescription: null, canonicalUrl: page(path), wordCount, h1Count: 1, imageCount: 6, imagesWithoutAlt: i === 4 ? 4 : 0,
  internalLinkCount: 48, externalLinkCount: 3, depth, finalUrl: page(path), redirectChain: null, skipReason: null,
  crawledAt: latest.completedAt, createdAt: latest.completedAt,
}));

export const webVitals = {
  state: 'READY', strategy: 'mobile', url: 'https://northwind-coffee.com/', performanceScore: 91,
  lab: { lcpMs: 1840, cls: 0.04, tbtMs: 120, fcpMs: 1080, speedIndexMs: 2310 },
  field: { lcpMs: 2120, cls: 0.05, inpMs: 160, category: 'FAST' },
  errorMessage: null, measuredAt: latest.completedAt,
};

export const reports = [
  { id: 9, auditId: latest.id, status: 'READY', sizeBytes: 418_000, whiteLabel: false, watermark: false, errorMessage: null, createdAt: latest.completedAt, completedAt: latest.completedAt },
];
export const shares = [
  { id: 4, auditId: latest.id, url: null, expiresAt: iso(-30), revokedAt: null, active: true, viewCount: 12, lastViewedAt: iso(0, 8), createdAt: iso(1) },
];

export const billing = {
  planCode: 'PRO', planName: 'Pro', status: 'ACTIVE', currentPeriodEnd: iso(-24), trialEnd: null, cancelAtPeriodEnd: false,
  limits: { websites: 10, pagesPerAudit: 2000, auditsPerMonth: 100, members: 3, schedule: 'WEEKLY', webhookAlerts: true, whiteLabel: false, retentionDays: 180 },
  auditsUsed: 23, websitesUsed: 3, provider: 'RAZORPAY', stripeAvailable: true, razorpayAvailable: true,
};
export const billingFree = {
  ...billing, planCode: 'FREE', planName: 'Free', status: 'ACTIVE', currentPeriodEnd: null, provider: null,
  limits: { websites: 3, pagesPerAudit: 100, auditsPerMonth: 5, members: 1, schedule: 'NONE', webhookAlerts: false, whiteLabel: false, retentionDays: 30 },
  auditsUsed: 2, websitesUsed: 1,
};

export const alerts = [
  { id: 1, websiteId: null, type: 'SCORE_DROP', threshold: 5, channel: 'EMAIL', target: null, enabled: true, signingSecret: null, secretRevealed: false, createdAt: iso(50) },
  { id: 2, websiteId: null, type: 'NEW_ERRORS', threshold: null, channel: 'SLACK_WEBHOOK', target: 'https://hooks.slack.com/…/9f2c', enabled: true, signingSecret: null, secretRevealed: false, createdAt: iso(40) },
  { id: 3, websiteId: 11, type: 'PAGE_UNREACHABLE', threshold: null, channel: 'EMAIL', target: null, enabled: true, signingSecret: null, secretRevealed: false, createdAt: iso(40) },
  { id: 4, websiteId: null, type: 'AUDIT_FAILED', threshold: null, channel: 'WEBHOOK', target: 'https://ops.northwind-coffee.com/…/hook', enabled: true, signingSecret: 'whsec_…3a1f', secretRevealed: false, createdAt: iso(30) },
];
export const deliveries = [
  { id: 31, ruleId: 2, auditId: latest.id, eventType: 'NEW_ERRORS', channel: 'SLACK_WEBHOOK', subject: 'northwind-coffee.com: 2 new issues', delivered: true, attempts: 1, lastError: null, createdAt: iso(0, 6), deliveredAt: iso(0, 6) },
  { id: 30, ruleId: 1, auditId: previous.id, eventType: 'SCORE_DROP', channel: 'EMAIL', subject: 'shop.northwind-coffee.com dropped 6 points', delivered: true, attempts: 1, lastError: null, createdAt: iso(7, 6), deliveredAt: iso(7, 6) },
];

export const members = [
  { userId: 1, name: 'Maya Chen', email: 'maya@northwind-coffee.com', role: 'OWNER' },
  { userId: 2, name: 'Leo Park', email: 'leo@northwind-coffee.com', role: 'ADMIN' },
  { userId: 3, name: 'Ana Ruiz', email: 'ana@northwind-coffee.com', role: 'MEMBER' },
];

export const onboarding = {
  dismissed: false,
  steps: [
    { id: 'VERIFY_EMAIL', done: true }, { id: 'ADD_WEBSITE', done: true }, { id: 'RUN_AUDIT', done: true },
    { id: 'SHARE_REPORT', done: false }, { id: 'INVITE_TEAM', done: false },
  ],
};

export const quickCheck = {
  url: 'https://northwind-coffee.com', score: 74,
  categoryScores: { CONTENT: 78, TECHNICAL: 69, LINKS: 72, SOCIAL: 58, PERFORMANCE: 81, SECURITY: 96 },
  pagesChecked: 5, partial: false, errorCount: 3, warningCount: 9, infoCount: 4, issueTypes: 11,
  issues: [
    { ruleCode: 'BROKEN_INTERNAL_LINK', title: 'Broken internal link', severity: 'ERROR', category: 'LINKS', recommendation: 'Fix or remove links that point at pages returning 404.', helpUrl: null, pages: 2 },
    { ruleCode: 'MISSING_TITLE', title: 'Missing title tag', severity: 'ERROR', category: 'CONTENT', recommendation: 'Give every page a unique, descriptive <title>.', helpUrl: null, pages: 1 },
    { ruleCode: 'DUPLICATE_TITLE', title: 'Duplicate title', severity: 'WARNING', category: 'CONTENT', recommendation: 'Make each title unique so search engines can tell pages apart.', helpUrl: null, pages: 3 },
  ],
};
