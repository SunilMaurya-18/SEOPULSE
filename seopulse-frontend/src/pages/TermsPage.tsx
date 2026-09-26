import { useCallback, useEffect, useState } from 'react'

import {
  CitationToast,
  DocumentNav,
  DocumentViewer,
  TermsFooter,
  TermsHeader,
  TermsHero,
} from '@/features/terms'

export function TermsPage() {
  const [activeId, setActiveId] = useState('section-1')
  const [toastMessage, setToastMessage] = useState<string | null>(null)

  const handleNavigate = useCallback((id: string) => {
    const el = document.getElementById(id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      history.pushState(null, '', `#${id}`)
      setActiveId(id)
    }
  }, [])

  const handleCopyCitation = useCallback((clauseName: string) => {
    setToastMessage(`Copied: ${clauseName}`)
    window.setTimeout(() => setToastMessage(null), 2500)
  }, [])

  useEffect(() => {
    const onScroll = () => {
      const sections = document.querySelectorAll<HTMLElement>('.clause-block')
      let current = 'section-1'
      sections.forEach((sec) => {
        const top = sec.offsetTop - 120
        if (window.scrollY >= top) {
          current = sec.id
        }
      })
      setActiveId(current)
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div className="flex min-h-screen flex-col bg-canvas font-sans text-sm text-main antialiased">
      <TermsHeader />
      <main className="w-full flex-1 bg-canvas pt-14">
        <TermsHero />
        <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6">
          <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-12">
            <DocumentNav activeId={activeId} onNavigate={handleNavigate} />
            <DocumentViewer onCopyCitation={handleCopyCitation} />
          </div>
        </div>
      </main>
      <TermsFooter />
      <CitationToast message={toastMessage} />
    </div>
  )
}
