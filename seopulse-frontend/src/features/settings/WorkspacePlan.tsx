import { useEffect, useState, type FormEvent } from 'react'
import { CalendarClock, CreditCard, Gauge, Globe2, IndianRupee, Mail, UserPlus, Users } from 'lucide-react'

import { saasApi, type BillingSnapshot, type Organization, type OrgInvite, type OrgMember } from '@/api/saas'
import { getErrorMessage } from '@/api/errors'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/lib/toast'
import { cn } from '@/lib/cn'
import { CheckoutDismissedError, openRazorpayCheckout, prefersInr } from '@/lib/razorpay'
import { AlertSettings } from './AlertSettings'
import { BrandingSettings } from './BrandingSettings'
import { SettingsGroup, SettingsRow } from './SettingsGroup'

function Meter({ used, limit }: { used: number; limit: number }) {
  const pct = limit <= 0 ? 0 : Math.min(100, Math.round((used / limit) * 100))
  return (
    <div className="flex items-center gap-3">
      <div className="hidden h-1.5 w-28 overflow-hidden rounded-full bg-surface-elevated sm:block">
        <div
          className={cn('h-full rounded-full', pct >= 90 ? 'bg-critical' : 'bg-accent')}
          style={{ width: `${Math.max(pct, 3)}%` }}
        />
      </div>
      <span className="text-[15px] text-dim font-tabular">
        {used} of {limit}
      </span>
    </div>
  )
}

