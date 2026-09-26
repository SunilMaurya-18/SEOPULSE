import { Copy } from 'lucide-react'
import type { ReactNode } from 'react'

import { TERMS_VERSION } from './data'
import { FormalNoticeForm } from './FormalNoticeForm'

type DocumentViewerProps = {
  onCopyCitation: (clauseName: string) => void
}

function ClauseHeader({
  clause,
  title,
  onCopy,
}: {
  clause: string
  title: string
  onCopy: () => void
}) {
  return (
    <div className="flex items-start justify-between gap-2 border-b border-[#27272a] pb-2">
      <div>
        <span className="font-mono text-[11px] font-bold tracking-wider text-[#dc2626] uppercase">
          {clause}
        </span>
        <h2 className="text-2xl font-bold tracking-tight text-white md:text-4xl md:leading-[44px]">
          {title}
        </h2>
      </div>
      <button
        type="button"
        title="Copy Citation"
        onClick={onCopy}
        className="rounded border border-[#27272a] bg-[#18181b] p-1.5 text-zinc-400 transition-colors hover:bg-[#27272a] hover:text-white"
      >
        <Copy className="h-[18px] w-[18px]" />
      </button>
    </div>
  )
}

function ClauseBlock({
  id,
  children,
}: {
  id: string
  children: ReactNode
}) {
  return (
    <section
      id={id}
      className="clause-block space-y-4 rounded-lg border border-[#27272a] bg-[#131315] p-4 shadow-sm scroll-mt-24"
    >
      {children}
    </section>
  )
}

function LegalP({ children }: { children: ReactNode }) {
  return (
    <p className="text-[13px] leading-[22px] text-white">{children}</p>
  )
}

