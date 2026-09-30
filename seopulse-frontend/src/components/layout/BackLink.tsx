import { ChevronLeft } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'

function parentPath(pathname: string): string {
  if (/^\/audits\/\d+\/(pages|issues)/.test(pathname)) {
    return pathname.replace(/\/(pages|issues)$/, '')
  }
  if (/^\/audits\/\d+/.test(pathname)) return '/audits'
  return '/dashboard'
}

export function BackLink() {
  const location = useLocation()
  const navigate = useNavigate()

  if (location.pathname === '/dashboard') return null

  function handleBack() {
    if (window.history.length > 1) {
      navigate(-1)
      return
    }
    navigate(parentPath(location.pathname))
  }

  return (
    <button
      type="button"
      onClick={handleBack}
      aria-label="Go back"
      className="-ml-1 mb-4 inline-flex h-8 items-center gap-0.5 rounded-full pr-2.5 pl-1 text-[15px] font-medium text-accent transition-opacity hover:opacity-75"
    >
      <ChevronLeft className="h-5 w-5" strokeWidth={2.25} />
      Back
    </button>
  )
}
