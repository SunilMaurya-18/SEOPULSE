import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import type { WebVitals } from '@/api/webVitals'
import { WebVitalsPanel } from './WebVitalsPanel'

const ready: WebVitals = {
  state: 'READY',
  strategy: 'mobile',
  url: 'https://example.com/',
  performanceScore: 73,
  lab: { lcpMs: 3120, cls: 0.04, tbtMs: 250, fcpMs: 1400, speedIndexMs: 2800 },
  field: { lcpMs: 2300, cls: 0.3, inpMs: 180, category: 'AVERAGE' },
  errorMessage: null,
  measuredAt: '2026-10-03T00:00:00Z',
}

describe('WebVitalsPanel', () => {
  it('rates real-visitor and lab metrics against Google thresholds', () => {
    render(<WebVitalsPanel vitals={ready} />)

    expect(screen.getByText('73')).toBeInTheDocument()
    expect(screen.getByText('Real visitors, last 28 days')).toBeInTheDocument()
    expect(screen.getByText('2.3 s')).toBeInTheDocument()
    expect(screen.getByText('0.30')).toBeInTheDocument()
    expect(screen.getAllByText('Poor')).toHaveLength(1)
    expect(screen.getByText('3.1 s')).toBeInTheDocument()
  })

  it('shows progress while measuring and hides itself when unavailable', () => {
    const { rerender, container } = render(<WebVitalsPanel vitals={{ ...ready, state: 'PENDING', lab: null, field: null }} />)
    expect(screen.getByText(/Measuring page speed/)).toBeInTheDocument()

    rerender(<WebVitalsPanel vitals={{ ...ready, state: 'UNAVAILABLE' }} />)
    expect(container).toBeEmptyDOMElement()
  })
})
