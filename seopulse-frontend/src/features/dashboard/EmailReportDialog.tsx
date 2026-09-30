import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { ArrowUp, FileText, X } from 'lucide-react'

import { auditApi, type Audit } from '@/api/audits'
import { getErrorMessage } from '@/api/errors'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { useToast } from '@/lib/toast'

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const MAX_RECIPIENTS = 5

interface EmailReportDialogProps {
  projectId: number
  audit: Audit
  onClose: () => void
}

function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

export function EmailReportDialog({ projectId, audit, onClose }: EmailReportDialogProps) {
  const { user } = useAuth()
  const { pushToast } = useToast()
  const [recipients, setRecipients] = useState<string[]>(user?.email ? [user.email] : [])
  const [draft, setDraft] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const host = hostOf(audit.websiteUrl)

  function commitDraft(): string[] | null {
    const value = draft.trim().replace(/,$/, '')
    if (!value) return recipients
    if (!EMAIL.test(value)) {
      setError(`"${value}" is not a valid email address.`)
      return null
    }
    if (recipients.includes(value.toLowerCase())) {
      setDraft('')
      return recipients
    }
    if (recipients.length >= MAX_RECIPIENTS) {
      setError(`Send to at most ${MAX_RECIPIENTS} people at a time.`)
      return null
    }
    const next = [...recipients, value.toLowerCase()]
    setRecipients(next)
    setDraft('')
    setError(null)
    return next
  }

  function onDraftKey(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' || event.key === ',' || event.key === 'Tab') {
      if (draft.trim()) {
        event.preventDefault()
        commitDraft()
      }
    } else if (event.key === 'Backspace' && !draft && recipients.length) {
      setRecipients(recipients.slice(0, -1))
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    const list = commitDraft()
    if (!list) return
    if (list.length === 0) {
      setError('Add at least one recipient.')
      return
    }
    setSending(true)
    try {
      const count = await auditApi.emailReport(projectId, audit.id, { recipients: list, note })
      pushToast({
        tone: 'success',
        title: 'Report sent',
        description: `Queued for ${count} recipient${count === 1 ? '' : 's'}.`,
      })
      onClose()
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to send the report.'))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="fixed inset-0 z-[70] flex items-end justify-center p-3 sm:items-center sm:p-4">
      <button type="button" aria-label="Close" className="absolute inset-0 bg-black/45 backdrop-blur-[6px]" onClick={onClose} />
      <form
        onSubmit={(event) => void submit(event)}
        role="dialog"
        aria-modal="true"
        aria-labelledby="email-report-title"
        className="animate-page-enter relative w-full max-w-[560px] overflow-hidden rounded-[26px] border border-default bg-surface shadow-[var(--sp-overlay-shadow)]"
      >
        <div className="grid grid-cols-[1fr_auto_1fr] items-center border-b border-default px-4 py-3">
          <button
            type="button"
            onClick={onClose}
            className="justify-self-start rounded-full px-2 py-1 text-[15px] font-medium text-accent hover:opacity-75"
          >
            Cancel
          </button>
          <h2 id="email-report-title" className="text-[15px] font-semibold text-main">
            Email audit report
          </h2>
          <button
            type="submit"
            disabled={sending}
            aria-label="Send report"
            className="flex h-8 w-8 items-center justify-center justify-self-end rounded-full bg-accent text-white shadow-[0_6px_16px_-8px_var(--sp-accent)] transition hover:bg-accent-hover disabled:opacity-50"
          >
            <ArrowUp className={cn('h-4 w-4', sending && 'animate-pulse')} strokeWidth={2.75} />
          </button>
        </div>

        <label className="flex min-h-12 flex-wrap items-center gap-1.5 border-b border-default px-5 py-2">
          <span className="mr-1 text-[15px] text-dim">To:</span>
          {recipients.map((email) => (
            <span
              key={email}
              className="inline-flex items-center gap-1 rounded-full bg-info-surface py-0.5 pr-1 pl-2.5 text-[13px] font-medium text-info"
            >
              {email}
              <button
                type="button"
                aria-label={`Remove ${email}`}
                onClick={() => setRecipients(recipients.filter((item) => item !== email))}
                className="flex h-4 w-4 items-center justify-center rounded-full hover:bg-info/20"
              >
                <X className="h-3 w-3" strokeWidth={2.5} />
              </button>
            </span>
          ))}
          <input
            type="email"
            value={draft}
            onChange={(event) => {
              setDraft(event.target.value)
              setError(null)
            }}
            onKeyDown={onDraftKey}
            onBlur={() => draft.trim() && commitDraft()}
            placeholder={recipients.length ? 'Add another' : 'client@company.com'}
            className="min-w-[10rem] flex-1 bg-transparent py-1 text-[15px] text-main outline-none placeholder:text-dim"
          />
        </label>

        <div className="flex items-center gap-2 border-b border-default px-5 py-3 text-[15px]">
          <span className="text-dim">Subject:</span>
          <span className="truncate font-medium text-main">SEO audit report: {host}</span>
        </div>

        <textarea
          value={note}
          onChange={(event) => setNote(event.target.value.slice(0, 1000))}
          rows={5}
          placeholder="Add a personal message (optional)…"
          className="block w-full resize-none bg-transparent px-5 py-4 text-[15px] leading-relaxed text-main outline-none placeholder:text-dim"
        />

        <div className="px-5 pb-5">
          <div className="flex items-center gap-4 rounded-2xl bg-surface-low p-3.5 dark:bg-surface-elevated/50">
            <ScoreRing score={audit.score} size={56} strokeWidth={6} label="" />
            <div className="min-w-0 flex-1">
              <p className="flex items-center gap-1.5 text-sm font-semibold text-main">
                <FileText className="h-4 w-4 text-dim" />
                SEO report · {host}
              </p>
              <p className="mt-0.5 text-xs text-dim">
                Health score {audit.score ?? 'n/a'} · {audit.pagesCrawled} pages · link to the full report
              </p>
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-dim">
            <span>Up to {MAX_RECIPIENTS} recipients · press Enter to add</span>
            <span className="font-tabular">{note.length}/1000</span>
          </div>
          {error && (
            <p role="alert" className="mt-3 rounded-xl bg-critical-surface px-3 py-2 text-sm font-medium text-critical">
              {error}
            </p>
          )}
        </div>
      </form>
    </div>
  )
}
