import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { auditApi } from '@/api/audits'
import { getErrorCode, getErrorMessage } from '@/api/errors'
import { websiteApi, websiteNameFromUrl } from '@/api/websites'
import {
  peekPendingWebsiteUrl,
  takePendingWebsiteUrl,
} from '@/lib/pendingWebsite'
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'

/**
 * Completes the landing → auth → workspace handoff:
 * create website from pending URL, start audit, open the report.
 */
export function OnboardingHandler() {
  const { projectId, notifyDataChanged } = useWorkspace()
  const { pushToast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const processing = useRef(false)

  useEffect(() => {
    if (processing.current) return
    if (!peekPendingWebsiteUrl()) return

    const pending = takePendingWebsiteUrl()
    if (!pending) return

    processing.current = true

    ;(async () => {
      let websiteId: number | null = null
      try {
        const website = await websiteApi.createWebsite(projectId, {
          name: websiteNameFromUrl(pending),
          url: pending,
        })
        websiteId = website.id
        notifyDataChanged()

        const audit = await auditApi.createAudit(projectId, website.id)
        notifyDataChanged()

        pushToast({
          tone: 'success',
          title: 'Website connected',
          description: `Audit #${audit.id} is running for ${website.url}`,
        })

        navigate(`/audits/${audit.id}`, { replace: true })
      } catch (err) {
        if (websiteId !== null && getErrorCode(err) === 'EMAIL_NOT_VERIFIED') {
          pushToast({
            tone: 'info',
            title: 'Website connected',
            description: 'Verify your email to run your first audit.',
          })
          navigate(`/audits?websiteId=${websiteId}`, { replace: true })
          return
        }

        console.error(err)
        pushToast({
          tone: 'error',
          title: 'Could not start onboarding audit',
          description: getErrorMessage(err, 'Open Websites to add the site manually.'),
        })
        navigate('/websites', { replace: true })
      } finally {
        processing.current = false
      }
    })()
  }, [projectId, location.pathname, navigate, notifyDataChanged, pushToast])

  return null
}
