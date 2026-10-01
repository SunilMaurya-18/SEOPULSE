import type { CategoryScores as Scores } from '@/api/audits'
import { cn } from '@/lib/cn'
import { scoreTone } from '@/lib/format'
import { categoryLabel, sortedCategories } from './categories'

function barTone(score: number) {
  if (score >= 80) return 'bg-success'
  if (score >= 60) return 'bg-warning'
  return 'bg-critical'
}

export function CategoryScores({ scores }: { scores: Scores | null | undefined }) {
  const entries = sortedCategories(scores)
  if (entries.length === 0) return null

  return (
    <section className="widget p-5 sm:p-6">
      <h2 className="text-headline text-main">Score by category</h2>
      <p className="mt-0.5 text-xs text-dim">Each category is scored on its own; the overall score weights them together.</p>
      <ul className="mt-5 grid gap-x-8 gap-y-4 sm:grid-cols-2">
        {entries.map(([category, score]) => (
          <li key={category}>
            <div className="flex items-center justify-between text-[13px]">
              <span className="font-medium text-main">{categoryLabel(category)}</span>
              <span className={cn('font-semibold font-tabular', scoreTone(score))}>{score}</span>
            </div>
            <div
              className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-surface-elevated"
              role="meter"
              aria-label={`${categoryLabel(category)} score`}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={score}
            >
              <div className={cn('h-full rounded-full', barTone(score))} style={{ width: `${Math.max(score, 2)}%` }} />
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
