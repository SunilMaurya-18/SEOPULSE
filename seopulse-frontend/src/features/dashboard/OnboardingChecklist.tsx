import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowRight, Check, X } from 'lucide-react'
import { Link } from 'react-router-dom'

import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/api/errors'
import { onboardingApi, type OnboardingStatus, type OnboardingStepId } from '@/api/onboarding'
import { queryKeys } from '@/api/queries/keys'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { useToast } from '@/lib/toast'

interface OnboardingChecklistProps {
  projectId: number
  status: OnboardingStatus
  firstWebsiteId: number | null
  activeAuditId: number | null
  latestCompletedAuditId: number | null
}

interface StepCopy {
  title: string
  description: string
  action?: { label: string; to: string } | null
}

export function OnboardingChecklist({
  projectId,
  status,
  firstWebsiteId,
  activeAuditId,
  latestCompletedAuditId,
}: OnboardingChecklistProps) {
  const { user } = useAuth()
  const { pushToast } = useToast()
  const queryClient = useQueryClient()
  const [resent, setResent] = useState(false)

  const dismiss = useMutation({
    mutationFn: onboardingApi.dismiss,
    onSuccess: () =>
      queryClient.setQueryData<OnboardingStatus>(queryKeys.onboarding(projectId), (current) =>
        current ? { ...current, dismissed: true } : current,
      ),
    onError: (err) =>
      pushToast({ tone: 'error', title: 'Could not hide the checklist', description: getErrorMessage(err, 'Try again.') }),
  })

  const resend = useMutation({
    mutationFn: authApi.resendVerification,
    onSuccess: () => {
      setResent(true)
      pushToast({ tone: 'success', title: 'Verification email sent', description: `Check ${user?.email} for the link.` })
    },
    onError: (err) =>
      pushToast({ tone: 'error', title: 'Could not send email', description: getErrorMessage(err, 'Try again later.') }),
  })

  const steps = status.steps
  const doneCount = steps.filter((step) => step.done).length
  const nextId = steps.find((step) => !step.done)?.id

  const copy: Record<OnboardingStepId, StepCopy> = {
    VERIFY_EMAIL: {
      title: 'Verify your email',
      description: `Confirm ${user?.email ?? 'your address'} so audits, alerts and reports reach you.`,
    },
    ADD_WEBSITE: {
      title: 'Add your first website',
      description: 'Connect a domain to unlock crawling, scoring and reports.',
      action: { label: 'Add website', to: '/websites' },
    },
    RUN_AUDIT: {
      title: 'Run your first audit',
      description: activeAuditId
        ? 'Your audit is running. Watch the crawl live.'
        : 'Crawl the site to get a health score and a ranked list of fixes.',
      action: activeAuditId
        ? { label: 'Open live audit', to: `/audits/${activeAuditId}` }
        : { label: 'Start audit', to: firstWebsiteId ? `/audits?websiteId=${firstWebsiteId}` : '/audits' },
    },
    SHARE_REPORT: {
      title: 'Share a report',
      description: 'Send a PDF or a share link to a client or teammate. No account needed to view it.',
      action: latestCompletedAuditId ? { label: 'Open report', to: `/audits/${latestCompletedAuditId}` } : null,
    },
    INVITE_TEAM: {
      title: 'Invite your team',
      description: 'Work on audits together. Pro and Agency plans include extra seats.',
      action: { label: 'Invite', to: '/settings#team' },
    },
  }

  return (
    <section aria-labelledby="onboarding-title" className="widget relative overflow-hidden p-5 sm:p-6">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(120%_140%_at_0%_0%,var(--sp-accent-surface),transparent_55%)]" />
      <div className="relative">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold text-accent">Getting started</p>
            <h2 id="onboarding-title" className="text-headline mt-0.5 text-main">
              Set up SEOPulse in a few minutes
            </h2>
            <p className="mt-1 text-sm text-muted">
              {doneCount} of {steps.length} done
            </p>
          </div>
          <button
            type="button"
            onClick={() => dismiss.mutate()}
            disabled={dismiss.isPending}
            aria-label="Hide getting started checklist"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-dim transition-colors hover:bg-surface-elevated hover:text-main"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div
          className="mt-4 h-1.5 overflow-hidden rounded-full bg-surface-elevated"
          role="progressbar"
          aria-label="Getting started progress"
          aria-valuemin={0}
          aria-valuemax={steps.length}
          aria-valuenow={doneCount}
        >
          <div
            className="h-full rounded-full bg-gradient-to-r from-success to-accent transition-all duration-700"
            style={{ width: `${(doneCount / steps.length) * 100}%` }}
          />
        </div>

        <ol className="mt-5 space-y-2">
          {steps.map((step) => {
            const text = copy[step.id]
            const isNext = step.id === nextId
            return (
              <li
                key={step.id}
                className={cn(
                  'flex flex-col gap-3 rounded-2xl px-3 py-3 sm:flex-row sm:items-center',
                  isNext && 'bg-surface-elevated/60',
                )}
              >
                <div className="flex min-w-0 flex-1 items-start gap-3">
                  <span
                    className={cn(
                      'mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full',
                      step.done ? 'bg-success text-white' : 'border-2 border-default',
                    )}
                    aria-hidden
                  >
                    {step.done && <Check className="h-3.5 w-3.5" strokeWidth={3} />}
                  </span>
                  <div className="min-w-0">
                    <p className={cn('text-sm font-semibold', step.done ? 'text-dim line-through' : 'text-main')}>
                      {text.title}
                      <span className="sr-only">{step.done ? ' (done)' : ''}</span>
                    </p>
                    {!step.done && <p className="mt-0.5 text-[13px] leading-relaxed text-muted">{text.description}</p>}
                  </div>
                </div>
                {!step.done && step.id === 'VERIFY_EMAIL' && (
                  <Button
                    size="sm"
                    variant={isNext ? 'primary' : 'secondary'}
                    loading={resend.isPending}
                    disabled={resent}
                    onClick={() => resend.mutate()}
                    className="self-start sm:self-auto"
                  >
                    {resent ? 'Email sent' : 'Resend email'}
                  </Button>
                )}
                {!step.done && text.action && (
                  <Link
                    to={text.action.to}
                    className={cn(
                      'inline-flex h-8 items-center gap-1.5 self-start rounded-full px-3.5 text-[13px] font-semibold transition sm:self-auto',
                      isNext
                        ? 'bg-accent text-on-accent hover:bg-accent-hover'
                        : 'border border-default text-main hover:bg-surface-elevated',
                    )}
                  >
                    {text.action.label}
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                )}
              </li>
            )
          })}
        </ol>
      </div>
    </section>
  )
}
