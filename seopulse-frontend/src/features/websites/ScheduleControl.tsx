import { useMemo, useState, type FormEvent } from 'react'
import { CalendarClock } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'

import { getErrorMessage } from '@/api/errors'
import { queryKeys } from '@/api/queries/keys'
import { scheduleApi, type AuditSchedule, type ScheduleFrequency } from '@/api/schedules'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { cn } from '@/lib/cn'
import { formatDateTime } from '@/lib/format'
import { useToast } from '@/lib/toast'

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']

function hourLabel(hour: number) {
  return `${String(hour).padStart(2, '0')}:00`
}

function describeSchedule(schedule: AuditSchedule | undefined) {
  if (!schedule?.configured || !schedule.frequency) return 'Not scheduled'
  if (!schedule.enabled) return 'Schedule paused'
  const at = hourLabel(schedule.hourOfDay ?? 0)
  if (schedule.frequency === 'DAILY') return `Daily at ${at}`
  return `${DAYS[(schedule.dayOfWeek ?? 1) - 1].slice(0, 3)} at ${at}`
}

function browserTimezone() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  } catch {
    return 'UTC'
  }
}

function timezones(current: string) {
  const supported =
    typeof Intl.supportedValuesOf === 'function' ? Intl.supportedValuesOf('timeZone') : ['UTC', browserTimezone()]
  return [...new Set(['UTC', current, ...supported])]
}

export function ScheduleControl({ projectId, websiteId }: { projectId: number; websiteId: number }) {
  const [open, setOpen] = useState(false)
  const schedule = useQuery({
    queryKey: queryKeys.websiteSchedule(projectId, websiteId),
    queryFn: () => scheduleApi.get(projectId, websiteId),
    retry: false,
    staleTime: 60_000,
  })
  const active = !!schedule.data?.configured && schedule.data.enabled

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        title={schedule.data?.nextRunAt ? `Next run ${formatDateTime(schedule.data.nextRunAt)}` : undefined}
        className={cn(
          'inline-flex h-8 items-center gap-1.5 rounded-full px-3 text-[13px] font-semibold transition-colors',
          active ? 'bg-accent-surface text-accent' : 'text-muted hover:bg-surface-elevated hover:text-main',
        )}
      >
        <CalendarClock className="h-3.5 w-3.5" />
        {schedule.isPending ? 'Schedule' : describeSchedule(schedule.data)}
      </button>
      {open && schedule.data && (
        <ScheduleDialog projectId={projectId} websiteId={websiteId} schedule={schedule.data} onClose={() => setOpen(false)} />
      )}
    </>
  )
}

