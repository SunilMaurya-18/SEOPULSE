import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import axios from 'axios'

import { auditApi } from '@/api/audits'
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
      try {
        const website = await websiteApi.createWebsite(projectId, {
          name: websiteNameFromUrl(pending),
          url: pending,
        })
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
        console.error(err)
        const message = axios.isAxiosError(err)
          ? err.response?.data?.message ??
            Object.values(
              (err.response?.data?.validationErrors as Record<string, string>) ??
                {},
            )[0]
          : null

        pushToast({
          tone: 'error',
          title: 'Could not start onboarding audit',
          description:
            typeof message === 'string'
              ? message
              : 'Open Websites to add the site manually.',
        })
        navigate('/websites', { replace: true })
      } finally {
        processing.current = false
      }
    })()
  }, [projectId, location.pathname, navigate, notifyDataChanged, pushToast])

  return null
}
