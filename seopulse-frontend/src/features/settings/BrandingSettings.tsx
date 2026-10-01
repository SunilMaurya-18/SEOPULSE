import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { ImagePlus, Palette } from 'lucide-react'
import { Link } from 'react-router-dom'

import { getErrorMessage } from '@/api/errors'
import { brandingApi, type BrandingState, type Organization, type OrganizationBranding } from '@/api/saas'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/lib/toast'
import { SettingsGroup, SettingsRow } from './SettingsGroup'

const MAX_LOGO_BYTES = 200 * 1024
const EMPTY: OrganizationBranding = { companyName: '', brandColor: '#f5504a', coverText: '', logoDataUrl: null }

function readAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

export function BrandingSettings({ org }: { org: Organization }) {
  const { pushToast } = useToast()
  const canManage = org.role === 'OWNER' || org.role === 'ADMIN'
  const [state, setState] = useState<BrandingState | null>(null)
  const [form, setForm] = useState<OrganizationBranding>(EMPTY)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    brandingApi
      .get(org.id)
      .then((next) => {
        setState(next)
        setForm({ ...EMPTY, ...stripNulls(next.branding) })
      })
      .catch(() => setState(null))
  }, [org.id])

  async function pickLogo(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (file.type !== 'image/png' && file.type !== 'image/jpeg') {
      pushToast({ tone: 'error', title: 'Unsupported logo', description: 'Use a PNG or JPEG image.' })
      return
    }
    if (file.size > MAX_LOGO_BYTES) {
      pushToast({ tone: 'error', title: 'Logo too large', description: 'The logo can be at most 200 KB.' })
      return
    }
    const logoDataUrl = await readAsDataUrl(file)
    setForm((current) => ({ ...current, logoDataUrl }))
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    try {
      const next = await brandingApi.save(org.id, form)
      setState(next)
      pushToast({ tone: 'success', title: 'Branding saved', description: 'New PDF reports and share links use it.' })
    } catch (err) {
      pushToast({ tone: 'error', title: 'Could not save branding', description: getErrorMessage(err, 'Please try again.') })
    } finally {
      setSaving(false)
    }
  }

  async function reset() {
    setSaving(true)
    try {
      await brandingApi.clear(org.id)
      setState((current) => (current ? { ...current, branding: null } : current))
      setForm(EMPTY)
      pushToast({ tone: 'info', title: 'Branding removed', description: 'Reports use the standard SEOPulse look again.' })
    } catch (err) {
      pushToast({ tone: 'error', title: 'Could not remove branding', description: getErrorMessage(err, 'Please try again.') })
    } finally {
      setSaving(false)
    }
  }

  if (!state) return null

  if (!state.whiteLabelAvailable) {
    return (
      <SettingsGroup id="branding" title="Report branding">
        <SettingsRow
          icon={<Palette className="h-4 w-4" />}
          tint="from-[#c58cff] to-[#9f5cf0]"
          label="White-label reports"
          detail="Put your logo, colors and company name on PDF reports and share links."
        >
          <Link to="/pricing">
            <Button size="sm" variant="secondary">
              Agency plan
            </Button>
          </Link>
        </SettingsRow>
      </SettingsGroup>
    )
  }

  const input = 'w-full bg-transparent text-[15px] text-main outline-none placeholder:text-dim disabled:opacity-60'

  return (
    <form onSubmit={(event) => void save(event)}>
      <SettingsGroup
        id="branding"
        title="Report branding"
        footer="Applies to new PDF reports and shared report pages. Logos must be PNG or JPEG, up to 200 KB."
      >
        <SettingsRow
          icon={<Palette className="h-4 w-4" />}
          tint="from-[#c58cff] to-[#9f5cf0]"
          label={
            <input
              className={input}
              value={form.companyName ?? ''}
              maxLength={100}
              disabled={!canManage}
              onChange={(event) => setForm((current) => ({ ...current, companyName: event.target.value }))}
              placeholder="Company name"
              aria-label="Company name"
            />
          }
        >
          <input
            type="color"
            value={form.brandColor || '#f5504a'}
            disabled={!canManage}
            onChange={(event) => setForm((current) => ({ ...current, brandColor: event.target.value }))}
            aria-label="Brand color"
            className="h-8 w-10 cursor-pointer rounded border border-default bg-transparent"
          />
        </SettingsRow>
        <SettingsRow
          icon={<span className="text-[12px] font-semibold">Aa</span>}
          tint="from-[#a1a1a6] to-[#6e6e73]"
          label={
            <input
              className={input}
              value={form.coverText ?? ''}
              maxLength={500}
              disabled={!canManage}
              onChange={(event) => setForm((current) => ({ ...current, coverText: event.target.value }))}
              placeholder="Cover text, e.g. Prepared for Acme Inc."
              aria-label="Cover text"
            />
          }
        />
        <SettingsRow
          icon={<ImagePlus className="h-4 w-4" />}
          tint="from-[#4aa8ff] to-[#0a84ff]"
          label={
            form.logoDataUrl ? (
              <img src={form.logoDataUrl} alt="Logo preview" className="h-8 max-w-[160px] object-contain" />
            ) : (
              <span className="text-dim">No logo</span>
            )
          }
        >
          {canManage && (
            <>
              <label className="inline-flex h-8 cursor-pointer items-center rounded-[var(--sp-btn-radius,0.375rem)] border border-default bg-surface px-3.5 text-[13px] font-semibold text-main hover:bg-surface-elevated">
                Upload
                <input type="file" accept="image/png,image/jpeg" className="sr-only" onChange={(event) => void pickLogo(event)} />
              </label>
              {form.logoDataUrl && (
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  onClick={() => setForm((current) => ({ ...current, logoDataUrl: null }))}
                >
                  Remove
                </Button>
              )}
            </>
          )}
        </SettingsRow>
        {canManage && (
          <SettingsRow label={<span className="text-dim">{state.branding ? 'Branding is active' : 'Not saved yet'}</span>}>
            {state.branding && (
              <Button type="button" size="sm" variant="ghost" disabled={saving} onClick={() => void reset()}>
                Reset
              </Button>
            )}
            <Button type="submit" size="sm" loading={saving}>
              Save branding
            </Button>
          </SettingsRow>
        )}
      </SettingsGroup>
    </form>
  )
}

function stripNulls(branding: OrganizationBranding | null): Partial<OrganizationBranding> {
  if (!branding) return {}
  return Object.fromEntries(Object.entries(branding).filter(([, value]) => value !== null)) as Partial<OrganizationBranding>
}
