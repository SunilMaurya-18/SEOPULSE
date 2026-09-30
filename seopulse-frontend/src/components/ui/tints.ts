export const TINTS = {
  coral: 'from-[#ff6b5f] to-[#f5504a]',
  red: 'from-[#ff6961] to-[#ff3b30]',
  pink: 'from-[#ff6b8b] to-[#ff2d55]',
  orange: 'from-[#ffb340] to-[#ff9500]',
  yellow: 'from-[#ffd60a] to-[#ffb800]',
  green: 'from-[#4ee37a] to-[#28b14c]',
  teal: 'from-[#5ef0ff] to-[#00a7d6]',
  blue: 'from-[#4aa8ff] to-[#0a84ff]',
  indigo: 'from-[#7d7aff] to-[#5856d6]',
  purple: 'from-[#c58cff] to-[#9f5cf0]',
  gray: 'from-[#a1a1a6] to-[#6e6e73]',
} as const

export type Tint = keyof typeof TINTS

const SITE_ORDER: Tint[] = ['blue', 'purple', 'green', 'orange', 'pink', 'teal', 'indigo']

export function siteTint(seed: number): Tint {
  return SITE_ORDER[Math.abs(seed) % SITE_ORDER.length]
}
