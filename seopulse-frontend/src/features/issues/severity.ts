import type { TabItem } from '@/components/ui/Tabs'

export type SeverityFilter = 'ALL' | 'ERROR' | 'WARNING' | 'INFO'

export function severityTabs(
  counts: { total: number; errors: number; warnings: number; info: number } | null,
): TabItem[] {
  return [
    { id: 'ALL', label: 'All', count: counts?.total },
    { id: 'ERROR', label: 'Errors', count: counts?.errors },
    { id: 'WARNING', label: 'Warnings', count: counts?.warnings },
    { id: 'INFO', label: 'Notices', count: counts?.info },
  ]
}
