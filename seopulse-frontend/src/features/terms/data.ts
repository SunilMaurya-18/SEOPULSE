export type TocItem = {
  id: string
  section: string
  title: string
  badge: string
  badgeTone?: 'default' | 'strict' | 'sla' | 'binding'
}

export const TERMS_VERSION = '3.2.0'
export const TERMS_DOC_ID = 'TOS-2024-v3.2.0-DELAWARE'
export const TERMS_EFFECTIVE = 'October 24, 2024'
export const TERMS_HASH = '9f8e4c...b12a'

export const TOC_ITEMS: TocItem[] = [
  {
    id: 'section-1',
    section: '§ 1.0',
    title: 'Acceptance & Eligibility',
    badge: 'BINDING',
    badgeTone: 'binding',
  },
  {
    id: 'section-2',
    section: '§ 2.0',
    title: 'Crawler Architecture & Scope',
    badge: 'ENGINE',
  },
  {
    id: 'section-3',
    section: '§ 3.0',
    title: 'Acceptable Use & Restrictions',
    badge: 'STRICT',
    badgeTone: 'strict',
  },
  {
    id: 'section-4',
    section: '§ 4.0',
    title: 'Account Security & API Keys',
    badge: 'AUTH',
  },
  {
    id: 'section-5',
    section: '§ 5.0',
    title: 'Subscription, Billing & Overages',
    badge: 'TIERS',
  },
  {
    id: 'section-6',
    section: '§ 6.0',
    title: 'Intellectual Property & Licenses',
    badge: 'RIGHTS',
  },
  {
    id: 'section-7',
    section: '§ 7.0',
    title: 'Service Level Agreement (SLA)',
    badge: '99.99%',
    badgeTone: 'sla',
  },
  {
    id: 'section-8',
    section: '§ 8.0',
    title: 'Disclaimers & Liability Cap',
    badge: 'LEGAL',
  },
  {
    id: 'section-9',
    section: '§ 9.0',
    title: 'Term, Suspension & Retention',
    badge: 'DATA',
  },
  {
    id: 'section-10',
    section: '§ 10.0',
    title: 'Governing Law & Arbitration',
    badge: 'DELAWARE',
  },
  {
    id: 'section-11',
    section: '§ 11.0',
    title: 'Contact & Legal Notices',
    badge: 'INQUIRY',
  },
]

export const ASSURANCE_CARDS = [
  {
    title: 'Zero Data Commercialization',
    body: 'Customer technical crawl logs, domain topology, and ranking metrics are strictly sequestered and never resold or trained into public LLMs.',
    icon: 'shield-check' as const,
  },
  {
    title: 'Compliant Crawler Engine',
    body: 'Automated Chromium telemetry processes follow strict rate controls, honouring custom robots.txt directives and verification tokens.',
    icon: 'globe' as const,
  },
  {
    title: '99.99% Core SLA Backing',
    body: 'Pro Team & Enterprise contracts maintain sub-second telemetry ingestion backed by contractual 4x billing credit penalization.',
    icon: 'gauge' as const,
  },
]
