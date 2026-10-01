import { useState, type FormEvent } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertOctagon, Bell, Hash, KeyRound, Mail, Plus, Send, TrendingDown, Trash2, Unplug, Webhook } from 'lucide-react'

import { getErrorMessage } from '@/api/errors'
import {
  alertApi,
  type AlertChannel,
  type AlertDelivery,
  type AlertRule,
  type AlertRuleInput,
  type AlertType,
  type Organization,
  type PlanLimits,
} from '@/api/saas'
import { queryKeys } from '@/api/queries/keys'
import { websiteApi, type Website } from '@/api/websites'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { formatDateTime, hostOf } from '@/lib/format'
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'
import { SettingsGroup, SettingsRow } from './SettingsGroup'

const TYPE_LABELS: Record<AlertType, string> = {
  SCORE_DROP: 'Score drops',
  NEW_ERRORS: 'New errors appear',
  PAGE_UNREACHABLE: 'Homepage unreachable',
  AUDIT_FAILED: 'Audit fails',
}

const CHANNEL_LABELS: Record<AlertChannel, string> = {
  EMAIL: 'Email',
  SLACK_WEBHOOK: 'Slack',
  WEBHOOK: 'Webhook',
}

const TYPE_ICONS: Record<AlertType, typeof Bell> = {
  SCORE_DROP: TrendingDown,
  NEW_ERRORS: AlertOctagon,
  PAGE_UNREACHABLE: Unplug,
  AUDIT_FAILED: Bell,
}

const CHANNEL_ICONS: Record<AlertChannel, typeof Mail> = {
  EMAIL: Mail,
  SLACK_WEBHOOK: Hash,
  WEBHOOK: Webhook,
}

const NO_RULES: AlertRule[] = []
const NO_DELIVERIES: AlertDelivery[] = []
const NO_WEBSITES: Website[] = []

function describeRule(rule: AlertRule) {
  if (rule.type === 'SCORE_DROP') return `Score drops by ${rule.threshold ?? 10}+ points`
  if (rule.type === 'NEW_ERRORS') return `${rule.threshold ?? 1}+ new errors`
  return TYPE_LABELS[rule.type]
}

function describeTarget(rule: AlertRule) {
  if (rule.channel === 'EMAIL') return rule.target || 'Owners and admins'
  return rule.target ?? ''
}

function toInput(rule: AlertRule, overrides: Partial<AlertRuleInput> = {}): AlertRuleInput {
  return {
    type: rule.type,
    threshold: rule.threshold,
    channel: rule.channel,
    target: rule.target,
    websiteId: rule.websiteId,
    enabled: rule.enabled,
    ...overrides,
  }
}