export function WorkspacePlan() {
  const { pushToast } = useToast()
  const [org, setOrg] = useState<Organization | null>(null)
  const [billing, setBilling] = useState<BillingSnapshot | null>(null)
  const [members, setMembers] = useState<OrgMember[]>([])
  const [invites, setInvites] = useState<OrgInvite[]>([])
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [currency, setCurrency] = useState<'USD' | 'INR'>(() => (prefersInr() ? 'INR' : 'USD'))
  const [paying, setPaying] = useState<string | null>(null)

  async function load() {
    const orgs = await saasApi.orgs()
    const current = orgs[0] ?? null
    setOrg(current)
    if (!current) return
    const [snapshot, people, pending] = await Promise.all([
      saasApi.billing(current.id),
      saasApi.members(current.id),
      current.role === 'OWNER' || current.role === 'ADMIN' ? saasApi.invites(current.id) : Promise.resolve([]),
    ])
    setBilling(snapshot)
    setMembers(people)
    setInvites(pending)
  }

  useEffect(() => {
    void load().catch(() => setOrg(null))
  }, [])

  async function invite(event: FormEvent) {
    event.preventDefault()
    if (!org) return
    setBusy(true)
    try {
      await saasApi.invite(org.id, email, 'MEMBER')
      setEmail('')
      pushToast({ tone: 'success', title: 'Invite sent', description: 'They will receive an email with a join link.' })
      await load()
    } catch (err) {
      pushToast({ tone: 'error', title: 'Invite failed', description: getErrorMessage(err, 'Unable to send the invite.') })
    } finally {
      setBusy(false)
    }
  }

  async function checkout(plan: string) {
    if (!org || !billing) return
    const viaRazorpay = billing.razorpayAvailable && (currency === 'INR' || !billing.stripeAvailable)
    if (!viaRazorpay) {
      try {
        window.location.assign(await saasApi.checkout(org.id, plan, 'month'))
      } catch (err) {
        pushToast({
          tone: 'error',
          title: 'Checkout unavailable',
          description: getErrorMessage(err, 'Add Stripe price IDs to start a paid checkout.'),
        })
      }
      return
    }
    setPaying(plan)
    try {
      const session = await saasApi.razorpaySubscribe(org.id, plan, 'month')
      const label = plan === 'AGENCY' ? 'Agency' : 'Pro'
      const payment = await openRazorpayCheckout(session, `${label} plan, billed monthly in INR`)
      await saasApi.razorpayVerify(org.id, payment)
      pushToast({ tone: 'success', title: `You're on ${label}`, description: 'Thanks! Your new limits apply right away.' })
      await load()
    } catch (err) {
      if (!(err instanceof CheckoutDismissedError)) {
        pushToast({
          tone: 'error',
          title: 'Payment not completed',
          description: getErrorMessage(err, 'Razorpay checkout could not be completed. You have not been charged.'),
        })
      }
    } finally {
      setPaying(null)
    }
  }

  async function cancelRazorpay() {
    if (!org) return
    if (!window.confirm('Cancel your subscription? You keep your plan until the end of the current billing period.')) return
    try {
      await saasApi.razorpayCancel(org.id)
      pushToast({ tone: 'info', title: 'Subscription will end', description: 'You keep your plan until the period ends.' })
      await load()
    } catch (err) {
      pushToast({ tone: 'error', title: 'Could not cancel', description: getErrorMessage(err, 'Please try again.') })
    }
  }

  async function openPortal() {
    if (!org) return
    try {
      window.location.assign(await saasApi.portal(org.id))
    } catch (err) {
      pushToast({
        tone: 'error',
        title: 'Billing portal unavailable',
        description: getErrorMessage(err, 'Unable to open the billing portal.'),
      })
    }
  }

  if (!org || !billing) return null
  const isOwner = org.role === 'OWNER'
  const canInvite = isOwner || org.role === 'ADMIN'
  const isFree = billing.planCode === 'FREE'
  // Razorpay subscriptions can't be switched in place; cancel first, then pick a new plan.
  const canUpgrade = billing.provider !== 'RAZORPAY'

  return (
    <>
      <SettingsGroup
        id="billing"
        title="Plan & billing"
        footer={billing.cancelAtPeriodEnd ? 'Your subscription cancels at the end of the current period.' : undefined}
      >
        <SettingsRow
          icon={<CreditCard className="h-4 w-4" />}
          tint="from-[#ff6b5f] to-[#f5504a]"
          label={<span className="font-semibold">{billing.planName}</span>}
          detail={
            billing.currentPeriodEnd
              ? `Renews ${new Date(billing.currentPeriodEnd).toLocaleDateString()}`
              : billing.status === 'ACTIVE'
                ? 'Active'
                : billing.status.toLowerCase().replace(/_/g, ' ')
          }
        >
          {isOwner && canUpgrade && billing.planCode !== 'PRO' && billing.planCode !== 'AGENCY' && (
            <Button size="sm" loading={paying === 'PRO'} disabled={paying !== null} onClick={() => void checkout('PRO')}>
              Upgrade to Pro
            </Button>
          )}
          {isOwner && canUpgrade && billing.planCode !== 'AGENCY' && (
            <Button
              size="sm"
              variant="secondary"
              loading={paying === 'AGENCY'}
              disabled={paying !== null}
              onClick={() => void checkout('AGENCY')}
            >
              Agency
            </Button>
          )}
          {isOwner && !isFree && billing.provider === 'RAZORPAY' && !billing.cancelAtPeriodEnd && (
            <Button size="sm" variant="secondary" onClick={() => void cancelRazorpay()}>
              Cancel subscription
            </Button>
          )}
          {isOwner && !isFree && billing.provider !== 'RAZORPAY' && (
            <Button size="sm" variant="secondary" onClick={() => void openPortal()}>
              Manage billing
            </Button>
          )}
        </SettingsRow>
        {isOwner && isFree && billing.stripeAvailable && billing.razorpayAvailable && (
          <SettingsRow
            icon={<IndianRupee className="h-4 w-4" />}
            tint="from-[#5ac8fa] to-[#007aff]"
            label="Pay in"
            detail={currency === 'INR' ? 'UPI, RuPay, cards and netbanking through Razorpay' : 'International cards through Stripe'}
          >
            <div role="radiogroup" aria-label="Billing currency" className="flex rounded-full bg-surface-elevated p-0.5">
              {(['USD', 'INR'] as const).map((option) => (
                <button
                  key={option}
                  type="button"
                  role="radio"
                  aria-checked={currency === option}
                  onClick={() => setCurrency(option)}
                  className={cn(
                    'rounded-full px-3 py-1 text-xs font-semibold transition-colors',
                    currency === option ? 'bg-surface text-main shadow-sm' : 'text-muted hover:text-main',
                  )}
                >
                  {option}
                </button>
              ))}
            </div>
          </SettingsRow>
        )}
        <SettingsRow icon={<Globe2 className="h-4 w-4" />} tint="from-[#4aa8ff] to-[#0a84ff]" label="Websites">
          <Meter used={billing.websitesUsed} limit={billing.limits.websites} />
        </SettingsRow>
        <SettingsRow icon={<Gauge className="h-4 w-4" />} tint="from-[#4ee37a] to-[#28b14c]" label="Audits this month">
          <Meter used={billing.auditsUsed} limit={billing.limits.auditsPerMonth} />
        </SettingsRow>
        <SettingsRow
          icon={<CalendarClock className="h-4 w-4" />}
          tint="from-[#ffb340] to-[#ff9500]"
          label="Scheduled audits"
          detail={`Audit history details kept for ${billing.limits.retentionDays} days`}
        >
          <span className="text-[15px] text-dim">
            {billing.limits.schedule === 'NONE' ? 'Not included' : billing.limits.schedule === 'DAILY' ? 'Daily' : 'Weekly'}
          </span>
        </SettingsRow>
      </SettingsGroup>

      <AlertSettings org={org} limits={billing.limits} />
      <BrandingSettings org={org} />

      <SettingsGroup
        id="team"
        title="Team"
        footer={invites.length > 0 ? `${invites.length} invite${invites.length === 1 ? '' : 's'} waiting to be accepted.` : undefined}
      >
        {members.map((member) => (
          <SettingsRow
            key={member.userId}
            icon={<span className="text-[12px] font-semibold">{(member.name || member.email).charAt(0).toUpperCase()}</span>}
            tint="from-[#c58cff] to-[#9f5cf0]"
            label={member.name || member.email}
            detail={member.email}
          >
            <span className="rounded-full bg-surface-elevated px-2.5 py-0.5 text-xs font-semibold text-muted capitalize">
              {member.role.toLowerCase()}
            </span>
          </SettingsRow>
        ))}
        {canInvite && (
          <form onSubmit={(event) => void invite(event)}>
            <SettingsRow icon={<UserPlus className="h-4 w-4" />} tint="from-[#a1a1a6] to-[#6e6e73]" label={
              <input
                type="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="Invite a teammate by email"
                aria-label="Teammate email"
                className="w-full bg-transparent text-[15px] text-main outline-none placeholder:text-dim"
              />
            }>
              <Button type="submit" size="sm" loading={busy} disabled={!email}>
                <Mail className="h-3.5 w-3.5" />
                Send invite
              </Button>
            </SettingsRow>
          </form>
        )}
        {!canInvite && members.length === 0 && (
          <SettingsRow icon={<Users className="h-4 w-4" />} tint="from-[#a1a1a6] to-[#6e6e73]" label="No teammates yet" />
        )}
      </SettingsGroup>
    </>
  )
}
