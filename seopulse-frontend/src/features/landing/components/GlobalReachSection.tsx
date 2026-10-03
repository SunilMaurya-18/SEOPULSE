import Features02 from '@/components/originkit/features-02'

const metrics = [
  { value: '22', label: 'Built-in SEO analyzers' },
  { value: '100', label: 'Point site health score' },
  { value: 'PDF + CSV', label: 'Shareable report formats' },
] as const

export default function GlobalReachSection() {
  return (
    <Features02
      id="stats"
      eyebrow="Audit any site, anywhere"
      title="Every page, every issue, one score."
      description="Point SEOPulse at any public URL. It crawls the whole site, checks every page against technical, content and performance rules, and turns the results into a report your team can act on."
      metrics={metrics}
    />
  )
}