export function AlertSettings({ org, limits }: { org: Organization; limits: PlanLimits }) {
  const { projectId } = useWorkspace()
  const { pushToast } = useToast()
  const canManage = org.role === 'OWNER' || org.role === 'ADMIN'
  const queryClient = useQueryClient()
  const rulesKey = ['orgs', org.id, 'alerts'] as const
  const deliveriesKey = ['orgs', org.id, 'alerts', 'deliveries'] as const
  const rulesQuery = useQuery({ queryKey: rulesKey, queryFn: () => alertApi.list(org.id), retry: false })
  const deliveriesQuery = useQuery({ queryKey: deliveriesKey, queryFn: () => alertApi.deliveries(org.id), retry: false })
  const websitesQuery = useQuery({
    queryKey: [...queryKeys.websites(projectId), 'all'],
    queryFn: async () => (await websiteApi.getWebsites(projectId, 0, 100)).content ?? [],
    retry: false,
  })
  const rules = rulesQuery.data ?? NO_RULES
  const deliveries = deliveriesQuery.data ?? NO_DELIVERIES
  const websites = websitesQuery.data ?? NO_WEBSITES
  const [adding, setAdding] = useState(false)
  const [revealed, setRevealed] = useState<AlertRule | null>(null)
  const [busyRule, setBusyRule] = useState<number | null>(null)
  const [showDeliveries, setShowDeliveries] = useState(false)

  const setRules = (update: (current: AlertRule[]) => AlertRule[]) =>
    queryClient.setQueryData<AlertRule[]>(rulesKey, (current) => update(current ?? []))
  const load = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: rulesKey, exact: true }),
      queryClient.invalidateQueries({ queryKey: deliveriesKey }),
    ])

  const siteName = (websiteId: number | null) => {
    if (websiteId === null) return 'All websites'
    const site = websites.find((item) => item.id === websiteId)
    return site ? site.name || hostOf(site.url) : `Website #${websiteId}`
  }

  async function withRule(rule: AlertRule, action: () => Promise<void>) {
    setBusyRule(rule.id)
    try {
      await action()
    } finally {
      setBusyRule(null)
    }
  }

  function toggle(rule: AlertRule) {
    void withRule(rule, async () => {
      try {
        const updated = await alertApi.update(org.id, rule.id, toInput(rule, { enabled: !rule.enabled }))
        setRules((current) => current.map((item) => (item.id === rule.id ? updated : item)))
      } catch (err) {
        pushToast({ tone: 'error', title: 'Update failed', description: getErrorMessage(err, 'Unable to update the alert.') })
      }
    })
  }

  function sendTest(rule: AlertRule) {
    void withRule(rule, async () => {
      try {
        const result = await alertApi.test(org.id, rule.id)
        pushToast(
          result.delivered
            ? { tone: 'success', title: 'Test alert sent', description: `Delivered via ${CHANNEL_LABELS[rule.channel]}.` }
            : { tone: 'error', title: 'Test alert failed', description: result.error ?? 'The destination rejected the alert.' },
        )
        await load()
      } catch (err) {
        pushToast({ tone: 'error', title: 'Test alert failed', description: getErrorMessage(err, 'Unable to send the test alert.') })
      }
    })
  }

  function rotate(rule: AlertRule) {
    void withRule(rule, async () => {
      try {
        const updated = await alertApi.rotateSecret(org.id, rule.id)
        setRules((current) => current.map((item) => (item.id === rule.id ? updated : item)))
        setRevealed(updated)
      } catch (err) {
        pushToast({ tone: 'error', title: 'Rotation failed', description: getErrorMessage(err, 'Unable to rotate the secret.') })
      }
    })
  }

  function remove(rule: AlertRule) {
    void withRule(rule, async () => {
      try {
        await alertApi.remove(org.id, rule.id)
        setRules((current) => current.filter((item) => item.id !== rule.id))
      } catch (err) {
        pushToast({ tone: 'error', title: 'Delete failed', description: getErrorMessage(err, 'Unable to delete the alert.') })
      }
    })
  }

  async function created(rule: AlertRule) {
    setAdding(false)
    if (rule.secretRevealed) setRevealed(rule)
    pushToast({ tone: 'success', title: 'Alert added' })
    await load()
  }

  return (
    <>
      <SettingsGroup
        id="alerts"
        title="Alerts"
        footer={
          limits.webhookAlerts
            ? 'Alerts fire after each audit. Webhook requests are signed with HMAC-SHA256 in the X-SEOPulse-Signature header.'
            : 'Email alerts are included on every plan. Slack and webhook alerts are available on Pro and Agency.'
        }
      >
        {rules.length === 0 && (
          <SettingsRow
            icon={<Bell className="h-4 w-4" />}
            tint="from-[#a1a1a6] to-[#6e6e73]"
            label="No alerts yet"
            detail="Default email alerts are added when you connect your first website."
          />
        )}
        {rules.map((rule) => {
          const TypeIcon = TYPE_ICONS[rule.type]
          const ChannelIcon = CHANNEL_ICONS[rule.channel]
          const busy = busyRule === rule.id
          return (
            <SettingsRow
              key={rule.id}
              icon={<TypeIcon className="h-4 w-4" />}
              tint={rule.enabled ? 'from-[#ffb340] to-[#ff9500]' : 'from-[#a1a1a6] to-[#6e6e73]'}
              label={
                <span className="flex items-center gap-2">
                  <span className={rule.enabled ? '' : 'text-dim line-through'}>{describeRule(rule)}</span>
                  <Badge>{siteName(rule.websiteId)}</Badge>
                </span>
              }
              detail={
                <span className="inline-flex items-center gap-1.5">
                  <ChannelIcon className="h-3.5 w-3.5" />
                  {CHANNEL_LABELS[rule.channel]} · {describeTarget(rule)}
                  {rule.signingSecret ? ` · secret ${rule.signingSecret}` : ''}
                </span>
              }
            >
              {canManage && (
                <>
                  <Button size="sm" variant="ghost" disabled={busy} onClick={() => toggle(rule)}>
                    {rule.enabled ? 'Pause' : 'Resume'}
                  </Button>
                  <Button size="sm" variant="secondary" loading={busy} onClick={() => sendTest(rule)}>
                    <Send className="h-3.5 w-3.5" />
                    Send test alert
                  </Button>
                  {rule.channel === 'WEBHOOK' && (
                    <Button size="sm" variant="ghost" disabled={busy} onClick={() => rotate(rule)} aria-label="Rotate signing secret">
                      <KeyRound className="h-3.5 w-3.5" />
                    </Button>
                  )}
                  <Button size="sm" variant="ghost" disabled={busy} onClick={() => remove(rule)} aria-label="Delete alert">
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </>
              )}
            </SettingsRow>
          )
        })}
        {canManage && (
          <button type="button" onClick={() => setAdding(true)} className="block w-full text-left hover:bg-surface-elevated/50">
            <SettingsRow icon={<Plus className="h-4 w-4" />} tint="from-[#4ee37a] to-[#28b14c]" label={<span className="text-accent">Add alert</span>} />
          </button>
        )}
        <button
          type="button"
          onClick={() => setShowDeliveries((value) => !value)}
          className="block w-full text-left hover:bg-surface-elevated/50"
          aria-expanded={showDeliveries}
        >
          <SettingsRow
            icon={<Send className="h-4 w-4" />}
            tint="from-[#4aa8ff] to-[#0a84ff]"
            label="Recent deliveries"
            detail={deliveries.length ? `${deliveries.filter((item) => !item.delivered).length} pending or failed` : 'None yet'}
          >
            <span className="text-[15px] text-dim font-tabular">{deliveries.length}</span>
          </SettingsRow>
        </button>
        {showDeliveries && deliveries.length > 0 && (
          <ul className="max-h-72 divide-y divide-default overflow-y-auto bg-surface-low/60 dark:bg-surface-elevated/25">
            {deliveries.map((delivery) => (
              <li key={delivery.id} className="flex items-center gap-3 px-5 py-2.5 text-[13px]">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium text-main">{delivery.subject}</p>
                  <p className="truncate text-xs text-dim">
                    {CHANNEL_LABELS[delivery.channel]} · {formatDateTime(delivery.createdAt)}
                    {delivery.lastError ? ` · ${delivery.lastError}` : ''}
                  </p>
                </div>
                {delivery.delivered ? (
                  <Badge variant="success">delivered</Badge>
                ) : (
                  <Badge variant={delivery.attempts > 0 ? 'warning' : 'neutral'}>
                    {delivery.attempts > 0 ? `retrying (${delivery.attempts})` : 'queued'}
                  </Badge>
                )}
              </li>
            ))}
          </ul>
        )}
      </SettingsGroup>

      {adding && (
        <AddAlertDialog
          orgId={org.id}
          webhooksAllowed={limits.webhookAlerts}
          websites={websites}
          onClose={() => setAdding(false)}
          onCreated={(rule) => void created(rule)}
        />
      )}

      {revealed?.signingSecret && (
        <Modal
          open
          onClose={() => setRevealed(null)}
          title="Webhook signing secret"
          description="Store this secret now. It will not be shown again. Use it to verify the X-SEOPulse-Signature header."
          size="md"
        >
          <code className="block rounded-2xl bg-surface-low p-3 text-sm break-all text-main dark:bg-surface-elevated">
            {revealed.signingSecret}
          </code>
          <p className="mt-3 text-xs leading-relaxed text-dim">
            The header has the form <code>t=&lt;unix seconds&gt;,v1=&lt;hex&gt;</code>, where v1 is the HMAC-SHA256 of
            <code> t + &quot;.&quot; + raw body</code> using this secret.
          </p>
          <div className="mt-5 flex justify-end">
            <Button onClick={() => setRevealed(null)}>Done</Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function AddAlertDialog({
  orgId,
  webhooksAllowed,
  websites,
  onClose,
  onCreated,
}: {
  orgId: number
  webhooksAllowed: boolean
  websites: Website[]
  onClose: () => void
  onCreated: (rule: AlertRule) => void
}) {
  const [type, setType] = useState<AlertType>('SCORE_DROP')
  const [threshold, setThreshold] = useState(10)
  const [channel, setChannel] = useState<AlertChannel>('EMAIL')
  const [target, setTarget] = useState('')
  const [websiteId, setWebsiteId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const hasThreshold = type === 'SCORE_DROP' || type === 'NEW_ERRORS'

  function changeType(next: AlertType) {
    setType(next)
    setThreshold(next === 'SCORE_DROP' ? 10 : 1)
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const rule = await alertApi.create(orgId, {
        type,
        threshold: hasThreshold ? threshold : null,
        channel,
        target: target.trim() || null,
        websiteId,
        enabled: true,
      })
      onCreated(rule)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to add the alert.'))
    } finally {
      setSaving(false)
    }
  }

  const field =
    'h-9 w-full rounded-[var(--sp-field-radius,0.5rem)] border border-default bg-[var(--sp-field-bg)] px-3 text-sm text-main'

  return (
    <Modal open onClose={onClose} title="Add alert" description="Choose what to watch and where to send it.">
      <form onSubmit={(event) => void submit(event)} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="text-[13px] text-muted">
            <span className="mb-1 block font-medium">When</span>
            <select className={field} value={type} onChange={(event) => changeType(event.target.value as AlertType)}>
              {(Object.keys(TYPE_LABELS) as AlertType[]).map((key) => (
                <option key={key} value={key}>
                  {TYPE_LABELS[key]}
                </option>
              ))}
            </select>
          </label>
          {hasThreshold && (
            <label className="text-[13px] text-muted">
              <span className="mb-1 block font-medium">{type === 'SCORE_DROP' ? 'By at least (points)' : 'At least (errors)'}</span>
              <input
                type="number"
                min={1}
                max={type === 'SCORE_DROP' ? 100 : 10000}
                required
                className={field}
                value={threshold}
                onChange={(event) => setThreshold(Number(event.target.value))}
              />
            </label>
          )}
          <label className="text-[13px] text-muted">
            <span className="mb-1 block font-medium">Website</span>
            <select
              className={field}
              value={websiteId ?? ''}
              onChange={(event) => setWebsiteId(event.target.value ? Number(event.target.value) : null)}
            >
              <option value="">All websites</option>
              {websites.map((site) => (
                <option key={site.id} value={site.id}>
                  {site.name || hostOf(site.url)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-[13px] text-muted">
            <span className="mb-1 block font-medium">Send to</span>
            <select
              className={field}
              value={channel}
              onChange={(event) => {
                setChannel(event.target.value as AlertChannel)
                setTarget('')
              }}
            >
              <option value="EMAIL">Email</option>
              <option value="SLACK_WEBHOOK" disabled={!webhooksAllowed}>
                Slack{webhooksAllowed ? '' : ' (Pro)'}
              </option>
              <option value="WEBHOOK" disabled={!webhooksAllowed}>
                Webhook{webhooksAllowed ? '' : ' (Pro)'}
              </option>
            </select>
          </label>
        </div>

        <label className="block text-[13px] text-muted">
          <span className="mb-1 block font-medium">
            {channel === 'EMAIL' ? 'Email address (optional)' : channel === 'SLACK_WEBHOOK' ? 'Slack webhook URL' : 'Endpoint URL'}
          </span>
          <input
            type={channel === 'EMAIL' ? 'email' : 'url'}
            required={channel !== 'EMAIL'}
            className={field}
            value={target}
            onChange={(event) => setTarget(event.target.value)}
            placeholder={
              channel === 'EMAIL'
                ? 'Leave blank to notify owners and admins'
                : channel === 'SLACK_WEBHOOK'
                  ? 'https://hooks.slack.com/services/…'
                  : 'https://example.com/hooks/seopulse'
            }
          />
        </label>
        {channel === 'WEBHOOK' && (
          <p className="text-xs text-dim">
            We POST JSON over HTTPS and sign every request. A signing secret is shown once after you add the alert.
          </p>
        )}
        {error && <p className="rounded-xl bg-critical-surface px-3 py-2 text-sm text-critical">{error}</p>}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            Add alert
          </Button>
        </div>
      </form>
    </Modal>
  )
}
