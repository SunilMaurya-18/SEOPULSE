export const CATEGORY_ORDER = ['CONTENT', 'TECHNICAL', 'LINKS', 'SOCIAL', 'PERFORMANCE', 'SECURITY'] as const

export function categoryLabel(category: string) {
  const lower = category.toLowerCase()
  return lower.charAt(0).toUpperCase() + lower.slice(1)
}

/** Known categories first in a stable order, then anything new the backend adds. */
export function sortedCategories(scores: Partial<Record<string, number>> | null | undefined) {
  if (!scores) return []
  const entries = Object.entries(scores).filter((entry): entry is [string, number] => typeof entry[1] === 'number')
  const rank = (key: string) => {
    const index = (CATEGORY_ORDER as readonly string[]).indexOf(key)
    return index === -1 ? CATEGORY_ORDER.length : index
  }
  return entries.sort((a, b) => rank(a[0]) - rank(b[0]) || a[0].localeCompare(b[0]))
}
