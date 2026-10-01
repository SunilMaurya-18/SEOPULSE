-- Phase 5: rule catalog, issue fingerprints for audit comparison,
-- per-audit aggregates for trends, and raw page signals for new analyzers.

CREATE TABLE seo_rules
(
    code           VARCHAR(100) PRIMARY KEY,
    category       VARCHAR(20)   NOT NULL,
    severity       VARCHAR(20)   NOT NULL,
    -- Points deducted from a page's score when the issue is present.
    weight         INTEGER       NOT NULL,
    title          VARCHAR(200)  NOT NULL,
    recommendation VARCHAR(1000) NOT NULL,
    help_url       VARCHAR(500),

    CONSTRAINT ck_seo_rules_category
        CHECK (category IN ('CONTENT', 'TECHNICAL', 'LINKS', 'SOCIAL', 'PERFORMANCE', 'SECURITY')),
    CONSTRAINT ck_seo_rules_severity CHECK (severity IN ('ERROR', 'WARNING', 'INFO')),
    CONSTRAINT ck_seo_rules_weight CHECK (weight BETWEEN 0 AND 100)
);

INSERT INTO seo_rules (code, category, severity, weight, title, recommendation, help_url)
VALUES
    -- Content
    ('TITLE_MISSING', 'CONTENT', 'ERROR', 10, 'Missing page title',
     'Add a unique and descriptive title to the page.',
     'https://developers.google.com/search/docs/appearance/title-link'),
    ('TITLE_TOO_SHORT', 'CONTENT', 'WARNING', 4, 'Page title is too short',
     'Make the page title more descriptive (30 to 60 characters works well).',
     'https://developers.google.com/search/docs/appearance/title-link'),
    ('TITLE_TOO_LONG', 'CONTENT', 'WARNING', 3, 'Page title is too long',
     'Shorten the page title so it is not truncated in search results.',
     'https://developers.google.com/search/docs/appearance/title-link'),
    ('DUPLICATE_TITLE', 'CONTENT', 'WARNING', 5, 'Duplicate page title',
     'Give every page its own title that describes that page specifically.',
     'https://developers.google.com/search/docs/appearance/title-link'),
    ('META_DESCRIPTION_MISSING', 'CONTENT', 'ERROR', 8, 'Missing meta description',
     'Add a unique meta description.',
     'https://developers.google.com/search/docs/appearance/snippet'),
    ('META_DESCRIPTION_TOO_SHORT', 'CONTENT', 'WARNING', 3, 'Meta description is too short',
     'Expand the meta description to provide useful page context.',
     'https://developers.google.com/search/docs/appearance/snippet'),
    ('META_DESCRIPTION_TOO_LONG', 'CONTENT', 'WARNING', 2, 'Meta description is too long',
     'Shorten the meta description.',
     'https://developers.google.com/search/docs/appearance/snippet'),
    ('DUPLICATE_META_DESCRIPTION', 'CONTENT', 'WARNING', 4, 'Duplicate meta description',
     'Write a meta description that summarizes this page specifically.',
     'https://developers.google.com/search/docs/appearance/snippet'),
    ('H1_MISSING', 'CONTENT', 'ERROR', 8, 'Missing H1 heading',
     'Add a clear primary H1 heading.',
     'https://developers.google.com/search/docs/fundamentals/seo-starter-guide'),
    ('MULTIPLE_H1', 'CONTENT', 'WARNING', 3, 'Multiple H1 headings',
     'Review the page structure and keep a clear primary H1.',
     'https://developers.google.com/search/docs/fundamentals/seo-starter-guide'),
    ('LOW_WORD_COUNT', 'CONTENT', 'WARNING', 4, 'Thin content',
     'Review whether the page provides sufficient useful content.',
     'https://developers.google.com/search/docs/fundamentals/creating-helpful-content'),
    ('IMAGE_ALT_MISSING', 'CONTENT', 'WARNING', 3, 'Images without alt text',
     'Add meaningful alt text to informative images.',
     'https://developers.google.com/search/docs/appearance/google-images'),

    -- Technical
    ('CANONICAL_MISSING', 'TECHNICAL', 'WARNING', 3, 'Missing canonical URL',
     'Add a canonical URL when appropriate.',
     'https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls'),
    ('CANONICAL_POINTS_ELSEWHERE', 'TECHNICAL', 'INFO', 1, 'Canonical points to another URL',
     'Confirm that this page is meant to defer to the canonical URL; otherwise point the canonical at itself.',
     'https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls'),
    ('NOINDEX', 'TECHNICAL', 'WARNING', 5, 'Page is excluded from indexing',
     'Remove noindex from the meta robots tag or X-Robots-Tag header if this page should appear in search.',
     'https://developers.google.com/search/docs/crawling-indexing/block-indexing'),
    ('HTTP_4XX', 'TECHNICAL', 'ERROR', 12, 'Page returns a client error',
     'Fix the broken or inaccessible page.',
     'https://developers.google.com/search/docs/crawling-indexing/http-network-errors'),
    ('HTTP_5XX', 'TECHNICAL', 'ERROR', 15, 'Page returns a server error',
     'Investigate the server error.',
     'https://developers.google.com/search/docs/crawling-indexing/http-network-errors'),
    ('REDIRECT_CHAIN', 'TECHNICAL', 'WARNING', 4, 'Redirect chain',
     'Point links and redirects straight at the final URL so visitors and crawlers take one hop.',
     'https://developers.google.com/search/docs/crawling-indexing/301-redirects'),
    ('REDIRECT_LOOP', 'TECHNICAL', 'ERROR', 10, 'Redirect loop',
     'Fix the redirect rules so the URL resolves to a page instead of looping.',
     'https://developers.google.com/search/docs/crawling-indexing/301-redirects'),
    ('VIEWPORT_MISSING', 'TECHNICAL', 'ERROR', 6, 'Missing mobile viewport',
     'Add <meta name="viewport" content="width=device-width, initial-scale=1"> to the page head.',
     'https://developer.mozilla.org/en-US/docs/Web/HTML/Viewport_meta_tag'),
    ('VIEWPORT_INVALID', 'TECHNICAL', 'WARNING', 3, 'Viewport does not adapt to the device',
     'Use width=device-width in the viewport meta tag and avoid disabling zoom.',
     'https://developer.mozilla.org/en-US/docs/Web/HTML/Viewport_meta_tag'),
    ('STRUCTURED_DATA_INVALID', 'TECHNICAL', 'WARNING', 4, 'Invalid structured data',
     'Fix the JSON-LD so it parses and declares a @type; validate it with the Rich Results Test.',
     'https://developers.google.com/search/docs/appearance/structured-data/intro-structured-data'),
    ('STRUCTURED_DATA_MISSING', 'TECHNICAL', 'INFO', 1, 'No structured data',
     'Consider adding JSON-LD structured data (for example Organization or Article) to qualify for rich results.',
     'https://developers.google.com/search/docs/appearance/structured-data/intro-structured-data'),
    ('HREFLANG_INVALID', 'TECHNICAL', 'WARNING', 3, 'Invalid hreflang value',
     'Use ISO 639-1 language codes, optionally followed by an ISO 3166-1 region (for example en-GB), or x-default.',
     'https://developers.google.com/search/docs/specialty/international/localized-versions'),
    ('HREFLANG_MISSING_RETURN', 'TECHNICAL', 'WARNING', 3, 'Hreflang without return link',
     'Make every alternate page link back to this page with its own hreflang annotation.',
     'https://developers.google.com/search/docs/specialty/international/localized-versions'),

    -- Links
    ('NO_INTERNAL_LINKS', 'LINKS', 'WARNING', 4, 'No internal links',
     'Add relevant internal links to help users and search engines discover related content.',
     'https://developers.google.com/search/docs/crawling-indexing/links-crawlable'),
    ('BROKEN_INTERNAL_LINK', 'LINKS', 'ERROR', 8, 'Links to broken internal pages',
     'Update or remove links that point to pages returning 4xx or 5xx errors.',
     'https://developers.google.com/search/docs/crawling-indexing/links-crawlable'),
    ('BROKEN_EXTERNAL_LINK', 'LINKS', 'WARNING', 3, 'Links to broken external pages',
     'Update or remove outbound links that no longer resolve.',
     'https://developers.google.com/search/docs/crawling-indexing/links-crawlable'),
    ('ORPHAN_PAGE', 'LINKS', 'WARNING', 4, 'Orphan page',
     'Link to this sitemap page from at least one other page so visitors and crawlers can find it.',
     'https://developers.google.com/search/docs/crawling-indexing/sitemaps/overview'),

    -- Social
    ('OPEN_GRAPH_INCOMPLETE', 'SOCIAL', 'WARNING', 2, 'Incomplete Open Graph tags',
     'Add og:title, og:description and og:image so shared links render a rich preview.',
     'https://ogp.me/'),
    ('TWITTER_CARD_MISSING', 'SOCIAL', 'INFO', 1, 'Missing Twitter card',
     'Add <meta name="twitter:card" content="summary_large_image"> for richer previews on X.',
     'https://developer.x.com/en/docs/x-for-websites/cards/overview/abouts-cards'),

    -- Performance
    ('LARGE_PAGE', 'PERFORMANCE', 'WARNING', 3, 'Large HTML document',
     'Reduce the HTML size: remove inline data, unused markup and very large inline scripts or styles.',
     'https://web.dev/articles/dom-size-and-interactivity'),

    -- Security
    ('MIXED_CONTENT', 'SECURITY', 'ERROR', 6, 'Mixed content',
     'Load every image, script, stylesheet and frame over HTTPS.',
     'https://developer.mozilla.org/en-US/docs/Web/Security/Mixed_content'),
    ('HTTPS_NOT_ENFORCED', 'SECURITY', 'ERROR', 8, 'HTTPS is not enforced',
     'Redirect every http:// request to the https:// version of the site.',
     'https://web.dev/articles/why-https-matters'),
    ('HSTS_MISSING', 'SECURITY', 'WARNING', 3, 'Missing HSTS header',
     'Send a Strict-Transport-Security header so browsers always use HTTPS.',
     'https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Strict-Transport-Security');


