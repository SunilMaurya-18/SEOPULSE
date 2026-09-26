import { useState } from 'react'
import { Download, FileCode2, FileText } from 'lucide-react'

import { Button } from '@/components/ui/Button'
import { downloadAuditReport, type ReportFormat } from '@/lib/auditReport'
import { useToast } from '@/lib/toast'
import { cn } from '@/lib/cn'

interface DownloadReportButtonProps {
  projectId: number
  auditId: number
  disabled?: boolean
  size?: 'sm' | 'md'
  className?: string
}

export function DownloadReportButton({
  projectId,
  auditId,
  disabled = false,
  size = 'sm',
  className,
}: DownloadReportButtonProps) {
  const { pushToast } = useToast()
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState<ReportFormat | null>(null)

  async function handleDownload(format: ReportFormat) {
    try {
      setLoading(format)
      setOpen(false)
      await downloadAuditReport(projectId, auditId, format)
      pushToast({
        tone: 'success',
        title: 'Report downloaded',
        description:
          format === 'html'
            ? 'Open the HTML file in a browser to print or save as PDF.'
            : 'JSON export is ready for tooling and archives.',
      })
    } catch (err) {
      console.error(err)
      pushToast({
        tone: 'error',
        title: 'Download failed',
        description: 'Unable to build the audit report. Please try again.',
      })
    } finally {
      setLoading(null)
    }
  }

  return (
    <div className={cn('relative inline-flex', className)}>
      <Button
        variant="secondary"
        size={size}
        disabled={disabled || loading !== null}
        loading={loading !== null}
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
      >
        <Download className="h-3.5 w-3.5" />
        Download report
      </Button>

      {open && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 cursor-default"
            aria-label="Close download menu"
            onClick={() => setOpen(false)}
          />
          <div
            role="menu"
            className="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-xl border border-default bg-surface shadow-overlay"
          >
            <button
              type="button"
              role="menuitem"
              className="flex w-full items-start gap-2.5 px-3 py-2.5 text-left transition-colors hover:bg-surface-low"
              onClick={() => void handleDownload('html')}
            >
              <FileText className="mt-0.5 h-4 w-4 text-accent" />
              <span>
                <span className="block text-sm font-medium text-main">
                  HTML report
                </span>
                <span className="block text-xs text-muted">
                  Printable summary + issues
                </span>
              </span>
            </button>
            <button
              type="button"
              role="menuitem"
              className="flex w-full items-start gap-2.5 border-t border-default px-3 py-2.5 text-left transition-colors hover:bg-surface-low"
              onClick={() => void handleDownload('json')}
            >
              <FileCode2 className="mt-0.5 h-4 w-4 text-info" />
              <span>
                <span className="block text-sm font-medium text-main">
                  JSON export
                </span>
                <span className="block text-xs text-muted">
                  Raw audit payload
                </span>
              </span>
            </button>
          </div>
        </>
      )}
    </div>
  )
}