export function DocumentViewer({ onCopyCitation }: DocumentViewerProps) {
  const copy = (name: string) => {
    const text = `SEOPulse Terms of Service (v${TERMS_VERSION}) ${name} - https://seopulse.io/terms`
    void navigator.clipboard.writeText(text).then(
      () => onCopyCitation(name),
      () => onCopyCitation(name),
    )
  }

  return (
    <article className="flex flex-col gap-10 lg:col-span-8">
      <section className="space-y-1 rounded-lg border border-[#27272a] bg-[#131315] p-4 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="font-mono text-[11px] font-semibold text-zinc-400 uppercase">
            Contractual Baseline Notice
          </span>
          <span className="font-mono text-[11px] font-bold text-[#dc2626]">
            MANDATORY EXECUTION
          </span>
        </div>
        <p className="text-[13px] leading-[22px] text-white">
          PLEASE CAREFULLY REVIEW THESE TERMS OF SERVICE BEFORE INITIALIZING
          SEOPULSE CRAWLER AGENTS, DEPLOYING EDGE CDN WORKERS, OR PASSING BEARER
          API TOKENS TO OUR REST ENDPOINTS. THIS INSTRUMENT ESTABLISHES
          CONTRACTUALLY BINDING OBLIGATIONS BETWEEN SEOPULSE TELEMETRY SYSTEMS,
          INC. (“SEOPULSE”, “WE”, OR “US”) AND THE CORPORATE ENTITY, DEVELOPER,
          OR PERSON ACCESSING THE SERVICE (“CUSTOMER”, “YOU”, OR “LICENSEE”).
        </p>
      </section>

      <ClauseBlock id="section-1">
        <ClauseHeader
          clause="Clause 1.0"
          title="Acceptance of Terms & Eligibility"
          onCopy={() =>
            copy('§ 1.0 Acceptance of Terms & Eligibility')
          }
        />
        <div className="space-y-2">
          <LegalP>
            <strong>1.1 Mutual Assent:</strong> By authenticating an
            administrative user profile, provisioning programmatic access
            credentials via CLI, configuring automated GitHub Action webhooks,
            or clicking a graphical agreement trigger, you unambiguously confirm
            your unreserved assent to these Terms, our Data Processing Addendum
            (DPA), and our Privacy Policy.
          </LegalP>
          <LegalP>
            <strong>1.2 Entity Authority:</strong> If you accept these Terms on
            behalf of an enterprise entity, subsidiary, agency, or corporation,
            you warrant and affirm under penalty of contract default that you
            possess requisite corporate authorization to bind said entity to all
            financial, technical, and operational obligations codified herein.
          </LegalP>
          <LegalP>
            <strong>1.3 Jurisdictional Capacity:</strong> You represent that you
            are at least 18 years of age (or the minimum legal age of contractual
            majority in your jurisdiction) and that you are not prohibited under
            United States export regulations (including OFAC sanctions lists)
            from consuming enterprise telemetric software.
          </LegalP>
        </div>
        <div className="flex items-center justify-between rounded border border-[#27272a] bg-[#0e0e10] p-2 font-mono text-xs">
          <span className="font-semibold text-zinc-500">
            CLAUSE REGISTRATION: VALIDATED
          </span>
          <span className="font-medium text-white">B2B COMMERCIAL SCOPE</span>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-2">
        <ClauseHeader
          clause="Clause 2.0"
          title="Autonomous Crawler & Diagnostic Services"
          onCopy={() =>
            copy('§ 2.0 Autonomous Crawler & Diagnostic Services')
          }
        />
        <div className="space-y-2">
          <LegalP>
            <strong>2.1 Functional Modality:</strong> SEOPulse operates a
            cloud-distributed, headless Chromium and HTTP/3 telemetric ingestion
            cluster designed to simulate crawler bots, measure Time to First
            Byte (TTFB), evaluate Cumulative Layout Shift (CLS), and extract
            structural search metadata (e.g., JSON-LD schema, canonical headers,
            OpenGraph tags, redirect chains).
          </LegalP>
          <LegalP>
            <strong>2.2 Rate Velocity Baseline:</strong> In default automated
            mode, the SEOPulse crawler initiates no more than twelve (12)
            concurrent requests per second per target fully-qualified domain
            name (FQDN). You may modulate this frequency via your workspace
            dashboard or robots.txt{' '}
            <code className="font-mono text-xs">crawl-delay</code> parameter.
          </LegalP>
          <LegalP>
            <strong>2.3 Execution Environment:</strong> You explicitly
            acknowledge that diagnostic audits run simulated DOM rendering
            within isolated Linux container sandboxes. While our agents strictly
            isolate state and sanitize executable scripts, SEOPulse is not
            responsible for side effects triggered by client-side JavaScript
            executing inside your application stack during synthetic crawls.
          </LegalP>
        </div>
        <div className="overflow-hidden rounded-lg border border-[#27272a] bg-[#101012] shadow-sm">
          <div className="flex items-center justify-between border-b border-[#27272a] bg-[#18181b] px-4 py-2 font-mono text-[11px] font-semibold text-white">
            <span>CRAWLER DEFAULT OPERATING BOUNDS</span>
            <span className="text-zinc-400">VERSION 3.2 RUNTIME</span>
          </div>
          <div className="divide-y divide-[#27272a]">
            {[
              ['Standard User-Agent', 'SEOPulseBot/3.2 (+https://seopulse.io/bot)'],
              ['Default Concurrency Cap', '12 HTTP/3 req/sec (Configurable to 64)'],
              ['Robots.txt Directive Parsing', 'Strict Compliance (RFC 9309)', true],
              ['Rendering Engine Support', 'V8 Chromium 128+ with WebGPU Emulation'],
            ].map(([label, value, emerald]) => (
              <div
                key={String(label)}
                className="flex justify-between p-2 font-mono text-xs"
              >
                <span className="text-zinc-400">{label}</span>
                <span
                  className={
                    emerald
                      ? 'font-semibold text-emerald-400'
                      : 'font-mono text-white'
                  }
                >
                  {value}
                </span>
              </div>
            ))}
          </div>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-3">
        <ClauseHeader
          clause="Clause 3.0"
          title="Acceptable Use Policy & Restrictions"
          onCopy={() =>
            copy('§ 3.0 Acceptable Use Policy & Restrictions')
          }
        />
        <div className="space-y-1 rounded-lg border border-[#27272a] border-l-4 border-l-[#b91c1c] bg-[#1c1b1d] p-4 shadow-sm">
          <div className="flex items-center gap-2 text-lg font-semibold text-[#dc2626]">
            <span className="text-white">
              Prohibited Target Verification Protocols
            </span>
          </div>
          <p className="text-[13px] leading-[22px] text-white">
            Customers may only initiate synthetic crawls against network hosts,
            web properties, or host servers for which they maintain legitimate
            legal ownership, explicit operational delegacy, or signed
            administrative authorization.
          </p>
        </div>
        <div className="space-y-2">
          <LegalP>
            <strong>3.1 Express Non-Permitted Uses:</strong> You covenant that
            you will not, nor will you permit any third party to:
          </LegalP>
          <ul className="list-disc space-y-2 pl-4 text-[13px] leading-[22px] text-white">
            <li>
              Deploy SEOPulse nodes to initiate Distributed Denial of Service
              (DDoS), volumetric resource exhaustion, or scraping assaults
              against competitors;
            </li>
            <li>
              Circumvent or tamper with explicit{' '}
              <code className="font-mono text-xs">Disallow</code> directives
              specified in a target property’s{' '}
              <code className="font-mono text-xs">robots.txt</code> file without
              providing cryptographic DNS TXT ownership verification;
            </li>
            <li>
              Reverse engineer, decompile, extract, or reconstruct the telemetry
              engine heuristics, audit scoring weights, or proprietary Chromium
              profiling harnesses;
            </li>
            <li>
              Resell, white-label, or sub-license SEOPulse telemetry API streams
              to competing platforms without prior written enterprise addendum.
            </li>
          </ul>
          <LegalP>
            <strong>3.2 Immediate Quarantine:</strong> Violation of this Section
            3 triggers immediate automated revocation of API tokens, quarantine
            of crawl workers, and potential notification to CFAA regulatory
            liaisons where malicious intent is demonstrated.
          </LegalP>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-4">
        <ClauseHeader
          clause="Clause 4.0"
          title="Account Security & API Keys"
          onCopy={() => copy('§ 4.0 Account Security & API Keys')}
        />
        <div className="space-y-2">
          <LegalP>
            <strong>4.1 Credential Custody:</strong> Customers are solely
            responsible for maintaining strict confidentiality of their root
            authentication tokens, team OAuth seats, and programmatic
            REST/GraphQL API bearer keys. All operations authenticated via your
            keys are contractually attributed to your organization.
          </LegalP>
          <LegalP>
            <strong>4.2 Token Rotation Protocol:</strong> We strongly mandate
            bi-annual key rotation. In the event of leaked credentials in public
            version control, our automated perimeter scanner will automatically
            invalidate the compromised token and emit an emergency notification
            to the primary administrative email on record.
          </LegalP>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-5">
        <ClauseHeader
          clause="Clause 5.0"
          title="Subscription Tiers, Billing & Overages"
          onCopy={() =>
            copy('§ 5.0 Subscription Tiers, Billing & Overages')
          }
        />
        <div className="grid grid-cols-1 gap-2 md:grid-cols-3">
          <div className="flex flex-col justify-between rounded-lg border border-[#27272a] bg-[#101012] p-4 shadow-sm">
            <div className="space-y-1">
              <span className="font-mono text-[11px] font-semibold text-zinc-400">
                STARTER
              </span>
              <div className="text-2xl font-bold text-white">
                $49
                <span className="text-xs font-normal text-zinc-400">/mo</span>
              </div>
              <p className="pt-2 text-xs text-white">
                Up to 100,000 synthetic crawl pages/mo. 1 concurrent engine.
                Daily telemetry sync.
              </p>
            </div>
            <div className="mt-4 border-t border-[#27272a] pt-2 font-mono text-[10px] text-zinc-400">
              Overage: $0.60 per 1k URLs
            </div>
          </div>
          <div className="relative flex flex-col justify-between rounded-lg border-2 border-[#b91c1c] bg-[#1c1b1d] p-4 shadow-sm">
            <div className="absolute top-2 right-2 rounded bg-[#b91c1c] px-2 py-0.5 font-mono text-[10px] font-bold text-white">
              POPULAR
            </div>
            <div className="space-y-1">
              <span className="font-mono text-[11px] font-semibold text-[#dc2626]">
                PRO TEAM
              </span>
              <div className="text-2xl font-bold text-white">
                $149
                <span className="text-xs font-normal text-zinc-400">/mo</span>
              </div>
              <p className="pt-2 text-xs text-white">
                Up to 1,000,000 synthetic crawl pages/mo. 5 concurrent engines.
                Hourly Web Vitals profiling.
              </p>
            </div>
            <div className="mt-4 border-t border-[#27272a] pt-2 font-mono text-[10px] text-zinc-400">
              Overage: $0.35 per 1k URLs
            </div>
          </div>
          <div className="flex flex-col justify-between rounded-lg border border-[#27272a] bg-[#101012] p-4 shadow-sm">
            <div className="space-y-1">
              <span className="font-mono text-[11px] font-semibold text-zinc-400">
                ENTERPRISE
              </span>
              <div className="text-2xl font-bold text-white">
                $499
                <span className="text-xs font-normal text-zinc-400">/mo</span>
              </div>
              <p className="pt-2 text-xs text-white">
                Unlimited domains, custom dedicated crawler nodes, 99.99%
                contractual uptime SLA.
              </p>
            </div>
            <div className="mt-4 border-t border-[#27272a] pt-2 font-mono text-[10px] text-zinc-400">
              Custom volume tiered pools
            </div>
          </div>
        </div>
        <div className="space-y-2">
          <LegalP>
            <strong>5.1 Automatic Renewal:</strong> All plans renew
            automatically at the expiration of the chosen billing cycle using
            the designated default payment instrument on file. Invoices are
            dispatched in electronic PDF form with full itemized breakdown of
            base fees, overages, and applicable taxes.
          </LegalP>
          <LegalP>
            <strong>5.2 14-Day Discretionary Refund Window:</strong> Initial
            recurring subscriptions are eligible for an unconditional 100%
            refund within fourteen (14) calendar days of transaction settlement,
            provided your crawl consumption has not exceeded 15% of your plan’s
            monthly URL quota allocation.
          </LegalP>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-6">
        <ClauseHeader
          clause="Clause 6.0"
          title="Intellectual Property & Telemetry Data Rights"
          onCopy={() =>
            copy('§ 6.0 Intellectual Property & Telemetry Data Rights')
          }
        />
        <div className="space-y-2">
          <LegalP>
            <strong>6.1 Customer Data Sovereignty:</strong> You retain complete
            and unencumbered ownership of all domain telemetry, crawl logs,
            search performance metrics, and application code profiles submitted
            to or generated within your SEOPulse workspace (“Customer Data”). We
            do not claim any proprietary title to your domain assets or content.
          </LegalP>
          <LegalP>
            <strong>6.2 SEOPulse Intellectual Property:</strong> SEOPulse and
            its licensors retain all right, title, and interest in and to the
            platform, including crawler automation scripts, algorithm
            heuristics, edge worker code, visual dashboards, trademarks,
            documentation, and all underlying software.
          </LegalP>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-7">
        <ClauseHeader
          clause="Clause 7.0"
          title="Service Level Agreement (SLA) & Uptime"
          onCopy={() =>
            copy('§ 7.0 Service Level Agreement (SLA) & Uptime')
          }
        />
        <div className="space-y-2">
          <LegalP>
            <strong>7.1 Uptime Commitment:</strong> For customers enrolled in
            active Pro Team and Enterprise subscriptions, SEOPulse guarantees a
            monthly uptime service percentage of not less than{' '}
            <strong className="text-white">99.99%</strong> (“SLA Commitment”)
            for its API endpoints and automated continuous audit pipelines.
          </LegalP>
        </div>
        <div className="space-y-2 rounded-lg border border-[#27272a] bg-[#101012] p-4 shadow-sm">
          <div className="flex items-center justify-between border-b border-[#27272a] pb-2">
            <span className="font-mono text-[11px] font-semibold text-white">
              SCHEDULED SLA PENALTY REBATE SCHEDULE
            </span>
            <span className="font-mono text-[11px] font-semibold text-emerald-400">
              CREDIT MULTIPLIER: 4X
            </span>
          </div>
          <div className="grid grid-cols-1 gap-2 text-center sm:grid-cols-3">
            <div className="rounded border border-[#27272a] bg-[#18181b] p-2">
              <div className="font-mono text-[11px] text-zinc-400">
                99.90% - 99.98%
              </div>
              <div className="text-lg font-bold text-white">15% CREDIT</div>
              <div className="text-xs text-zinc-400">Applied to next invoice</div>
            </div>
            <div className="rounded border border-[#b91c1c]/40 bg-[#18181b] p-2">
              <div className="font-mono text-[11px] font-semibold text-[#dc2626]">
                99.00% - 99.89%
              </div>
              <div className="text-lg font-bold text-white">30% CREDIT</div>
              <div className="text-xs text-zinc-400">Applied to next invoice</div>
            </div>
            <div className="rounded border border-[#b91c1c] bg-[#18181b] p-2">
              <div className="font-mono text-[11px] font-bold text-[#dc2626]">
                &lt; 99.00% CATASTROPHIC
              </div>
              <div className="text-lg font-bold text-[#dc2626]">100% CREDIT</div>
              <div className="text-xs text-zinc-400">Full monthly refund</div>
            </div>
          </div>
          <p className="text-xs text-zinc-400 italic">
            *Excludes pre-notified scheduled maintenance windows (conducted
            Sundays between 02:00 and 04:00 UTC) and force majeure events.
          </p>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-8">
        <ClauseHeader
          clause="Clause 8.0"
          title="Disclaimers & Limitation of Liability"
          onCopy={() =>
            copy('§ 8.0 Disclaimers & Limitation of Liability')
          }
        />
        <div className="space-y-2">
          <p className="text-[13px] leading-[22px] font-semibold tracking-wide text-white uppercase">
            8.1 “AS IS” OPERATIONAL WARRANTY: EXCEPT AS EXPRESSLY CODIFIED IN
            SECTION 7 (SLA), THE SERVICES, DOCUMENTATION, CRAWLER SIMULATIONS,
            AUDIT HEURISTICS, AND RECOMMENDATIONS ARE PROVIDED “AS IS” AND “AS
            AVAILABLE” WITHOUT WARRANTY OF ANY KIND.
          </p>
          <LegalP>
            <strong>8.2 Search Ranking Disclaimer:</strong> SEOPulse provides
            diagnostic technical analysis and performance modeling. We do not
            guarantee search engine ranking elevations, algorithm stability from
            third parties, or indexing velocities.
          </LegalP>
          <LegalP>
            <strong>8.3 Liability Cap:</strong> TO THE MAXIMUM EXTENT PERMITTED
            UNDER APPLICABLE STATUTE, IN NO EVENT SHALL SEOPULSE’S CUMULATIVE
            AGGREGATE LIABILITY ARISING FROM OR RELATING TO THIS AGREEMENT
            EXCEED THE TOTAL SUMS ACTUALLY PAID BY CUSTOMER IN THE TWELVE (12)
            MONTH PERIOD PRECEDING THE INCIDENT GIVING RISE TO LIABILITY.
          </LegalP>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-9">
        <ClauseHeader
          clause="Clause 9.0"
          title="Term, Suspension & Data Deletion"
          onCopy={() =>
            copy('§ 9.0 Term, Suspension & Data Deletion')
          }
        />
        <div className="space-y-2">
          <LegalP>
            <strong>9.1 Termination for Convenience:</strong> Customers may
            terminate their active account subscription at any time directly
            through the organization settings console. Cancellation takes effect
            at the culmination of the ongoing pre-paid billing cycle.
          </LegalP>
          <LegalP>
            <strong>9.2 Thirty-Day Post-Termination Grace Window:</strong> Upon
            account deactivation, SEOPulse guarantees retention of historic
            crawl databases and Core Web Vitals telemetry for precisely thirty
            (30) calendar days. During this window, you may execute raw CSV/JSON
            dumps without fee. Following day 30, all tenant data is
            cryptographically expunged.
          </LegalP>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-10">
        <ClauseHeader
          clause="Clause 10.0"
          title="Governing Law & Dispute Resolution"
          onCopy={() =>
            copy('§ 10.0 Governing Law & Dispute Resolution')
          }
        />
        <div className="space-y-2">
          <LegalP>
            <strong>10.1 Delaware Choice of Law:</strong> This Agreement shall
            be governed exclusively by the substantive laws of the State of
            Delaware, United States, without regard to conflicts of law
            jurisprudence.
          </LegalP>
          <LegalP>
            <strong>10.2 Binding Commercial Arbitration:</strong> Any
            controversy or claim arising out of this contract shall be settled
            by binding arbitration administered by the American Arbitration
            Association (AAA) under its Commercial Arbitration Rules.
            Proceedings shall be conducted virtually or within New Castle
            County, Delaware, in the English language.
          </LegalP>
        </div>
      </ClauseBlock>

      <ClauseBlock id="section-11">
        <ClauseHeader
          clause="Clause 11.0"
          title="Amendments & Contact Inquiries"
          onCopy={() => copy('§ 11.0 Amendments & Contact Inquiries')}
        />
        <div className="space-y-2">
          <LegalP>
            <strong>11.1 Revisions:</strong> We reserve the right to revise these
            Terms to reflect legislative changes, new crawler capabilities, or
            modified billing architectures. Material updates will be
            communicated at least thirty (30) days in advance via platform alert
            banner and direct administrative dispatch.
          </LegalP>
        </div>
        <FormalNoticeForm />
      </ClauseBlock>
    </article>
  )
}