ALTER TABLE seo_issues
    ADD COLUMN fingerprint VARCHAR(64),
    ADD COLUMN category    VARCHAR(20);

-- Must stay identical to IssueFingerprint.of in the application.
UPDATE seo_issues s
SET fingerprint = encode(sha256(convert_to(
        s.rule_code || '|' ||
        regexp_replace(regexp_replace(p.url, '^https?://(www\.)?', '', 'i'), '/+$', ''),
        'UTF8')), 'hex')
FROM audit_pages p
WHERE p.id = s.audit_page_id;

UPDATE seo_issues s
SET category = r.category
FROM seo_rules r
WHERE r.code = s.rule_code;

UPDATE seo_issues
SET category = 'TECHNICAL'
WHERE category IS NULL;

CREATE INDEX idx_seo_issues_fingerprint ON seo_issues (fingerprint);


ALTER TABLE audits
    ADD COLUMN issue_count       INTEGER,
    ADD COLUMN error_count       INTEGER,
    ADD COLUMN warning_count     INTEGER,
    ADD COLUMN info_count        INTEGER,
    ADD COLUMN category_scores   JSONB,
    ADD COLUMN score_version     INTEGER,
    -- Set when retention cleanup removed the audit's pages and issues.
    ADD COLUMN details_purged_at TIMESTAMP WITH TIME ZONE;

UPDATE audits a
SET issue_count   = c.total,
    error_count   = c.errors,
    warning_count = c.warnings,
    info_count    = c.infos
FROM (SELECT p.audit_id,
             COUNT(*)                                          AS total,
             COUNT(*) FILTER (WHERE UPPER(s.severity) = 'ERROR')   AS errors,
             COUNT(*) FILTER (WHERE UPPER(s.severity) = 'WARNING') AS warnings,
             COUNT(*) FILTER (WHERE UPPER(s.severity) = 'INFO')    AS infos
      FROM seo_issues s
               JOIN audit_pages p ON p.id = s.audit_page_id
      GROUP BY p.audit_id) c
WHERE c.audit_id = a.id;

UPDATE audits
SET issue_count   = COALESCE(issue_count, 0),
    error_count   = COALESCE(error_count, 0),
    warning_count = COALESCE(warning_count, 0),
    info_count    = COALESCE(info_count, 0),
    score_version = 1
WHERE status = 'COMPLETED';

CREATE INDEX idx_audits_website_completed
    ON audits (website_id, completed_at)
    WHERE status = 'COMPLETED';


ALTER TABLE audit_pages
    ADD COLUMN signals JSONB;