function ScheduleDialog({
  projectId,
  websiteId,
  schedule,
  onClose,
}: {
  projectId: number
  websiteId: number
  schedule: AuditSchedule
  onClose: () => void
}) {
  const { pushToast } = useToast()
  const queryClient = useQueryClient()
  const allowsDaily = schedule.planSchedule === 'DAILY'
  const [frequency, setFrequency] = useState<ScheduleFrequency>(
    schedule.frequency ?? (allowsDaily ? 'DAILY' : 'WEEKLY'),
  )
  const [dayOfWeek, setDayOfWeek] = useState(schedule.dayOfWeek ?? 1)
  const [hourOfDay, setHourOfDay] = useState(schedule.hourOfDay ?? 6)
  const [timezone, setTimezone] = useState(schedule.timezone ?? browserTimezone())
  const [enabled, setEnabled] = useState(schedule.configured ? schedule.enabled : true)
  const [saving, setSaving] = useState(false)
  const zones = useMemo(() => timezones(timezone), [timezone])

  const key = queryKeys.websiteSchedule(projectId, websiteId)

  async function save(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    try {
      const saved = await scheduleApi.save(projectId, websiteId, {
        frequency,
        dayOfWeek: frequency === 'WEEKLY' ? dayOfWeek : undefined,
        hourOfDay,
        timezone,
        enabled,
      })
      queryClient.setQueryData(key, saved)
      pushToast({
        tone: 'success',
        title: 'Schedule saved',
        description: saved.nextRunAt ? `Next audit ${formatDateTime(saved.nextRunAt)}.` : undefined,
      })
      onClose()
    } catch (err) {
      pushToast({ tone: 'error', title: 'Could not save schedule', description: getErrorMessage(err, 'Please try again.') })
    } finally {
      setSaving(false)
    }
  }

  async function remove() {
    setSaving(true)
    try {
      await scheduleApi.remove(projectId, websiteId)
      await queryClient.invalidateQueries({ queryKey: key })
      pushToast({ tone: 'info', title: 'Schedule removed' })
      onClose()
    } catch (err) {
      pushToast({ tone: 'error', title: 'Could not remove schedule', description: getErrorMessage(err, 'Please try again.') })
    } finally {
      setSaving(false)
    }
  }

  if (schedule.planSchedule === 'NONE') {
    return (
      <Modal open onClose={onClose} title="Scheduled audits" size="sm">
        <p className="text-sm leading-relaxed text-muted">
          Automatic audits are available on Pro (weekly) and Agency (daily). Each run is compared with the previous one and
          can trigger alerts when the score drops or new errors appear.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>
            Not now
          </Button>
          <Link to="/pricing">
            <Button>View plans</Button>
          </Link>
        </div>
      </Modal>
    )
  }

  const field =
    'h-9 w-full rounded-[var(--sp-field-radius,0.5rem)] border border-default bg-[var(--sp-field-bg)] px-3 text-sm text-main'

  return (
    <Modal
      open
      onClose={onClose}
      title="Scheduled audits"
      description="SEOPulse audits this website automatically and alerts you about regressions."
    >
      <form onSubmit={(event) => void save(event)} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="text-[13px] text-muted">
            <span className="mb-1 block font-medium">Frequency</span>
            <select
              className={field}
              value={frequency}
              onChange={(event) => setFrequency(event.target.value as ScheduleFrequency)}
            >
              <option value="WEEKLY">Weekly</option>
              <option value="DAILY" disabled={!allowsDaily}>
                Daily{allowsDaily ? '' : ' (Agency)'}
              </option>
            </select>
          </label>
          {frequency === 'WEEKLY' && (
            <label className="text-[13px] text-muted">
              <span className="mb-1 block font-medium">Day</span>
              <select className={field} value={dayOfWeek} onChange={(event) => setDayOfWeek(Number(event.target.value))}>
                {DAYS.map((day, index) => (
                  <option key={day} value={index + 1}>
                    {day}
                  </option>
                ))}
              </select>
            </label>
          )}
          <label className="text-[13px] text-muted">
            <span className="mb-1 block font-medium">Time</span>
            <select className={field} value={hourOfDay} onChange={(event) => setHourOfDay(Number(event.target.value))}>
              {Array.from({ length: 24 }, (_, hour) => (
                <option key={hour} value={hour}>
                  {hourLabel(hour)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-[13px] text-muted">
            <span className="mb-1 block font-medium">Timezone</span>
            <select className={field} value={timezone} onChange={(event) => setTimezone(event.target.value)}>
              {zones.map((zone) => (
                <option key={zone} value={zone}>
                  {zone}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="flex items-center gap-2 text-sm text-main">
          <input type="checkbox" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} />
          Enabled
        </label>

        <p className="text-xs text-dim">
          Runs start within 30 minutes of the chosen time, and are skipped if an audit of this site is already running.
          {schedule.lastRunAt ? ` Last run ${formatDateTime(schedule.lastRunAt)}.` : ''}
        </p>
        {schedule.lastError && (
          <p className="rounded-xl bg-warning-surface px-3 py-2 text-xs text-warning">Last attempt: {schedule.lastError}</p>
        )}

        <div className="flex items-center justify-between gap-2 pt-1">
          {schedule.configured ? (
            <Button type="button" variant="danger" size="sm" disabled={saving} onClick={() => void remove()}>
              Remove schedule
            </Button>
          ) : (
            <span />
          )}
          <div className="flex gap-2">
            <Button type="button" variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={saving}>
              Save schedule
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  )
}
